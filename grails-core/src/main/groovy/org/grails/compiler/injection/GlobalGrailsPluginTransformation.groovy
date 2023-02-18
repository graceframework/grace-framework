/*
 * Copyright 2014-2023 the original author or authors.
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
package org.grails.compiler.injection

import java.lang.reflect.Modifier

import groovy.transform.CompilationUnitAware
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import grails.core.ArtefactHandler
import grails.plugins.GrailsPluginInfo
import grails.plugins.metadata.GrailsPlugin
import grails.util.GrailsNameUtils

import org.grails.core.io.support.GrailsFactoriesLoader
import org.grails.io.support.AntPathMatcher
import org.grails.io.support.GrailsResourceUtils
import org.grails.io.support.SpringIOUtils
import org.grails.io.support.UrlResource

/**
 * A global transformation that applies Grails' transformations to classes within a Grails project
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@CompileStatic
class GlobalGrailsPluginTransformation implements ASTTransformation, CompilationUnitAware {

    static Set<String> pendingArtefactClasses = []
    static Set<String> pluginExcludes = []

    CompilationUnit compilationUnit

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ModuleNode ast = source.getAST()

        URL url = GrailsASTUtils.getSourceUrl(source)
        if (!url || !GrailsResourceUtils.isProjectSource(new UrlResource(url))) {
            return
        }

        List<ArtefactHandler> artefactHandlers = GrailsFactoriesLoader.loadFactories(ArtefactHandler)

        Set<String> transformedClasses = []
        boolean isPlugin = false
        Object projectName = null
        Object projectVersion = null
        ClassNode pluginClassNode = null

        List<ClassNode> classes = new ArrayList<>(ast.getClasses())
        for (ClassNode classNode : classes) {
            isPlugin = Boolean.parseBoolean((String) classNode.getNodeMetaData('isPlugin'))
            projectName = classNode.getNodeMetaData('projectName')
            projectVersion = classNode.getNodeMetaData('projectVersion') ?: getClass().getPackage().getImplementationVersion()

            String classNodeName = classNode.name

            if (classNodeName.endsWith('GrailsPlugin') && !classNode.isAbstract()) {
                pluginClassNode = classNode

                if (!classNode.getProperty('version')) {
                    classNode.addProperty(new PropertyNode('version', Modifier.PUBLIC,
                            ClassHelper.make(Object), classNode,
                            new ConstantExpression(projectVersion), null, null))
                }

                continue
            }

            if (!GrailsResourceUtils.isGrailsResource(new UrlResource(url))) {
                continue
            }

            if (projectName && projectVersion && isPlugin) {
                Map<String, Object> members = [
                        name: GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(String.valueOf(projectName)),
                        version: projectVersion]
                GrailsASTUtils.addAnnotationOrGetExisting(classNode, GrailsPlugin, members)
            }

            for (ArtefactHandler handler in artefactHandlers) {
                if (handler.isArtefact(classNode)) {
                    transformedClasses.add classNodeName
                }
            }
        }

        if (isPlugin) {
            File compilationTargetDirectory = resolveCompilationTargetDirectory(source)
            File pluginXmlFile = new File(compilationTargetDirectory, 'META-INF/grails-plugin.xml')
            pluginXmlFile.parentFile.mkdirs()
            generatePluginXml(pluginClassNode, String.valueOf(projectVersion), transformedClasses, pluginXmlFile)
        }
    }

    static File resolveCompilationTargetDirectory(SourceUnit source) {
        File targetDirectory
        if (source.class.name == 'org.codehaus.jdt.groovy.control.EclipseSourceUnit') {
            targetDirectory = GroovyEclipseCompilationHelper.resolveEclipseCompilationTargetDirectory(source)
        }
        else {
            targetDirectory = source.configuration.targetDirectory
        }

        targetDirectory ?: new File('build/classes/main')
    }

    static void generatePluginXml(ClassNode pluginClassNode, String projectVersion,
                                  Set<String> transformedClasses, File pluginXmlFile) {
        boolean pluginXmlExists = pluginXmlFile.exists()
        Set<String> artefactClasses = []
        artefactClasses.addAll(transformedClasses)
        artefactClasses.addAll(pendingArtefactClasses)

        // if the class being transformed is a *GrailsPlugin class then if it doesn't exist create it
        if (pluginClassNode && !pluginClassNode.isAbstract()) {
            if (pluginXmlExists) {
                // if the file does exist, update it with the plugin name
                updatePluginXml(pluginClassNode, projectVersion, pluginXmlFile, artefactClasses)
            }
            else {
                writePluginXml(pluginClassNode, projectVersion, pluginXmlFile, artefactClasses)
            }
        }
        else if (pluginXmlExists) {
            // if the class isn't the *GrailsPlugin class then only update the plugin.xml if it already exists
            updatePluginXml(null, projectVersion, pluginXmlFile, artefactClasses)
        }
        else {
            // otherwise add it to a list of pending classes to populated when the plugin.xml is created
            pendingArtefactClasses.addAll(transformedClasses)
        }
    }

    @CompileDynamic
    static void writePluginXml(ClassNode pluginClassNode, String projectVersion,
                               File pluginXmlFile, Set<String> artefactClasses) {
        if (pluginClassNode) {
            PluginAstReader pluginAstReader = new PluginAstReader()
            GrailsPluginInfo info = pluginAstReader.readPluginInfo(pluginClassNode)

            pluginXmlFile.withWriter('UTF-8') { Writer writer ->
                MarkupBuilder mkp = new MarkupBuilder(writer)
                String pluginName = GrailsNameUtils.getLogicalPropertyName(pluginClassNode.name, 'GrailsPlugin')

                Map<String, Object> pluginProperties = info.getProperties()
                String pluginVersion = pluginProperties['version'] ?: projectVersion
                String grailsVersion = pluginProperties['grailsVersion'] ?: getClass().getPackage().getImplementationVersion() + ' > *'
                String excludes = pluginProperties['pluginExcludes']
                if (excludes instanceof List) {
                    pluginExcludes.clear()
                    pluginExcludes.addAll(excludes)
                }

                mkp.plugin(name: pluginName, version: pluginVersion, grailsVersion: grailsVersion) {
                    type(pluginClassNode.name)

                    for (entry in pluginProperties) {
                        delegate."$entry.key"(entry.value)
                    }

                    // if there are pending classes to add to the plugin.xml add those
                    if (artefactClasses) {
                        AntPathMatcher antPathMatcher = new AntPathMatcher()
                        resources {
                            for (String cn in artefactClasses) {
                                if (!pluginExcludes.any { String exc -> antPathMatcher.match(exc, cn.replace('.', '/')) }) {
                                    resource cn
                                }
                            }
                        }
                    }
                }
            }

            pendingArtefactClasses.clear()
        }
    }

    @CompileDynamic
    static void updatePluginXml(ClassNode pluginClassNode, String projectVersion,
                                File pluginXmlFile, Set<String> artefactClasses) {
        try {
            XmlSlurper xmlSlurper = SpringIOUtils.createXmlSlurper()

            GPathResult pluginXml = xmlSlurper.parse(pluginXmlFile)
            if (pluginClassNode) {
                PluginAstReader pluginAstReader = new PluginAstReader()
                GrailsPluginInfo info = pluginAstReader.readPluginInfo(pluginClassNode)

                Map<String, Object> pluginProperties = info.getProperties()
                String pluginName = GrailsNameUtils.getLogicalPropertyName(pluginClassNode.name, 'GrailsPlugin')
                String pluginVersion = pluginProperties['version'] ?: projectVersion
                String grailsVersion = pluginProperties['grailsVersion'] ?: getClass().getPackage().getImplementationVersion() + ' > *'

                pluginXml.@name = pluginName
                pluginXml.@version = pluginVersion
                pluginXml.type = pluginClassNode.name
                pluginXml.@grailsVersion = grailsVersion
                for (entry in pluginProperties) {
                    pluginXml."$entry.key" = entry.value
                }

                Object excludes = pluginProperties['pluginExcludes']
                if (excludes instanceof List) {
                    pluginExcludes.clear()
                    pluginExcludes.addAll(excludes)
                }
            }

            if (artefactClasses) {
                def resources = pluginXml.resources

                for (String cn in artefactClasses) {
                    if (!resources.resource.find { it.text() == cn }) {
                        resources.appendNode {
                            resource(cn)
                        }
                    }
                }
            }

            handleExcludes(pluginXml)

            Writable writable = new StreamingMarkupBuilder().bind {
                mkp.yield pluginXml
            }

            pluginXmlFile.withWriter('UTF-8') { Writer writer ->
                writable.writeTo(writer)
            }

            pendingArtefactClasses.clear()
        }
        catch (ignored) {
            // corrupt, recreate
            writePluginXml(pluginClassNode, projectVersion, pluginXmlFile, artefactClasses)
        }
    }

    @CompileDynamic
    protected static void handleExcludes(GPathResult pluginXml) {
        if (pluginExcludes) {
            AntPathMatcher antPathMatcher = new AntPathMatcher()
            pluginXml.resources.resource.each { res ->
                if (pluginExcludes.any { String exc -> antPathMatcher.match(exc, res.text().replace('.', '/')) }) {
                    res.replaceNode {
                    }
                }
            }
        }
    }

}
