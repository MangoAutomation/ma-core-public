/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.web.mvc.rest.v1.converters.CsvMessageConverter;
import com.serotonin.m2m2.web.mvc.rest.v1.converters.CsvRowMessageConverter;
import com.serotonin.m2m2.web.mvc.rest.v1.mapping.JUnitModule;
import com.serotonin.m2m2.web.mvc.rest.v1.mapping.MangoCoreModule;

/**
 * @author Terry Packer
 * 
 */
@Configuration
@EnableWebMvc
// Scan for the Rest Controller and Exclude swagger
@ComponentScan(basePackages = { "com.serotonin.m2m2.web.mvc.rest" }, excludeFilters = { @ComponentScan.Filter(pattern = "com\\.serotonin\\.m2m2\\.web\\.mvc\\.rest\\.swagger.*", type = FilterType.REGEX) })
public class MangoRestSpringConfiguration extends WebMvcConfigurerAdapter {

	// TODO Make this a Bean by annotating the createObjectmapper method
	// This is public for use in testing
	public static final ObjectMapper objectMapper = createObjectMapper();

	/**
	 * 
	 * TODO EXPERIMENTAL SUPPORT FOR PROPERTY CONFIGURATION IN ANNOTATIONS Setup
	 * properties to be used in the Spring templates
	 * 
	 * @return
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		// note the static method! important!!
		PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		Resource[] resources = new ClassPathResource[] { new ClassPathResource(
				"env.properties") };
		configurer.setLocations(resources);
		configurer.setIgnoreUnresolvablePlaceholders(true);

		// Create and add any properties for Spring Annotations
		Properties properties = new Properties();
		String inputDateFormat = Common.envProps.getString(
				"rest.customDateInputFormat", "YYYY-MM-dd HH:mm:ss.SSS Z");
		properties.put("rest.customDateInputFormat", inputDateFormat);

		configurer.setProperties(properties);
		return configurer;
	}

	/**
	 * Setup Content Negotiation to map url extensions to returned data types
	 * 
	 * @see http
	 *      ://spring.io/blog/2013/05/11/content-negotiation-using-spring-mvc
	 */
	@Override
	public void configureContentNegotiation(
			ContentNegotiationConfigurer configurer) {
		// Simple strategy: only path extension is taken into account
		configurer.favorPathExtension(true).ignoreAcceptHeader(false)
				.useJaf(false). // TODO get maven jar to use application types
				defaultContentType(MediaType.TEXT_HTML).
				//Add Some file extension default mappings
				// mediaType("html", MediaType.TEXT_HTML).
				mediaType("xml", MediaType.APPLICATION_XML). // TODO add jaxb
																// for this to
																// work
				mediaType("json", MediaType.APPLICATION_JSON). 
				mediaType("csv", new MediaType("text", "csv"));
				//mediaType("csv", new MediaType("text", "csv", Common.UTF8_CS));
	}

	/**
	 * Configure the Message Converters for the API for now only JSON
	 */
	@Override
	public void configureMessageConverters(
			List<HttpMessageConverter<?>> converters) {

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setObjectMapper(objectMapper);
		converters.add(converter);
		converters.add(new CsvMessageConverter());
		converters.add(new CsvRowMessageConverter());

	}

	/**
	 * 
	 * @return
	 */
	private static ObjectMapper createObjectMapper() {
		// For raw Jackson
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		// JUnit Module
		JUnitModule jUnitModule = new JUnitModule();
		objectMapper.registerModule(jUnitModule);

		// Mango Core JSON Module
		MangoCoreModule mangoCore = new MangoCoreModule();
		objectMapper.registerModule(mangoCore);

		// Custom Date Output Format
		String customDateFormat = Common.envProps
				.getString("rest.customDateOutputFormat");
		if (customDateFormat != null) {
			objectMapper.configure(
					SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
			DateFormat dateFormat = new SimpleDateFormat(customDateFormat);
			objectMapper.setDateFormat(dateFormat);
		}
		//This will allow messy JSON to be imported even if all the properties in it are part of the POJOs
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return objectMapper;
	}
}
