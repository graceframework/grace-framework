package grails.plugin.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.mock.web.MockServletContext;

import grails.core.DefaultGrailsApplication;
import org.grails.plugin.cache.GrailsCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link grails.plugin.cache.CacheAutoConfiguration}.
 *
 * @author Michael Yan
 */
public class CacheAutoConfigurationTests {

    private final AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext();

    @BeforeEach
    void setupContext() {
        this.context.setServletContext(new MockServletContext());
    }

    @AfterEach
    void close() {
        this.context.close();
    }

    @Test
    void defaultConfiguration() {
        registerAndRefreshContext();
        assertThat(this.context.getBean(CustomCacheKeyGenerator.class)).isNotNull();
        assertThat(this.context.getBean(GrailsCacheManager.class)).isNotNull();
    }

    private void registerAndRefreshContext(String... env) {
        TestPropertyValues.of(env).applyTo(this.context);
        this.context.registerBean("grailsApplication", DefaultGrailsApplication.class);
        this.context.register(CacheAutoConfiguration.class);
        this.context.refresh();
    }

}
