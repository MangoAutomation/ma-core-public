/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.servlet;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.ILifecycle;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.rt.console.LoggingConsoleRT;
import com.serotonin.provider.Providers;

/**
 * Class to provide JSON status of Mango
 * 
 * @author Terry Packer
 *
 */
public class StatusServlet extends HttpServlet{
	
	private static final long serialVersionUID = 1L;
	private final Log LOG = LogFactory.getLog(StatusServlet.class);
	private Translations translations;
	
	public StatusServlet(){
		this.translations = Translations.getTranslations();
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		ILifecycle lifecycle = Providers.get(ILifecycle.class);
		
		String timeString = request.getParameter("time");
		long time = -1;
		if(timeString != null){
			try{
				time = Long.parseLong(timeString);
			}catch(Exception e){
				LOG.error(e.getMessage(), e);
			}
		}
		
		response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

		//Get the Info and pack it up
        Map<String,Object> data = new HashMap<String,Object>();
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, sw);
        
		//Limit to logged in users while running
        //TODO create a nice way of sharing states between ma-priv and core
		if((lifecycle.getLifecycleState() != 200)||(Common.getUser(request) != null)){
			data.put("messages", LoggingConsoleRT.instance.getMessagesSince(time));
		}else{
			data.put("messages", new ArrayList<String>());
		}
		
        data.put("startupProgress", lifecycle.getStartupProgress());
        data.put("shutdownProgress", lifecycle.getShutdownProgress());
    	data.put("state", getLifecycleStateMessage(lifecycle.getLifecycleState()));
    	
		data.put("startupUri", DefaultPagesDefinition.getLoginUri(request,response));
    	
        try {
			writer.writeObject(data);
            response.getWriter().write(sw.toString());

		} catch (JsonException e) {
			LOG.error(e);
		}

	}

	/**
	 * Method to handle a request from an external handler or process
	 * 
	 * @param request
	 * @param response
	 * @throws IOException 
	 * @throws ServletException 
	 */
	public void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		this.doGet(request, response);
	}
	
	/**
	 * Convert a numeric state to the translated message
	 * @param state
	 * @return
	 */
	private String getLifecycleStateMessage(int state){
		switch(state){
		case 0:
			return this.translations.translate("startup.state.notStarted");
		case 10:
			return this.translations.translate("startup.state.webServerInitialize");
		case 20:
			return this.translations.translate("startup.state.preInitialize");
		case 30:
			return this.translations.translate("startup.state.timerInitialize");
		case 40:
			return this.translations.translate("startup.state.jsonInitialize");
		case 50:
			return this.translations.translate("startup.state.epollInitialize");
		case 60:
			return this.translations.translate("startup.state.licenseCheck");
		case 70:
			return this.translations.translate("startup.state.freeMarkerInitialize");
		case 80:
			return this.translations.translate("startup.state.databaseInitialize");
		case 90:
			return this.translations.translate("startup.state.postDatabaseInitialize");
		case 100:
			return this.translations.translate("startup.state.utilitesInitialize");
		case 110:
			return this.translations.translate("startup.state.eventManagerInitialize");
		case 150:
			return this.translations.translate("startup.state.runtimeManagerInitialize");
		case 160:
			return this.translations.translate("startup.state.maintenanceInitialize");
		case 170:
			return this.translations.translate("startup.state.imageSetInitialize");
		case 175:
			return this.translations.translate("startup.state.webServerFinalize");
		case 180:
			return this.translations.translate("startup.state.postInitialize");
		case 190:
			return this.translations.translate("startup.state.startupTasksRunning");
		case 200:
			return this.translations.translate("startup.state.running");
		case 210:
			return this.translations.translate("shutdown.state.preTerminate");
		case 220:
			return this.translations.translate("shutdown.state.shutdownTasksRunning");
		case 230:
			return this.translations.translate("shutdown.state.webServerTerminate");
		default:
			return this.translations.translate("startup.state.unknown");
		}
	}
}
