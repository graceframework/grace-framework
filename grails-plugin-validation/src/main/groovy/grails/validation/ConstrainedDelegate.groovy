package grails.validation

import grails.gorm.validation.ConstrainedProperty
import grails.gorm.validation.Constraint
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.validation.Errors

/**
 * Bridge from the old API to the new
 *
 * @author Graeme Rocher
 * @since 6.1
 *
 */
@CompileStatic
@Slf4j
class ConstrainedDelegate implements Constrained, ConstrainedProperty {

    final ConstrainedProperty property

    ConstrainedDelegate(ConstrainedProperty property) {
        this.property = property
    }

    /**
     * @return Returns the appliedConstraints.
     */
    Collection<Constraint> getAppliedConstraints() {
        property.appliedConstraints
    }

    @Override
    String getPropertyName() {
        property.getPropertyName()
    }

    @Override
    Constraint getAppliedConstraint(String name) {
        property.getAppliedConstraint(name)
    }

    @Override
    void validate(Object target, Object propertyValue, Errors errors) {
        property.validate(target, propertyValue, errors)
    }

    @Override
    String getWidget() {
        property.getWidget()
    }

    @Override
    boolean hasAppliedConstraint(String constraintName) {
        property.hasAppliedConstraint(constraintName)
    }

    @Override
    Class<?> getPropertyType() {
        property.getPropertyType()
    }

    @Override
    Comparable getMax() {
        property.getMax()
    }

    @Override
    Comparable getMin() {
        property.getMin()
    }

    @Override
    List getInList() {
        property.getInList()
    }

    @Override
    Range getRange() {
        property.getRange()
    }

    @Override
    Integer getScale() {
        property.getScale()
    }

    @Override
    Range getSize() {
        property.getSize()
    }

    @Override
    boolean isBlank() {
        property.isBlank()
    }

    @Override
    boolean isEmail() {
        property.isEmail()
    }

    @Override
    boolean isCreditCard() {
        property.isCreditCard()
    }

    @Override
    String getMatches() {
        property.getMatches()
    }

    @Override
    Object getNotEqual() {
        property.getNotEqual()
    }

    @Override
    Integer getMaxSize() {
        property.getMaxSize()
    }

    @Override
    Integer getMinSize() {
        property.getMinSize()
    }

    @Override
    boolean isNullable() {
        property.isNullable()
    }

    @Override
    boolean isUrl() {
        property.isUrl()
    }

    @Override
    boolean isDisplay() {
        property.isDisplay()
    }

    @Override
    boolean isEditable() {
        property.isEditable()
    }

    @Override
    int getOrder() {
        property.getOrder()
    }

    @Override
    String getFormat() {
        property.getFormat()
    }

    @Override
    boolean isPassword() {
        property.isPassword()
    }

    @Override
    boolean supportsContraint(String constraintName) {
        property.supportsContraint(constraintName)
    }

    @Override
    void applyConstraint(String constraintName, Object constrainingValue) {
        property.applyConstraint(constraintName, constrainingValue)
    }

    @Override
    Class getOwner() {
        property.getOwner()
    }

}
