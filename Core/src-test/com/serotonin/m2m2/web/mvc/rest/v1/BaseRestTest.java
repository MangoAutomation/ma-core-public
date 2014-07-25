/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.BeforeClass;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestInstance;
import com.serotonin.m2m2.db.H2Proxy;
import com.serotonin.m2m2.web.mvc.spring.MangoRestSpringConfiguration;
import com.serotonin.util.properties.ReloadingProperties;

/**
 * @author Terry Packer
 *
 */
public class BaseRestTest {

	protected MockMvc mockMvc;



    @BeforeClass
    public static void setupMango(){
    	
    	
    	Common.envProps = new ReloadingProperties("test-env");
        Common.MA_HOME = "/Users/tpacker/Documents/Work/Infinite/development/git/infiniteautomation/ma-core-public/Core";
    	

    	H2Proxy proxy = new H2Proxy();
        Common.databaseProxy = proxy;
        proxy.initialize(null);
        
    	String envPropertiesName = "test-env";
        String maHome = "/Users/tpacker/Documents/Work/Infinite/development/git/infiniteautomation/ma-core-public/Core";
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
        this.mockMvc = standaloneSetup(controllers).setMessageConverters(MangoRestSpringConfiguration.createMappingJackson2HttpMessageConverter()).build();
    }
    
}
