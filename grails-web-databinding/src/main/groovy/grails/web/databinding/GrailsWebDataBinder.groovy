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
package grails.web.databinding

import java.lang.annotation.Annotation

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.slurpersupport.GPathResult
import org.apache.commons.lang3.StringUtils
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.MetaClassHelper
import org.codehaus.groovy.runtime.metaclass.ThreadManagedMetaBeanProperty
import org.springframework.context.MessageSource
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError

import grails.core.GrailsApplication
import grails.databinding.BindingFormat
import grails.databinding.DataBindingSource
import grails.databinding.SimpleDataBinder
import grails.databinding.SimpleMapDataBindingSource
import grails.databinding.TypedStructuredBindingEditor
import grails.databinding.converters.FormattedValueConverter
import grails.databinding.converters.ValueConverter
import grails.databinding.events.DataBindingListener
import grails.util.GrailsClassUtils
import grails.util.GrailsMetaClassUtils
import grails.util.GrailsNameUtils
import grails.validation.DeferredBindingActions
import grails.validation.ValidationErrors

import org.grails.core.artefact.AnnotationDomainClassArtefactHandler
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.core.exceptions.GrailsConfigurationException
import org.grails.databinding.IndexedPropertyReferenceDescriptor
import org.grails.databinding.xml.GPathResultMap
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Basic
import org.grails.datastore.mapping.model.types.ManyToOne
import org.grails.datastore.mapping.model.types.OneToMany
import org.grails.datastore.mapping.model.types.OneToOne
import org.grails.datastore.mapping.model.types.Simple
import org.grails.web.databinding.DataBindingEventMulticastListener
import org.grails.web.databinding.GrailsWebDataBindingListener
import org.grails.web.databinding.SpringConversionServiceAdapter
import org.grails.web.databinding.converters.ByteArrayMultipartFileValueConverter
import org.grails.web.servlet.mvc.GrailsWebRequest

import static grails.web.databinding.DataBindingUtils.getBindingIncludeList

@CompileStatic
class GrailsWebDataBinder extends SimpleDataBinder {

    protected GrailsApplication grailsApplication
    protected MessageSource messageSource
    boolean trimStrings = true
    boolean convertEmptyStringsToNull = true
    protected List<DataBindingListener> listeners = []

    GrailsWebDataBinder(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
        this.conversionService = new SpringConversionServiceAdapter()
        registerConverter(new ByteArrayMultipartFileValueConverter())
    }

    @Override
    void bind(Object obj, DataBindingSource source) {
        bind(obj, source, null, getBindingIncludeList(obj), null, null)
    }

    @Override
    void bind(Object obj, DataBindingSource source, DataBindingListener listener) {
        bind(obj, source, null, getBindingIncludeList(obj), null, listener)
    }

