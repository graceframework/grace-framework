/*
 * Copyright 2004-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.gsp.compiler;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import grails.config.ConfigMap;
import grails.io.IOUtils;
import grails.util.Environment;
import grails.util.GrailsStringUtils;

import org.grails.buffer.FastStringWriter;
import org.grails.buffer.StreamByteBuffer;
import org.grails.buffer.StreamCharBuffer;
import org.grails.gsp.CompileStaticGroovyPage;
import org.grails.gsp.GroovyPage;
import org.grails.gsp.ModelRecordingGroovyPage;
import org.grails.gsp.compiler.tags.GrailsTagRegistry;
import org.grails.gsp.compiler.tags.GroovySyntaxTag;
import org.grails.io.support.SpringIOUtils;
import org.grails.taglib.GrailsTagException;
import org.grails.taglib.encoder.OutputEncodingSettings;

/**
 * NOTE: Based on work done by the GSP standalone project (https://gsp.dev.java.net/).
 *
 * Parsing implementation for GSP files
 *
 * @author Troy Heninger
 * @author Graeme Rocher
 * @author Lari Hotari
 */
public class GroovyPageParser implements Tokens {

    public static final Log logger = LogFactory.getLog(GroovyPageParser.class);

    private static final Pattern PARA_BREAK = Pattern.compile(
            "/p>\\s*<p[^>]*>", Pattern.CASE_INSENSITIVE);

    private static final Pattern ROW_BREAK = Pattern.compile(
            "((/td>\\s*</tr>\\s*<)?tr[^>]*>\\s*<)?td[^>]*>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PAGE_DIRECTIVE_PATTERN = Pattern.compile(
            "(\\w+)\\s*=\\s*(\"\"\"|\"(?!\"\")|'''|'(?!''))(.*?)\\2", Pattern.DOTALL);

    private static final String TAGLIB_DIRECTIVE = "taglib";

    private static final Pattern PRESCAN_PAGE_DIRECTIVE_PATTERN = Pattern.compile("<%@\\s*(?!" + TAGLIB_DIRECTIVE + " )(.*?)\\s*%>", Pattern.DOTALL);

    private static final Pattern PRESCAN_COMMENT_PATTERN = Pattern.compile("<%--.*?%>", Pattern.DOTALL);

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    private static final Pattern NON_WHITESPACE_PATTERN = Pattern.compile("\\S");

    private static final Pattern IMPORT_SEMICOLON_PATTERN = Pattern.compile(";");

    public static final String CONSTANT_NAME_JSP_TAGS = "JSP_TAGS";

    public static final String CONSTANT_NAME_CONTENT_TYPE = "CONTENT_TYPE";

    public static final String CONSTANT_NAME_LAST_MODIFIED = "LAST_MODIFIED";

    public static final String CONSTANT_NAME_EXPRESSION_CODEC = "EXPRESSION_CODEC";

    public static final String CONSTANT_NAME_STATIC_CODEC = "STATIC_CODEC";

    public static final String CONSTANT_NAME_OUT_CODEC = "OUT_CODEC";

    public static final String CONSTANT_NAME_TAGLIB_CODEC = "TAGLIB_CODEC";

    public static final String CONSTANT_NAME_COMPILE_STATIC_MODE = "COMPILE_STATIC_MODE";

    public static final String CONSTANT_NAME_MODEL_FIELDS_MODE = "MODEL_FIELDS_MODE";

    public static final String DEFAULT_ENCODING = "UTF-8";

    private static final String MULTILINE_GROOVY_STRING_DOUBLEQUOTES = "\"\"\"";

    private static final String MULTILINE_GROOVY_STRING_SINGLEQUOTES = "'''";

    public static final String MODEL_DIRECTIVE = "model";

    public static final String COMPILE_STATIC_DIRECTIVE = "compileStatic";

    public static final String TAGLIBS_DIRECTIVE = "taglibs";

    public static final List<String> DEFAULT_TAGLIB_NAMESPACES = Collections.unmodifiableList(Arrays.asList("g", "tmpl", "f", "asset", "plugin"));

    private GroovyPageScanner scan;

    private GSPWriter out;

    private String className;

    private String packageName;

    private String sourceName; // last segment of the file name (eg- index.gsp)

    private boolean finalPass = false;

    private int tagIndex;

    private Map<Object, Object> tagContext;

    private Stack<TagMeta> tagMetaStack = new Stack<>();

    private GrailsTagRegistry tagRegistry = GrailsTagRegistry.getInstance();

    private Environment environment;

    private List<String> htmlParts = new ArrayList<>();

    private static SitemeshPreprocessor sitemeshPreprocessor = new SitemeshPreprocessor();

    Set<Integer> bodyVarsDefined = new HashSet<>();

    Map<Integer, String> attrsVarsMapDefinition = new HashMap<>();

    int closureLevel = 0;

    /*
     * Set to true when whitespace is currently being saved for later output if
     * the next tag isn't set to swallow it
     */
    private boolean currentlyBufferingWhitespace;

    /*
     * Set to true if the last output was not whitespace, so that we can detect
     * when a tag has illegal content before it
     */
    private boolean previousContentWasNonWhitespace;

    private StringBuffer whitespaceBuffer = new StringBuffer();

    private String contentType = DEFAULT_CONTENT_TYPE;

    private boolean doNextScan = true;

    private int state;

    private static final String DEFAULT_CONTENT_TYPE = "text/html;charset=UTF-8";

    private int constantCount = 0;

    private Map<String, Integer> constantsToNumbers = new HashMap<>();

    private final String pageName;

    public static final String[] DEFAULT_IMPORTS = {
            "grails.plugins.metadata.GrailsPlugin",
            "org.grails.gsp.compiler.transform.LineNumber",
            "org.grails.gsp.GroovyPage",
            "org.grails.web.taglib.*",
            "org.grails.taglib.GrailsTagException",
            "org.springframework.web.util.*",
            "grails.util.GrailsUtil"
    };

    public static final String CONFIG_PROPERTY_DEFAULT_CODEC = "grails.views.default.codec";

    public static final String CONFIG_PROPERTY_GSP_ENCODING = "grails.views.gsp.encoding";

    public static final String CONFIG_PROPERTY_GSP_KEEPGENERATED_DIR = "grails.views.gsp.keepgenerateddir";

    public static final String CONFIG_PROPERTY_GSP_SITEMESH_PREPROCESS = "grails.views.gsp.sitemesh.preprocess";

    public static final String CONFIG_PROPERTY_GSP_COMPILESTATIC = "grails.views.gsp.compileStatic";

