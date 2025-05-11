/*
 * Copyright 2004-2025 the original author or authors.
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
package grails.doc

import java.nio.charset.StandardCharsets

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.w3c.dom.Document
import org.xhtmlrenderer.pdf.ITextRenderer

/**
 * Use {@link org.xhtmlrenderer.pdf.ITextRenderer} to generate PDF guide
 *
 * @author Burt Beckwith
 * @author Lari Hotar
 * @author Michael Yan
 * @since 1.2
 */
class PdfBuilder {

    private static final String LIVE_DOC_SITE = 'https://graceframework.org'

    static boolean cleanHtml = System.getProperty('grails.docs.clean.html') == null ? true : Boolean.getBoolean('grails.docs.clean.html')
    static boolean debugPdf = Boolean.getBoolean('grails.docs.debug.pdf')

    void build(String baseDir, String htmlFile, String outputFile) {
        build basedir: baseDir, htmlFile: htmlFile, outputFile: outputFile
    }

    /**
     * Builds a PDF file from the manual's single.html file.<p>
     * The following directories are assumed to exist:<ul>
     * <li> $basedir/guide/single.html</li>
     * <li> $basedir/guide/css/</li>
     * <li> $basedir/guide/img/</li>
     * </ul>
     *
     * The {@code options} map should have the following key/value pairs<ul>
     * <li>basedir = points to the root directory that contains the generated manual <b>required</b></li>
     * </ul>
     */
    void build(Map<String, String> options) {
        File baseDir = new File(options.basedir).canonicalFile

        File guideDir = new File(baseDir, 'guide')
        File htmlFile = new File(guideDir, options.htmlFile)
        File outputFile = new File(guideDir, options.outputFile)

        String xml = createXml(htmlFile, baseDir.absolutePath)
        createPdf xml, outputFile, guideDir
    }

    String createXml(File htmlFile, String base) {
        String xml = htmlFile.getText('UTF-8')

        // fix inner anchors
        xml = xml.replaceAll('<a href="\\.\\./guide/single\\.html', '<a href="')
        // fix image refs to absolute paths
        xml = xml.replaceAll('src="\\.\\./img/', "src=\"file://${base}/img/")

        // convert tabs to spaces otherwise they only take up one space
        xml = xml.replaceAll('\t', '    ')
        cleanupHtml(htmlFile, xml)
    }

    Document createDocument(String xml) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance()
        dbf.validating = false
        dbf.setFeature 'http://apache.org/xml/features/nonvalidating/load-external-dtd', false
        dbf.setFeature 'http://apache.org/xml/features/nonvalidating/load-dtd-grammar', false

