package grails.boot.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import grails.boot.config.GrailsComponentScanPackages;

/**
 * Configures the base packages used by auto-configuration when scanning for Grails
 * classes.
 * <p>
 * Using {@code @EntityScan} will cause auto-configuration to:
 * <ul>
 * <li>Set the
 * {@link grails.boot.config.GrailsApplicationPostProcessor @GrailsApplicationPostProcessor}
 * for Grails {@link grails.artefact.Artefact classes}, such as Application, Domain, Controller.</li>
 * </ul>
 * <p>
 * One of {@link #basePackageClasses()}, {@link #basePackages()} or its alias
 * {@link #value()} may be specified to define specific packages to scan. If specific
 * packages are not defined scanning will occur from the package of the class with this
 * annotation.
 *
 * @author Michael Yan
 * @since 2022.0.0
 *
 * @see org.springframework.context.annotation.ComponentScan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({ GrailsComponentScanPackages.Registrar.class })
public @interface GrailsComponentScan {

    /**
     * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
     * declarations e.g.: {@code @EntityScan("org.my.pkg")} instead of
     * {@code @GrailsComponentScan(basePackages="org.my.pkg")}.
     * @return the base packages to scan
     */
    @AliasFor("basePackages")
    String[] value() default {};

    /**
     * Base packages to scan for entities. {@link #value()} is an alias for (and mutually
     * exclusive with) this attribute.
     * <p>
     * Use {@link #basePackageClasses()} for a type-safe alternative to String-based
     * package names.
     * @return the base packages to scan
     */
    @AliasFor("value")
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying the packages to
     * scan for entities. The package of each class specified will be scanned.
     * <p>
     * Consider creating a special no-op marker class or interface in each package that
     * serves no purpose other than being referenced by this attribute.
     * @return classes from the base packages to scan
     */
    Class<?>[] basePackageClasses() default {};

}
