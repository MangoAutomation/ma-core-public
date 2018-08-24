/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.infiniteautomation.mango.rest.v2.mapping.MangoRestV2JacksonModule;
import com.infiniteautomation.mango.spring.components.executors.MangoExecutors;
import com.infiniteautomation.mango.spring.eventMulticaster.EventMulticasterRegistry;
import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEventMulticaster;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.JacksonModuleDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.rest.v1.mapping.JScienceModule;
import com.serotonin.m2m2.web.mvc.rest.v1.mapping.MangoCoreModule;
import com.serotonin.m2m2.web.mvc.spring.MangoPropertySource;
import com.serotonin.m2m2.web.mvc.spring.MangoWebApplicationInitializer;

/**
 *
 * @author Terry Packer
 */
@Configuration
@ComponentScan(basePackages = {
        "com.infiniteautomation.mango.spring",  //General Runtime Spring Components
        "com.serotonin.m2m2.db.dao" //DAOs
})
public class MangoRuntimeContextConfiguration {
    private static final AtomicReference<ApplicationContext> RUNTIME_CONTEXT_HOLDER = new AtomicReference<>();
    private static final AtomicReference<WebApplicationContext> ROOT_WEB_CONTEXT_HOLDER = new AtomicReference<>();

    /**
     * <p>Gets the spring runtime application context, only set after the context is refreshed (started).
     * If its not null, its safe to use.</p>
     *
     * @return
     */
    public static ApplicationContext getRuntimeContext() {
        return RUNTIME_CONTEXT_HOLDER.get();
    }

    /**
     * Gets the spring root web application context, only set after the context is refreshed (started).
     * If its not null, its safe to use.
     *
     * Prefer getting the application context from the request/servlet context if possible:
     * <p><code>WebApplicationContext webApplicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());</code></p>
     *
     * @return
     */
    public static WebApplicationContext getRootWebContext() {
        return ROOT_WEB_CONTEXT_HOLDER.get();
    }

    public static final String REST_OBJECT_MAPPER_NAME = "restObjectMapper";
    public static final String COMMON_OBJECT_MAPPER_NAME = "commonObjectMapper";
    public static final String SCHEDULED_EXECUTOR_SERVICE_NAME = "scheduledExecutorService";
    public static final String EXECUTOR_SERVICE_NAME = "executorService";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * ContextStartedEvent is not fired for root web context, only ContextRefreshedEvent.
     * Initialize our static holders in this event handler.
     *
     * @param event
     */
    @EventListener
    private void contextRefreshed(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();

        if (log.isInfoEnabled()) {
            log.info("Spring context '" + context.getId() +"' refreshed: " + context.getDisplayName());
        }

        if (MangoWebApplicationInitializer.RUNTIME_CONTEXT_ID.equals(context.getId())) {
            RUNTIME_CONTEXT_HOLDER.compareAndSet(null, context);
        } else if (MangoWebApplicationInitializer.ROOT_WEB_CONTEXT_ID.equals(context.getId()) && context instanceof WebApplicationContext) {
            ROOT_WEB_CONTEXT_HOLDER.compareAndSet(null, (WebApplicationContext) context);
        }
    }

    @EventListener
    private void contextStarted(ContextStartedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        if (log.isInfoEnabled()) {
            log.info("Spring context '" + context.getId() +"' started: " + context.getDisplayName());
        }
    }

    @Bean(REST_OBJECT_MAPPER_NAME)
    public static ObjectMapper getObjectMapper() {
        // For raw Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        if(Common.envProps.getBoolean("rest.indentJSON", false))
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // JScience
        JScienceModule jScienceModule = new JScienceModule();
        objectMapper.registerModule(jScienceModule);

        // Mango Core JSON Modules
        MangoCoreModule mangoCore = new MangoCoreModule();
        objectMapper.registerModule(mangoCore);
        MangoRestV2JacksonModule mangoCoreV2 = new MangoRestV2JacksonModule();
        objectMapper.registerModule(mangoCoreV2);

        //Setup Module Defined JSON Modules
        List<JacksonModuleDefinition> defs = ModuleRegistry.getDefinitions(JacksonModuleDefinition.class);
        for(JacksonModuleDefinition def : defs) {
            if(def.getSourceMapperType() == JacksonModuleDefinition.ObjectMapperSource.REST)
                objectMapper.registerModule(def.getJacksonModule());
        }

        //Always output dates in ISO 8601
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        objectMapper.setDateFormat(dateFormat);

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setTimeZone(TimeZone.getDefault()); //Set to system tz

        //This will allow messy JSON to be imported even if all the properties in it are part of the POJOs
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Primary
    @Bean(COMMON_OBJECT_MAPPER_NAME)
    public ObjectMapper getCommonObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setTimeZone(TimeZone.getDefault());

        //Setup Module Defined JSON Modules
        List<JacksonModuleDefinition> defs = ModuleRegistry.getDefinitions(JacksonModuleDefinition.class);
        for(JacksonModuleDefinition def : defs) {
            if(def.getSourceMapperType() == JacksonModuleDefinition.ObjectMapperSource.COMMON)
                mapper.registerModule(def.getJacksonModule());
        }
        return mapper;
    }

    // ScheduledExecutorService cannot be annotated with @Primary as it is also an ExecutorService
    @Bean(SCHEDULED_EXECUTOR_SERVICE_NAME)
    public ScheduledExecutorService scheduledExecutorService(MangoExecutors executors) {
        return executors.getScheduledExecutor();
    }

    @Primary
    @Bean(EXECUTOR_SERVICE_NAME)
    public ExecutorService executorService(MangoExecutors executors) {
        return executors.getExecutor();
    }

    /**
     * J.W. This is used to convert properties from strings into lists of integers etc when they are injected by Spring
     * @return
     */
    @Bean
    public static ConfigurableConversionService conversionService() {
        return new DefaultConversionService();
    }

    @Bean
    public static MangoPropertySource propertySource() {
        return new MangoPropertySource("envProps", Common.envProps);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(ConfigurableEnvironment env, ConfigurableConversionService conversionService, MangoPropertySource mangoPropertySource) {
        env.getPropertySources().addLast(mangoPropertySource);
        env.setConversionService(conversionService);

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreUnresolvablePlaceholders(false);
        return configurer;
    }

    @Bean
    public EventMulticasterRegistry eventMulticasterRegistry() {
        return new EventMulticasterRegistry();
    }

    @Bean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
    public ApplicationEventMulticaster eventMulticaster(ApplicationContext context, EventMulticasterRegistry eventMulticasterRegistry) {
        return new PropagatingEventMulticaster(context, eventMulticasterRegistry);
    }
}
