/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
    public static final Pattern VERSION_QUERY_PARAMETER = Pattern.compile("(?:^|&)v=");

    final RequestMatcher restMatcher;
    final RequestMatcher resourcesMatcher;
    final RequestMatcher getMatcher;

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

        defaultNoCache = CacheControl.noStore().getHeaderValue();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws ServletException, IOException {

        if (!(servletRequest instanceof HttpServletRequest) || !(servletResponse instanceof HttpServletResponse)) {
            throw new ServletException("MangoCacheControlHeaderFilter just supports HTTP requests");
        }
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String header = null;

        //Detect the type of settings to use, allow overriding the matching
        CacheControlLevel level = null;
        if (request.getAttribute(CACHE_OVERRIDE_SETTING) != null) {
            Object o = request.getAttribute(CACHE_OVERRIDE_SETTING);
            if(o instanceof CacheControlLevel) {
                level = (CacheControlLevel)o;
            }
        }

        if (level == null) {
            if (restMatcher.matches(request)) {
                level = CacheControlLevel.REST;
            }else if (resourcesMatcher.matches(request)) {
                String queryString = request.getQueryString();
                if (queryString != null && VERSION_QUERY_PARAMETER.matcher(queryString).find()) {
                    level = CacheControlLevel.VERSIONED_RESOURCE;
                }else {
                    level = CacheControlLevel.RESOURCE;
                }
            }else if (getMatcher.matches(request)) {
                level = CacheControlLevel.DEFAULT;
            }else {
                level = CacheControlLevel.NON_GET;
            }
        }

        switch(level) {
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

        if (header != null) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, header);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }

    public static enum CacheControlLevel {

        /**
         * Unmatched request settings
         *  web.cache.noStore=true -> no-store
         *  web.cache.noStore=false -> max-age={web.cache.maxAge}
         * env property defaults: max-age=0
         */
        DEFAULT,

        /**
         * REST request settings
         *  web.cache.noStore.rest=true -> no-store
         *  web.cache.noStore.rest=false -> max-age={web.cache.maxAge.rest}
         * env property defaults: no-store
         */
        REST,

        /**
         * Resource request settings
         *  web.cache.noStore.resources=true -> no-store
         *  web.cache.noStore.resources=false -> max-age={web.cache.maxAge.resources}
         * env property defaults: max-age=86400
         */
        RESOURCE,

        /**
         * Versioned resource request settings
         *  web.cache.noStore.versionedResources=true -> no-store
         *  web.cache.noStore.versionedResources=false -> max-age={web.cache.maxAge.versionedResources}
         * env property defaults: max-age=31536000
         */
        VERSIONED_RESOURCE,

        /**
         * Non GET request will not be cached
         * results in using: no-store
         */
        NON_GET,

        /**
         * Don't modify the cache control header, useful if the value is already set by custom code elsewhere
         */
        DO_NOT_MODIFY;

    }
}
