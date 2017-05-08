/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Matthew Lohbihler
 */
public class ProtocolRedirectionFilter extends BaseRedirectionFilter {
    private static final Log logger = LogFactory.getLog(ProtocolRedirectionFilter.class);

    // Standard constants.
    private static final String SCHEME_SUFFIX = "://";
    private static final String PORT_SEPARATOR = ":";
    protected static final String HTTP_SCHEME = "http";
    protected static final String DEFAULT_HTTP_PORT = "80";
    protected static final String HTTPS_SCHEME = "https";
    protected static final String DEFAULT_HTTPS_PORT = "443";

    private static String HOST;
    private static String PLAIN_PORT;
    private static String SECURE_PORT;
    private static String CONTEXT_PATH;

    private static List<String> secureServletPaths = null;
    private static List<String> noRedirectPaths = null;

    public void init(FilterConfig config) {
        if (CONTEXT_PATH == null) {
            ResourceBundle props = ResourceBundle.getBundle("env");
            HOST = props.getString("host");
            PLAIN_PORT = props.getString("plain.port");
            SECURE_PORT = props.getString("secure.port");
            CONTEXT_PATH = props.getString("contextPath");
            if (CONTEXT_PATH.equals("/"))
                CONTEXT_PATH = "";
        }

        ResourceBundle props = ResourceBundle.getBundle("protocolRedirection");

        if (secureServletPaths == null) {
            String secureServletPathMapProperty = props.getString("secureServletPaths");
            secureServletPaths = new ArrayList<String>();
            populateList(secureServletPathMapProperty, secureServletPaths);
        }

        if (noRedirectPaths == null) {
            String noRedirectProp = props.getString("noRedirectPaths");
            noRedirectPaths = new ArrayList<String>();
            populateList(noRedirectProp, noRedirectPaths);
        }
    }

    private void populateList(String stringList, List<String> list) {
        if (stringList == null)
            return;

        String[] items = stringList.split(",");
        for (int i = 0; i < items.length; i++)
            list.add(items[i]);
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        // Assume an http request.
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Obtain current request servlet path
        String servletPath = request.getServletPath();

        // Make sure a session id is created.
        request.getSession();

        // // Dump the cookies.
        // Cookie[] cookies = request.getCookies();
        // if (cookies != null) {
        // for (Cookie cookie : cookies)
        // logger.debug("Request cookie: name="+ cookie.getName() +", value="+ cookie.getValue()
        // +", secure="+ cookie.getSecure() +", comment="+ cookie.getComment()
        // +", domain="+ cookie.getDomain() +", maxAge="+ cookie.getMaxAge()
        // +", path="+ cookie.getPath() +", version="+ cookie.getVersion());
        // }
        // else
        // logger.debug("Request cookie: (none)");
        //        
        // // Dump the session id.
        // if (session == null)
        // logger.debug("User session: (none), path="+ servletPath);
        // else
        // logger.debug("User session: "+ session.getId() +", path="+ servletPath);

        Cookie sessionCookie = getSessionCookie(request);
        if (sessionCookie == null && request.getRequestURL().toString().startsWith("https")) {
            // This is a fix for a problem with cookies and hybrid secure/plain text sites. If the
            // session cookie is established in https mode, it seems to be unusable in plain text
            // (http) mode. This code will recognize that the cookie will be established in secure
            // mode (i.e. we currently are https and there is no existing cookie) and forward to to
            // plain text mode so that we will be forwarded back to secure mode, causing the cookie
            // to be properly established.
            logger.debug("Redirecting to non-secure page to establish session cookie");
            String redirectURL = getSchemeRedirectURL(request, response, false);
            redirectResponse(response, redirectURL);
            return;
        }

        // Check if we should ignore this path.
        if (!noRedirectPaths.contains(servletPath)) {
            // Determine if mapping must be secure or not
            boolean mustSecure = false;
            if (secureServletPaths.contains(servletPath))
                mustSecure = true;

            String redirectURL = getSchemeRedirectURL(request, response, mustSecure);
            if (redirectURL != null && redirectURL.length() > 0) {
                logger.debug("Scheme Redirect: requestURL:" + request.getRequestURL().toString() + ", redirectURL: "
                        + redirectURL);
                try {
                    redirectResponse(response, redirectURL);
                    return;
                }
                catch (IllegalStateException ise) {
                    logger.error("IllegalStateException occur attempting to sendRedirect to: " + redirectURL, ise);
                    throw ise;
                }
            }
        }

        // Continue with the chain.
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {
        // no op
    }

    public String getSchemeRedirectURL(HttpServletRequest request, HttpServletResponse response, boolean secured) {
        // Get the desired scheme/port and the request scheme/port.
        String requiredScheme = ((secured) ? HTTPS_SCHEME : HTTP_SCHEME);
        String requestScheme = request.getScheme();
        String requiredPort = ((secured) ? SECURE_PORT : PLAIN_PORT);
        String requiredContextPath = CONTEXT_PATH;

        // Check that the scheme and ports are as required.
        if (requiredScheme.equalsIgnoreCase(requestScheme))
            return null;

        return this.buildSchemeRedirectURL(request, response, requiredScheme, requiredPort, requiredContextPath,
                secured);
    }

    private String buildSchemeRedirectURL(HttpServletRequest request, HttpServletResponse response,
            String requiredScheme, String requiredPort, String requiredContextPath, boolean secured) {

        StringBuffer url = new StringBuffer(requiredScheme);

        // Scheme.
        url.append(SCHEME_SUFFIX);

        // Server name.
        String serverName = HOST;
        if (serverName == null || serverName.length() == 0)
            serverName = request.getServerName();
        url.append(serverName);

        // Port.
        if (!((secured) ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT).equals(requiredPort)) {
            url.append(PORT_SEPARATOR);
            url.append(requiredPort);
        }

        // Context and Servlet Path.
        if (requiredContextPath != null && requiredContextPath.length() > 0) {
            url.append(requiredContextPath);
            url.append(request.getServletPath());
        }
        else
            url.append(request.getRequestURI());

        // Add the query string (if any) to the URL.
        String queryString = getQueryString(request);
        if (queryString != null)
            url.append(queryString);

        String targetURL = response.encodeRedirectURL(url.toString());

        return targetURL;
    }

    private void redirectResponse(HttpServletResponse response, String redirectURL) throws IOException {
        PrintWriter out = response.getWriter();
        out.write("<script language=\"JavaScript\">window.location=\"");
        out.write(redirectURL);
        out.write("\";</script>");
        response.setContentType("text/html");
        preventCaching(response);
    }

    private Cookie getSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if ("JSESSIONID".equals(cookies[i].getName()))
                    return cookies[i];
            }
        }
        return null;
    }
}
