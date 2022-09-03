/*
 * Copyright 2004-2022 the original author or authors.
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
package grails.web.servlet.mvc;

import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import grails.databinding.DataBinder;
import grails.io.IOUtils;
import grails.util.TypeConvertingMap;
import grails.web.mime.MimeType;

import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.web.binding.StructuredDateEditor;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.grails.web.util.WebUtils;

/**
 * A parameter map class that allows mixing of request parameters and controller parameters. If a controller
 * parameter is set with the same name as a request parameter the controller parameter value is retrieved.
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 *
 * @since Oct 24, 2005
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GrailsParameterMap extends TypeConvertingMap implements Cloneable {

    private static final Logger logger = LoggerFactory.getLogger(GrailsParameterMap.class);

    private static final Map<String, String> CACHED_DATE_FORMATS = new ConcurrentHashMap<>();

    private final Map nestedDateMap = new LinkedHashMap();

    private final HttpServletRequest request;

    public static final String REQUEST_BODY_PARSED = "org.codehaus.groovy.grails.web.REQUEST_BODY_PARSED";

    public static final Object[] EMPTY_ARGS = new Object[0];

    /**
     * Does not populate the GrailsParameterMap from the request but instead uses the supplied values.
     *
     * @param values The values to populate with
     * @param request The request object
     */
    public GrailsParameterMap(Map values, HttpServletRequest request) {
        this.request = request;
        wrappedMap.putAll(values);
    }

    /**
     * Creates a GrailsParameterMap populating from the given request object
     * @param request The request object
     */
    public GrailsParameterMap(HttpServletRequest request) {
        this.request = request;
        final Map requestMap = new LinkedHashMap(request.getParameterMap());
        if (requestMap.isEmpty() && ("PUT".equals(request.getMethod()) || "PATCH".equals(request.getMethod())) &&
                request.getAttribute(REQUEST_BODY_PARSED) == null) {

            // attempt manual parse of request body. This is here because some containers don't parse the request body automatically for PUT request
            String contentType = request.getContentType();
            if (MimeType.FORM.equals(new MimeType(contentType))) {
                try {
                    Reader reader = request.getReader();
                    if (reader != null) {
                        String contents = IOUtils.toString(reader);
                        request.setAttribute(REQUEST_BODY_PARSED, true);
                        requestMap.putAll(WebUtils.fromQueryString(contents));
                    }
                }
                catch (Exception e) {
                    logger.error("Error processing form encoded " + request.getMethod() + " request", e);
                }
            }
        }

        if (request instanceof MultipartHttpServletRequest) {
            MultiValueMap<String, MultipartFile> fileMap = ((MultipartHttpServletRequest) request).getMultiFileMap();
            for (Entry<String, List<MultipartFile>> entry : fileMap.entrySet()) {
                List<MultipartFile> value = entry.getValue();
                if (value.size() == 1) {
                    requestMap.put(entry.getKey(), value.get(0));
                }
                else {
                    requestMap.put(entry.getKey(), value);
                }
            }
        }

        updateNestedKeys(requestMap);
    }

    @Override
    public Object clone() {
        if (wrappedMap.isEmpty()) {
            return new GrailsParameterMap(new LinkedHashMap(), this.request);
        }
        else {
            Map clonedMap = new LinkedHashMap(wrappedMap);
            // deep clone nested entries
            for (Object obj : clonedMap.entrySet()) {
                Entry entry = (Entry) obj;
                if (entry.getValue() instanceof GrailsParameterMap) {
                    entry.setValue(((GrailsParameterMap) entry.getValue()).clone());
                }
            }
            return new GrailsParameterMap(clonedMap, this.request);
        }
    }

    public void addParametersFrom(GrailsParameterMap otherMap) {
        wrappedMap.putAll((GrailsParameterMap) otherMap.clone());
    }

    /**
     * @return Returns the request.
     */
    public HttpServletRequest getRequest() {
        return this.request;
    }

    @Override
    public Object get(Object key) {
        // removed test for String key because there
        // should be no limitations on what you shove in or take out
        Object returnValue;
        if (this.nestedDateMap.containsKey(key)) {
            returnValue = this.nestedDateMap.get(key);
        }
        else {
            returnValue = wrappedMap.get(key);
            if (returnValue instanceof String[]) {
                String[] valueArray = (String[]) returnValue;
                if (valueArray.length == 1) {
                    returnValue = valueArray[0];
                }
                else {
                    returnValue = valueArray;
                }
            }
            else if (returnValue == null && (key instanceof Collection)) {
                return DefaultGroovyMethods.subMap(wrappedMap, (Collection) key);
            }
        }
        return returnValue;
    }

    @Override
    public Object put(Object key, Object value) {
        if (value instanceof CharSequence) {
            value = value.toString();
        }
        if (key instanceof CharSequence) {
            key = key.toString();
        }
        this.nestedDateMap.remove(key);
        Object returnValue = wrappedMap.put(key, value);
        if (key instanceof String) {
            String keyString = (String) key;
            if (keyString.contains(".")) {
                processNestedKeys(this, keyString, keyString, wrappedMap);
            }
        }
        return returnValue;
    }

    @Override
    public Object remove(Object key) {
        this.nestedDateMap.remove(key);
        return wrappedMap.remove(key);
    }

    @Override
    public void putAll(Map map) {
        for (Object entryObj : map.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObj;
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Obtains a date for the parameter name using the default format
     *
     * @param name The name of the parameter
     * @return A date or null
     */
    @Override
    public Date getDate(String name) {
        Object returnValue = wrappedMap.get(name);
        if ("date.struct".equals(returnValue)) {
            returnValue = lazyEvaluateDateParam(name);
            this.nestedDateMap.put(name, returnValue);
            return (Date) returnValue;
        }
        Date date = super.getDate(name);
        if (date == null) {
            // try lookup format from messages.properties
            String format = lookupFormat(name);
            if (format != null) {
                return getDate(name, format);
            }
        }
        return date;
    }

    /**
     * Converts this parameter map into a query String. Note that this will flatten nested keys separating them with the
     * . character and URL encode the result
     *
     * @return A query String starting with the ? character
     */
    public String toQueryString() {
        String encoding = this.request.getCharacterEncoding();
        try {
            return WebUtils.toQueryString(this, encoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new ControllerExecutionException("Unable to convert parameter map [" + this +
                    "] to a query string: " + e.getMessage(), e);
        }
    }

    /**
     * @return The identifier in the request
     */
    public Object getIdentifier() {
        return get(GormProperties.IDENTITY);
    }

    private String lookupFormat(String name) {
        String format = CACHED_DATE_FORMATS.get(name);
        if (format == null) {
            GrailsWebRequest webRequest = GrailsWebRequest.lookup(this.request);
            if (webRequest != null) {
                MessageSource messageSource = webRequest.getApplicationContext();
                if (messageSource != null) {
                    format = messageSource.getMessage("date." + name + ".format", EMPTY_ARGS, webRequest.getLocale());
                    CACHED_DATE_FORMATS.put(name, format);
                }
            }
        }
        return format;
    }

    protected void updateNestedKeys(Map keys) {
        for (Object keyObject : keys.keySet()) {
            String key = (String) keyObject;
            Object paramValue = getParameterValue(keys, key);
            wrappedMap.put(key, paramValue);
            processNestedKeys(keys, key, key, wrappedMap);
        }
    }

    private Date lazyEvaluateDateParam(Object key) {
        // parse date structs automatically
        Map dateParams = new LinkedHashMap();
        for (Object entryObj : entrySet()) {
            Map.Entry entry = (Map.Entry) entryObj;
            Object entryKey = entry.getKey();
            if (entryKey instanceof String) {
                String paramName = (String) entryKey;
                final String prefix = key + "_";
                if (paramName.startsWith(prefix)) {
                    dateParams.put(paramName.substring(prefix.length()), entry.getValue());
                }
            }
        }

        DateFormat dateFormat = new SimpleDateFormat(DataBinder.DEFAULT_DATE_FORMAT,
                LocaleContextHolder.getLocale());
        StructuredDateEditor editor = new StructuredDateEditor(dateFormat, true);
        try {
            return (Date) editor.assemble(Date.class, dateParams);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Object getParameterValue(Map requestMap, String key) {
        Object paramValue = requestMap.get(key);
        if (paramValue instanceof String[]) {
            if (((String[]) paramValue).length == 1) {
                paramValue = ((String[]) paramValue)[0];
            }
        }
        return paramValue;
    }

    /*
     * Builds up a multi dimensional hash structure from the parameters so that nested keys such as
     * "book.author.name" can be addressed like params['author'].name
     *
     * This also allows data binding to occur for only a subset of the properties in the parameter map.
     */
    private void processNestedKeys(Map requestMap, String key, String nestedKey, Map nestedLevel) {
        final int nestedIndex = nestedKey.indexOf('.');
        if (nestedIndex == -1) {
            return;
        }

        // We have at least one sub-key, so extract the first element
        // of the nested key as the prfix. In other words, if we have
        // 'nestedKey' == "a.b.c", the prefix is "a".
        String nestedPrefix = nestedKey.substring(0, nestedIndex);
        boolean prefixedByUnderscore = false;

        // Use the same prefix even if it starts with an '_'
        if (nestedPrefix.startsWith("_")) {
            prefixedByUnderscore = true;
            nestedPrefix = nestedPrefix.substring(1);
        }
        // Let's see if we already have a value in the current map for the prefix.
        Object prefixValue = nestedLevel.get(nestedPrefix);
        if (prefixValue == null) {
            // No value. So, since there is at least one sub-key,
            // we create a sub-map for this prefix.

            prefixValue = new GrailsParameterMap(new LinkedHashMap(), this.request);
            nestedLevel.put(nestedPrefix, prefixValue);
        }

        // If the value against the prefix is a map, then we store the sub-keys in that map.
        if (!(prefixValue instanceof Map)) {
            return;
        }

        Map nestedMap = (Map) prefixValue;
        if (nestedIndex < nestedKey.length() - 1) {
            String remainderOfKey = nestedKey.substring(nestedIndex + 1);
            // GRAILS-2486 Cascade the '_' prefix in order to bind checkboxes properly
            if (prefixedByUnderscore) {
                remainderOfKey = '_' + remainderOfKey;
            }
            nestedMap.put(remainderOfKey, getParameterValue(requestMap, key));
            if (!(nestedMap instanceof GrailsParameterMap) && remainderOfKey.contains(".")) {
                processNestedKeys(requestMap, remainderOfKey, remainderOfKey, nestedMap);
            }
        }
    }

}
