/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.web.filter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Matthew Lohbihler
 */
public class HttpResponseHeaderFilter implements Filter {
    private static final Pattern DATE = Pattern.compile("date\\s*\\+\\s*(\\d+)");

    private final Map<String, HeaderValue> headers = new LinkedHashMap<String, HeaderValue>();

    public void init(FilterConfig filterConfig) {
        @SuppressWarnings("unchecked")
        Enumeration<String> names = filterConfig.getInitParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String value = filterConfig.getInitParameter(name);
            HeaderValue parsed = null;

            if (StringUtils.startsWith(value, "{") && StringUtils.endsWith(value, "}")) {
                value = value.substring(1, value.length() - 1);

                if (StringUtils.equals(value, "date"))
                    parsed = new DateValue(0);
                else {
                    String plusStr = com.serotonin.util.StringUtils.findGroup(DATE, value);
                    if (plusStr != null)
                        parsed = new DateValue(Long.parseLong(plusStr) * 1000);
                }
            }
            else
                parsed = new LiteralValue(value);

            if (parsed == null)
                throw new RuntimeException("Unknown value macro in " + filterConfig.getInitParameter(name));

            headers.put(name, parsed);
        }
    }

    public void destroy() {
        // no op
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        for (Map.Entry<String, HeaderValue> e : headers.entrySet())
            e.getValue().addHeader(response, e.getKey());

        // Continue the chain
        filterChain.doFilter(servletRequest, response);
    }

    static abstract class HeaderValue {
        abstract void addHeader(HttpServletResponse response, String name);
    }

    static class LiteralValue extends HeaderValue {
        private final String literal;

        public LiteralValue(String literal) {
            this.literal = literal;
        }

        @Override
        void addHeader(HttpServletResponse response, String name) {
            response.addHeader(name, literal);
        }
    }

    static class DateValue extends HeaderValue {
        private final long plus;

        public DateValue(long plus) {
            this.plus = plus;
        }

        @Override
        void addHeader(HttpServletResponse response, String name) {
            response.addDateHeader(name, System.currentTimeMillis() + plus);
        }
    }
}
