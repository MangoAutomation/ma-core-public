/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.filter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.support.WebContentGenerator;

/**
 * Provides some common services for redirection filters.
 * 
 * @author Matthew Lohbihler
 */
abstract public class BaseRedirectionFilter extends WebContentGenerator implements Filter {
    private static final String QUERY_STRING_PREFIX = "?";
    private static final String QUERY_STRING_SEPARATOR = "&";

    protected String getQueryString(HttpServletRequest request) {
        // If a query string exists, use it.
        String queryString = request.getQueryString();

        if (StringUtils.isBlank(queryString))
            // Otherwise, check explicitly for parameters and create a query string from them. (Actually, one could
            // create a form page that automatically submits itself to post data instead...)
            queryString = createQueryStringFromParameters(request);

        if (StringUtils.isEmpty(queryString))
            return null;
        return QUERY_STRING_PREFIX + queryString;
    }

    protected String createQueryStringFromParameters(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        boolean firstParameter = true;

        for (Enumeration<?> enumeration = request.getParameterNames(); enumeration.hasMoreElements();) {
            String parameterName = (String) enumeration.nextElement();
            String[] values = request.getParameterValues(parameterName);
            try {
                if (values != null) {
                    String encodedParameterName = URLEncoder.encode(parameterName, "UTF-8");
                    for (int i = 0; i < values.length; i++) {
                        if (!firstParameter)
                            sb.append(QUERY_STRING_SEPARATOR);
                        sb.append(encodedParameterName);
                        sb.append("=");
                        sb.append(URLEncoder.encode(values[i], "UTF-8"));
                    }
                    firstParameter = false;
                }
            }
            catch (UnsupportedEncodingException e) {
                logger.debug("getRequestParameters().UnsupportedEncoding: " + e.getMessage());
            }
        }

        return sb.toString();
    }
}