        DocumentBuilder builder = dbf.newDocumentBuilder()
        builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
    }

    void createPdfWithDocument(Document doc, File outputFile, File urlBase) {
        ITextRenderer renderer = new ITextRenderer()
        renderer.setDocument(doc, urlBase.toURI().toString())

        OutputStream outputStream
        try {
            outputStream = new FileOutputStream(outputFile)
            renderer.layout()
            renderer.createPDF(outputStream)
        }
        finally {
            outputStream?.close()
        }
    }

    void createPdf(String xml, File outputFile, File urlBase) {
        Document doc = createDocument(xml)
        createPdfWithDocument(doc, outputFile, urlBase)
    }

    String cleanupHtml(File htmlFile, String xml) {
        String result = cleanHtml ? Jsoup.parse(xml, '', Parser.xmlParser()).outerHtml() : xml
        result = removeCssLinks(result)
        result = result.replaceAll('</head>', pdfCss() + '</head>')
        if (debugPdf) {
            File before = new File(htmlFile.absolutePath + '.before.xml')
            before.setText(xml, 'UTF-8')
            if (result != xml) {
                File after = new File(htmlFile.absolutePath + '.after.xml')
                after.setText(result, 'UTF-8')
            }
        }
        result
    }

    String removeCssLinks(String html) {
        String str = html
        for (; ;) {
            int index = str.indexOf('<link rel="stylesheet"')
            if (index == -1) {
                break
            }
            str = removeCssLink(str)
        }
        str
    }

    private static String removeCssLink(String htmlString) {
        String output
        String str = htmlString
        int index = str.indexOf('<link rel="stylesheet"')
        output = str.substring(0, index)
        String end = str.substring(index, str.size())
        output += end.substring(end.indexOf('/>') + '/>'.length(), end.size())
        output
    }

    private static String pdfCss() {
        '''<style type="text/css">
        @page {
            size: A4 portrait;
            margin-top: 2cm;
            margin-bottom: 2cm;
            @top-center {
                content: element(docTitle);
            }
            @bottom-center {
                content: element(docVersion);
            }
        }
        @page :left {
            @bottom-left {
                content: counter(page);
                vertical-align: middle;
                margin: 0.8em 0;
                font-size: 12px;
            }
        }
        @page :right {
            @bottom-right {
                content: counter(page);
                vertical-align: middle;
                margin: 0.8em 0;
                font-size: 12pt;
            }
        }
        .docTitle {display: inline;position: running(docTitle); text-align: center; font-size: 18px; font-weight: bold;}
        .docSubTitle {display: none;}
        .docVersion {display: block;position: running(docVersion); text-align: center; font-size: 12px;}
        h1 { page-break-before: always; }
        h2 { page-break-after: avoid; }
        table.frame-all, table.grid-all {border-collapse: collapse; border: 1px solid #dedede; }
        table.grid-all th, table.grid-all td {border-collapse: collapse; border: 1px solid #dedede; }
        table.grid-all th { padding: 10px; }
        .warning, .note, table {
          margin-top: 1em;
          margin-bottom: 1em;
          page-break-inside: avoid;
        }
        pre.CodeRay {background-color:#f7f7f8;}
        .CodeRay .line-numbers{border-right:1px solid #d8d8d8;padding:0 0.5em 0 .25em}
        .CodeRay span.line-numbers{display:inline-block;margin-right:.5em;color:rgba(0,0,0,.3)}
        .CodeRay .line-numbers strong{color:rgba(0,0,0,.4)}
        table.CodeRay{border-collapse:separate;border-spacing:0;margin-bottom:0;border:0;background:none}
        table.CodeRay td{vertical-align: top;line-height:1.45}
        table.CodeRay td.line-numbers{text-align:right}
        table.CodeRay td.line-numbers>pre{padding:0;color:rgba(0,0,0,.3)}
        table.CodeRay td.code{padding:0 0 0 .5em}
        table.CodeRay td.code>pre{padding:0}
        .CodeRay .debug{color:#fff !important;background:#000080 !important}
        .CodeRay .annotation{color:#007}
        .CodeRay .attribute-name{color:#000080}
        .CodeRay .attribute-value{color:#700}
        .CodeRay .binary{color:#509}
        .CodeRay .comment{color:#998;font-style:italic}
        .CodeRay .char{color:#04d}
        .CodeRay .char .content{color:#04d}
        .CodeRay .char .delimiter{color:#039}
        .CodeRay .class{color:#458;font-weight:bold}
        .CodeRay .complex{color:#a08}
        .CodeRay .constant,.CodeRay .predefined-constant{color:#008080}
        .CodeRay .color{color:#099}
        .CodeRay .class-variable{color:#369}
        .CodeRay .decorator{color:#b0b}
        .CodeRay .definition{color:#099}
        .CodeRay .delimiter{color:#000}
        .CodeRay .doc{color:#970}
        .CodeRay .doctype{color:#34b}
        .CodeRay .doc-string{color:#d42}
        .CodeRay .escape{color:#666}
        .CodeRay .entity{color:#800}
        .CodeRay .error{color:#808}
        .CodeRay .exception{color:inherit}
        .CodeRay .filename{color:#099}
        .CodeRay .function{color:#900;font-weight:bold}
        .CodeRay .global-variable{color:#008080}
        .CodeRay .hex{color:#058}
        .CodeRay .integer,.CodeRay .float{color:#099}
        .CodeRay .include{color:#555}
        .CodeRay .inline{color:#000}
        .CodeRay .inline .inline{background:#ccc}
        .CodeRay .inline .inline .inline{background:#bbb}
        .CodeRay .inline .inline-delimiter{color:#d14}
        .CodeRay .inline-delimiter{color:#d14}
        .CodeRay .important{color:#555;font-weight:bold}
        .CodeRay .interpreted{color:#b2b}
        .CodeRay .instance-variable{color:#008080}
        .CodeRay .label{color:#970}
        .CodeRay .local-variable{color:#963}
        .CodeRay .octal{color:#40e}
        .CodeRay .predefined{color:#369}
        .CodeRay .preprocessor{color:#579}
        .CodeRay .pseudo-class{color:#555}
        .CodeRay .directive{font-weight:bold}
        .CodeRay .type{font-weight:bold}
        .CodeRay .predefined-type{color:inherit}
        .CodeRay .reserved,.CodeRay .keyword {color:#000;font-weight:bold}
        .CodeRay .key{color:#808}
        .CodeRay .key .delimiter{color:#606}
        .CodeRay .key .char{color:#80f}
        .CodeRay .value{color:#088}
        .CodeRay .regexp .delimiter{color:#808}
        .CodeRay .regexp .content{color:#808}
        .CodeRay .regexp .modifier{color:#808}
        .CodeRay .regexp .char{color:#d14}
        .CodeRay .regexp .function{color:#404;font-weight:bold}
        .CodeRay .string{color:#d20}
        .CodeRay .string .string .string{background:#ffd0d0}
        .CodeRay .string .content{color:#d14}
        .CodeRay .string .char{color:#d14}
        .CodeRay .string .delimiter{color:#d14}
        .CodeRay .shell{color:#d14}
        .CodeRay .shell .delimiter{color:#d14}
        .CodeRay .symbol{color:#990073}
        .CodeRay .symbol .content{color:#a60}
        .CodeRay .symbol .delimiter{color:#630}
        .CodeRay .tag{color:#008080}
        .CodeRay .tag-special{color:#d70}
        .CodeRay .variable{color:#036}
        .CodeRay .insert{background:#afa}
        .CodeRay .delete{background:#faa}
        .CodeRay .change{color:#aaf;background:#007}
        .CodeRay .head{color:#f8f;background:#505}
        .CodeRay .insert .insert{color:#080}
        .CodeRay .delete .delete{color:#800}
        .CodeRay .change .change{color:#66f}
        .CodeRay .head .head{color:#f4f}
        .literalblock pre, .literalblock pre[class], .listingblock pre, .listingblock pre[class] {
            word-wrap: break-word;
            padding: 1em;
            font-family: "Courier New", monospace, serif;
            font-size: .8125em;
        }
        .toc-item { margin-bottom: 2px; }
        .toc-item strong { margin-right: 2px; }
        .contribute-btn, #navigation .navLinks, #ref-button, #toggle-col1, #col2 { display: none; }
        .paragraph, table, h2, h3, h4, h5, h6, li, pre, code {
          width: 595px;
        }
        </style>
        '''
    }

}
