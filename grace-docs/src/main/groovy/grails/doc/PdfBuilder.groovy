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
         pre, code {
          font-size: 10px;
         }
         .toc-item { margin-bottom: 2px; }
         .toc-item strong { margin-right: 2px; }
        .contribute-btn, #navigation, #ref-button, #toggle-col1 { display: none; }
                .paragraph, table, h2, h3, h4, h5, h6, li, pre, code {
            width: 595px;
        }
        </style>
        '''
    }

}
