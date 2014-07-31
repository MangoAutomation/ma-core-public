/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest;

import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.BeforeClass;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestInstance;
import com.serotonin.m2m2.db.H2Proxy;
import com.serotonin.m2m2.web.mvc.spring.MangoRestSpringConfiguration;
import com.serotonin.util.properties.ReloadingProperties;

/**
 * @See http://spring.io/guides/tutorials/rest/2/ 
 * 
 * 
 * @author Terry Packer
 *
 */
public class BaseRestTest {

	protected MockMvc mockMvc;
	
	protected ObjectMapper objectMapper; //Access to Object mapper configured for Mango Json 


    @BeforeClass
    public static void setupMango(){
    	
    	
    	Common.envProps = new ReloadingProperties("test-env");
        Common.MA_HOME = "/Users/tpacker/Documents/Work/Infinite/development/git/infiniteautomation/ma-core-public/Core";
    	
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
    	this.objectMapper = MangoRestSpringConfiguration.createObjectMapper();
    	
    	MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    	converter.setObjectMapper(this.objectMapper);
    	
        this.mockMvc = standaloneSetup(controllers).setMessageConverters(converter).build();
    }
    
}