    public static final String CONFIG_PROPERTY_GSP_ALLOWED_TAGLIB_NAMESPACES = "grails.views.gsp.compileStaticConfig.taglibs";

    public static final String CONFIG_PROPERTY_GSP_CODECS = "grails.views.gsp.codecs";

    private static final String IMPORT_DIRECTIVE = "import";

    private static final String CONTENT_TYPE_DIRECTIVE = "contentType";

    public static final String CODEC_DIRECTIVE_POSTFIX = "Codec";

    private static final String EXPRESSION_CODEC_DIRECTIVE = OutputEncodingSettings.EXPRESSION_CODEC_NAME + CODEC_DIRECTIVE_POSTFIX;

    private static final String EXPRESSION_CODEC_DIRECTIVE_ALIAS = "default" + CODEC_DIRECTIVE_POSTFIX;

    private static final String STATIC_CODEC_DIRECTIVE = OutputEncodingSettings.STATIC_CODEC_NAME + CODEC_DIRECTIVE_POSTFIX;

    private static final String OUT_CODEC_DIRECTIVE = OutputEncodingSettings.OUT_CODEC_NAME + CODEC_DIRECTIVE_POSTFIX;

    private static final String TAGLIB_CODEC_DIRECTIVE = OutputEncodingSettings.TAGLIB_CODEC_NAME + CODEC_DIRECTIVE_POSTFIX;

    private static final String SITEMESH_PREPROCESS_DIRECTIVE = "sitemeshPreprocess";

    private String pluginAnnotation;

    public static final String GROOVY_SOURCE_CHAR_ENCODING = "UTF-8";

    private Map<String, String> jspTags = new HashMap<>();

    private long lastModified;

    private boolean precompileMode;

    private Boolean compileStaticMode;

    private boolean modelFieldsMode;

    private boolean sitemeshPreprocessMode = false;

    private String expressionCodecDirectiveValue = OutputEncodingSettings.getDefaultValue(OutputEncodingSettings.EXPRESSION_CODEC_NAME);

    private String outCodecDirectiveValue = OutputEncodingSettings.getDefaultValue(OutputEncodingSettings.OUT_CODEC_NAME);

    private String staticCodecDirectiveValue = OutputEncodingSettings.getDefaultValue(OutputEncodingSettings.STATIC_CODEC_NAME);

    private String taglibCodecDirectiveValue = OutputEncodingSettings.getDefaultValue(OutputEncodingSettings.TAGLIB_CODEC_NAME);

    private String modelDirectiveValue;

    private boolean enableSitemeshPreprocessing = true;

    private File keepGeneratedDirectory;

    private Set<String> allowedTaglibNamespaces = new LinkedHashSet<>(DEFAULT_TAGLIB_NAMESPACES);

    public GroovyPageParser(String name, String uri, String filename, InputStream in, String encoding, String expressionCodecName)
            throws IOException {
        this(name, uri, filename, readStream(in, encoding), expressionCodecName);
    }

    public GroovyPageParser(String name, String uri, String filename, String gspSource) throws IOException {
        this(name, uri, filename, gspSource, null);
    }

    public GroovyPageParser(String name, String uri, String filename, String gspSource, String expressionCodecName) throws IOException {
        this.expressionCodecDirectiveValue = expressionCodecName;
        if (this.expressionCodecDirectiveValue == null) {
            this.expressionCodecDirectiveValue = OutputEncodingSettings.getDefaultValue(OutputEncodingSettings.EXPRESSION_CODEC_NAME);
        }

        Map<String, String> directives = parseDirectives(gspSource);

        if (isSitemeshPreprocessingEnabled(directives.get(SITEMESH_PREPROCESS_DIRECTIVE))) {
            // GSP preprocessing for direct sitemesh integration: replace head -> g:captureHead,
            // title -> g:captureTitle, meta -> g:captureMeta, body -> g:captureBody
            gspSource = sitemeshPreprocessor.addGspSitemeshCapturing(gspSource);
            this.sitemeshPreprocessMode = true;
        }
        this.scan = new GroovyPageScanner(gspSource, uri);
        this.pageName = uri;
        this.environment = Environment.getCurrent();
        makeName(name);
        makeSourceName(filename);
    }

    public GroovyPageParser(String name, String uri, String filename, InputStream in) throws IOException {
        this(name, uri, filename, in, null, null);
    }

    public String getContentType() {
        return this.contentType;
    }

    public int getCurrentOutputLineNumber() {
        return this.scan.getLineNumberForToken();
    }

    public Map<String, String> getJspTags() {
        return this.jspTags;
    }

    public void setKeepGeneratedDirectory(File keepGeneratedDirectory) {
        this.keepGeneratedDirectory = keepGeneratedDirectory;
    }

    public void setEnableSitemeshPreprocessing(boolean enableSitemeshPreprocessing) {
        this.enableSitemeshPreprocessing = enableSitemeshPreprocessing;
    }

    /**
     * Configures the parser for the given Config map
     *
     * @param config The config map
     */
    public void configure(ConfigMap config) {
        this.compileStaticMode = config.getProperty(GroovyPageParser.CONFIG_PROPERTY_GSP_COMPILESTATIC, Boolean.class);

        Object allowedTagLibsConfigValue = config.getProperty(CONFIG_PROPERTY_GSP_ALLOWED_TAGLIB_NAMESPACES, Object.class);
        if (allowedTagLibsConfigValue instanceof Iterable) {
            for (Object val : ((Iterable) allowedTagLibsConfigValue)) {
                this.allowedTaglibNamespaces.add(String.valueOf(val).trim());
            }
        }
        else if (allowedTagLibsConfigValue instanceof CharSequence) {
            this.allowedTaglibNamespaces.addAll(Arrays.asList(allowedTagLibsConfigValue.toString().split("\\s*,\\s*")));
        }

        setEnableSitemeshPreprocessing(
                config.getProperty(GroovyPageParser.CONFIG_PROPERTY_GSP_SITEMESH_PREPROCESS, Boolean.class, true)
        );

        setExpressionCodecDirectiveValue(
                config.getProperty(OutputEncodingSettings.CONFIG_PROPERTY_GSP_CODECS + '.' + OutputEncodingSettings.EXPRESSION_CODEC_NAME,
                        String.class,
                        config.getProperty(OutputEncodingSettings.CONFIG_PROPERTY_DEFAULT_CODEC, String.class,
                                OutputEncodingSettings.getDefaultValue(OutputEncodingSettings.EXPRESSION_CODEC_NAME)))
        );

        setStaticCodecDirectiveValue(
                config.getProperty(OutputEncodingSettings.CONFIG_PROPERTY_GSP_CODECS + '.' + OutputEncodingSettings.STATIC_CODEC_NAME,
                        String.class, OutputEncodingSettings.getDefaultValue(OutputEncodingSettings.STATIC_CODEC_NAME))
        );

        setTaglibCodecDirectiveValue(
                config.getProperty(OutputEncodingSettings.CONFIG_PROPERTY_GSP_CODECS + '.' + OutputEncodingSettings.TAGLIB_CODEC_NAME,
                        String.class, OutputEncodingSettings.getDefaultValue(OutputEncodingSettings.TAGLIB_CODEC_NAME))
        );

        setOutCodecDirectiveValue(
                config.getProperty(OutputEncodingSettings.CONFIG_PROPERTY_GSP_CODECS + '.' + OutputEncodingSettings.OUT_CODEC_NAME,
                        String.class, OutputEncodingSettings.getDefaultValue(OutputEncodingSettings.OUT_CODEC_NAME))
        );

        Object keepDirObj = config.getProperty(GroovyPageParser.CONFIG_PROPERTY_GSP_KEEPGENERATED_DIR, Object.class);
        if (keepDirObj instanceof File) {
            setKeepGeneratedDirectory((File) keepDirObj);
        }
        else if (keepDirObj != null) {
            setKeepGeneratedDirectory(new File(String.valueOf(keepDirObj)));
        }

    }

