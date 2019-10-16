/*
   Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
   @author Terry Packer
 */
package com.infiniteautomation.mango.webapp.filters;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * Filter to apply cache control headers for all requests
 *
 * This filter is configured via web.xml for now
 *
 * @author Terry Packer
 */
@Component
@WebFilter(
        filterName = MangoCacheControlHeaderFilter.NAME,
        asyncSupported = true,
        urlPatterns = {"/*"},
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC})
@Order(FilterOrder.CORE_FILTERS)
public class MangoCacheControlHeaderFilter implements Filter {
    public static final String NAME = "cacheControlFilter";

    public static final String CACHE_OVERRIDE_SETTING = "CACHE_OVERRIDE_SETTING";
    public static final String MAX_AGE_TEMPLATE = "max-age=%d, must-revalidate";
    public static final Pattern VERSION_QUERY_PARAMETER = Pattern.compile("(?:^|&)v=");

    final RequestMatcher restMatcher;
    final RequestMatcher resourcesMatcher;
    final RequestMatcher getMatcher;
    final RequestMatcher uiServletMatcher;

    final String defaultNoCache;
    final Environment env;

    @Autowired
    private MangoCacheControlHeaderFilter(Environment env) {
        this.env = env;

        getMatcher = new AntPathRequestMatcher("/**", HttpMethod.GET.name());
        restMatcher = new AntPathRequestMatcher("/rest/**", HttpMethod.GET.name());
        resourcesMatcher = new OrRequestMatcher(
                new AntPathRequestMatcher("/resources/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/modules/*/web/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/images/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/audio/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/dwr/**", HttpMethod.GET.name()));

        uiServletMatcher = new AntPathRequestMatcher("/ui/**", HttpMethod.GET.name());
        defaultNoCache = CacheControl.noStore().getHeaderValue();
    }

    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        if(uiServletMatcher.matches(request)) {
            return true;
        }else {
            return false;
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws ServletException, IOException {

        if (!(servletRequest instanceof HttpServletRequest) || !(servletResponse instanceof HttpServletResponse)) {
            throw new ServletException("MangoCacheControlHeaderFilter just supports HTTP requests");
        }
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if(shouldNotFilter(request)) {
            // Proceed without invoking this filter...
            filterChain.doFilter(request, response);
        }else {
            String header = null;

            //Detect the type of settings to use, allow overriding the matching
            CacheControlLevel override = null;
            if(request.getAttribute(CACHE_OVERRIDE_SETTING) != null) {
                Object o = request.getAttribute(CACHE_OVERRIDE_SETTING);
                if(o instanceof CacheControlLevel) {
                    override = (CacheControlLevel)o;
                }
            }

            if(override == null) {
                if (restMatcher.matches(request)) {
                    override = CacheControlLevel.REST;
                }else if (resourcesMatcher.matches(request)) {
                    String queryString = request.getQueryString();
                    if (queryString != null && VERSION_QUERY_PARAMETER.matcher(queryString).find()) {
                        override = CacheControlLevel.VERSIONED_RESOURCE;
                    }else {
                        override = CacheControlLevel.RESOURCE;
                    }
                }else if (getMatcher.matches(request)) {
                    override = CacheControlLevel.DEFAULT;
                }else {
                    override = CacheControlLevel.NON_GET;
                }
            }
            switch(override) {
                case REST:
                    if(this.env.getProperty("web.cache.noStore.rest", Boolean.class, true)) {
                        header = defaultNoCache;
                    }else {
                        header = CacheControl.maxAge(this.env.getProperty("web.cache.maxAge.rest", Long.class, 0L), TimeUnit.SECONDS).getHeaderValue();
                    }
                    break;
                case VERSIONED_RESOURCE:
                    if(this.env.getProperty("web.cache.noStore.resources", Boolean.class, false)) {
                        header = defaultNoCache;
                    }else {
                        header = CacheControl.maxAge(this.env.getProperty("web.cache.maxAge.versionedResources", Long.class, 31536000L), TimeUnit.SECONDS).getHeaderValue();
                    }
                    break;
                case RESOURCE:
                    if(this.env.getProperty("web.cache.noStore.resources", Boolean.class, false)) {
                        header = defaultNoCache;
                    }else {
                        header = CacheControl.maxAge(this.env.getProperty("web.cache.maxAge.resources", Long.class, 86400L), TimeUnit.SECONDS).getHeaderValue();
                    }
                    break;
                case NON_GET:
                    header = defaultNoCache;
                    break;
                case DO_NOT_MODIFY:
                    break;
                case DEFAULT:
                default:
                    if(this.env.getProperty("web.cache.noStore", Boolean.class, false)) {
                        header = defaultNoCache;
                    }else {
                        header = CacheControl.maxAge(this.env.getProperty("web.cache.maxAge", Long.class, 0L), TimeUnit.SECONDS).getHeaderValue();
                    }
                    break;
            }
            response.setHeader(HttpHeaders.CACHE_CONTROL, header);
            filterChain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }

    public static enum CacheControlLevel {

        /**
         * Settings for all unmatched requests
         *  no-store
         */
        DEFAULT,

        /**
         * REST requests can be set by env properties
         *  web.cache.noStore.rest=true -> no-store
         *  web.cache.maxAge.rest=0 -> max-age=0, must-revalidate
         * Defaults: not-store, must-revalidate
         */
        REST,

        /**
         * Resource requests can be set by env properties
         *  web.cache.noStore.resources=true -> no-store
         *  web.cache.maxAge.resources=86400 -> max-age=86400, must-revalidate
         * Defaults: max-age=86400, must-revalidate
         */
        RESOURCE,

        /**
         * Versioned resource requests can be set by env properties
         *  web.cache.noStore.versionedResources=true -> no-store
         *  web.cache.maxAge.versionedResources=31536000 -> max-age=31536000, must-revalidate
         * Defaults: max-age=31536000, must-revalidate
         */
        VERSIONED_RESOURCE,

        /**
         * Non GET request will not be cached
         * Default: no-store
         */
        NON_GET,

        /**
         * Don't modify the cache control header, useful if the value is already set by custom code elsewhere
         */
        DO_NOT_MODIFY;

    }
}
