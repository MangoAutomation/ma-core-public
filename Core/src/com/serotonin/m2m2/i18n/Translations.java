/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.i18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Translations {
    private static final Logger LOG = LoggerFactory.getLogger(Translations.class);

    private static final String BASE_NAME = "i18n";
    private static final Map<Locale, Translations> TRANSLATIONS_CACHE = new ConcurrentHashMap<Locale, Translations>();
    //Class loader that contains all of the translation property files (including ones from modules)
    private static final AtomicReference<ClassLoader> ROOT_CLASSLOADER = new AtomicReference<>();

    public static Translations getTranslations() {
        return getTranslations(Locale.getDefault());
    }

    public static Translations getTranslations(Locale locale) {
        Translations cachedTranslations;
        if ((cachedTranslations = TRANSLATIONS_CACHE.get(locale)) != null) {
            return cachedTranslations;
        }

        Locale parentLocale = getParentLocale(locale);
        final Translations parentTranslations = parentLocale != null ? getTranslations(parentLocale) : null;

        return TRANSLATIONS_CACHE.computeIfAbsent(locale, (l) -> {
            try {
                return new Translations(l, parentTranslations);
            } catch (IOException e) {
                LOG.warn("Error while loading translations", e);
                return null;
            }
        });
    }

    public static Locale getParentLocale(Locale locale) {
        if (!StringUtils.isBlank(locale.getLanguage())) {
            if (!StringUtils.isBlank(locale.getCountry())) {
                if (!StringUtils.isBlank(locale.getVariant())) {
                    return new Locale(locale.getLanguage(), locale.getCountry());
                }
                return new Locale(locale.getLanguage());
            }
            return new Locale("");
        }
        return null;
    }

    public static void setRootClassLoader(ClassLoader root) {
        if (ROOT_CLASSLOADER.compareAndSet(null, root)) {
            // clear the cache so we get all the translations after the modules have been loaded
            TRANSLATIONS_CACHE.clear();
        }
    }

    public static void clearCache() {
        TRANSLATIONS_CACHE.clear();
    }

    private final String name;
    private final Translations parent;
    private final Map<String, String> pairs = new HashMap<String, String>();
    private final Map<String, Map<String, String>> namespaces = new HashMap<String, Map<String, String>>();

    private Translations(Locale locale, Translations parent) throws IOException {
        this.parent = parent;

        StringBuilder sb = new StringBuilder();
        sb.append(BASE_NAME);

        if (!StringUtils.isBlank(locale.getLanguage())) {
            sb.append('_').append(locale.getLanguage());
            if (!StringUtils.isBlank(locale.getCountry())) {
                sb.append('_').append(locale.getCountry());
                if (!StringUtils.isBlank(locale.getVariant())) {
                    sb.append('_').append(locale.getVariant());
                }
            }
        }

        name = sb.toString();

        ClassLoader classLoader = ROOT_CLASSLOADER.get();

        // use the current thread's class loader if the root class loader has not been set yet
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        List<URL> urls = Collections.list(classLoader.getResources(name + ".properties"));
        // iterate urls in reverse so that those on the classpath first take priority
        Collections.reverse(urls);
        for (URL url : urls) {
            Properties props = new Properties();

            Charset charset = StandardCharsets.UTF_8;
            try (BOMInputStream is = new BOMInputStream(url.openStream(), false,
                    ByteOrderMark.UTF_8,
                    ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE,
                    ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE)) {

                String charsetName = is.getBOMCharsetName();
                if (charsetName != null) {
                    charset = Charset.forName(charsetName);
                }

                try (Reader r = new BufferedReader(new InputStreamReader(is, charset))) {
                    props.load(r);
                }
            }

            for (Object keyo : props.keySet()) {
                String key = (String) keyo;
                String translation = props.getProperty(key);
                String namespace = StringUtils.substringBefore(key, ".");
                Map<String, String> namespacedTranslations = namespaces.get(namespace);
                if (namespacedTranslations == null) {
                    namespacedTranslations = new HashMap<String, String>();
                    namespaces.put(namespace, namespacedTranslations);
                    if(LOG.isDebugEnabled())
                        LOG.debug("Adding i18n namespace " + namespace);
                }

                pairs.put(key, translation);
                namespacedTranslations.put(key, translation);
            }
        }
    }

    public String getName() {
        return name;
    }

    public String translate(String key) {
        String t = translateImpl(key);

        if (t == null)
            t = "???" + key + "(" + name + ")???";

        return t;
    }

    public String translateAllowNull(String key) {
        return translateImpl(key);
    }

    protected String translateImpl(String key) {
        String t = pairs.get(key);
        if (t == null && parent != null)
            t = parent.translateImpl(key);
        return t;
    }

    public Map<String, Map<String, String>> asMap() {
        return asMap(null);
    }

    public Map<String, Map<String, String>> asMap(String namespace) {
        Map<String, Map<String, String>> map;

        if (parent == null) {
            map = new HashMap<String, Map<String, String>>();
        }
        else {
            map = parent.asMap(namespace);
        }

        Map<String, String> translations;
        if (namespace == null) {
            translations = pairs;
        }
        else {
            translations = namespaces.get(namespace);
        }

        if (translations == null) {
            translations = new HashMap<String, String>();
        }

        map.put(languageTag(), translations);

        return map;
    }

    private String languageTag() {
        if (name.equals(BASE_NAME))
            return "root";

        String language;
        if (name.startsWith(BASE_NAME + "_"))
            language = StringUtils.substringAfter(name, BASE_NAME + "_");
        else
            language = name;

        return language.replace("_", "-");
    }
}
