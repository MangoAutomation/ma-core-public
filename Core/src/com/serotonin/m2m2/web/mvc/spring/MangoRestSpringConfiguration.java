/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import java.util.List;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.serotonin.m2m2.web.mvc.rest.v1.mapping.JUnitModule;
import com.serotonin.m2m2.web.mvc.rest.v1.mapping.MangoCoreModule;

/**
 * @author Terry Packer
 * 
 */
@Configuration
@EnableWebMvc
// Scan for the Rest Controller and Exclude swagger
@ComponentScan(
		basePackages = { "com.serotonin.m2m2.web.mvc.rest" }, 
		excludeFilters = { @ComponentScan.Filter( pattern = "com\\.serotonin\\.m2m2\\.web\\.mvc\\.rest\\.swagger.*", type = FilterType.REGEX) })
public class MangoRestSpringConfiguration extends WebMvcConfigurerAdapter {

	
	/**
	 * Configure the Message Converters for the API
	 * for now only JSON
	 */
	@Override
	public void configureMessageConverters(
			List<HttpMessageConverter<?>> converters) {
		converters.add(createMappingJackson2HttpMessageConverter());
	}
	
	/**
	 * Exposed for use in testing
	 * @return
	 */
	public static MappingJackson2HttpMessageConverter createMappingJackson2HttpMessageConverter(){
		// For raw Jackson
		MappingJackson2HttpMessageConverter jackson2Converter = new MappingJackson2HttpMessageConverter();

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		// JUnit Module
		JUnitModule jUnitModule = new JUnitModule();
		objectMapper.registerModule(jUnitModule);

		// Mango Core JSON Module
		MangoCoreModule mangoCore = new MangoCoreModule();
		objectMapper.registerModule(mangoCore);

		jackson2Converter.setObjectMapper(objectMapper);
		// For Sero JSON (NOT USING)
		// JsonMessageConverter seroJson = new
		// JsonMessageConverter(jackson2Converter);
		// converters.add(seroJson);
		
		return jackson2Converter;
	}

}
