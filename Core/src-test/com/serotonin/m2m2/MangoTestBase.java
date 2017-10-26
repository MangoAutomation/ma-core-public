/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;
import com.serotonin.timer.SimulationTimer;

/**
 * 
 * Base Class for all JUnit Tests
 * @author Terry Packer
 *
 */
public class MangoTestBase {

    protected static MockMangoLifecycle lifecycle;
    protected static List<ModuleElementDefinition> definitions = new ArrayList<>();
    protected SimulationTimer timer;

	
	public MangoTestBase() {

	}
	
	@BeforeClass
	public static void staticSetup() throws IOException{
	    
	    //Configure Log4j2
        ConfigurationSource source = new ConfigurationSource(MangoTestBase.class.getClass().getResource("/test-log4j2.xml").openStream());
        Configurator.initialize(null, source);
	}
	
	@Before
	public void before() {
	    if(lifecycle == null) {
    	        lifecycle = new MockMangoLifecycle(definitions);
            lifecycle.initialize();
	    }
	    
	    SimulationTimerProvider provider = (SimulationTimerProvider) Providers.get(TimerProvider.class);
	    this.timer = provider.reset();
	}
	
	@AfterClass
	public static void staticTearDown() {
	    if(lifecycle != null) {
	        lifecycle.terminate();
	    }
	}
	
	//Helper Methods
	   /**
     * Delete this file or if a directory all files and directories within
     * @param f
     * @throws IOException
     */
    public static void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (f.exists() && !f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }
    
    /**
     * Validate a vo, fail if invalid
     * @param vo
     */
    public void validate(AbstractVO<?> vo) {
        ProcessResult result = new ProcessResult();
        vo.validate(result);
        if(result.getHasMessages()) {
            String messages = new String();
            for(ProcessMessage message : result.getMessages())
                messages += message.toString(Common.getTranslations()) + " ";
            fail("Validation of " + vo.getClass().getName() + " failed because " + messages);
        }
    }
	
}
