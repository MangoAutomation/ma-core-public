/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.ILifecycle;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.controller.ControllerUtils;
import com.serotonin.provider.Providers;

/**
 * @author Matthew Lohbihler
 */
public class CommonDataInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("availableLanguages", Common.getLanguages());
        request.setAttribute("lang", ControllerUtils.getLocale(request).getLanguage());
        //If database isn't ready we can't do this
        //TODO Maybe do this differently?
        ILifecycle lifecycle = Providers.get(ILifecycle.class);
    	if(lifecycle.getStartupProgress() >= 100f){
    		request.setAttribute("instanceDescription", SystemSettingsDao.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
    		//Only output the site analytics if we are NOT on the system settings page
    		if(!request.getRequestURL().toString().endsWith(ModuleRegistry.SYSTEM_SETTINGS_URL)){
	    		request.setAttribute("siteAnalyticsHead", SystemSettingsDao.getValue(SystemSettingsDao.SITE_ANALYTICS_HEAD));
	    		request.setAttribute("siteAnalyticsBody", SystemSettingsDao.getValue(SystemSettingsDao.SITE_ANALYTICS_BODY));
    		}
    	}
    	request.setAttribute("NEW_ID", Common.NEW_ID);
        
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) {
        // no op
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // no op
    }
}
