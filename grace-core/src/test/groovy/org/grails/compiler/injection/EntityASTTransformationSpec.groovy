package org.grails.compiler.injection

import org.codehaus.groovy.ast.ClassHelper
import spock.lang.Specification

import grails.artefact.Artefact

class EntityASTTransformationSpec extends Specification {

    def "Test Entity class was applied by EntityASTTransformation"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true

        when:
        def clazz = gcl.parseClass('''
@grails.persistence.Entity
class Message {
}
''', '/Users/grails/grails-demo-project/grails-app/src/main/groovy/org/demo/Message.groovy')

        def classNode = gcl.getClassNode('Message')

        then:
        clazz.getAnnotationsByType(Artefact)
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.injection.EntityASTTransformation')
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.injection.DefaultGrailsDomainClassInjector')
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.converters.ConvertersDomainTransformer')
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.ControllerDomainTransformer')
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.gorm.GormTransformer')
    }

}
