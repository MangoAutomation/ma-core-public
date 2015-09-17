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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.AntPathMatcher;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.ControllerMappingDefinition;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.UriMappingDefinition;
import com.serotonin.m2m2.module.UrlMappingDefinition;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;

public class UrlSecurityFilter implements Filter {
    private static final Log LOG = LogFactory.getLog(UrlSecurityFilter.class);

    private  AntPathMatcher matcher;

    
    @Override
    public void init(FilterConfig arg0) throws ServletException {
        matcher = new AntPathMatcher();
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

        boolean foundMapping = false;
       
        String uri = request.getRequestURI();
        for (UriMappingDefinition uriDef : ModuleRegistry.getDefinitions(UriMappingDefinition.class)) {
            if(matcher.match(uriDef.getPath(), uri)){
                boolean allowed = true;
                foundMapping = true;
                User user = Common.getUser(request);
                
                switch (uriDef.getPermission()) {
                case ADMINISTRATOR:
                    if ((user==null)||(!Permissions.hasAdmin(user)))
                        allowed = false;
                    break;
                case DATA_SOURCE:
                    if ((user==null)||(!user.isDataSourcePermission()))
                        allowed = false;
                    break;
                case USER:
                	if(user == null){
                		allowed = false;
                	}
                break;
                case CUSTOM:
                	try{
                		allowed = uriDef.hasCustomPermission(user);
                	}catch(PermissionException e){
                		allowed = false;
                	}
                break;
                case ANONYMOUS:
                break;
                }

                if (!allowed) {
                	String msg;
                	if(user == null){
                		msg = "Denying access to page where user isn't logged in, uri=" + uri;
                	}else{
                		msg = "Denying access to page where user hasn't sufficient permission, user="
                                + user.getUsername() + ", uri=" + uri;
                		response.sendRedirect(DefaultPagesDefinition.getUnauthorizedUri(request, response, user));
                		return;
                	}
                    LOG.info(msg);
                }else{
                	request.setAttribute("urlSecurity", true);
                }

                break;
            }
        }

        //if not set then check our other definitions
        if(!foundMapping){
	        for (ControllerMappingDefinition uriDef : ModuleRegistry.getDefinitions(ControllerMappingDefinition.class)) {
	            if(matcher.match(uriDef.getPath(), uri)){
	                boolean allowed = true;
	                foundMapping = true;
	
	                User user = Common.getUser(request);
	                
	                switch (uriDef.getPermission()) {
	                case ADMINISTRATOR:
	                    if ((user==null)||(!Permissions.hasAdmin(user)))
	                        allowed = false;
	                    break;
	                case DATA_SOURCE:
	                    if ((user==null)||(!user.isDataSourcePermission()))
	                        allowed = false;
	                    break;
	                case USER:
	                	if(user == null){
	                		allowed = false;
	                	}
	                break;
	                case CUSTOM:
	                	try{
	                		allowed = uriDef.hasCustomPermission(user);
	                	}catch(PermissionException e){
	                		allowed = false;
	                	}
	                break;
	                case ANONYMOUS:
	                break;
	                }
	
	                if (!allowed) {
	                	String msg;
	                	if(user == null){
	                		msg = "Denying access to page where user isn't logged in, uri=" + uri;
	                	}else{
	                		msg = "Denying access to page where user hasn't sufficient permission, user="
	                                + user.getUsername() + ", uri=" + uri;
	                		response.sendRedirect(DefaultPagesDefinition.getUnauthorizedUri(request, response, user));
	                		return;
	                	}
	                    LOG.info(msg);
	                }else{
	                	request.setAttribute("urlSecurity", true);
	                }
	
	                break;
	            }
	        }
        }
        
        //if not set then check our other definitions
        if(!foundMapping){
	        for (UrlMappingDefinition uriDef : ModuleRegistry.getDefinitions(UrlMappingDefinition.class)) {
	            if(matcher.match(uriDef.getUrlPath(), uri)){
	                boolean allowed = true;
	                foundMapping = true;
	
	                User user = Common.getUser(request);
	                
	                switch (uriDef.getPermission()) {
	                case ADMINISTRATOR:
	                    if ((user==null)||(!Permissions.hasAdmin(user)))
	                        allowed = false;
	                    break;
	                case DATA_SOURCE:
	                    if ((user==null)||(!user.isDataSourcePermission()))
	                        allowed = false;
	                    break;
	                case USER:
	                	if(user == null){
	                		allowed = false;
	                	}
	                break;
	                case ANONYMOUS:
	                break;
	                }
	
	                if (!allowed) {
	                	String msg;
	                	if(user == null){
	                		msg = "Denying access to page where user isn't logged in, uri=" + uri;
	                	}else{
	                		msg = "Denying access to page where user hasn't sufficient permission, user="
	                                + user.getUsername() + ", uri=" + uri;
	                		response.sendRedirect(DefaultPagesDefinition.getUnauthorizedUri(request, response, user));
	                		return;
	                	}
	                    LOG.info(msg);
	                }else{
	                	request.setAttribute("urlSecurity", true);
	                }
	
	                break;
	            }
	        }
        }
        
        
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
