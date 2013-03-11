package com.serotonin.m2m2.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.UriMappingDefinition;
import com.serotonin.m2m2.vo.User;

public class UrlSecurityFilter implements Filter {
    private static final Log LOG = LogFactory.getLog(UrlSecurityFilter.class);

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // no p
    }

    @Override
    public void destroy() {
        // no op
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        // Assume an http request.
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String uri = request.getRequestURI();
        for (UriMappingDefinition uriDef : ModuleRegistry.getDefinitions(UriMappingDefinition.class)) {
            if (StringUtils.equals(uri, uriDef.getPath())) {
                boolean allowed = true;

                User user = Common.getUser(request);
                switch (uriDef.getPermission()) {
                case ADMINISTRATOR:
                    if (!user.isAdmin())
                        allowed = false;
                    break;
                case DATA_SOURCE:
                    if (!user.isDataSourcePermission())
                        allowed = false;
                    break;
                }

                if (!allowed) {
                    LOG.info("Denying access to page where user hasn't sufficient permission, user="
                            + user.getUsername() + ", uri=" + uri);
                    response.sendRedirect(DefaultPagesDefinition.getUnauthorizedUri(request, response, user));
                    return;
                }

                break;
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
