/*
   Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
   @author Terry Packer
 */
package com.serotonin.m2m2.web.filter;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import com.serotonin.m2m2.Common;

/**
 * @author Terry Packer
 * TODO Mango 3.7 Remove Filter
 */
public class MangoShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {

    public static final String MAX_AGE_TEMPLATE = "max-age=%d, must-revalidate";
    public static final String NO_STORE = "no-cache, no-store, max-age=0, must-revalidate";
    private static final String DIRECTIVE_NO_STORE = "no-store";
    public static final Pattern VERSION_QUERY_PARAMETER = Pattern.compile("(?:^|&)v=");

    final RequestMatcher restMatcher;
    final RequestMatcher resourcesMatcher;
    final RequestMatcher getMatcher;
    final String defaultHeader;
    final String restHeader;
    final String resourcesHeader;
    final String versionedResourcesHeader;

    public MangoShallowEtagHeaderFilter() {
        getMatcher = new AntPathRequestMatcher("/**", HttpMethod.GET.name());
        restMatcher = new AntPathRequestMatcher("/rest/**", HttpMethod.GET.name());

        resourcesMatcher = new OrRequestMatcher(
                new AntPathRequestMatcher("/resources/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/modules/*/web/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/images/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/audio/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/dwr/**", HttpMethod.GET.name()));

        boolean noStore = Common.envProps.getBoolean("web.cache.noStore", false);
        boolean restNoStore = Common.envProps.getBoolean("web.cache.noStore.rest", true);
        boolean resourcesNoStore = Common.envProps.getBoolean("web.cache.noStore.resources", false);

        defaultHeader = noStore ? NO_STORE : String.format(MAX_AGE_TEMPLATE, Common.envProps.getLong("web.cache.maxAge", 0L));
        restHeader = restNoStore ? NO_STORE : String.format(MAX_AGE_TEMPLATE, Common.envProps.getLong("web.cache.maxAge.rest", 0L));
        resourcesHeader = resourcesNoStore ? NO_STORE : String.format(MAX_AGE_TEMPLATE, Common.envProps.getLong("web.cache.maxAge.resources", 86400L));
        versionedResourcesHeader = resourcesNoStore ? NO_STORE : String.format(MAX_AGE_TEMPLATE, Common.envProps.getLong("web.cache.maxAge.versionedResources", 31536000L));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = null;

        if (restMatcher.matches(request)) {
            header = restHeader;
        } else if (resourcesMatcher.matches(request)) {
            String queryString = request.getQueryString();
            if (queryString != null && VERSION_QUERY_PARAMETER.matcher(queryString).find()) {
                header = versionedResourcesHeader;
            } else {
                header = resourcesHeader;
            }
        } else if (getMatcher.matches(request)) {
            header = defaultHeader;
        } else {
            header = NO_STORE;
        }

        response.addHeader(HttpHeaders.CACHE_CONTROL, header);
        
        //Disable content caching for non GET or no-store requests 
        if(!HttpMethod.GET.matches(request.getMethod()) || header.contains(DIRECTIVE_NO_STORE)) {
            disableContentCaching(request);
        }
        
        super.doFilterInternal(request, response, filterChain);
    }
}
