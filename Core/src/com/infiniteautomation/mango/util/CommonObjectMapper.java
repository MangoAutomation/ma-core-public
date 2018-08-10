/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.util;

import java.util.List;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.JacksonModuleDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

/**
 * Wrapper to use Object Mapper Factory methods
 * 
 * @author Terry Packer
 */
public class CommonObjectMapper {
    
    private final ObjectMapper mapper;
    private final LazyInitSupplier<ObjectMapper> restObjectMapper = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean("restObjectMapper");
        if(o == null)
            throw new ShouldNeverHappenException("REST Object Mapper not initialized in Spring Context");
        return (ObjectMapper)o;
    });
    
    public CommonObjectMapper() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setTimeZone(TimeZone.getDefault());
        //Setup Module Defined JSON Modules
        List<JacksonModuleDefinition> defs = ModuleRegistry.getDefinitions(JacksonModuleDefinition.class);
        for(JacksonModuleDefinition def : defs) {
            if(def.getSourceMapperType() == JacksonModuleDefinition.ObjectMapperSource.COMMON)
                mapper.registerModule(def.getJacksonModule());
        }
    }
    
    /**
     * Get a writer for serializing JSON from the common mapper
     * @return
     */
    public ObjectWriter getObjectWriter(Class<?> type) {
        return mapper.writerFor(type);
    }
    
    /**
     * Get default writer, less efficient
     * @return
     */
    public ObjectWriter getObjectWriter() {
        return mapper.writer();
    }
    
    /**
     * Get a reader for use de-serializing JSON
     * @return
     */
    public ObjectReader getObjectReader(Class<?> type) {
        return mapper.readerFor(type);
    }
    
    /**
     * Get default reader, less efficient
     * @return
     */
    public ObjectReader getObjectReader() {
        return mapper.reader();
    }
    
    /**
     * Get a writer for serializing JSON using the Spring configured REST
     * @return
     */
    public ObjectWriter getRestObjectWriter(Class<?> type) {
        return restObjectMapper.get().writerFor(type);
    }
    
    /**
     * Get default writer, less efficient, using the Spring configured REST mapper
     * @return
     */
    public ObjectWriter getRestObjectWriter() {
        return restObjectMapper.get().writer();
    }
    /**
     * Get reader using the Spring configured REST mapper
     * @return
     */
    public ObjectReader getRestObjectReader(Class<?> type) {
        return restObjectMapper.get().readerFor(type);
    }
    /**
     * Get default reader, less efficient, using the Spring configured REST mapper
     * @return
     */
    public ObjectReader getRestObjectReader() {
        return restObjectMapper.get().reader();
    }
}
