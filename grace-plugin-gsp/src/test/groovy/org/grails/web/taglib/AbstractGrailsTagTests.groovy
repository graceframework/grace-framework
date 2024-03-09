package org.grails.web.taglib

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

import com.opensymphony.module.sitemesh.RequestConstants
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.MessageSource
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.core.io.DescriptiveResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.ui.context.Theme
import org.springframework.ui.context.ThemeSource
import org.springframework.ui.context.support.SimpleTheme
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import org.springframework.web.servlet.support.JstlUtils
import org.springframework.web.servlet.theme.SessionThemeResolver
import org.w3c.dom.Document

import grails.build.support.MetaClassRegistryCleaner
import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.gorm.validation.PersistentEntityValidator
import grails.plugins.GrailsPluginManager
import grails.util.GrailsWebMockUtil
import grails.util.Holders
import grails.util.Metadata
import grails.web.pages.GroovyPagesUriService

import org.grails.buffer.FastStringWriter
import org.grails.commons.CodecArtefactHandler
import org.grails.config.PropertySourcesConfig
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.core.artefact.gsp.TagLibArtefactHandler
import org.grails.core.support.GrailsApplicationAwareBeanPostProcessor
import org.grails.encoder.Encoder
import org.grails.encoder.impl.HTML4Codec
import org.grails.encoder.impl.HTMLJSCodec
import org.grails.encoder.impl.JavaScriptCodec
import org.grails.encoder.impl.RawCodec
import org.grails.gsp.GroovyPage
import org.grails.gsp.GroovyPageMetaInfo
import org.grails.gsp.GroovyPageTemplate
import org.grails.gsp.GroovyPagesTemplateEngine
import org.grails.gsp.compiler.SitemeshPreprocessor
import org.grails.plugins.DefaultGrailsPlugin
import org.grails.plugins.MockGrailsPluginManager
import org.grails.plugins.codecs.CodecsPluginConfiguration
import org.grails.plugins.codecs.HTMLCodec
import org.grails.plugins.codecs.URLCodec
import org.grails.plugins.core.CoreConfiguration
import org.grails.plugins.databinding.DataBindingConfiguration
import org.grails.plugins.support.PluginManagerAwareBeanPostProcessor
import org.grails.plugins.web.controllers.ControllersPluginConfiguration
import org.grails.plugins.web.mapping.UrlMappingsPluginConfiguration
import org.grails.plugins.web.mime.MimeTypesConfiguration
import org.grails.taglib.GroovyPageAttributes
import org.grails.taglib.TagOutput
import org.grails.taglib.encoder.OutputContextLookupHelper
import org.grails.taglib.encoder.OutputEncodingStack
import org.grails.taglib.encoder.WithCodecHelper
import org.grails.web.context.ServletEnvironmentGrailsApplicationDiscoveryStrategy
import org.grails.web.mapping.DefaultLinkGenerator
import org.grails.web.pages.DefaultGroovyPagesUriService
import org.grails.web.pages.GSPResponseWriter
import org.grails.web.servlet.context.support.WebRuntimeSpringConfiguration
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.sitemesh.GSPSitemeshPage
import org.grails.web.sitemesh.GrailsHTMLPageParser
import org.grails.web.sitemesh.GrailsLayoutView
import org.grails.web.util.GrailsApplicationAttributes

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assertions.fail

abstract class AbstractGrailsTagTests {

    static final Class[] DEFAULT_AUTO_CONFIGURATIONS = [
            CoreConfiguration,
            CodecsPluginConfiguration,
            ControllersPluginConfiguration,
            DataBindingConfiguration,
            MimeTypesConfiguration,
            UrlMappingsPluginConfiguration]

    MockServletContext servletContext
    GrailsWebRequest webRequest
    MockHttpServletRequest request
    MockHttpServletResponse response
    AnnotationConfigServletWebApplicationContext ctx
    def originalHandler
    ApplicationContext appCtx
    GrailsApplication ga
    GrailsPluginManager mockManager
    GroovyClassLoader gcl = new GroovyClassLoader()
    MetaClassRegistryCleaner registryCleaner = MetaClassRegistryCleaner.createAndRegister()

    boolean enableProfile = false

    GrailsApplication grailsApplication
    MessageSource messageSource

    DocumentBuilder domBuilder
    XPath xpath