    @Override
    void bind(Object object, DataBindingSource source, String filter, List whiteList, List blackList, DataBindingListener listener) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(object, object.getClass().name)
        doBind(object, source, filter, whiteList, blackList, listener, bindingResult)
    }

    @Override
    protected void doBind(Object object, DataBindingSource source, String filter, List whiteList, List blackList,
                          DataBindingListener listener, Object errors) {
        BeanPropertyBindingResult bindingResult = (BeanPropertyBindingResult) errors
        DataBindingListener errorHandlingListener = new GrailsWebDataBindingListener(messageSource)

        List<DataBindingListener> allListeners = []
        allListeners << errorHandlingListener
        if (listener != null && !(listener instanceof DataBindingEventMulticastListener)) {
            allListeners << listener
        }
        allListeners.addAll listeners.findAll { DataBindingListener l -> l.supports(object.getClass()) }

        DataBindingListener listenerWrapper = new DataBindingEventMulticastListener(allListeners)

        boolean bind = listenerWrapper.beforeBinding(object, bindingResult)

        if (bind) {
            super.doBind(object, source, filter, whiteList, blackList, listenerWrapper, bindingResult)
        }

        listenerWrapper.afterBinding(object, bindingResult)

        populateErrors(object, bindingResult)
    }

    @Override
    void bind(Object obj, GPathResult gpath) {
        bind(obj, new SimpleMapDataBindingSource(new GPathResultMap(gpath)), getBindingIncludeList(obj))
    }

    protected void populateErrors(Object obj, BindingResult bindingResult) {
        PersistentEntity domain = getPersistentEntity(obj.getClass())

        if (domain != null && bindingResult != null) {
            ValidationErrors newResult = new ValidationErrors(obj)
            for (Object error : bindingResult.getAllErrors()) {
                if (error instanceof FieldError) {
                    FieldError fieldError = (FieldError) error
                    boolean isBlank = StringUtils.isBlank((String) fieldError.getRejectedValue())
                    if (isBlank) {
                        PersistentProperty prop = domain.getPropertyByName(fieldError.getField())
                        if (prop != null) {
                            boolean isOptional = prop.isNullable()
                            if (!isOptional) {
                                newResult.addError(fieldError)
                            }
                        }
                        else {
                            newResult.addError(fieldError)
                        }
                    }
                    else {
                        newResult.addError(fieldError)
                    }
                }
                else {
                    newResult.addError((ObjectError) error)
                }
            }
            bindingResult = newResult
        }
        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(obj.getClass())
        if (mc.hasProperty(obj, 'errors') != null && bindingResult != null) {
            ValidationErrors errors = new ValidationErrors(obj)
            errors.addAllErrors(bindingResult)
            mc.setProperty(obj, 'errors', errors)
        }
    }

    @Override
    protected Class<?> getReferencedTypeForCollection(String name, Object target) {
        Class<?> referencedType = super.getReferencedTypeForCollection(name, target)
        if (referencedType == null) {
            PersistentEntity dc = getPersistentEntity(target.getClass())

            if (dc != null) {
                PersistentProperty domainProperty = dc.getPropertyByName(name)
                if (domainProperty != null) {
                    if (domainProperty instanceof Association) {
                        Association association = ((Association) domainProperty)
                        PersistentEntity entity = association.getAssociatedEntity()
                        if (entity != null) {
                            referencedType = entity.getJavaClass()
                        }
                        else if (association.isBasic()) {
                            referencedType = ((Basic) association).getComponentType()
                        }
                    }
                    else if (domainProperty instanceof Simple) {
                        referencedType = domainProperty.getType()
                    }
                }
            }
        }
        referencedType
    }

    @Override
    protected void initializeProperty(Object obj, String propName, Class propertyType, DataBindingSource source) {
        boolean isInitialized = false
        if (source.dataSourceAware) {
            boolean isDomainClass = isDomainClass propertyType
            if (isDomainClass && source.containsProperty(propName)) {
                Object val = source.getPropertyValue(propName)
                Object idValue = getIdentifierValueFrom(val)
                if (idValue != null) {
                    Object persistentInstance = getPersistentInstance(propertyType, idValue)
                    if (persistentInstance != null) {
                        obj[propName] = persistentInstance
                        isInitialized = true
                    }
                }
            }
        }
        if (!isInitialized) {
            super.initializeProperty(obj, propName, propertyType, source)
        }
    }

    protected Object getPersistentInstance(Class<?> type, Object id) {
        try {
            InvokerHelper.invokeStaticMethod(type, 'get', id)
        }
        catch (Exception ignored) {
            return null
        }
    }

    /**
     * @param obj any object
     * @param propName the name of a property on obj
     * @return the Class of the domain class referenced by propName, null if propName does not reference a domain class
     */
    protected Class getDomainClassType(Object obj, String propName) {
        Class domainClassType
        Class objClass = obj.getClass()
        Class propertyType = GrailsClassUtils.getPropertyType(objClass, propName)
        if (propertyType && isDomainClass(propertyType)) {
            domainClassType = propertyType
        }
        domainClassType
    }

    protected boolean isDomainClass(final Class<?> clazz) {
        DomainClassArtefactHandler.isDomainClass(clazz) || AnnotationDomainClassArtefactHandler.isJPADomainClass(clazz)
    }

    protected Object getIdentifierValueFrom(source) {
        Object idValue = null
        if (source instanceof DataBindingSource && ((DataBindingSource) source).hasIdentifier()) {
            idValue = source.getIdentifierValue()
        }
        else if (source instanceof CharSequence) {
            idValue = source
        }
        else if (source instanceof Map && ((Map) source).containsKey('id')) {
            idValue = source['id']
        }
        else if (source instanceof Number) {
            idValue = source.toString()
        }
        if (idValue instanceof GString) {
            idValue = idValue.toString()
        }
        idValue
    }

    @Override
    protected void processProperty(Object obj, MetaProperty metaProperty, Object val,
                                   DataBindingSource source, DataBindingListener listener, Object errors) {
        boolean needsBinding = true

        if (source.dataSourceAware) {
            String propName = metaProperty.name
            Class propertyType = getDomainClassType(obj, metaProperty.name)
            if (propertyType && isDomainClass(propertyType)) {
                Object idValue = getIdentifierValueFrom(val)
                if (idValue != 'null' && idValue != null && idValue != '') {
                    Object persistedInstance = getPersistentInstance(propertyType, idValue)
                    if (persistedInstance != null) {
                        needsBinding = false
                        bindProperty(obj, source, metaProperty, persistedInstance, listener, errors)
                        if (persistedInstance != null) {
                            if (val instanceof Map) {
                                bind(persistedInstance, new SimpleMapDataBindingSource(val), listener)
                            }
                            else if (val instanceof DataBindingSource) {
                                bind(persistedInstance, val, listener)
                            }
                        }
                    }
                }
                else {
                    boolean shouldBindNull = false
                    if (val instanceof DataBindingSource) {
                        // bind null if this binding source does contain an identifier
                        shouldBindNull = ((DataBindingSource) val).hasIdentifier()
                    }
                    else if (val instanceof Map) {
                        // bind null if this Map does contain an id
                        shouldBindNull = ((Map) val).containsKey('id')
                    }
                    else if (idValue instanceof CharSequence) {
                        // bind null if idValue is a CharSequence because it would have
                        // to be 'null' or '' in order for control to be in this else block
                        shouldBindNull = true
                    }
                    if (shouldBindNull) {
                        needsBinding = false
                        bindProperty obj, source, metaProperty, null, listener, errors
                    }
                }
            }
            else if (Collection.isAssignableFrom(metaProperty.type)) {
                Class referencedType = getReferencedTypeForCollection(propName, obj)
                if (referencedType) {
                    List listValue
                    if (val instanceof List) {
                        listValue = (List) val
                    }
                    else if (val instanceof GPathResultMap && ((GPathResultMap) val).size() == 1) {
                        GPathResultMap mapValue = (GPathResultMap) val
                        Object valueInMap = mapValue[mapValue.keySet()[0]]
                        if (valueInMap instanceof List) {
                            listValue = (List) valueInMap
                        }
                        else {
                            listValue = [valueInMap]
                        }
                    }
                    if (listValue != null) {
                        needsBinding = false
                        Collection coll = initializeCollection obj, metaProperty.name, metaProperty.type
                        if (coll instanceof Collection) {
                            coll.clear()
                        }
                        List itemsWhichNeedBinding = []
                        listValue.each { item ->
                            Object persistentInstance
                            if (isDomainClass(referencedType)) {
                                if (item instanceof Map || item instanceof DataBindingSource) {
                                    Object idValue = getIdentifierValueFrom(item)
                                    if (idValue != null) {
                                        persistentInstance = getPersistentInstance(referencedType, idValue)
                                        if (persistentInstance != null) {
                                            DataBindingSource newBindingSource
                                            if (item instanceof DataBindingSource) {
                                                newBindingSource = (DataBindingSource) item
                                            }
                                            else {
                                                newBindingSource = new SimpleMapDataBindingSource((Map) item)
                                            }
                                            bind(persistentInstance, newBindingSource, listener)
                                            itemsWhichNeedBinding << persistentInstance
                                        }
                                    }
                                }
                            }
                            if (persistentInstance == null) {
                                itemsWhichNeedBinding << item
                            }
                        }
                        if (itemsWhichNeedBinding) {
                            for (Object item in itemsWhichNeedBinding) {
                                addElementToCollection(obj, metaProperty.name, metaProperty.type, item, false)
                            }
                        }
                    }
                }
            }
            else if (grailsApplication != null) { // Fixes bidirectional oneToOne binding issue #9308
                PersistentEntity domainClass = getPersistentEntity(obj.getClass())

                if (domainClass != null) {
                    PersistentProperty property = domainClass.getPropertyByName(metaProperty.name)
                    if (property instanceof Association) {
                        Association association = (Association) property
                        if (association.isBidirectional()) {
                            Association otherSide = association.inverseSide
                            if (otherSide instanceof OneToOne) {
                                val[otherSide.name] = obj
                            }
                        }
                    }
                }
            }
        }
        if (needsBinding) {
            super.processProperty obj, metaProperty, val, source, listener, errors
        }
    }

    @Override
    protected void processIndexedProperty(obj, MetaProperty metaProperty,
                                     IndexedPropertyReferenceDescriptor indexedPropertyReferenceDescriptor, val,
                                     DataBindingSource source, DataBindingListener listener, errors) {
        boolean needsBinding = true
        if (source.dataSourceAware) {
            String propName = indexedPropertyReferenceDescriptor.propertyName

            Object idValue = getIdentifierValueFrom(val)
            if (idValue != null && idValue != '') {
                Class propertyType = getDomainClassType(obj, propName)
                Class referencedType = getReferencedTypeForCollection(propName, obj)
                if (referencedType != null && isDomainClass(referencedType)) {
                    needsBinding = false
                    if (Set.isAssignableFrom(metaProperty.type)) {
                        Collection collection = initializeCollection(obj, propName, metaProperty.type)
                        Object instance
                        if (collection != null) {
                            instance = findAlementWithId((Set) collection, idValue)
                        }
                        if (instance == null) {
                            if ('null' != idValue) {
                                instance = getPersistentInstance(referencedType, idValue)
                            }
                            if (instance == null) {
                                String message = "Illegal attempt to update element in [${propName}] Set with id [${idValue}]. " +
                                        'No such record was found.'
                                Exception e = new IllegalArgumentException(message)
                                addBindingError(obj, propName, idValue, e, listener, errors)
                            }
                            else {
                                addElementToCollectionAt(obj, propName, collection,
                                        Integer.parseInt(indexedPropertyReferenceDescriptor.index), instance)
                            }
                        }
                        if (instance != null) {
                            if (val instanceof Map) {
                                bind(instance, new SimpleMapDataBindingSource(val), listener)
                            }
                            else if (val instanceof DataBindingSource) {
                                bind(instance, val, listener)
                            }
                        }
                    }
                    else if (Collection.isAssignableFrom(metaProperty.type)) {
                        Collection collection = initializeCollection(obj, propName, metaProperty.type)
                        int idx = Integer.parseInt(indexedPropertyReferenceDescriptor.index)
                        if (idValue == 'null') {
                            if (idx < collection.size()) {
                                Object element = collection[idx]
                                if (element != null) {
                                    collection.remove(element)
                                }
                            }
                        }
                        else {
                            Object instance = getPersistentInstance(referencedType, idValue)
                            addElementToCollectionAt(obj, propName, collection, idx, instance)
                            if (instance != null) {
                                if (val instanceof Map) {
                                    bind(instance, new SimpleMapDataBindingSource(val), listener)
                                }
                                else if (val instanceof DataBindingSource) {
                                    bind(instance, val, listener)
                                }
                            }
                        }
                    }
                    else if (Map.isAssignableFrom(metaProperty.type)) {
                        Map map = (Map) obj[propName]
                        if (idValue == 'null' || idValue == null || idValue == '') {
                            if (map != null) {
                                map.remove(indexedPropertyReferenceDescriptor.index)
                            }
                        }
                        else {
                            map = initializeMap(obj, propName)
                            Object persistedInstance = getPersistentInstance(referencedType, idValue)
                            if (persistedInstance != null) {
                                if (map.size() < autoGrowCollectionLimit || map.containsKey(indexedPropertyReferenceDescriptor.index)) {
                                    map[indexedPropertyReferenceDescriptor.index] = persistedInstance
                                    if (val instanceof Map) {
                                        bind(persistedInstance, new SimpleMapDataBindingSource(val), listener)
                                    }
                                    else if (val instanceof DataBindingSource) {
                                        bind(persistedInstance, val, listener)
                                    }
                                }
                            }
                            else {
                                map.remove(indexedPropertyReferenceDescriptor.index)
                            }
                        }
                    }
                }
            }
        }
        if (needsBinding) {
            super.processIndexedProperty obj, metaProperty, indexedPropertyReferenceDescriptor, val, source, listener, errors
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private Object findAlementWithId(Set set, idValue) {
        set.find {
            it.id == idValue
        }
    }

    @Override
    protected void addElementToCollectionAt(Object obj, String propertyName, Collection collection, int index, Object val) {
        super.addElementToCollectionAt(obj, propertyName, collection, index, val)

        PersistentEntity domainClass = getPersistentEntity(obj.getClass())
        if (domainClass != null) {
            PersistentProperty property = domainClass.getPropertyByName(propertyName)
            if (property instanceof Association) {
                Association association = (Association) property
                if (association.isBidirectional()) {
                    Association otherSide = association.inverseSide
                    if (otherSide instanceof ManyToOne) {
                        val[otherSide.name] = obj
                    }
                }
            }
        }
    }

    private Map resolveConstrainedProperties(Object object) {
        Map constrainedProperties = null
        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(object.getClass())
        MetaProperty metaProp = mc.getMetaProperty('constraints')
        if (metaProp != null) {
            Object constrainedPropsObj = getMetaPropertyValue(metaProp, object)
            if (constrainedPropsObj instanceof Map) {
                constrainedProperties = (Map) constrainedPropsObj
            }
        }
        constrainedProperties
    }

    private Object getMetaPropertyValue(MetaProperty metaProperty, delegate) {
        if (metaProperty instanceof ThreadManagedMetaBeanProperty) {
            return ((ThreadManagedMetaBeanProperty) metaProperty).getGetter().invoke(delegate, MetaClassHelper.EMPTY_ARRAY)
        }

        metaProperty.getProperty(delegate)
    }

    @Override
    protected void setPropertyValue(obj, DataBindingSource source, MetaProperty metaProperty, propertyValue, DataBindingListener listener) {
        String propName = metaProperty.name
        boolean isSet = false
        PersistentEntity domainClass = getPersistentEntity(obj.getClass())
        if (domainClass != null) {
            PersistentProperty property = domainClass.getPropertyByName(propName)
            if (property != null) {
                if (Collection.isAssignableFrom(property.type)) {
                    if (propertyValue instanceof String) {
                        isSet = addElementToCollection(obj, propName, property, propertyValue, true)
                    }
                    else if (propertyValue instanceof String[]) {
                        if (property instanceof Association) {
                            Association association = (Association) property
                            if (association.associatedEntity != null) {
                                propertyValue.each { String val ->
                                    boolean clearCollection = !isSet
                                    isSet = addElementToCollection(obj, propName, association, val, clearCollection) || isSet
                                }
                            }
                        }
                    }
                }
                PersistentProperty otherSide
                if (property instanceof Association) {
                    if (((Association) property).bidirectional) {
                        otherSide = ((Association) property).inverseSide
                    }
                }
                if (otherSide != null && List.isAssignableFrom(otherSide.getType()) && !property.isNullable()) {
                    DeferredBindingActions.addBindingAction(new Runnable() {

                        void run() {
                            if (obj[propName] != null && otherSide instanceof OneToMany) {
                                Collection collection = GrailsMetaClassUtils.getPropertyIfExists(obj[propName], otherSide.name, Collection)
                                if (collection == null || !collection.contains(obj)) {
                                    String methodName = 'addTo' + GrailsNameUtils.getClassName(otherSide.name)
                                    GrailsMetaClassUtils.invokeMethodIfExists(obj[propName], methodName, [obj] as Object[])
                                }
                            }
                        }

                    })
                }
            }
        }

        if (!isSet) {
            super.setPropertyValue(obj, source, metaProperty, propertyValue, listener)
        }
    }

    @Override
    protected Object preprocessValue(propertyValue) {
        if (propertyValue instanceof CharSequence) {
            String stringValue = propertyValue
            if (trimStrings) {
                stringValue = stringValue.trim()
            }
            if (convertEmptyStringsToNull && stringValue == '') {
                stringValue = null
            }
            return stringValue
        }
        propertyValue
    }

    @Override
    protected boolean addElementToCollection(Object obj, String propName, Class propertyType, Object propertyValue, boolean clearCollection) {
        // Fix for issue #9308 sets propertyValue's otherside value to the owning object for bidirectional manyToOne relationships
        PersistentEntity domainClass = getPersistentEntity(obj.getClass())
        if (domainClass != null) {
            PersistentProperty property = domainClass.getPropertyByName(propName)
            if (property instanceof Association) {
                Association association = (Association) property
                if (association.bidirectional) {
                    Association otherSide = association.inverseSide
                    if (otherSide instanceof ManyToOne) {
                        propertyValue[otherSide.name] = obj
                    }
                }
            }
        }

        Object elementToAdd = propertyValue
        Class referencedType = getReferencedTypeForCollection(propName, obj)
        if (referencedType != null) {
            if (isDomainClass(referencedType)) {
                Object persistentInstance = getPersistentInstance(referencedType, propertyValue)
                if (persistentInstance != null) {
                    elementToAdd = persistentInstance
                }
            }
        }
        super.addElementToCollection(obj, propName, propertyType, elementToAdd, clearCollection)
    }

    protected boolean addElementToCollection(Object obj, String propName, PersistentProperty property, Object propertyValue,
                                             boolean clearCollection) {
        addElementToCollection(obj, propName, property.type, propertyValue, clearCollection)
    }

    void setStructuredBindingEditors(TypedStructuredBindingEditor[] editors) {
        editors.each { TypedStructuredBindingEditor editor ->
            registerStructuredEditor(editor.targetType, editor)
        }
    }

    void setValueConverters(ValueConverter[] converters) {
        converters.each { ValueConverter converter ->
            registerConverter(converter)
        }
    }

    void setFormattedValueConverters(FormattedValueConverter[] converters) {
        converters.each { FormattedValueConverter converter ->
            registerFormattedValueConverter(converter)
        }
    }

    void setDataBindingListeners(DataBindingListener[] listeners) {
        this.listeners.addAll(Arrays.asList(listeners))
    }

    @Override
    protected Object convert(Class typeToConvertTo, Object value) {
        if (value == null) {
            return null
        }
        Object persistentInstance
        if (isDomainClass(typeToConvertTo)) {
            persistentInstance = getPersistentInstance(typeToConvertTo, value)
        }
        persistentInstance ?: super.convert(typeToConvertTo, value)
    }

    void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource
    }

    @Override
    protected String getFormatString(Annotation annotation) {
        assert annotation instanceof BindingFormat
        String code
        if (annotation instanceof BindingFormat) {
            code = ((BindingFormat) annotation).code()
        }
        String formatString
        if (code) {
            Locale locale = getLocale()
            formatString = messageSource.getMessage(code, [] as Object[], locale)
        }

        formatString ?: super.getFormatString(annotation)
    }

    protected Locale getLocale() {
        GrailsWebRequest request = GrailsWebRequest.lookup()
        request?.getLocale() ?: Locale.getDefault()
    }

    private PersistentEntity getPersistentEntity(Class clazz) {
        try {
            return grailsApplication?.mappingContext?.getPersistentEntity(clazz.name)
        }
        catch (GrailsConfigurationException ignored) {
            return null
        }
    }

}
