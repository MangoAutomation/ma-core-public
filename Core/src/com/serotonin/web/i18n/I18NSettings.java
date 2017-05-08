package com.serotonin.web.i18n;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.LocaleResolver;

/**
 * A Spring bean that can be configured in the application context.
 * 
 * @author Matthew Lohbihler
 */
public class I18NSettings implements ServletContextAware {
    private static final Log LOG = LogFactory.getLog(I18NSettings.class);

    public static final String BEAN_NAME = "I18NSettings";

    private LocaleResolver localeResolver;
    private String bundleBaseName = "messages";
    private String resourceBundleDirectory = null;

    private ResourceBundleLoader resourceBundleLoader;

    public void setServletContext(ServletContext ctx) {
        if (LOG.isInfoEnabled())
            LOG.info("Creating resource bundle loader");
        resourceBundleLoader = new ResourceBundleLoader(ctx.getRealPath(resourceBundleDirectory));
    }

    public LocaleResolver getLocaleResolver() {
        return localeResolver;
    }

    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    public String getBundleBaseName() {
        return bundleBaseName;
    }

    public void setBundleBaseName(String bundleBaseName) {
        this.bundleBaseName = bundleBaseName;
    }

    public ResourceBundleLoader getResourceBundleLoader() {
        return resourceBundleLoader;
    }

    public void setResourceBundleDirectory(String resourceBundleDirectory) {
        this.resourceBundleDirectory = resourceBundleDirectory;
    }
}