    def withConfig(String text, Closure callable) {
        def config = new ConfigSlurper().parse(text)
        try {
            buildMockRequest(config)
            callable()
        }
        finally {
            RequestContextHolder.resetRequestAttributes()
            Holders.config = null
        }
    }

    GrailsWebRequest buildMockRequest(ConfigObject co) throws Exception {
        co.grails.resources.pattern = '/**'
        def config = new PropertySourcesConfig().merge(co)

        ga.config = config
        Holders.config = config
        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
        webRequest = GrailsWebMockUtil.bindMockWebRequest(appCtx)
        initRequestAndResponse()
        return webRequest
    }

    def profile(String name, Closure callable) {
        if (!enableProfile) return callable.call()

        def now = System.currentTimeMillis()
        for (i in 0..100) {
            callable.call()
        }
        println "$name took ${System.currentTimeMillis() - now}ms"
    }

    def withTag(String tagName, Writer out, String tagNamespace = "g", Closure callable) {
        def result = null
        runTest {
            def webRequest = RequestContextHolder.currentRequestAttributes()
            webRequest.out = out

            def mockController = grailsApplication.getControllerClass("MockController").newInstance()

            request.setAttribute(GrailsApplicationAttributes.CONTROLLER, mockController)
            request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver())

            def tagLibrary = grailsApplication.getArtefactForFeature(TagLibArtefactHandler.TYPE, tagNamespace + ":" + tagName)
            if (!tagLibrary) {
                fail("No tag library found for tag $tagName")
            }
            def go = tagLibrary.newInstance()
            appCtx.autowireCapableBeanFactory.autowireBeanProperties(go, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
            if (go instanceof ApplicationContextAware) {
                go.applicationContext = appCtx
            }
            def gspTagLibraryLookup = appCtx.gspTagLibraryLookup

            OutputEncodingStack stack = OutputEncodingStack.currentStack(OutputContextLookupHelper.lookupOutputContext(), true)

            stack.push(out)
            try {
                println "calling tag '${tagName}'"
                def tag = go.getProperty(tagName)?.clone()

                def tagWrapper = { Object[] args ->
                    def attrs = args?.size() > 0 ? args[0] : [:]
                    if (!(attrs instanceof GroovyPageAttributes)) {
                        attrs = new GroovyPageAttributes(attrs)
                    }
                    ((GroovyPageAttributes) attrs).setGspTagSyntaxCall(true)
                    def body = args?.size() > 1 ? args[1] : null
                    if (body && !(body instanceof Closure)) {
                        body = new TagOutput.ConstantClosure(body)
                    }

                    def tagresult = null

                    boolean encodeAsPushedToStack = false
                    try {
                        boolean returnsObject = gspTagLibraryLookup.doesTagReturnObject(tagNamespace, tagName)
                        Object codecInfo = gspTagLibraryLookup.getEncodeAsForTag(tagNamespace, tagName)
                        if (attrs.containsKey(GroovyPage.ENCODE_AS_ATTRIBUTE_NAME)) {
                            codecInfo = attrs.get(GroovyPage.ENCODE_AS_ATTRIBUTE_NAME)
                        }
                        else if (GroovyPage.DEFAULT_NAMESPACE.equals(tagNamespace) && GroovyPage.APPLY_CODEC_TAG_NAME.equals(tagName)) {
                            codecInfo = attrs
                        }
                        if (codecInfo != null) {
                            stack.push(WithCodecHelper.createOutputStackAttributesBuilder(codecInfo, webRequest.getAttributes().getGrailsApplication()).build())
                            encodeAsPushedToStack = true
                        }
                        switch (tag.getParameterTypes().length) {
                            case 1:
                                tagresult = tag.call(attrs)
                                outputTagResult(stack.taglibWriter, returnsObject, tagresult)
                                if (body) {
                                    body.call()
                                }
                                break
                            case 2:
                                tagresult = tag.call(attrs, (body != null) ? body : TagOutput.EMPTY_BODY_CLOSURE)
                                outputTagResult(stack.taglibWriter, returnsObject, tagresult)
                                break
                        }

                        Encoder taglibEncoder = stack.taglibEncoder
                        if (returnsObject && tagresult && !(tagresult instanceof Writer) && taglibEncoder) {
                            tagresult = taglibEncoder.encode(tagresult)
                        }
                        tagresult
                    }
                    finally {
                        if (encodeAsPushedToStack) stack.pop()
                    }
                }
                result = callable.call(tagWrapper)
            }
            finally {
                stack.pop()
            }
        }
        return result
    }

