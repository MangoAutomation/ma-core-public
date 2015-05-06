/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.handler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Constants;
import com.serotonin.m2m2.web.OverridingFileResource;
import com.serotonin.m2m2.web.servlet.StatusServlet;

/**
 * Handle All Requests During the startup phase of Mango
 * 
 * @author Terry Packer
 *
 */
public class StartupContextHandler extends ResourceHandler{
    //private final Log LOG = LogFactory.getLog(StartupContextHandler.class);
	private final String STARTUP_PAGE_TEMPLATE = "startupTemplate.htm";
	private final static int PAGE = 1;
	private final static int RESOURCE = 2;
	private final static int JSON = 3;
	
	private StatusServlet statusServlet;
	private String pageTemplate;
	
	public StartupContextHandler() throws IOException{
		setBaseResource(new OverridingFileResource(Resource.newResource(Common.MA_HOME + "/overrides/" + Constants.DIR_WEB),
                Resource.newResource(Common.MA_HOME + "/" + Constants.DIR_WEB)));
		this.statusServlet = new StatusServlet();

	}
	
	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
        //Allow access to js and css files
        int requestType = PAGE;
        
        if(request.getPathInfo().endsWith(".css") || 
        		request.getPathInfo().endsWith(".js") ||
        		request.getPathInfo().endsWith(".ico") ||
        		request.getPathInfo().endsWith(".jpg") ||
        		request.getPathInfo().endsWith(".gif") ||
        		request.getPathInfo().endsWith(".png") ||
        		request.getPathInfo().endsWith(".map")){
        	requestType = RESOURCE;
        }else if(request.getPathInfo().endsWith(".json")){
        	requestType = JSON;
        }

        switch(requestType){
        	case RESOURCE:
        		super.handle(target, baseRequest, request, response);
        	break;
        	case JSON:
        		
				statusServlet.handleRequest(request,response);
	            baseRequest.setHandled(true);
        		
        	break;
        	case PAGE:
        	default:
        		response.setContentType("text/html;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);

	        	baseRequest.setHandled(true);
	            //Load page template
	    		byte[] fileData = Files.readAllBytes(Paths.get(Common.MA_HOME +  "/" + Constants.DIR_WEB +"/"+ STARTUP_PAGE_TEMPLATE));
	    		pageTemplate = new String(fileData, Common.UTF8_CS);
	
	            String processedTemplate = pageTemplate;
	            response.getWriter().write(processedTemplate);
            break;
        }
		
	}


	
}
