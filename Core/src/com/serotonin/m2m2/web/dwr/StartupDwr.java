/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.spi.LoggerRepository;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.serotonin.m2m2.ILifecycle;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.rt.console.LoggingConsoleRT;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.provider.Providers;

/**
 * @author Terry Packer
 *
 */
public class StartupDwr {

	private Translations translations = Translations.getTranslations();
	
	@DwrPermission(anonymous = true)
	public ProcessResult getStartingMessage(){
		
		ProcessResult result = new ProcessResult();
    	
		result.addData("message", this.translations.translate("startup.startingUp"));
		return result;
	}
	
	
	@DwrPermission(anonymous = true)
	public ProcessResult getStartupProgress(){
		
		ProcessResult result = new ProcessResult();
		
		ILifecycle lifecycle = Providers.get(ILifecycle.class);
    	float progress =  lifecycle.getStartupProgress();
    	

    	String message =  LoggingConsoleRT.instance.getCurrentMessage();
    	result.addData("message", message);
    	
		result.addData("progress", progress);
		result.addData("state", getLifecycleStateMessage(lifecycle.getLifecycleState()));
		
		if(progress >= 100){
			WebContext ctx = WebContextFactory.get();
			result.addData("startupUri", DefaultPagesDefinition.getLoginUri(ctx.getHttpServletRequest(), ctx.getHttpServletResponse()));
		}
		
		return result;
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
		case 180:
			return this.translations.translate("startup.state.postInitialize");
		case 190:
			return this.translations.translate("startup.state.startupTasksRunning");
		case 200:
			return this.translations.translate("startup.state.running");
		
		default:
			return this.translations.translate(
					"startup.state.unknown");
		}
	}
	
}