    private Map<String, String> parseDirectives(String gspSource) {
        Map<String, String> result = new HashMap<>();
        // strip gsp comments
        String input = PRESCAN_COMMENT_PATTERN.matcher(gspSource).replaceAll("");
        // find page directives
        Matcher m = PRESCAN_PAGE_DIRECTIVE_PATTERN.matcher(input);
        if (m.find()) {
            Matcher mat = PAGE_DIRECTIVE_PATTERN.matcher(m.group(1));
            while (mat.find()) {
                String name = mat.group(1);
                String value = mat.group(3);
                result.put(name, value);
            }
        }
        return result;
    }

    private boolean isSitemeshPreprocessingEnabled(String gspFilePreprocessDirective) {
        if (gspFilePreprocessDirective != null) {
            return GrailsStringUtils.toBoolean(gspFilePreprocessDirective.trim());
        }
        return this.enableSitemeshPreprocessing;
    }

    public int[] getLineNumberMatrix() {
        return this.out.getLineNumbers();
    }

    public String getClassName() {
        return this.className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public InputStream parse() {
        resolveKeepGeneratedDirectory();

        StreamCharBuffer streamBuffer = new StreamCharBuffer(1024);
        StreamByteBuffer byteOutputBuffer = new StreamByteBuffer(1024,
                StreamByteBuffer.ReadMode.RETAIN_AFTER_READING);

        try {
            streamBuffer.connectTo(new OutputStreamWriter(byteOutputBuffer.getOutputStream(),
                    GROOVY_SOURCE_CHAR_ENCODING), true);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Grails cannot run unless your environment supports UTF-8!");
        }

        File keepGeneratedFile = null;
        Writer keepGeneratedWriter = null;
        if (this.keepGeneratedDirectory != null) {
            keepGeneratedFile = new File(this.keepGeneratedDirectory, this.className);
            try {
                keepGeneratedWriter = new OutputStreamWriter(
                        new FileOutputStream(keepGeneratedFile),
                        GROOVY_SOURCE_CHAR_ENCODING);
            }
            catch (IOException e) {
                logger.warn("Cannot open keepgenerated file for writing. File's absolute path is '" +
                        keepGeneratedFile.getAbsolutePath() + "'");
                keepGeneratedFile = null;
            }
            streamBuffer.connectTo(keepGeneratedWriter, true);
        }

        Writer target = streamBuffer.getWriter();
        try {
            generateGsp(target, false);
            return byteOutputBuffer.getInputStream();
        }
        finally {
            SpringIOUtils.closeQuietly(keepGeneratedWriter);
        }
    }

    private void resolveKeepGeneratedDirectory() {
        if (this.keepGeneratedDirectory != null && !this.keepGeneratedDirectory.isDirectory()) {
            logger.warn("The directory specified with " + CONFIG_PROPERTY_GSP_KEEPGENERATED_DIR +
                    " config parameter doesn't exist or isn't a readable directory. Absolute path: '" +
                    this.keepGeneratedDirectory.getAbsolutePath() + "' Keepgenerated will be disabled.");
            this.keepGeneratedDirectory = null;
        }
    }

    public void generateGsp(Writer target) {
        generateGsp(target, true);
    }

    public void generateGsp(Writer target, boolean precompileMode) {
        this.precompileMode = precompileMode;

        this.out = new GSPWriter(target, this);
        if (this.packageName != null && this.packageName.length() > 0) {
            this.out.println("package " + this.packageName);
            this.out.println();
        }
        page();
        this.finalPass = true;
        this.scan.reset();
        this.previousContentWasNonWhitespace = false;
        this.currentlyBufferingWhitespace = false;
        page();

        this.out.close();
        this.scan = null;
    }

    public void writeHtmlParts(File filename) throws IOException {
        DataOutputStream dataOut = null;
        try {
            dataOut = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(filename)));
            dataOut.writeInt(this.htmlParts.size());
            for (String part : this.htmlParts) {
                dataOut.writeUTF(part);
            }
        }
        finally {
            SpringIOUtils.closeQuietly(dataOut);
        }
    }

    public void writeLineNumbers(File filename) throws IOException {
        DataOutputStream dataOut = null;
        try {
            dataOut = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(filename)));
            int lineNumbersCount = this.out.getCurrentLineNumber() - 1;
            int[] lineNumbers = this.out.getLineNumbers();
            dataOut.writeInt(lineNumbersCount);
            for (int i = 0; i < lineNumbersCount; i++) {
                dataOut.writeInt(lineNumbers[i]);
            }
        }
        finally {
            SpringIOUtils.closeQuietly(dataOut);
        }
    }

    private void declare(boolean gsp) {
        if (this.finalPass) {
            return;
        }

        this.out.println();
        write(this.scan.getToken().trim(), gsp);
        this.out.println();
        this.out.println();
    }

    private void direct() {
        if (this.finalPass) {
            return;
        }

        String text = this.scan.getToken();
        text = text.trim();
        if (text.startsWith(TAGLIB_DIRECTIVE + " ")) {
            directJspTagLib(text);
        }
        else {
            directPage(text);
        }
    }

    private void directPage(String text) {
        text = text.trim();
        Matcher mat = PAGE_DIRECTIVE_PATTERN.matcher(text);
        Boolean compileStaticModeSetting = this.compileStaticMode;
        while (mat.find()) {
            String name = mat.group(1);
            String value = mat.group(3);
            if (name.equals(IMPORT_DIRECTIVE)) {
                pageImport(value);
            }
            if (name.equalsIgnoreCase(CONTENT_TYPE_DIRECTIVE)) {
                contentType(value);
            }
            if (name.equalsIgnoreCase(EXPRESSION_CODEC_DIRECTIVE)) {
                this.expressionCodecDirectiveValue = value.trim();
            }
            if (name.equalsIgnoreCase(EXPRESSION_CODEC_DIRECTIVE_ALIAS)) {
                this.expressionCodecDirectiveValue = value.trim();
            }
            if (name.equalsIgnoreCase(STATIC_CODEC_DIRECTIVE)) {
                this.staticCodecDirectiveValue = value.trim();
            }
            if (name.equalsIgnoreCase(OUT_CODEC_DIRECTIVE)) {
                this.outCodecDirectiveValue = value.trim();
            }
            if (name.equalsIgnoreCase(TAGLIB_CODEC_DIRECTIVE)) {
                this.taglibCodecDirectiveValue = value.trim();
            }
            if (name.equalsIgnoreCase(MODEL_DIRECTIVE)) {
                if (this.modelDirectiveValue != null) {
                    this.modelDirectiveValue += "\n" + value.trim();
                }
                else {
                    this.modelDirectiveValue = value.trim();
                }
                this.modelFieldsMode = true;
                if (compileStaticModeSetting == null) {
                    compileStaticModeSetting = true;
                }
            }
            if (name.equalsIgnoreCase(COMPILE_STATIC_DIRECTIVE)) {
                compileStaticModeSetting = GrailsStringUtils.toBoolean(value.trim());
            }
            if (name.equalsIgnoreCase(TAGLIBS_DIRECTIVE)) {
                this.allowedTaglibNamespaces.addAll(Arrays.asList(value.trim().split("\\s*,\\s*")));
            }
        }
        this.compileStaticMode = compileStaticModeSetting != null ? compileStaticModeSetting : false;
    }

    private void directJspTagLib(String text) {

        text = text.substring(TAGLIB_DIRECTIVE.length() + 1, text.length());
        Map<String, String> attrs = new LinkedHashMap<>();
        populateMapWithAttributes(attrs, text);

        String prefix = attrs.get("\"prefix\"");
        String uri = attrs.get("\"uri\"");

        if (uri != null && prefix != null) {
            final String namespace = prefix.substring(1, prefix.length() - 1);
            if (!GroovyPage.DEFAULT_NAMESPACE.equals(namespace)) {
                this.jspTags.put(namespace, uri.substring(1, uri.length() - 1));
            }
            else {
                logger.error("You cannot override the default 'g' namespace with the directive <%@ taglib prefix=\"g\" %>. "
                        + "Please select another namespace.");
            }
        }
    }

    private void contentType(String value) {
        this.contentType = value;
    }

    private void scriptletExpr() {
        if (!this.finalPass) {
            return;
        }

        String text = this.scan.getToken().trim();
        this.out.printlnToResponse(text);
    }

    private void expr() {
        if (!this.finalPass) {
            return;
        }

        String text = this.scan.getToken().trim();
        text = getExpressionText(text);
        if (text != null && text.length() > 2 && text.startsWith("(") && text.endsWith(")")) {
            this.out.printlnToResponse(GroovyPage.EXPRESSION_OUT_STATEMENT, text.substring(1, text.length() - 1));
        }
        else {
            this.out.printlnToResponse(GroovyPage.EXPRESSION_OUT_STATEMENT, text);
        }
    }

    /**
     * Returns an expression text for the given expression
     *
     * @param text
     *            The text
     * @return An expression text
     */
    public String getExpressionText(String text) {
        return getExpressionText(text, true);
    }

    public String getExpressionText(String text, boolean _safeDereference) {
        boolean safeDereference = false;
        if (text.endsWith("?")) {
            text = text.substring(0, text.length() - 1);
            safeDereference = _safeDereference;
        }
        if (!this.precompileMode && !isCompileStaticMode() &&
                (this.environment == Environment.DEVELOPMENT || this.environment == Environment.TEST)) {
            String escaped = escapeGroovy(text);
            text = "evaluate('" + escaped + "', " +
                    getCurrentOutputLineNumber() + ", it) { return " + text +
                    " }" + (safeDereference ? "?" : "");
        }
        else {
            // add extra parenthesis, see http://jira.codehaus.org/browse/GRAILS-4351
            // or GroovyPagesTemplateEngineTests.testForEachInProductionMode

            text = "(" + text + ")" + (safeDereference ? "?" : "");
        }
        return text;
    }

    private String escapeGroovy(String text) {
        return text.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * Write to the outputstream ONLY if the string is not blank, else we hold
     * it back in case it is to be swallowed between tags
     */
    @SuppressWarnings("unused")
    private void bufferedPrintlnToResponse(String s) {
        if (this.currentlyBufferingWhitespace) {
            this.whitespaceBuffer.append(s);
        }
        else {
            flushTagBuffering();
            this.out.printlnToResponse(s);
        }
    }

    private void htmlPartPrintlnToResponse(int partNumber) {
        if (!this.tagMetaStack.isEmpty()) {
            TagMeta tm = this.tagMetaStack.peek();
            if (tm.bufferMode && tm.bufferPartNumber == -1) {
                tm.bufferPartNumber = partNumber;
                return;
            }
        }

        flushTagBuffering();

        htmlPartPrintlnRaw(partNumber);
    }

    private void htmlPartPrintlnRaw(int partNumber) {
        this.out.print("printHtmlPart(");
        this.out.print(String.valueOf(partNumber));
        this.out.print(")");
        this.out.println();
    }

    public void flushTagBuffering() {
        if (!this.tagMetaStack.isEmpty()) {
            TagMeta tm = this.tagMetaStack.peek();
            if (tm.bufferMode) {
                writeTagBodyStart(tm);
                if (tm.bufferPartNumber != -1) {
                    htmlPartPrintlnRaw(tm.bufferPartNumber);
                }
                tm.bufferMode = false;
            }
        }
    }

    private void html() {
        if (!this.finalPass) {
            return;
        }

        String text = this.scan.getToken();
        if (text.length() == 0) {
            return;
        }

        // If we detect it is all whitespace, we need to keep it for later
        // If it is not whitespace, we need to flush any whitespace we do have
        boolean contentIsWhitespace = !NON_WHITESPACE_PATTERN.matcher(text).find();
        if (!contentIsWhitespace && this.currentlyBufferingWhitespace) {
            flushBufferedWhiteSpace();
        }
        else {
            this.currentlyBufferingWhitespace = contentIsWhitespace;
        }
        // We need to know if the last content output was not whitespace, for tag safety checks
        this.previousContentWasNonWhitespace = !contentIsWhitespace;

        if (this.currentlyBufferingWhitespace) {
            this.whitespaceBuffer.append(text);
        }
        else {
            appendHtmlPart(text);
        }
    }

    private void appendHtmlPart(String text) {
        // flush previous white space if any
        if (this.whitespaceBuffer.length() > 0) {
            if (text != null) {
                this.whitespaceBuffer.append(text);
            }
            text = this.whitespaceBuffer.toString();
            clearBufferedWhiteSpace();
        }

        // de-dupe constants
        Integer constantNumber = this.constantsToNumbers.get(text);
        if (constantNumber == null) {
            constantNumber = this.constantCount++;
            this.constantsToNumbers.put(text, constantNumber);
            this.htmlParts.add(text);
        }
        htmlPartPrintlnToResponse(constantNumber);
    }

    private void makeName(String uri) {
        String name;
        int slash = uri.lastIndexOf('/');
        if (slash > -1) {
            name = uri.substring(slash + 1);
            uri = uri.substring(0, (uri.length() - 1) - name.length());
            while (uri.endsWith("/")) {
                uri = uri.substring(0, uri.length() - 1);
            }
            slash = uri.lastIndexOf('/');
            if (slash > -1) {
                name = uri.substring(slash + 1) + '_' + name;
            }
        }
        else {
            name = uri;
        }
        StringBuilder buf = new StringBuilder(name.length());
        for (int ix = 0, ixz = name.length(); ix < ixz; ix++) {
            char c = name.charAt(ix);
            if (c < '0' || (c > '9' && c < '@') || (c > 'Z' && c < '_') ||
                    (c > '_' && c < 'a') || c > 'z') {
                c = '_';
            }
            else if (ix == 0 && c >= '0' && c <= '9') {
                c = '_';
            }
            buf.append(c);
        }
        this.className = buf.toString();
    }

    /**
     * find the simple name of this gsp
     * @param filename the fully qualified file name
     */
    private void makeSourceName(String filename) {
        if (filename != null) {
            int lastSegmentStart = filename.lastIndexOf('/');
            if (lastSegmentStart == -1) {
                lastSegmentStart = filename.lastIndexOf('\\');
            }
            this.sourceName = filename.substring(lastSegmentStart + 1);
        }
        else {
            this.sourceName = this.className;
        }
    }

    private static boolean match(CharSequence pat, CharSequence text, int start) {
        int ix = start;
        int ixz = text.length();
        int ixy = start + pat.length();

        if (ixz > ixy) {
            ixz = ixy;
        }
        if (pat.length() > ixz - start) {
            return false;
        }

        for (; ix < ixz; ix++) {
            if (Character.toLowerCase(text.charAt(ix)) != Character.toLowerCase(pat.charAt(ix - start))) {
                return false;
            }
        }
        return true;
    }

    private static int match(Pattern pat, CharSequence text, int start) {
        Matcher mat = pat.matcher(text);
        if (mat.find(start) && mat.start() == start) {
            return mat.end();
        }
        return 0;
    }

    private void page() {
        if (this.finalPass) {
            this.out.println();
            if (this.pluginAnnotation != null) {
                this.out.println(this.pluginAnnotation);
            }
            if (isCompileStaticMode()) {
                this.out.println("@groovy.transform.CompileStatic(extensions = ['" + GroovyPageTypeCheckingExtension.class.getName() + "'])");
                if (this.allowedTaglibNamespaces != null && !this.allowedTaglibNamespaces.isEmpty()) {
                    this.out.println("@" + GroovyPageTypeCheckingConfig.class.getName() + "(taglibs = ['"
                            + DefaultGroovyMethods.join((Iterable) this.allowedTaglibNamespaces, "','") + "'])");
                }
            }
            this.out.print("class ");
            this.out.print(this.className);
            this.out.print(" extends ");
            this.out.print(resolveGspSuperClassName());
            this.out.println(" {");
            if (this.modelDirectiveValue != null) {
                this.out.println("// start model fields");
                this.out.println(this.modelDirectiveValue);
                this.out.println("// end model fields");
            }
            this.out.println("public String getGroovyPageFileName() { \"" +
                    this.pageName.replaceAll("\\\\", "/") + "\" }");
            this.out.println("public Object run() {");
            /*
            out.println("def params = binding.params");
            out.println("def request = binding.request");
            out.println("def flash = binding.flash");
            out.println("def response = binding.response");
            */
            this.out.println("Writer " + GroovyPage.OUT + " = getOut()");
            this.out.println("Writer " + GroovyPage.EXPRESSION_OUT + " = getExpressionOut()");
            //out.println("JspTagLib jspTag");
            if (this.sitemeshPreprocessMode) {
                this.out.println("registerSitemeshPreprocessMode()");
            }
        }

        loop:
        for (; ; ) {
            if (this.doNextScan) {
                this.state = this.scan.nextToken();
            }
            else {
                this.doNextScan = true;
            }

            // Flush any buffered whitespace if there's not a possibility of more whitespace
            // or a new tag which will handle flushing as necessary
            if ((this.state != GSTART_TAG) && (this.state != HTML)) {
                flushBufferedWhiteSpace();
                this.previousContentWasNonWhitespace = false; // well, we don't know
            }

            switch (this.state) {
                case EOF:
                    break loop;
                case HTML:
                    html();
                    break;
                case JEXPR:
                    scriptletExpr();
                    break;
                case JSCRIPT:
                    script(false);
                    break;
                case JDIRECT:
                    direct();
                    break;
                case JDECLAR:
                    declare(false);
                    break;
                case GEXPR:
                    expr();
                    break;
                case GSCRIPT:
                    script(true);
                    break;
                case GDIRECT:
                    direct();
                    break;
                case GDECLAR:
                    declare(true);
                    break;
                case GSTART_TAG:
                    startTag();
                    break;
                case GEND_EMPTY_TAG:
                case GEND_TAG:
                    endTag();
                    break;
            }
        }

        if (this.finalPass) {
            if (!this.tagMetaStack.isEmpty()) {
                throw new GrailsTagException("Grails tags were not closed! [" +
                        this.tagMetaStack + "] in GSP " + this.pageName + "", this.pageName,
                        getCurrentOutputLineNumber());
            }

            this.out.println("}");

            this.out.println("public static final Map " + CONSTANT_NAME_JSP_TAGS + " = new HashMap()");
            if (this.jspTags != null && this.jspTags.size() > 0) {
                this.out.println("static {");
                for (Map.Entry<String, String> entry : this.jspTags.entrySet()) {
                    this.out.print("\t" + CONSTANT_NAME_JSP_TAGS + ".put('");
                    this.out.print(escapeGroovy(entry.getKey()));
                    this.out.print("','");
                    this.out.print(escapeGroovy(entry.getValue()));
                    this.out.println("')");
                }
                this.out.println("}");
            }

            this.out.println("protected void init() {");
            this.out.println("\tthis.jspTags = " + CONSTANT_NAME_JSP_TAGS);
            this.out.println("}");

            this.out.println("public static final String " +
                    CONSTANT_NAME_CONTENT_TYPE + " = '" +
                    escapeGroovy(this.contentType) + "'");

            this.out.println("public static final long " +
                    CONSTANT_NAME_LAST_MODIFIED + " = " + this.lastModified + "L");

            this.out.println("public static final String " +
                    CONSTANT_NAME_EXPRESSION_CODEC + " = '" + escapeGroovy(this.expressionCodecDirectiveValue) + "'");
            this.out.println("public static final String " +
                    CONSTANT_NAME_STATIC_CODEC + " = '" + escapeGroovy(this.staticCodecDirectiveValue) + "'");
            this.out.println("public static final String " +
                    CONSTANT_NAME_OUT_CODEC + " = '" + escapeGroovy(this.outCodecDirectiveValue) + "'");
            this.out.println("public static final String " +
                    CONSTANT_NAME_TAGLIB_CODEC + " = '" + escapeGroovy(this.taglibCodecDirectiveValue) + "'");

            if (isCompileStaticMode()) {
                this.out.println("public static final boolean " +
                        CONSTANT_NAME_COMPILE_STATIC_MODE + " = " + isCompileStaticMode());
            }
            if (this.modelFieldsMode) {
                this.out.println("public static final boolean " +
                        CONSTANT_NAME_MODEL_FIELDS_MODE + " = " + this.modelFieldsMode);
            }

            this.out.println("}");

            if (shouldAddLineNumbers()) {
                addLineNumbers();
            }
        }
        else {
            for (int i = 0; i < DEFAULT_IMPORTS.length; i++) {
                this.out.print("import ");
                this.out.println(DEFAULT_IMPORTS[i]);
            }
        }
    }

    private String resolveGspSuperClassName() {
        Class<?> gspSuperClass = isCompileStaticMode()
                ? CompileStaticGroovyPage.class
                : (isModelRecordingModeEnabled() ? ModelRecordingGroovyPage.class : GroovyPage.class);
        return gspSuperClass.getName();
    }

    private boolean isModelRecordingModeEnabled() {
        return ModelRecordingGroovyPage.ENABLED;
    }

    /**
     * Determines if the line numbers array should be added to the generated Groovy class.
     * @return true if they should
     */
    private boolean shouldAddLineNumbers() {
        try {
            // for now, we support this through a system property.
            return Boolean.valueOf(System.getenv("GROOVY_PAGE_ADD_LINE_NUMBERS"));
        }
        catch (Exception e) {
            // something wild happened
            return false;
        }
    }

    /**
     * Adds the line numbers array to the end of the generated Groovy ModuleNode
     * in a way suitable for the LineNumberTransform AST transform to operate on it
     */
    private void addLineNumbers() {
        this.out.println();
        this.out.println("@LineNumber(");
        this.out.print("\tlines = [");
        // get the line numbers here.  this will mean that the last 2 lines will not be captured in the
        // line number information, but that's OK since a user cannot set a breakpoint there anyway.
        int[] lineNumbers = filterTrailing0s(this.out.getLineNumbers());

        for (int i = 0; i < lineNumbers.length; i++) {
            this.out.print(lineNumbers[i]);
            if (i < lineNumbers.length - 1) {
                this.out.print(", ");
            }
        }
        this.out.println("],");
        this.out.println("\tsourceName = \"" + this.sourceName + "\"");
        this.out.println(")");
        this.out.println("class ___LineNumberPlaceholder { }");
    }

    /**
     * Filters trailing 0s from the line number array
     * @param lineNumbers the line number array
     * @return a new array that removes all 0s from the end of it
     */
    private int[] filterTrailing0s(int[] lineNumbers) {
        int startLocation = lineNumbers.length - 1;
        for (int i = lineNumbers.length - 1; i >= 0; i--) {
            if (lineNumbers[i] > 0) {
                startLocation = i + 1;
                break;
            }
        }

        int[] newLineNumbers = new int[startLocation];
        System.arraycopy(lineNumbers, 0, newLineNumbers, 0, startLocation);
        return newLineNumbers;
    }

    private void endTag() {
        if (!this.finalPass) {
            return;
        }

        String tagName = this.scan.getToken().trim();
        String ns = this.scan.getNamespace();

        if (this.tagMetaStack.isEmpty()) {
            throw new GrailsTagException(
                    "Found closing Grails tag with no opening [" + tagName + "]", this.pageName,
                    getCurrentOutputLineNumber());
        }

        TagMeta tm = this.tagMetaStack.pop();
        String lastInStack = tm.name;
        String lastNamespaceInStack = tm.namespace;

        // if the tag name is blank then it has been closed by the start tag ie <tag />
        if (GrailsStringUtils.isBlank(tagName)) {
            tagName = lastInStack;
        }

        if (!lastInStack.equals(tagName) || !lastNamespaceInStack.equals(ns)) {
            throw new GrailsTagException("Grails tag [" + lastNamespaceInStack +
                    ":" + lastInStack + "] was not closed", this.pageName, getCurrentOutputLineNumber());
        }

        if (GroovyPage.DEFAULT_NAMESPACE.equals(ns) && this.tagRegistry.isSyntaxTag(tagName)) {
            if (tm.instance instanceof GroovySyntaxTag) {
                GroovySyntaxTag tag = (GroovySyntaxTag) tm.instance;
                tag.doEndTag();
            }
            else {
                throw new GrailsTagException("Grails tag [" + tagName +
                        "] was not closed", this.pageName,
                        getCurrentOutputLineNumber());
            }
        }
        else {
            int bodyTagIndex = -1;
            if (!tm.emptyTag && !tm.bufferMode) {
                bodyTagIndex = this.tagIndex;
                this.out.println("})");
                this.closureLevel--;
            }

            if (tm.bufferMode && tm.bufferPartNumber != -1) {
                if (!this.bodyVarsDefined.contains(tm.tagIndex)) {
                    //out.print("def ");
                    this.bodyVarsDefined.add(tm.tagIndex);
                }
                this.out.println("createClosureForHtmlPart(" + tm.bufferPartNumber + ", " + tm.tagIndex + ")");
                bodyTagIndex = tm.tagIndex;
                tm.bufferMode = false;
            }

            if (this.jspTags.containsKey(ns)) {
                String uri = this.jspTags.get(ns);
                this.out.println("jspTag = getJspTag('" + uri + "', '" + tagName + "')");
                this.out.println("if (!jspTag) throw new GrailsTagException('Unknown JSP tag " +
                        ns + ":" + tagName + "')");
                this.out.print("jspTag.doTag(out," + this.attrsVarsMapDefinition.get(this.tagIndex) + ",");
                if (bodyTagIndex > -1) {
                    this.out.print("getBodyClosure(" + bodyTagIndex + ")");
                }
                else {
                    this.out.print("null");
                }
                this.out.println(")");
            }
            else {
                if (tm.hasAttributes) {
                    this.out.println("invokeTag('" + tagName + "','" + ns + "'," +
                            getCurrentOutputLineNumber() + "," + this.attrsVarsMapDefinition.get(this.tagIndex) +
                            "," + bodyTagIndex + ")");
                }
                else {
                    this.out.println("invokeTag('" + tagName + "','" + ns + "'," +
                            getCurrentOutputLineNumber() + ",[:]," + bodyTagIndex + ")");
                }
            }
        }

        tm.bufferMode = false;

        this.tagIndex--;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void startTag() {
        if (!this.finalPass) {
            return;
        }

        this.tagIndex++;

        String text;
        StringBuilder buf = new StringBuilder(this.scan.getToken());
        String ns = this.scan.getNamespace();

        boolean emptyTag = false;

        this.state = this.scan.nextToken();
        while (this.state != HTML && this.state != GEND_TAG && this.state != GEND_EMPTY_TAG && this.state != EOF) {
            if (this.state == GTAG_EXPR) {
                buf.append("${");
                buf.append(this.scan.getToken().trim());
                buf.append("}");
            }
            else {
                buf.append(this.scan.getToken());
            }
            this.state = this.scan.nextToken();
        }
        if (this.state == GEND_EMPTY_TAG) {
            emptyTag = true;
        }

        this.doNextScan = false;

        text = buf.toString();

        String tagName;
        Map attrs = new LinkedHashMap();

        Matcher m = WHITESPACE_PATTERN.matcher(text);

        if (m.find()) { // ignores carriage returns and new lines
            tagName = text.substring(0, m.start());
            if (this.state != EOF) {
                String attrTokens = text.substring(m.start(), text.length());
                populateMapWithAttributes(attrs, attrTokens);
            }
        }
        else {
            tagName = text;
        }

        if (this.state == EOF) {
            throw new GrailsTagException(
                    "Unexpected end of file encountered parsing Tag [" + tagName + "] for " + this.className +
                            ". Are you missing a closing brace '}'?", this.pageName,
                    getCurrentOutputLineNumber());
        }

        flushTagBuffering();

        TagMeta tm = new TagMeta();
        tm.name = tagName;
        tm.namespace = ns;
        tm.hasAttributes = !attrs.isEmpty();
        tm.lineNumber = getCurrentOutputLineNumber();
        tm.emptyTag = emptyTag;
        tm.tagIndex = this.tagIndex;
        this.tagMetaStack.push(tm);

        if (GroovyPage.DEFAULT_NAMESPACE.equals(ns) && this.tagRegistry.isSyntaxTag(tagName)) {
            if (this.tagContext == null) {
                this.tagContext = new HashMap<Object, Object>();
                this.tagContext.put(GroovyPage.OUT, this.out);
                this.tagContext.put(GroovyPageParser.class, this);
            }
            GroovySyntaxTag tag = (GroovySyntaxTag) this.tagRegistry.newTag(tagName);
            tag.init(this.tagContext);
            tag.setAttributes(attrs);

            if (tag.isKeepPrecedingWhiteSpace() && this.currentlyBufferingWhitespace) {
                flushBufferedWhiteSpace();
            }
            else if (!tag.isAllowPrecedingContent() && this.previousContentWasNonWhitespace) {
                throw new GrailsTagException("Tag [" + tag.getName() +
                        "] cannot have non-whitespace characters directly preceding it.", this.pageName,
                        getCurrentOutputLineNumber());
            }
            else {
                // If tag does not specify buffering of WS, we swallow it here
                clearBufferedWhiteSpace();
            }

            tag.doStartTag();

            tm.instance = tag;
        }
        else {
            // Custom taglibs have to always flush the whitespace, there's no
            // "allowPrecedingWhitespace" property on tags yet
            flushBufferedWhiteSpace();

            if (attrs.size() > 0) {
                FastStringWriter buffer = new FastStringWriter();
                buffer.print("[");
                for (Iterator<?> i = attrs.keySet().iterator(); i.hasNext(); ) {
                    String name = (String) i.next();
                    String cleanedName = name;
                    if (name.startsWith("\"") && name.endsWith("\"")) {
                        cleanedName = "'" + name.substring(1, name.length() - 1) + "'";
                    }
                    buffer.print(cleanedName);
                    buffer.print(':');

                    buffer.print(getExpressionText(attrs.get(name).toString()));
                    if (i.hasNext()) {
                        buffer.print(',');
                    }
                    else {
                        buffer.print("]");
                    }
                }
                this.attrsVarsMapDefinition.put(this.tagIndex, buffer.toString());
                buffer.close();
            }

            if (!emptyTag) {
                tm.bufferMode = true;
            }
        }
    }

    private void writeTagBodyStart(TagMeta tm) {
        if (tm.bufferMode) {
            tm.bufferMode = false;
            if (!this.bodyVarsDefined.contains(tm.tagIndex)) {
                //out.print("def ");
                this.bodyVarsDefined.add(tm.tagIndex);
            }
            this.out.println("createTagBody(" + tm.tagIndex + ", {->");
            this.closureLevel++;
        }
    }

    private void clearBufferedWhiteSpace() {
        this.whitespaceBuffer.delete(0, this.whitespaceBuffer.length());
        this.currentlyBufferingWhitespace = false;
    }

    // Write out any whitespace we saved between tags
    private void flushBufferedWhiteSpace() {
        if (this.currentlyBufferingWhitespace) {
            appendHtmlPart(null);
        }
        this.currentlyBufferingWhitespace = false;
    }

    private void populateMapWithAttributes(Map<String, String> attrs, String attrTokens) {
        attrTokens = attrTokens.trim();
        int startPos = 0;
        while (startPos < attrTokens.length()) {
            // parse name (before '=' character)
            int equalsignPos = attrTokens.indexOf('=', startPos);
            if (equalsignPos == -1) {
                throw new GrailsTagException("Expecting '=' after attribute name (" + attrTokens + ").", this.pageName, getCurrentOutputLineNumber());
            }
            String name = attrTokens.substring(startPos, equalsignPos).trim();

            // parse value
            startPos = equalsignPos + 1;
            char ch = attrTokens.charAt(startPos++);
            while (Character.isWhitespace(ch) && startPos < attrTokens.length()) {
                ch = attrTokens.charAt(startPos++);
            }
            if (!(ch == '\'' || ch == '"')) {
                throw new GrailsTagException("Attribute value must be quoted (" + attrTokens + ").", this.pageName, getCurrentOutputLineNumber());
            }
            char quoteChar = ch;

            GroovyPageExpressionParser expressionParser =
                    new GroovyPageExpressionParser(attrTokens, startPos, quoteChar, (char) 0, false);
            int endQuotepos = expressionParser.parse();
            if (endQuotepos == -1) {
                throw new GrailsTagException("Attribute value quote wasn't closed (" + attrTokens + ").",
                        this.pageName, getCurrentOutputLineNumber());
            }

            String val = attrTokens.substring(startPos, endQuotepos);

            if (val.startsWith("${") && val.endsWith("}") && !expressionParser.isContainsGstrings()) {
                val = val.substring(2, val.length() - 1);
            }
            else if (!(val.startsWith("[") && val.endsWith("]"))) {
                if (val.indexOf('"') == -1) {
                    quoteChar = '"';
                }
                String quoteStr;
                // use multiline groovy string if the value contains newlines
                if (val.indexOf('\n') != -1 || val.indexOf('\r') != -1) {
                    if (quoteChar == '"') {
                        quoteStr = MULTILINE_GROOVY_STRING_DOUBLEQUOTES;
                    }
                    else {
                        quoteStr = MULTILINE_GROOVY_STRING_SINGLEQUOTES;
                    }
                }
                else {
                    quoteStr = String.valueOf(quoteChar);
                }
                val = quoteStr + val + quoteStr;
            }
            attrs.put("\"" + name + "\"", val);
            startPos = endQuotepos + 1;
        }
    }

    private void pageImport(String value) {
        String[] imports = IMPORT_SEMICOLON_PATTERN.split(value.subSequence(0, value.length()));
        for (int ix = 0; ix < imports.length; ix++) {
            this.out.print("import ");
            this.out.print(imports[ix]);
            this.out.println();
        }
    }

    private static String readStream(InputStream in, String gspEncoding) throws IOException {
        if (gspEncoding == null) {
            gspEncoding = DEFAULT_ENCODING;
        }
        return IOUtils.toString(in, gspEncoding);
    }

    public static String getGspEncoding() {
        return DEFAULT_ENCODING;
    }

    private void script(boolean gsp) {
        flushTagBuffering();
        if (!this.finalPass) {
            return;
        }

        this.out.println();
        write(this.scan.getToken().trim(), gsp);
        this.out.println();
        this.out.println();
    }

    private void write(CharSequence text, boolean gsp) {
        if (!gsp) {
            this.out.print(text);
            return;
        }

        for (int ix = 0, ixz = text.length(); ix < ixz; ix++) {
            char c = text.charAt(ix);
            String rep = null;
            if (Character.isWhitespace(c)) {
                for (ix++; ix < ixz; ix++) {
                    if (Character.isWhitespace(text.charAt(ix))) {
                        continue;
                    }
                    ix--;
                    rep = " ";
                    break;
                }
            }
            else if (c == '&') {
                if (match("&semi;", text, ix)) {
                    rep = ";";
                    ix += 5;
                }
                else if (match("&amp;", text, ix)) {
                    rep = "&";
                    ix += 4;
                }
                else if (match("&lt;", text, ix)) {
                    rep = "<";
                    ix += 3;
                }
                else if (match("&gt;", text, ix)) {
                    rep = ">";
                    ix += 3;
                }
            }
            else if (c == '<') {
                if (match("<br>", text, ix) || match("<hr>", text, ix)) {
                    rep = "\n";
                    //incrementLineNumber();
                    ix += 3;
                }
                else {
                    int end = match(PARA_BREAK, text, ix);
                    if (end <= 0) {
                        end = match(ROW_BREAK, text, ix);
                    }
                    if (end > 0) {
                        rep = "\n";
                        //incrementLineNumber();
                        ix = end;
                    }
                }
            }
            if (rep != null) {
                this.out.print(rep);
            }
            else {
                this.out.print(c);
            }
        }
    }

    public long getLastModified() {
        return this.lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public List<String> getHtmlParts() {
        return this.htmlParts;
    }

    public String[] getHtmlPartsArray() {
        return this.htmlParts.toArray(new String[0]);
    }

    public boolean isInClosure() {
        return this.closureLevel > 0;
    }

    public String getExpressionCodecDirectiveValue() {
        return this.expressionCodecDirectiveValue;
    }

    public String getPageName() {
        return this.pageName;
    }

    public String getOutCodecDirectiveValue() {
        return this.outCodecDirectiveValue;
    }

    public String getStaticCodecDirectiveValue() {
        return this.staticCodecDirectiveValue;
    }

    public String getTaglibCodecDirectiveValue() {
        return this.taglibCodecDirectiveValue;
    }

    public void setTaglibCodecDirectiveValue(String taglibCodecDirectiveValue) {
        this.taglibCodecDirectiveValue = taglibCodecDirectiveValue;
    }

    public void setExpressionCodecDirectiveValue(String expressionCodecDirectiveValue) {
        this.expressionCodecDirectiveValue = expressionCodecDirectiveValue;
    }

    public void setOutCodecDirectiveValue(String outCodecDirectiveValue) {
        this.outCodecDirectiveValue = outCodecDirectiveValue;
    }

    public void setStaticCodecDirectiveValue(String staticCodecDirectiveValue) {
        this.staticCodecDirectiveValue = staticCodecDirectiveValue;
    }

    public boolean isCompileStaticMode() {
        return this.compileStaticMode != null ? this.compileStaticMode : false;
    }

    public boolean isModelFieldsMode() {
        return this.modelFieldsMode;
    }

    class TagMeta {

        String name;

        String namespace;

        Object instance;

        boolean isDynamic;

        boolean hasAttributes;

        int lineNumber;

        boolean emptyTag;

        int tagIndex;

        boolean bufferMode = false;

        int bufferPartNumber = -1;

        @Override
        public String toString() {
            return "<" + this.namespace + ":" + this.name + ">";
        }

    }

}
