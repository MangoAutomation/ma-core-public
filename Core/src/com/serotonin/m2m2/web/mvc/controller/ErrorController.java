/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Class to manage custom errors for known situations where an error can occur
 * and throwing an exception is not ideal.
 * 
 * 
 * @author Terry Packer
 * 
 */
public class ErrorController extends ParameterizableViewController {

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request,
			HttpServletResponse response) {

		Map<String, Object> model = new HashMap<String, Object>();

		String key = request.getParameter("key");
		String parameters = request.getParameter("params");
		if(key != null){
			String errorMessage = "";
			TranslatableMessage msg;
			if (parameters != null) {
				Object[] parts = parameters.split(",");
				msg = new TranslatableMessage(key, parts);
				errorMessage = msg.translate(Common.getTranslations());
			} else {
				msg = new TranslatableMessage(key);
			}
	
			errorMessage = msg.translate(Common.getTranslations());
			model.put("errorMessage", errorMessage);
		}
		
		return new ModelAndView(getViewName(), model);
	}

}
