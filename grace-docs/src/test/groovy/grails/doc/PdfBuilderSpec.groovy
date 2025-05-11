package grails.doc

import spock.lang.Specification

class PdfBuilderSpec extends Specification {

    void "remove CssLinks"() {
        given:
        PdfBuilder pdfBuilder = new PdfBuilder()
        String html = """
<head>
        <title>The Grails Framework 3.2.11</title>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <link rel="stylesheet" href="../css/main.css" type="text/css" media="screen, print" title="Style" charset="utf-8" />
        <link rel="stylesheet" href="../css/pdf.css" type="text/css" media="print" title="PDF" charset="utf-8" />
    <script type="text/javascript">
function addJsClass(el) {
    var classes = document.body.className.split(" ");
    classes.push("js");
    document.body.className = classes.join(" ");
}
    </script>
    </head>
    """
        String expected = """
<head>
        <title>The Grails Framework 3.2.11</title>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        
        
    <script type="text/javascript">
function addJsClass(el) {
    var classes = document.body.className.split(" ");
    classes.push("js");
    document.body.className = classes.join(" ");
}
    </script>
    </head>
    """
        when:
        String output = pdfBuilder.removeCssLinks(html)

        then:
        output == expected

    }

    void "generate pdf from sample docs"() {
        given:
        PdfBuilder pdfBuilder = new PdfBuilder()
        String sampleDocsDir = 'build/resources/test/docs'
        File sampleDocsFolder = new File(sampleDocsDir)
        String singleHtml = "single.html"
        String singlePdf = 'single.pdf'

        expect:
        sampleDocsFolder.exists()
        !new File("${sampleDocsDir}/guide/${singlePdf}").exists()

        when:
        pdfBuilder.build(sampleDocsDir, singleHtml, singlePdf)

        then:
        noExceptionThrown()
        new File("${sampleDocsDir}/guide/${singlePdf}").exists()
    }
}
