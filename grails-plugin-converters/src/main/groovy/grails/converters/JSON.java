/*
 * Copyright 2006-2022 the original author or authors.
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
package grails.converters;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import groovy.lang.Closure;
import groovy.util.BuilderSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import grails.io.IOUtils;
import grails.util.GrailsWebUtil;
import grails.web.mime.MimeType;

import org.grails.web.converters.AbstractConverter;
import org.grails.web.converters.Converter;
import org.grails.web.converters.ConverterUtil;
import org.grails.web.converters.IncludeExcludeConverter;
import org.grails.web.converters.configuration.ConverterConfiguration;
import org.grails.web.converters.configuration.ConvertersConfigurationHolder;
import org.grails.web.converters.configuration.DefaultConverterConfiguration;
import org.grails.web.converters.exceptions.ConverterException;
import org.grails.web.converters.marshaller.ClosureObjectMarshaller;
import org.grails.web.converters.marshaller.ObjectMarshaller;
import org.grails.web.json.JSONArray;
import org.grails.web.json.JSONElement;
import org.grails.web.json.JSONException;
import org.grails.web.json.JSONObject;
import org.grails.web.json.JSONTokener;
import org.grails.web.json.JSONWriter;
import org.grails.web.json.PathCapturingJSONWriterWrapper;
import org.grails.web.json.PrettyPrintJSONWriter;

/**
 * A converter that converts domain classes, Maps, Lists, Arrays, POJOs and POGOs to JSON.
 *
 * @author Siegfried Puchbauer
 * @author Graeme Rocher
 */
public class JSON extends AbstractConverter<JSONWriter> implements IncludeExcludeConverter<JSONWriter> {

    private static final Log log = LogFactory.getLog(JSON.class);

    private static final String CACHED_JSON = "org.codehaus.groovy.grails.CACHED_JSON_REQUEST_CONTENT";

    protected Object target;

    protected final ConverterConfiguration<JSON> config;

    protected final CircularReferenceBehaviour circularReferenceBehaviour;

    protected boolean prettyPrint;

    protected JSONWriter writer;

    protected Stack<Object> referenceStack;

    protected ConverterConfiguration<JSON> initConfig() {
        return ConvertersConfigurationHolder.getConverterConfiguration(JSON.class);
    }

    /**
     * Default Constructor for a JSON Converter
     */
    public JSON() {
        this.config = initConfig();
        encoding = this.config != null ? this.config.getEncoding() : "UTF-8";
        contentType = MimeType.JSON.getName();
        this.circularReferenceBehaviour = this.config != null ? this.config.getCircularReferenceBehaviour() : CircularReferenceBehaviour.DEFAULT;
        this.prettyPrint = this.config != null && this.config.isPrettyPrint();
    }

