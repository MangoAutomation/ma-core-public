/*
   Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
   @author Terry Packer
 */
package com.serotonin.m2m2.web.filter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.DigestUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Constants;

/**
 * Filter to apply ETag to static resources and cache control headers for all requests
 * 
 * Configured via web.xml for now
 * 
 * @author Terry Packer
 */
public class MangoCacheControlHeaderFilter extends OncePerRequestFilter {

    public static final String MAX_AGE_TEMPLATE = "max-age=%d, must-revalidate";
    public static final String NO_STORE = "no-cache, no-store, max-age=0, must-revalidate";
    private static final String DIRECTIVE_NO_STORE = "no-store";
    public static final Pattern VERSION_QUERY_PARAMETER = Pattern.compile("(?:^|&)v=");
    private static final String HEADER_ETAG = "ETag";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    
    final RequestMatcher restMatcher;
    final RequestMatcher resourcesMatcher;
    final RequestMatcher getMatcher;
    //So we don't filter on the UI servlet requests we will filter on the forwarded request
    final RequestMatcher uiServletMatcher; 
    final String defaultHeader;
    final String restHeader;
    final String resourcesHeader;
    final String versionedResourcesHeader;
    
    /**
     * Set whether the ETag value written to the response should be weak, as per RFC 7232.
     * <p>Should be configured using an {@code <init-param>} for parameter name
     * "writeWeakETag" in the filter definition in {@code web.xml}.
     * @since 4.3
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">RFC 7232 section 2.3</a>
     */
    private boolean writeWeakETag = false;

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
        
        boolean noStore = Common.envProps.getBoolean("web.cache.noStore", false);
        boolean restNoStore = Common.envProps.getBoolean("web.cache.noStore.rest", true);
        boolean resourcesNoStore = Common.envProps.getBoolean("web.cache.noStore.resources", false);

        defaultHeader = noStore ? NO_STORE : String.format(MAX_AGE_TEMPLATE, Common.envProps.getLong("web.cache.maxAge", 0L));
        restHeader = restNoStore ? NO_STORE : String.format(MAX_AGE_TEMPLATE, Common.envProps.getLong("web.cache.maxAge.rest", 0L));
        resourcesHeader = resourcesNoStore ? NO_STORE : String.format(MAX_AGE_TEMPLATE, Common.envProps.getLong("web.cache.maxAge.resources", 86400L));
        versionedResourcesHeader = resourcesNoStore ? NO_STORE : String.format(MAX_AGE_TEMPLATE, Common.envProps.getLong("web.cache.maxAge.versionedResources", 31536000L));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        if(uiServletMatcher.matches(request)) {
            return true;
        }else {
            return false;
        }
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
        
        //Shall we generate an ETag?
        if(isEligibleForEtag(request, response, response.getStatus())) {
            String path = request.getRequestURI();
            if(path.startsWith("/")) {
                path = path.substring(1, path.length());
            }
            Path resourcePath = Common.MA_HOME_PATH.resolve(Constants.DIR_WEB).resolve(path);
            if(resourcePath.toFile().exists()) {
                //Compute hash of last modified since as ETag
                long lastModified = resourcePath.toFile().lastModified();
                String eTag = generateETagHeaderValue(lastModified, writeWeakETag);
                response.setHeader(HEADER_ETAG, eTag);
                //Compare ETag from client if provided and send not modified if same
                String requestETag = request.getHeader(HEADER_IF_NONE_MATCH);
                if (requestETag != null && ("*".equals(requestETag) || compareETagHeaderValue(requestETag, eTag))) {
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
            }
            //If this was a forwarded 
            String originalRequestURI = (String)request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
            if (originalRequestURI != null) {
                
            }
        }
        filterChain.doFilter(request, response);
    }
    
    /**
     * Indicates whether the given request and response are eligible for ETag generation.
     * <p>The default implementation returns {@code true} if all conditions match:
     * <ul>
     * <li>response status codes in the {@code 2xx} series</li>
     * <li>request method is a GET</li>
     * <li>response Cache-Control header is not set or does not contain a "no-store" directive</li>
     * </ul>
     * @param request the HTTP request
     * @param response the HTTP response
     * @param responseStatusCode the HTTP response status code
     * @return {@code true} if eligible for ETag generation, {@code false} otherwise
     */
    protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response,
            int responseStatusCode) {

        String method = request.getMethod();
        if (responseStatusCode >= 200 && responseStatusCode < 300 && HttpMethod.GET.matches(method)) {
            String cacheControl = response.getHeader(HEADER_CACHE_CONTROL);
            return (cacheControl == null || !cacheControl.contains(DIRECTIVE_NO_STORE));
        }
        return false;
    }
    
    /**
     * Generate the ETag header value from the given response body byte array.
     * <p>The default implementation generates an MD5 hash.
     * @param inputStream the response body as an InputStream
     * @param isWeak whether the generated ETag should be weak
     * @return the ETag header value
     * @see org.springframework.util.DigestUtils
     */
    protected String generateETagHeaderValue(long lastModified, boolean isWeak) throws IOException {
        // length of W/ + " + 0 + 32bits md5 hash + "
        StringBuilder builder = new StringBuilder(37);
        if (isWeak) {
            builder.append("W/");
        }
        builder.append("\"0");
        builder.append(DigestUtils.md5DigestAsHex(Long.toString(lastModified).getBytes()));
        builder.append('"');
        return builder.toString();
    }
    
    private boolean compareETagHeaderValue(String requestETag, String responseETag) {
        if (requestETag.startsWith("W/")) {
            requestETag = requestETag.substring(2);
        }
        if (responseETag.startsWith("W/")) {
            responseETag = responseETag.substring(2);
        }
        return requestETag.equals(responseETag);
    }
}