    private void outputTagResult(Writer taglibWriter, boolean returnsObject, Object tagresult) {
        if (returnsObject && tagresult != null && !(tagresult instanceof Writer)) {
            taglibWriter.print(tagresult)
        }
    }

    protected void onSetUp() {
    }

    @BeforeEach
    protected void setUp() throws Exception {
        GroovySystem.metaClassRegistry.addMetaClassRegistryChangeEventListener(registryCleaner)
        GroovyPageMetaInfo.DEFAULT_PLUGIN_PATH = null
        domBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        xpath = XPathFactory.newInstance().newXPath()
        originalHandler = GroovySystem.metaClassRegistry.metaClassCreationHandle

        GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle()
        onSetUp()
        Metadata.getInstance(new ByteArrayInputStream("""
info.app.name: ${getClass().name}
""".bytes))
        grailsApplication = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga = grailsApplication
        ga.config.grails.resources.pattern = '/**'
        ga.config.grails.gsp.tldScanPattern = 'classpath*:/META-INF/spring*.tld,classpath*:/META-INF/fmt.tld,classpath*:/META-INF/c.tld,classpath*:/META-INF/core.tld,classpath*:/META-INF/c-1_0-rt.tld'
        grailsApplication.initialise()
        mockManager = new MockGrailsPluginManager(grailsApplication)
        mockManager.registerProvidedArtefacts(grailsApplication)

        def mockControllerClass = gcl.parseClass("class MockController {  def index = {} } ")
        grailsApplication.addArtefact(ControllerArtefactHandler.TYPE, mockControllerClass)
        grailsApplication.addArtefact(CodecArtefactHandler.TYPE, HTMLCodec)
        grailsApplication.addArtefact(CodecArtefactHandler.TYPE, HTML4Codec)
        grailsApplication.addArtefact(CodecArtefactHandler.TYPE, JavaScriptCodec)
        grailsApplication.addArtefact(CodecArtefactHandler.TYPE, HTMLJSCodec)
        grailsApplication.addArtefact(CodecArtefactHandler.TYPE, URLCodec)
        grailsApplication.addArtefact(CodecArtefactHandler.TYPE, RawCodec)

        ctx = new AnnotationConfigServletWebApplicationContext()
        ctx.setServletContext(new MockServletContext())
        ctx.registerBeanDefinition("messageSource", new RootBeanDefinition(StaticMessageSource))

        ctx.beanFactory.registerSingleton(GrailsApplication.APPLICATION_ID, grailsApplication)
        ctx.beanFactory.registerSingleton("pluginManager", mockManager)
        ctx.beanFactory.registerSingleton("grailsLinkGenerator", new DefaultLinkGenerator("http://localhost:8080"))
        ctx.beanFactory.registerSingleton("manager", mockManager)
        ctx.beanFactory.registerSingleton("conversionService", new DefaultConversionService())
        ctx.beanFactory.registerSingleton(GroovyPagesUriService.BEAN_ID, new DefaultGroovyPagesUriService())
        ctx.register(DEFAULT_AUTO_CONFIGURATIONS)
        ctx.refresh()
        ctx.servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, ctx)

        grailsApplication.setApplicationContext(ctx)

        messageSource = ctx.getBean("messageSource", MessageSource)

        onInitMockBeans()

        List<Class> dependantPluginClasses = []
        dependantPluginClasses << gcl.loadClass("org.grails.plugins.core.CoreGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.grails.plugins.codecs.CodecsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.grails.plugins.domain.DomainClassGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.grails.plugins.i18n.I18nGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.grails.plugins.web.mapping.UrlMappingsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.grails.plugins.web.controllers.ControllersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.grails.plugins.web.GroovyPagesGrailsPlugin")

        List<DefaultGrailsPlugin> dependentPlugins = dependantPluginClasses.collect {
            new DefaultGrailsPlugin(it, new DescriptiveResource(it.toString()), grailsApplication)
        }

