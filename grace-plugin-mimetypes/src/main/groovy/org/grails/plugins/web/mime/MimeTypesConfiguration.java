/*
 * Copyright 2004-2023 the original author or authors.
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
package org.grails.plugins.web.mime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import grails.config.Config;
import grails.config.Settings;
import grails.core.GrailsApplication;
import grails.web.mime.MimeType;
import grails.web.mime.MimeTypeProvider;
import grails.web.mime.MimeTypeResolver;
import grails.web.mime.MimeUtility;

import org.grails.web.mime.DefaultMimeTypeResolver;
import org.grails.web.mime.DefaultMimeUtility;

/**
 * Configuration for Codecs
 *
 * @author graemerocher
 * @author Michael Yan
 * @since 4.0
 */
@AutoConfiguration
@AutoConfigureOrder
public class MimeTypesConfiguration {

    private final GrailsApplication grailsApplication;

    private final List<MimeTypeProvider> mimeTypeProviders;

    public MimeTypesConfiguration(ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<MimeTypeProvider> mimeTypeProviders) {
        this.grailsApplication = grailsApplication.getIfAvailable();
        this.mimeTypeProviders = mimeTypeProviders.orderedStream().collect(Collectors.toList());
    }

    @Bean
    public MimeTypesHolder mimeTypesHolder(MimeType[] mimeTypes) {
        return new MimeTypesHolder(mimeTypes);
    }

    @Bean
    public MimeType[] mimeTypes() {
        Config config = this.grailsApplication.getConfig();
        Map<CharSequence, Object> mimeConfig = getMimeConfig(config);
        MimeType[] mimeTypes;
        if (mimeConfig == null || mimeConfig.isEmpty()) {
            mimeTypes = MimeType.createDefaults();
        }
        else {
            List<MimeType> mimes = new ArrayList<>();
            for (Map.Entry<CharSequence, Object> entry : mimeConfig.entrySet()) {
                String key = entry.getKey().toString();
                Object v = entry.getValue();
                if (v instanceof List) {
                    List list = (List) v;
                    for (Object i : list) {
                        mimes.add(new MimeType(i.toString(), key));
                    }
                }
                else {
                    mimes.add(new MimeType(v.toString(), key));
                }

            }

            processProviders(mimes, this.mimeTypeProviders);
            mimeTypes = mimes.toArray(new MimeType[0]);
        }
        return mimeTypes;
    }

    @Bean
    @Primary
    public MimeUtility grailsMimeUtility(ObjectProvider<MimeTypesHolder> mimeTypesHolder) {
        return new DefaultMimeUtility(mimeTypesHolder.getIfAvailable().getMimeTypes());
    }

    @Bean
    @Primary
    public MimeTypeResolver mimeTypeResolver() {
        return new DefaultMimeTypeResolver();
    }

    @SuppressWarnings("unchecked")
    protected Map<CharSequence, Object> getMimeConfig(Config config) {
        return config.getProperty(Settings.MIME_TYPES, Map.class);
    }

    private void processProviders(List<MimeType> mimes, Iterable<MimeTypeProvider> mimeTypeProviders) {
        for (MimeTypeProvider mimeTypeProvider : mimeTypeProviders) {
            for (MimeType mimeType : mimeTypeProvider.getMimeTypes()) {
                if (!mimes.contains(mimeType)) {
                    mimes.add(mimeType);
                }
            }
        }
    }

}
