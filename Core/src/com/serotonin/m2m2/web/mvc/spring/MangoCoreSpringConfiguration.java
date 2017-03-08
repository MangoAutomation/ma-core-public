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
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import com.serotonin.m2m2.web.mvc.BlabberUrlHandlerMapping;
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
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
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
	public BlabberUrlHandlerMapping blabberUrlHandlerMapping(CommonDataInterceptor commonDataInterceptor){
		
		BlabberUrlHandlerMapping mapping = new BlabberUrlHandlerMapping();
		mapping.addInterceptor(commonDataInterceptor);

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
