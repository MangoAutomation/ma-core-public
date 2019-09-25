/*
   Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
   @author Terry Packer
 */
package com.serotonin.m2m2.web.filter;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.serotonin.m2m2.Common;

/**
 * Filter to apply cache control headers for all requests
 * 
 * This filter is configured via web.xml for now
 * 
 * @author Terry Packer
 */
public class MangoCacheControlHeaderFilter implements Filter {

    public static final String CACHE_OVERRIDE_SETTING = "CACHE_OVERRIDE_SETTING";
    public static final String CUSTOM_CONTROL_LEVEL = "CUSTOM_CONTROL_LEVEL";
    public static final String MAX_AGE_TEMPLATE = "max-age=%d, must-revalidate";
    public static final Pattern VERSION_QUERY_PARAMETER = Pattern.compile("(?:^|&)v=");
    
    final RequestMatcher restMatcher;
    final RequestMatcher resourcesMatcher;
    final RequestMatcher getMatcher;
    final RequestMatcher uiServletMatcher; 
    
    final CacheControl defaultNoCache;

    public MangoCacheControlHeaderFilter() {
        getMatcher = new AntPathRequestMatcher("/**", HttpMethod.GET.name());
        restMatcher = new AntPathRequestMatcher("/rest/**", HttpMethod.GET.name());
        resourcesMatcher = new OrRequestMatcher(
                new AntPathRequestMatcher("/resources/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/modules/*/web/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/images/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/audio/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/dwr/**", HttpMethod.GET.name()));

        uiServletMatcher = new AntPathRequestMatcher("/ui/**", HttpMethod.GET.name());
        //TODO This class is not designed to use no-store and no-cache via builder, do we really want to use those together here along with the max-age header?
        defaultNoCache = CacheControl.noStore().mustRevalidate();
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
            }else {
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
                    override = CacheControlLevel.WEB;
                }else {
                    override = CacheControlLevel.DEFAULT;
                }
            }
            
            switch(override) {
                case REST:
                    if(Common.envProps.getBoolean("web.cache.noStore.rest", true)) {
                        header = defaultNoCache.getHeaderValue();
                    }else {
                        header = CacheControl.maxAge(Common.envProps.getLong("web.cache.maxAge.rest", 0L), TimeUnit.SECONDS).mustRevalidate().getHeaderValue();
                    }
                    break;
                case WEB:
                    if(Common.envProps.getBoolean("web.cache.noStore", false)) {
                        header = defaultNoCache.getHeaderValue();
                    }else {
                        header = CacheControl.maxAge(Common.envProps.getLong("web.cache.maxAge", 0L), TimeUnit.SECONDS).mustRevalidate().getHeaderValue();
                    }
                    break;
                case VERSIONED_RESOURCE:
                    if(Common.envProps.getBoolean("web.cache.noStore.resources", false)) {
                        header = defaultNoCache.getHeaderValue();
                    }else {
                        header = CacheControl.maxAge(Common.envProps.getLong("web.cache.maxAge.versionedResources", 31536000L), TimeUnit.SECONDS).mustRevalidate().getHeaderValue();
                    }
                    break;
                case RESOURCE:
                    if(Common.envProps.getBoolean("web.cache.noStore.resources", false)) {
                        header = defaultNoCache.getHeaderValue();
                    }else {
                        header = CacheControl.maxAge(Common.envProps.getLong("web.cache.maxAge.resources", 86400L), TimeUnit.SECONDS).mustRevalidate().getHeaderValue();
                    }
                    break;
                case CUSTOM:
                    header = (String)request.getAttribute(CUSTOM_CONTROL_LEVEL);
                    Objects.requireNonNull(header, "Must supply CUSTOM_CONTROL_LEVEL attribute on request");
                    break;
                case DEFAULT:
               default:
                   header = defaultNoCache.getHeaderValue();
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
         *  no-store, must-revalidate
         */
        DEFAULT,
        
        /**
         * REST requests can be set by env properties
         *  web.cache.noStore.rest=true -> no-store, must-revalidate
         *  web.cache.maxAge.rest=0 -> max-age=0, must-revalidate
         * Defaults: not-store, must-revalidate
         */
        REST,
        
        /**
         * Web requests can be set by env properties
         *  web.cache.noStore=true -> no-store, must-revalidate
         *  web.cache.maxAge=0 -> max-age=0, must-revalidate
         * Defaults: max-age=0, must-revalidate
         */
        WEB,
        
        /**
         * Resource requests can be set by env properties
         *  web.cache.noStore.resources=true -> no-store, must-revalidate
         *  web.cache.maxAge.resources=86400 -> max-age=86400, must-revalidate
         * Defaults: max-age=86400, must-revalidate
         */
        RESOURCE,
        
        /**
         * Versioned resource requests can be set by env properties
         *  web.cache.noStore.versionedResources=true -> no-store, must-revalidate
         *  web.cache.maxAge.versionedResources=31536000 -> max-age=31536000, must-revalidate
         * Defaults: max-age=31536000, must-revalidate
         */
        VERSIONED_RESOURCE,
        
        /**
         * Allows to set the contents of the Cache-Control header by placing a String
         *  into the CUSTOM_CONTROL_LEVEL attribute of the response
         */
        CUSTOM;
       
    }
}