    /**
     * Creates a new JSON Converter for the given Object
     *
     * @param target the Object to convert
     */
    public JSON(Object target) {
        this();
        setTarget(target);
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    private void prepareRender(Writer out) {
        this.writer = this.prettyPrint ? new PrettyPrintJSONWriter(out) : new JSONWriter(out);
        if (this.circularReferenceBehaviour == CircularReferenceBehaviour.PATH) {
            if (log.isInfoEnabled()) {
                log.info(String.format("Using experimental CircularReferenceBehaviour.PATH for %s", getClass().getName()));
            }
            this.writer = new PathCapturingJSONWriterWrapper(this.writer);
        }
        this.referenceStack = new Stack<>();
    }

    private void finalizeRender(Writer out) {
        try {
            out.flush();
            out.close();
        }
        catch (Exception e) {
            log.warn("Unexpected exception while closing a writer: " + e.getMessage());
        }
    }

    /**
     * Directs the JSON Writer to the given Writer
     *
     * @param out the Writer
     * @throws org.grails.web.converters.exceptions.ConverterException
     *
     */
    public void render(Writer out) throws ConverterException {
        prepareRender(out);
        value(this.target);
        finalizeRender(out);
    }

    /**
     * Directs the JSON Writer to the Outputstream of the HttpServletResponse and sets the Content-Type to application/json
     *
     * @param response a HttpServletResponse
     * @throws ConverterException
     */
    public void render(HttpServletResponse response) throws ConverterException {
        response.setContentType(GrailsWebUtil.getContentType(contentType, encoding));
        try {
            render(response.getWriter());
        }
        catch (IOException e) {
            throw new ConverterException(e);
        }
    }

    public JSONWriter getWriter() throws ConverterException {
        return this.writer;
    }

    public void convertAnother(Object o) throws ConverterException {
        value(o);
    }

    public void build(@SuppressWarnings("rawtypes") Closure c) throws ConverterException {
        new Builder(this).execute(c);
    }

    /**
     * @param o
     * @throws ConverterException
     */
    public void value(Object o) throws ConverterException {
        o = this.config.getProxyHandler().unwrapIfProxy(o);
        try {
            if (o == null) {
                this.writer.valueNull();
            }
            else if (o instanceof CharSequence) {
                this.writer.value(o);
            }
            else if (o instanceof Class<?>) {
                this.writer.value(((Class<?>) o).getName());
            }
            else if (o instanceof Number) {
                this.writer.value((Number) o);
            }
            else if (o instanceof Boolean) {
                this.writer.value((Boolean) o);
            }
            else if (o.getClass().isPrimitive() && !o.getClass().equals(byte[].class)) {
                this.writer.value(o);
            }
            else {
                if (this.referenceStack.contains(o)) {
                    handleCircularRelationship(o);
                }
                else {
                    this.referenceStack.push(o);
                    ObjectMarshaller<JSON> marshaller = this.config.getMarshaller(o);
                    if (marshaller == null) {
                        throw new ConverterException("Unconvertable Object of class: " + o.getClass().getName());
                    }
                    marshaller.marshalObject(o, this);
                    this.referenceStack.pop();
                }
            }
        }
        catch (ConverterException ce) {
            throw ce;
        }
        catch (JSONException e) {
            throw new ConverterException(e);
        }
    }

    public ObjectMarshaller<JSON> lookupObjectMarshaller(Object target) {
        return this.config.getMarshaller(target);
    }

    public int getDepth() {
        return this.referenceStack.size();
    }

    public void property(String key, Object value) throws JSONException, ConverterException {
        this.writer.key(key);
        value(value);
    }

    /**
     * Performs the conversion and returns the resulting JSON as String
     *
     * @param prettyPrint true, if the output should be indented, otherwise false
     * @return a JSON String
     * @throws JSONException
     */
    public String toString(boolean prettyPrint) throws JSONException {
        String json = super.toString();
        if (prettyPrint) {
            Object jsonObject = new JSONTokener(json).nextValue();
            if (jsonObject instanceof JSONObject) {
                return ((JSONObject) jsonObject).toString(3);
            }
            if (jsonObject instanceof JSONArray) {
                return ((JSONArray) jsonObject).toString(3);
            }
        }
        return json;
    }

    /**
     * Parses the given JSON String and returns either a JSONObject or a JSONArray
     *
     * @param reader JSON source
     * @return either a JSONObject or a JSONArray - depending on the given JSON
     * @throws ConverterException when the JSON content is not valid
     */
    public static JSONElement parse(Reader reader) throws ConverterException {
        return parse(IOUtils.toString(reader));
    }

    /**
     * Parses the given JSON String and returns either a JSONObject or a JSONArray
     *
     * @param source A string containing some JSON
     * @return either a JSONObject or a JSONArray - depending on the given JSON
     * @throws ConverterException when the JSON content is not valid
     */
    public static JSONElement parse(String source) throws ConverterException {
        try {
            final Object value = new JSONTokener(source).nextValue();
            if (value instanceof JSONElement) {
                return (JSONElement) value;
            }

            // return empty object
            return new JSONObject();
        }
        catch (JSONException e) {
            throw new ConverterException("Error parsing JSON", e);
        }
    }

    /**
     * Parses the given JSON and returns either a JSONObject or a JSONArray
     *
     * @param is       An InputStream which delivers some JSON
     * @param encoding the Character Encoding to use
     * @return either a JSONObject or a JSONArray - depending on the given JSON
     * @throws ConverterException when the JSON content is not valid
     */
    public static JSONElement parse(InputStream is, String encoding) throws ConverterException {
        return parse(IOUtils.toString(is, encoding));
    }

    /**
     * Parses the given request's InputStream and returns either a JSONObject or a JSONArray
     *
     * @param request the JSON Request
     * @return either a JSONObject or a JSONArray - depending on the given JSON
     * @throws ConverterException when the JSON content is not valid
     */
    public static Object parse(HttpServletRequest request) throws ConverterException {
        Object json = request.getAttribute(CACHED_JSON);
        if (json != null) {
            return json;
        }

        String encoding = request.getCharacterEncoding();
        if (encoding == null) {
            encoding = Converter.DEFAULT_REQUEST_ENCODING;
        }
        try {
            PushbackInputStream pushbackInputStream = null;
            int firstByte = -1;
            try {
                pushbackInputStream = new PushbackInputStream(request.getInputStream());
                firstByte = pushbackInputStream.read();
            }
            catch (IOException ignore) {
            }

            // code has only been changed from here down
            if (firstByte == -1) {
                json = new JSONObject();
            }
            else {
                pushbackInputStream.unread(firstByte);
                json = parse(pushbackInputStream, encoding);
            }
            request.setAttribute(CACHED_JSON, json);
            return json;
        }
        catch (IOException e) {
            throw new ConverterException("Error parsing JSON", e);
        }
    }

    /**
     * Sets the Object which is later converted to JSON
     *
     * @param target the Object
     * @see org.grails.web.converters.Converter
     */
    @Override
    public void setTarget(Object target) {
        this.target = target;
    }

    protected void handleCircularRelationship(Object o) throws ConverterException {
        switch (this.circularReferenceBehaviour) {
            case DEFAULT:
                final boolean isCollection = Collection.class.isAssignableFrom(o.getClass());
                final boolean isMap = Map.class.isAssignableFrom(o.getClass());
                if (!(isMap || isCollection)) {
                    Map<String, Object> props = new HashMap<>();
                    props.put("class", o.getClass());
                    StringBuilder ref = new StringBuilder();
                    int idx = this.referenceStack.indexOf(o);
                    for (int i = this.referenceStack.size() - 1; i > idx; i--) {
                        ref.append("../");
                    }
                    props.put("_ref", ref.substring(0, ref.length() - 1));
                    value(props);
                }
                else {
                    if (isMap) {
                        this.writer.object();
                        this.writer.endObject();
                    }
                    else {
                        this.writer.array();
                        this.writer.endArray();
                    }
                }
                break;
            case EXCEPTION:
                throw new ConverterException("Circular Reference detected: class " + o.getClass().getName());
            case INSERT_NULL:
                value(null);
                break;
            case PATH:
                Map<String, Object> props = new HashMap<String, Object>();
                props.put("class", o.getClass());
                int idx = this.referenceStack.indexOf(o);
                PathCapturingJSONWriterWrapper pcWriter = (PathCapturingJSONWriterWrapper) this.writer;
                props.put("ref", String.format("root%s", pcWriter.getStackReference(idx)));
                value(props);
                break;
            case IGNORE:
                break;
        }
    }

    public static ConverterConfiguration<JSON> getNamedConfig(String configName) throws ConverterException {
        ConverterConfiguration<JSON> cfg = ConvertersConfigurationHolder.getNamedConverterConfiguration(configName, JSON.class);
        if (cfg == null) {
            throw new ConverterException(String.format("Converter Configuration with name '%s' not found!", configName));
        }
        return cfg;
    }

    public static Object use(String configName, Closure<?> callable) throws ConverterException {
        ConverterConfiguration<JSON> old = ConvertersConfigurationHolder.getThreadLocalConverterConfiguration(JSON.class);
        ConverterConfiguration<JSON> cfg = getNamedConfig(configName);
        ConvertersConfigurationHolder.setThreadLocalConverterConfiguration(JSON.class, cfg);
        try {
            return callable.call();
        }
        finally {
            ConvertersConfigurationHolder.setThreadLocalConverterConfiguration(JSON.class, old);
        }
    }

    public static void use(String cfgName) throws ConverterException {
        if (cfgName == null || "default".equals(cfgName)) {
            ConvertersConfigurationHolder.setThreadLocalConverterConfiguration(JSON.class, null);
        }
        else {
            ConvertersConfigurationHolder.setThreadLocalConverterConfiguration(JSON.class, getNamedConfig(cfgName));
        }
    }

    public static void registerObjectMarshaller(Class<?> clazz, Closure<?> callable) throws ConverterException {
        registerObjectMarshaller(new ClosureObjectMarshaller<JSON>(clazz, callable));
    }

    public static void registerObjectMarshaller(Class<?> clazz, int priority, Closure<?> callable) throws ConverterException {
        registerObjectMarshaller(new ClosureObjectMarshaller<JSON>(clazz, callable), priority);
    }

    public static void registerObjectMarshaller(ObjectMarshaller<JSON> om) throws ConverterException {
        ConverterConfiguration<JSON> cfg = ConvertersConfigurationHolder.getConverterConfiguration(JSON.class);
        if (cfg == null) {
            throw new ConverterException("Default Configuration not found for class " + JSON.class.getName());
        }
        if (!(cfg instanceof DefaultConverterConfiguration<?>)) {
            cfg = new DefaultConverterConfiguration<>(cfg);
            ConvertersConfigurationHolder.setDefaultConfiguration(JSON.class, cfg);
        }
        ((DefaultConverterConfiguration<JSON>) cfg).registerObjectMarshaller(om);
    }

    public static void registerObjectMarshaller(ObjectMarshaller<JSON> om, int priority) throws ConverterException {
        ConverterConfiguration<JSON> cfg = ConvertersConfigurationHolder.getConverterConfiguration(JSON.class);
        if (cfg == null) {
            throw new ConverterException("Default Configuration not found for class " + JSON.class.getName());
        }
        if (!(cfg instanceof DefaultConverterConfiguration<?>)) {
            cfg = new DefaultConverterConfiguration<>(cfg);
            ConvertersConfigurationHolder.setDefaultConfiguration(JSON.class, cfg);
        }
        ((DefaultConverterConfiguration<JSON>) cfg).registerObjectMarshaller(om, priority);
    }

    public static void createNamedConfig(String name, Closure<?> callable) throws ConverterException {
        DefaultConverterConfiguration<JSON> cfg =
                new DefaultConverterConfiguration<>(ConvertersConfigurationHolder.getConverterConfiguration(JSON.class));
        try {
            callable.call(cfg);
            ConvertersConfigurationHolder.setNamedConverterConfiguration(JSON.class, name, cfg);
        }
        catch (Exception e) {
            throw ConverterUtil.resolveConverterException(e);
        }
    }

    public static void withDefaultConfiguration(Closure<?> callable) throws ConverterException {
        ConverterConfiguration<JSON> cfg = ConvertersConfigurationHolder.getConverterConfiguration(JSON.class);
        if (!(cfg instanceof DefaultConverterConfiguration<?>)) {
            cfg = new DefaultConverterConfiguration<>(cfg);
        }
        try {
            callable.call(cfg);
            ConvertersConfigurationHolder.setDefaultConfiguration(JSON.class, cfg);
            ConvertersConfigurationHolder.setDefaultConfiguration(JSON.class, cfg);
        }
        catch (Throwable t) {
            throw ConverterUtil.resolveConverterException(t);
        }
    }

    @Override
    public void setIncludes(List<String> includes) {
        setIncludes(this.target.getClass(), includes);
    }

    @Override
    public void setExcludes(List<String> excludes) {
        setExcludes(this.target.getClass(), excludes);
    }

    public class Builder extends BuilderSupport {

        private final JSON json;

        private final Stack<BuilderMode> stack = new Stack<>();

        private boolean start = true;

        private final JSONWriter writer;

        public Builder(JSON json) {
            this.json = json;
            this.writer = json.writer;
        }

        public void execute(Closure<?> callable) {
            callable.setDelegate(this);
//            callable.setDelegate(Closure.DELEGATE_FIRST);
            invokeMethod("json", new Object[] { callable });
        }

        @Override
        protected Object createNode(Object name) {
            int retVal = 1;
            try {
                if (this.start) {
                    this.start = false;
                    writeObject();
                }
                else {
                    if (getCurrent() == null && this.stack.peek() == BuilderMode.OBJECT) {
                        throw new IllegalArgumentException("only call to [element { }] is allowed when creating array");
                    }
                    if (this.stack.peek() == BuilderMode.ARRAY) {
                        writeObject();
                        retVal = 2;
                    }
                    this.writer.key(String.valueOf(name)).array();
                    this.stack.push(BuilderMode.ARRAY);
                }
            }
            catch (JSONException e) {
                throw new IllegalArgumentException("invalid element");
            }

            return retVal;
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected Object createNode(Object key, Map valueMap) {
            try {
                if (this.stack.peek().equals(BuilderMode.OBJECT)) {
                    this.writer.key(String.valueOf(key));
                }
                this.writer.object();
                for (Object o : valueMap.entrySet()) {
                    Map.Entry element = (Map.Entry) o;
                    this.writer.key(String.valueOf(element.getKey())); //.value(element.getValue());
                    this.json.convertAnother(element.getValue());
                }
                this.writer.endObject();
                return null;
            }
            catch (JSONException e) {
                throw new IllegalArgumentException("invalid element");
            }
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected Object createNode(Object arg0, Map arg1, Object arg2) {
            throw new IllegalArgumentException("not implemented");
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected Object createNode(Object key, Object value) {
            if (getCurrent() == null && this.stack.peek() == BuilderMode.OBJECT) {
                throw new IllegalArgumentException("only call to [element { }] is allowed when creating array");
            }

            try {
                int retVal = 0;
                if (this.stack.peek().equals(BuilderMode.ARRAY)) {
                    writeObject();
                    retVal = 1;
                }
                if (value instanceof Collection) {
                    Collection c = (Collection) value;
                    this.writer.key(String.valueOf(key));
                    handleCollectionRecurse(c);
                }
                else {
                    this.writer.key(String.valueOf(key));
                    this.json.convertAnother(value); //.value(value);
                }
                return retVal != 0 ? retVal : null;
            }
            catch (JSONException e) {
                throw new IllegalArgumentException("invalid element");
            }
        }

        @SuppressWarnings("rawtypes")
        private void handleCollectionRecurse(Collection c) throws JSONException {
            this.writer.array();
            for (Object element : c) {
                if (element instanceof Collection) {
                    handleCollectionRecurse((Collection) element);
                }
                else {
                    this.json.convertAnother(element);
                }
            }
            this.writer.endArray();
        }

        @Override
        protected void nodeCompleted(Object parent, Object node) {
            Object last = null;

            if (node == null) {
                return;
            }

            try {
                int i = ((Integer) node);
                while (i-- > 0) {
                    last = this.stack.pop();
                    if (BuilderMode.ARRAY == last) {
                        this.writer.endArray();
                    }
                    if (BuilderMode.OBJECT == last) {
                        this.writer.endObject();
                    }
                }
            }
            catch (JSONException e) {
                throw new IllegalArgumentException("invalid element on the stack");
            }
        }

        @Override
        protected void setParent(Object arg0, Object arg1) {
            /* do nothing */
        }

        private void writeObject() throws JSONException {
            this.writer.object();
            this.stack.push(BuilderMode.OBJECT);
        }

    }

    private enum BuilderMode {
        ARRAY,
        OBJECT
    }

}
