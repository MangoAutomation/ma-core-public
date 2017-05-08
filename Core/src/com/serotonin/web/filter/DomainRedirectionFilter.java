/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.filter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.serotonin.util.properties.ReloadingProperties;

/**
 * @author Matthew Lohbihler
 */
public class DomainRedirectionFilter extends BaseRedirectionFilter {
    private ReloadingProperties props;

    public void init(FilterConfig arg0) {
        props = new ReloadingProperties("domainRedirection");
    }

    public void destroy() {
        // no op
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        // Assume an http request.
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String serverName = request.getServerName();
        String uri = request.getRequestURI();

        String redirect = props.getString(serverName + uri, null);
        if (redirect == null)
            redirect = props.getString(serverName, null);

        if (redirect != null) {
            String redirectType = props.getString("redirectType");
            if (redirectType.equalsIgnoreCase("javascript")) {
                String queryString = getQueryString(request);
                if (queryString != null)
                    redirect += queryString;

                PrintWriter out = response.getWriter();
                out.write("<html><head>");
                out.write("<script type=\"text/javascript\">window.location=\"");
                out.write(redirect);
                out.write("\";</script>");
                out.write("</head><body>");
                out.write("If your browser does not automatically redirect, <a href=\"");
                out.write(redirect);
                out.write("\">click here</a>.</body></html>");
                response.setContentType("text/html");
                preventCaching(response);
            }
            else
                // HTTP
                response.sendRedirect(redirect);
        }
        else
            filterChain.doFilter(servletRequest, servletResponse);
    }
}
