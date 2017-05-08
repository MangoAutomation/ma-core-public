/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.ServletConfig;

import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.context.ServletConfigAware;

/**
 * @author Matthew Lohbihler
 */
public class Utf8ResourceBundleMessageSource extends ResourceBundleMessageSource implements ServletConfigAware {
    private static final Utf8ResourceBundleControl CONTROL = new Utf8ResourceBundleControl();
    private String resourceBundleDirectory = null;
    private ResourceBundleLoader resourceBundleLoader;

    public String getResourceBundleDirectory() {
        return resourceBundleDirectory;
    }

    public void setResourceBundleDirectory(String resourceBundleDirectory) {
        this.resourceBundleDirectory = resourceBundleDirectory;
    }

    @Override
    protected ResourceBundle doGetBundle(String basename, Locale locale) throws MissingResourceException {
        return ResourceBundle.getBundle(basename, locale, getBundleClassLoader(), CONTROL);
    }

    @Override
    protected ClassLoader getBundleClassLoader() {
        if (resourceBundleLoader != null)
            return resourceBundleLoader;
        return super.getBundleClassLoader();
    }

    public void setServletConfig(ServletConfig servletConfig) {
        if (resourceBundleDirectory != null)
            resourceBundleLoader = new ResourceBundleLoader(servletConfig.getServletContext().getRealPath(
                    resourceBundleDirectory));
    }
}
