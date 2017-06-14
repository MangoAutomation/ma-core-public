/*
   Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
   @author Terry Packer
 */
package com.serotonin.m2m2.web.filter;

import java.io.IOException;

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
 *
 */
public class MangoShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {

    static final String MAX_AGE_TEMPLATE = "max-age=%d, must-revalidate";
    static final String NO_STORE = "no-cache, no-store, max-age=0, must-revalidate";

	final RequestMatcher restMatcher;
    final RequestMatcher resourcesMatcher;
    final RequestMatcher getMatcher;
	final String defaultHeader;
    final String restHeader;
    final String resourcesHeader;
	
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
	    
	    defaultHeader = noStore ? NO_STORE : String.format(MAX_AGE_TEMPLATE, Common.envProps.getLong("web.cache.maxAge", 3600L));
        restHeader = restNoStore ? NO_STORE : String.format(MAX_AGE_TEMPLATE, Common.envProps.getLong("web.cache.maxAge.rest", 0L));
        resourcesHeader = resourcesNoStore ? NO_STORE : String.format(MAX_AGE_TEMPLATE, Common.envProps.getLong("web.cache.maxAge.resources", 86400L));
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.web.filter.ShallowEtagHeaderFilter#doFilterInternal(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
	    
	    String header = null;
	    
	    if (restMatcher.matches(request)) {
	        header = restHeader;
	    } else if (resourcesMatcher.matches(request)) {
            header = resourcesHeader;
	    } else if (getMatcher.matches(request)) {
	        header = defaultHeader;
	    } else {
	        header = NO_STORE;
	    }
	    
	    response.addHeader(HttpHeaders.CACHE_CONTROL, header);
	    
        super.doFilterInternal(request, response, filterChain);
	}
}