        dependentPlugins.each { mockManager.registerMockPlugin(it); it.manager = mockManager }
        mockManager.registerProvidedArtefacts(grailsApplication)
        mockManager.setApplicationContext(ctx)

        def springConfig = new WebRuntimeSpringConfiguration(ctx)

        webRequest = GrailsWebMockUtil.bindMockWebRequest(ctx)
        onInit()
        try {
            JstlUtils.exposeLocalizationContext webRequest.getRequest(), null
        }
        catch (Throwable ignore) {
        }

        servletContext = webRequest.servletContext
        Holders.servletContext = servletContext
        Holders.addApplicationDiscoveryStrategy(new ServletEnvironmentGrailsApplicationDiscoveryStrategy(servletContext));

        springConfig.servletContext = servletContext
        springConfig.registerPostProcessor(new BeanFactoryPostProcessor() {

            @Override
            void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                beanFactory.addBeanPostProcessor(new GrailsApplicationAwareBeanPostProcessor(grailsApplication))
                beanFactory.addBeanPostProcessor(new PluginManagerAwareBeanPostProcessor(mockManager))
            }
        })

        dependentPlugins*.doWithRuntimeConfiguration(springConfig)

        grailsApplication.mainContext = springConfig.getUnrefreshedApplicationContext()
        appCtx = springConfig.getApplicationContext()

        ctx.servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
        mockManager.applicationContext = appCtx

        //GroovySystem.metaClassRegistry.removeMetaClass(String)
        //GroovySystem.metaClassRegistry.removeMetaClass(Object)

        mockManager.doDynamicMethods()
        initRequestAndResponse()

        ga.domainClasses.each { dc ->
            def v = new PersistentEntityValidator()
            v.targetClass = dc
            dc.validator.messageSource = messageSource
        }
    }

    private initRequestAndResponse() {
        request = webRequest.currentRequest
        initThemeSource(request, messageSource)
        request.characterEncoding = "utf-8"
        response = webRequest.currentResponse
    }

    private void initThemeSource(request, MessageSource messageSource) {
        request.setAttribute(DispatcherServlet.THEME_SOURCE_ATTRIBUTE, new MockThemeSource(messageSource))
        request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new SessionThemeResolver())
    }

    @AfterEach
    protected void tearDown() {
        // Clear the page cache in the template engine since it's
        // static and likely to cause tests to interfere with each other.
        appCtx.groovyPagesTemplateEngine.clearPageCache()

        RequestContextHolder.resetRequestAttributes()
        GroovySystem.metaClassRegistry.setMetaClassCreationHandle(originalHandler)

        onDestroy()
        ga.mainContext.close()

        Holders.servletContext = null
        GroovyPageMetaInfo.DEFAULT_PLUGIN_PATH = ""
        registryCleaner.clean()
        GroovySystem.metaClassRegistry.removeMetaClassRegistryChangeEventListener(registryCleaner)
    }

    protected void onInit() {
    }

    protected void onDestroy() {
    }

    protected void onInitMockBeans() {
    }

    protected Resource[] getResources(String pattern) {
        new PathMatchingResourcePatternResolver().getResources(pattern)
    }

    void runTest(Closure callable) {
        callable.call()
    }

    void printCompiledSource(template, params = [:]) {
        //        def text =  getCompiledSource(template, params)
        //        println "----- GSP SOURCE -----"
        //        println text
    }

    def getCompiledSource(template, params = [:]) {
        def engine = appCtx.groovyPagesTemplateEngine

        assert engine
        def t = engine.createTemplate(template, "test_" + System.currentTimeMillis())

        def w = t.make(params)
        w.showSource = true

        def sw = new StringWriter()
        def out = new PrintWriter(sw)
        webRequest.out = out
        w.writeTo(out)

        String text = sw.toString()
    }

    def assertCompiledSourceContains(expected, template, params = [:]) {
        def text = getCompiledSource(template, params)
        return text.indexOf(expected) > -1
    }

    void assertOutputContains(expected, template, params = [:]) {
        def result = applyTemplate(template, params)
        assertTrue result.indexOf(expected) > -1,
                "Output does not contain expected string [$expected]. Output was: [${result}]"
    }

    void assertOutputNotContains(expected, template, params = [:]) {
        def result = applyTemplate(template, params)
        assertFalse result.indexOf(expected) > -1,
                "Output should not contain the expected string [$expected]. Output was: [${result}]"
    }

    /**
     * Compares the output generated by a template with a string.
     * @param expected The string that the template output is expected
     * to match.
     * @param template The template to run.
     * @param params A map of variables to pass to the template - by
     * default an empty map is used.
     * @param transform A closure that is passed a StringWriter instance
     * containing the output generated by the template. It is the result
     * of this transformation that is actually compared with the expected
     * string. The default transform simply converts the contents of the
     * writer to a string.
     */
    void assertOutputEquals(expected, template, params = [:], Closure transform = { it.toString() }) {

        GroovyPageTemplate t = createTemplate(template)

        /*
         println "------------HTMLPARTS----------------------"
         t.metaInfo.htmlParts.eachWithIndex {it, i -> print "htmlpart[${i}]:\n>${it}<\n--------\n" }
         */

        assertTemplateOutputEquals(expected, t, params, transform)
    }

    protected GroovyPageTemplate createTemplate(template) {
        GroovyPagesTemplateEngine engine = appCtx.groovyPagesTemplateEngine

        //printCompiledSource(template)

        assert engine
        GroovyPageTemplate t = engine.createTemplate(template, "test_" + System.currentTimeMillis())
        t.allowSettingContentType = true
        return t
    }

    protected def assertTemplateOutputEquals(String expected, GroovyPageTemplate template, params = [:], Closure transform = { it.toString() }) {
        def w = template.make(params)

        MockHttpServletResponse mockResponse = new MockHttpServletResponse()
        mockResponse.setCharacterEncoding("UTF-8")
        GSPResponseWriter writer = GSPResponseWriter.getInstance(mockResponse)
        webRequest.out = writer
        w.writeTo(writer)

        writer.flush()
        assertEquals(expected, transform(mockResponse.contentAsString))
    }

    def applyTemplate(template, params = [:], target = null, String filename = null) {

        GroovyPagesTemplateEngine engine = appCtx.groovyPagesTemplateEngine

        //printCompiledSource(template)

        assert engine
        def t = engine.createTemplate(template, filename ?: "test_" + System.currentTimeMillis())

        def w = t.make(params)

        if (!target) {
            target = new FastStringWriter()
        }
        webRequest.out = target

        w.writeTo(target)
        target.flush()

        return target.toString()
    }

    /**
     * Applies sitemesh preprocessing to a template
     */
    String sitemeshPreprocess(String template) {
        def preprocessor = new SitemeshPreprocessor()
        preprocessor.addGspSitemeshCapturing(template)
    }

    String applyLayout(String layout, String template, Map params = [:]) {
        def gspSiteMeshPage = new GSPSitemeshPage()
        request.setAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE, gspSiteMeshPage)
        def content = applyTemplate(template, params)
        request.removeAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE)

        def page = null
        if (!params.parse && gspSiteMeshPage != null && gspSiteMeshPage.isUsed()) {
            page = gspSiteMeshPage
        }
        else {
            def parser = new GrailsHTMLPageParser()
            page = parser.parse(content.toCharArray())
        }
        try {
            request.setAttribute(RequestConstants.PAGE, page)
            request.setAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE, new GSPSitemeshPage())
            return applyTemplate(layout, params, null, "/layouts/test_" + System.currentTimeMillis())
        }
        finally {
            request.removeAttribute(RequestConstants.PAGE)
            request.removeAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE)
        }
    }
    /**
     * Parses the given XML text and creates a DOM document from it.
     */
    protected final Document parseText(String xml) {
        return domBuilder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")))
    }

    /**
     * Asserts that the given XPath expression matches at least one
     * node in the given DOM document.
     */
    protected final void assertXPathExists(Document doc, String expr) {
        assertTrue xpath.evaluate(expr, doc, XPathConstants.BOOLEAN)
    }

    /**
     * Asserts that the given XPath expression matches no nodes in the
     * given DOM document.
     */
    protected final void assertXPathNotExists(Document doc, String expr) {
        assertFalse xpath.evaluate(expr, doc, XPathConstants.BOOLEAN)
    }
}

class MockThemeSource implements ThemeSource {

    private messageSource

    MockThemeSource(MessageSource messageSource) {
        this.messageSource = messageSource
    }

    Theme getTheme(String themeName) { new SimpleTheme(themeName, messageSource) }
}
