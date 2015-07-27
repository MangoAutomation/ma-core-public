/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import com.serotonin.m2m2.web.mvc.BlabberUrlHandlerMapping;
import com.serotonin.m2m2.web.mvc.controller.DataPointEditController;
import com.serotonin.m2m2.web.mvc.controller.DataSourceEditController;
import com.serotonin.m2m2.web.mvc.controller.DataSourceErrorController;
import com.serotonin.m2m2.web.mvc.controller.DataSourcePropertiesController;
import com.serotonin.m2m2.web.mvc.controller.HelpController;
import com.serotonin.m2m2.web.mvc.controller.LoginController;
import com.serotonin.m2m2.web.mvc.controller.LogoutController;
import com.serotonin.m2m2.web.mvc.controller.PublisherEditController;
import com.serotonin.m2m2.web.mvc.controller.ShutdownController;
import com.serotonin.m2m2.web.mvc.controller.StartupController;
import com.serotonin.m2m2.web.mvc.controller.UnauthorizedController;
import com.serotonin.m2m2.web.mvc.controller.UsersController;
import com.serotonin.m2m2.web.mvc.interceptor.CommonDataInterceptor;
import com.serotonin.propertyEditor.DefaultMessageCodesResolver;

/**
 * Spring Configuration for Mango Automation Core
 * 
 * 
 * @author Terry Packer
 *
 */
@SuppressWarnings("deprecation")
@Configuration
@EnableWebMvc
@ComponentScan(
		basePackages = { "com.serotonin.m2m2.web.mvc.controller" })
public class MangoCoreSpringConfiguration implements BeanFactoryAware{


	private BeanFactory beanFactory;
	
	@Bean(name="viewResolver")
	public InternalResourceViewResolver internalResourceViewResolver(){
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setViewClass(JstlView.class);
		return resolver;
	}
	
	@Bean(name="defaultMessageCodeResolver")
	public DefaultMessageCodesResolver defaultMessageCodesResolver(){
		return new DefaultMessageCodesResolver();
	}
	
	@Bean(name="commonData")
	public CommonDataInterceptor commonDataInterceptor(){
		return new CommonDataInterceptor();
	}
	
	@Bean(name="mappings")
	public BlabberUrlHandlerMapping blabberUrlHandlerMapping(
			CommonDataInterceptor commonDataInterceptor,
			DataPointEditController dataPointEditController,
			DataSourcePropertiesController dataSourcePropertiesController,
			DataSourceEditController dataSourceEditController,
			DataSourceErrorController dataSourceErrorController,
			HelpController helpController,
			StartupController startupController,
			ShutdownController shutdownController,
			LoginController loginController,
			LogoutController logoutController,
			PublisherEditController publisherEditController,
			UnauthorizedController unauthorizedController,
			UsersController usersController
			){
		
		BlabberUrlHandlerMapping mapping = new BlabberUrlHandlerMapping();
		mapping.addInterceptor(commonDataInterceptor);

		//Register the Controller to its appropriate URL (This could be put into the class itself now)
		mapping.registerHandler("/data_point_edit.shtm", dataPointEditController);
		mapping.registerHandler("/data_source_properties.shtm", dataSourcePropertiesController);
		mapping.registerHandler("/data_source_edit.shtm", dataSourceEditController);
		mapping.registerHandler("/data_source_properties_error.shtm", dataSourceErrorController);
		mapping.registerHandler("/help.shtm", helpController);
		mapping.registerHandler("/startup.htm", startupController);
		mapping.registerHandler("/shutdown.htm", shutdownController);
		mapping.registerHandler("/login.htm", loginController);
		mapping.registerHandler("/logout.htm", logoutController);
		mapping.registerHandler("/publisher_edit.shtm", publisherEditController);
		mapping.registerHandler("/unauthorized.htm", unauthorizedController);
		mapping.registerHandler("/users.shtm", usersController);
		
		return mapping;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	@Override
	public void setBeanFactory(BeanFactory factory) throws BeansException{
		this.beanFactory = factory;
	}
	public BeanFactory getBeanFactory(){
		return this.beanFactory;
	}
}
