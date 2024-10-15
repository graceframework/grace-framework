package grails.test.mixin.hibernate

import grails.test.hibernate.HibernateSpec
import org.grails.datastore.mapping.config.Settings

class HibernateSpecOverrideSpec extends HibernateSpec {
    @Override
    Map getConfiguration() {
        [(Settings.SETTING_FAIL_ON_ERROR): true, 'dataSource.url': 'jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000']
    }

    void "Configuration Overrides values in application.yml/groovy"() {
        expect:
        hibernateDatastore.failOnError == true
    }
}
