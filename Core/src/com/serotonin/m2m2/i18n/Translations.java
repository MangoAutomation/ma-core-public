/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.i18n;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Translations {
    private static final Log LOG = LogFactory.getLog(Translations.class);

    private static final String BASE_NAME = "i18n";
    private static final Map<Locale, Translations> TRANSLATIONS_CACHE = new ConcurrentHashMap<Locale, Translations>();

    static final Charset UTF8 = Charset.forName("UTF-8");

    public static Translations getTranslations() {
        return getTranslations(Locale.getDefault());
    }

    public static Translations getTranslations(Locale locale) {
        Translations translations = TRANSLATIONS_CACHE.get(locale);

        if (translations == null) {
            synchronized (TRANSLATIONS_CACHE) {
                translations = TRANSLATIONS_CACHE.get(locale);
                if (translations == null) {
                    try {
                        translations = new Translations(locale);
                    }
                    catch (IOException e) {
                        LOG.warn("Error while loading translations", e);
                    }
                    TRANSLATIONS_CACHE.put(locale, translations);
                }
            }
        }

        return translations;
    }

    public static void clearCache() {
        TRANSLATIONS_CACHE.clear();
    }

    private final String name;
    private final Translations parent;
    private final Map<String, String> pairs = new HashMap<String, String>();

    private Translations(Locale locale) throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append(BASE_NAME);

        Locale parentLocale = null;

        if (!StringUtils.isBlank(locale.getLanguage())) {
            sb.append('_').append(locale.getLanguage());
            if (!StringUtils.isBlank(locale.getCountry())) {
                sb.append('_').append(locale.getCountry());
                if (!StringUtils.isBlank(locale.getVariant())) {
                    sb.append('_').append(locale.getVariant());
                    parentLocale = new Locale(locale.getLanguage(), locale.getCountry());
                }
                else
                    parentLocale = new Locale(locale.getLanguage());
            }
            else
                parentLocale = new Locale("");
        }

        name = sb.toString();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> urls = cl.getResources(name + ".properties");

        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();

            Properties props = new Properties();
            InputStreamReader r = new InputStreamReader(url.openStream(), UTF8);
            props.load(r);
            r.close();

            for (Object keyo : props.keySet()) {
                String key = (String) keyo;
                pairs.put(key, props.getProperty(key));
            }
        }

        if (parentLocale != null)
            parent = Translations.getTranslations(parentLocale);
        else
            parent = null;
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
}
