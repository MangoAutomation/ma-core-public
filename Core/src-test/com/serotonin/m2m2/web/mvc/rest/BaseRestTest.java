/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest;

import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.mockito.Mock;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestInstance;
import com.serotonin.m2m2.db.H2Proxy;
import com.serotonin.m2m2.db.dao.DaoRegistry;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.web.mvc.rest.v1.ExceptionHandlingController;
import com.serotonin.m2m2.web.mvc.spring.MangoRestSpringConfiguration;
import com.serotonin.util.properties.ReloadingProperties;

/**
 * @See http://spring.io/guides/tutorials/rest/2/ 
 * 
 * For all Tests ma.home must be set just like when running Mango
 * 
 * 
 * @author Terry Packer
 *
 */
public class BaseRestTest {

	protected MockMvc mockMvc;
	
	protected ObjectMapper objectMapper; //Access to Object mapper configured for Mango Json 

    @Mock
	protected UserDao userDao;
    @Mock
    protected DataSourceDao dataSourceDao;
    @Mock
    protected DataPointDao dataPointDao;
	
    /**
     * Setup DAO Layer
     * this must be called after MockitoAnnotations.initMocks()
     * in the subclass
     */
	public void setup(){

	}

    @BeforeClass
    public static void setupMango(){
    	
    	
    	Common.envProps = new ReloadingProperties("test-env");
        Common.MA_HOME =  System.getProperty("ma.home"); 
    	
        //Start the Database so we can use Daos (Base Dao requires this)
    	H2Proxy proxy = new H2Proxy();
        Common.databaseProxy = proxy;
        proxy.initialize(null);
        
        try {
			MangoTestInstance.startModules();
		} catch (Exception e) {
			fail(e.getMessage());
		}
        
    }
    
    /**
     * 
     * @param controllers
     */
    protected void setupMvc(Object... controllers){
    	
        //Mock our Daos so they
        // return exactly what we want.
    	DaoRegistry.dataPointDao = this.dataPointDao;
    	DaoRegistry.dataSourceDao = this.dataSourceDao;
    	DaoRegistry.userDao = this.userDao;
    	
    	this.objectMapper = MangoRestSpringConfiguration.objectMapper;
    	
    	MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    	converter.setObjectMapper(this.objectMapper);
    	
    	List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
    	converters.add(converter);
    	
        this.mockMvc = standaloneSetup(controllers)
        		.setMessageConverters(converter)
        		.setHandlerExceptionResolvers(createHandlerExceptionResolvers(converters))
        		.build();
    }

    /**
     * Create Handlers for use in Testing
     * @return
     */
    private ExceptionHandlerExceptionResolver createHandlerExceptionResolvers(final List<HttpMessageConverter<?>> converters) {
    	
        ExceptionHandlerExceptionResolver exceptionResolver = new ExceptionHandlerExceptionResolver() {
        	
        	/* (non-Javadoc)
        	 * @see org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver#getMessageConverters()
        	 */
        	@Override
        	public List<HttpMessageConverter<?>> getMessageConverters() {
        		return converters;
        	}
        	@Override
            protected ServletInvocableHandlerMethod getExceptionHandlerMethod(HandlerMethod handlerMethod, Exception exception) {
            	
                Method method = new ExceptionHandlerMethodResolver(ExceptionHandlingController.class).resolveMethod(exception);
                return new ServletInvocableHandlerMethod(new ExceptionHandlingController(), method);
            }
        };
        exceptionResolver.afterPropertiesSet();
        return exceptionResolver;
    }
}
