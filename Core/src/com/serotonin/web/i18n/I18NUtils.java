/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.i18n;

import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;

import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.LocaleResolver;

/**
 * @author Matthew Lohbihler
 */
public class I18NUtils {
    private static final Utf8ResourceBundleControl CONTROL = new Utf8ResourceBundleControl();

    public static void prepareRequest(HttpServletRequest request, String localeResolverKey, String bundleBaseName) {
        prepareRequest(request, localeResolverKey, bundleBaseName, null);
    }

    public static void prepareRequest(HttpServletRequest request, String localeResolverKey, String bundleBaseName,
            ClassLoader classLoader) {
        WebApplicationContext webApplicationContext = WebApplicationContextUtils
                .getRequiredWebApplicationContext(request.getSession().getServletContext());
        LocaleResolver localeResolver = (LocaleResolver) webApplicationContext.getBean(localeResolverKey);
        prepareRequest(request, localeResolver, bundleBaseName, classLoader);
    }

    public static void prepareRequest(HttpServletRequest request, I18NSettings settings) {
        prepareRequest(request, settings.getLocaleResolver(), settings.getBundleBaseName(),
                settings.getResourceBundleLoader());
    }

    public static void prepareRequest(HttpServletRequest request, LocaleResolver localeResolver, String bundleBaseName,
            ClassLoader classLoader) {
        Locale locale = localeResolver.resolveLocale(request);
        ResourceBundle resourceBundle;
        if (classLoader == null)
            resourceBundle = ResourceBundle.getBundle(bundleBaseName, locale, CONTROL);
        else
            resourceBundle = ResourceBundle.getBundle(bundleBaseName, locale, classLoader, CONTROL);
        LocalizationContext localizationContext = new LocalizationContext(resourceBundle, locale);
        Config.set(request, Config.FMT_LOCALIZATION_CONTEXT, localizationContext);
    }

    public static ResourceBundle getBundle(String localeString, I18NSettings settings) {
        return getBundle(localeString, settings.getBundleBaseName(), settings.getResourceBundleLoader());
    }

    public static ResourceBundle getBundle(String localeString, String bundleBaseName, ClassLoader classLoader) {
        Locale locale = StringUtils.parseLocaleString(localeString);
        return ResourceBundle.getBundle(bundleBaseName, locale, classLoader, CONTROL);
    }

    public static ResourceBundle getBundle(ServletRequest request) {
        LocalizationContext lc = getLocalizationContext(request);

        if (lc != null)
            return lc.getResourceBundle();

        return null;
    }

    public static ResourceBundle getBundle(PageContext pc) {
        return getBundle(pc.getRequest());
    }

    public static LocalizationContext getLocalizationContext(ServletRequest request) {
        return (LocalizationContext) Config.get(request, Config.FMT_LOCALIZATION_CONTEXT);
    }

    public static String getMessage(ServletRequest request, String key) {
        return getMessage(getBundle(request), key);
    }

    public static String getMessage(PageContext pc, String key) {
        return getMessage(getBundle(pc), key);
    }

    public static String getMessage(PageContext pc, LocalizableMessage lm) {
        return lm.getLocalizedMessage(getBundle(pc));
    }

    public static String getMessage(ServletRequest request, LocalizableMessage lm) {
        return lm.getLocalizedMessage(getBundle(request));
    }

    public static String getMessage(ResourceBundle bundle, String key) {
        if (bundle == null)
            return "?x?" + key + "?x?";

        try {
            return bundle.getString(key);
        }
        catch (MissingResourceException e) { /* no op */
        }

        return "???" + key + "(20:" + (bundle.getLocale() == null ? "null" : bundle.getLocale().getLanguage()) + ")???";
    }

    public static void writeLabel(PageContext pc, String key, String fallback) throws IOException {
        if (key != null)
            pc.getOut().write(getMessage(pc, key));
        else
            pc.getOut().write(fallback);
    }
}
