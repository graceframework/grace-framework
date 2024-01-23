/*
 * Copyright 2012-2022 the original author or authors.
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
package grails.util;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.support.RequestDataValueProcessor;

public class MockRequestDataValueProcessor implements RequestDataValueProcessor {

    public Map<String, String> getExtraHiddenFields(HttpServletRequest request) {
        Map<String, String> extraHiddenFields = new HashMap<>();
        extraHiddenFields.put("requestDataValueProcessorHiddenName", "hiddenValue");
        return extraHiddenFields;
    }

    public String processAction(HttpServletRequest request, String action, String httpMethod) {
        if (action.contains("requestDataValueProcessorParamName=paramValue")) {
            action = action.replace("?requestDataValueProcessorParamName=paramValue&", "?");
            action = action.replace("?requestDataValueProcessorParamName=paramValue", "");
            action = action.replace("&requestDataValueProcessorParamName=paramValue", "");
        }
        return action;
    }

    public String processFormFieldValue(HttpServletRequest request, String name, String value, String type) {
        return value + "_PROCESSED_";
    }

    public String processUrl(HttpServletRequest request, String url) {
        String toAppend;
        if (url.contains("?")) {
            toAppend = "&requestDataValueProcessorParamName=paramValue";
        }
        else {
            toAppend = "?requestDataValueProcessorParamName=paramValue";
        }
        if (url.contains("#")) {
            url = url.replace("#", toAppend + "#");
        }
        else {
            url = url + toAppend;
        }
        return url;
    }

}
