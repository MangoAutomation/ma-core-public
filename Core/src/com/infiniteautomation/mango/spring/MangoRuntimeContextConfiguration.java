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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.infiniteautomation.mango.rest.v2.mapping.MangoRestV2JacksonModule;
import com.infiniteautomation.mango.spring.components.MangoExecutors;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.JacksonModuleDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.rest.v1.mapping.JScienceModule;
import com.serotonin.m2m2.web.mvc.rest.v1.mapping.MangoCoreModule;

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

    public static final String REST_OBJECT_MAPPER_NAME = "restObjectMapper";
    public static final String COMMON_OBJECT_MAPPER_NAME = "commonObjectMapper";
    public static final String SCHEDULED_EXECUTOR_SERVICE_NAME = "scheduledExecutorService";
    public static final String EXECUTOR_SERVICE_NAME = "executorService";

    @Primary
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

    // ScheduledExecutorService cannot be annotated with @Primary as it is also a ExecutorService
    @Bean(SCHEDULED_EXECUTOR_SERVICE_NAME)
    public ScheduledExecutorService scheduledExecutorService(MangoExecutors executors) {
        return executors.getScheduledExecutor();
    }

    @Primary
    @Bean(EXECUTOR_SERVICE_NAME)
    public ExecutorService executorService(MangoExecutors executors) {
        return executors.getExecutor();
    }

}
