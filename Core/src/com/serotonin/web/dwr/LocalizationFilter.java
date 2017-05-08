/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.dwr;

import java.lang.reflect.Method;

import org.directwebremoting.AjaxFilter;
import org.directwebremoting.AjaxFilterChain;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.serotonin.web.i18n.I18NUtils;
import com.serotonin.web.i18n.ResourceBundleLoader;

/**
 * @author Matthew Lohbihler
 */
public class LocalizationFilter implements AjaxFilter {
    private String localeResolverName = "localeResolver";
    private String bundleBaseName = "messages";
    private String resourceBundleDirectory = null;

    private ResourceBundleLoader resourceBundleLoader;

    public String getLocaleResolverName() {
        return localeResolverName;
    }

    public void setLocaleResolverName(String localeResolverName) {
        this.localeResolverName = localeResolverName;
    }

    public String getBundleBaseName() {
        return bundleBaseName;
    }

    public void setBundleBaseName(String bundleBaseName) {
        this.bundleBaseName = bundleBaseName;
    }

    public String getResourceBundleDirectory() {
        return resourceBundleDirectory;
    }

    public void setResourceBundleDirectory(String resourceBundleDirectory) {
        this.resourceBundleDirectory = resourceBundleDirectory;
        // Reset the class loader to null so that doFilter creates it anew
        resourceBundleLoader = null;
    }

    public Object doFilter(Object obj, Method method, Object[] params, AjaxFilterChain chain) throws Exception {
        WebContext webContext = WebContextFactory.get();

        if (resourceBundleDirectory != null && resourceBundleLoader == null)
            resourceBundleLoader = new ResourceBundleLoader(webContext.getServletContext().getRealPath(
                    resourceBundleDirectory));

        I18NUtils.prepareRequest(webContext.getHttpServletRequest(), localeResolverName, bundleBaseName,
                resourceBundleLoader);
        return chain.doFilter(obj, method, params);
    }
}
