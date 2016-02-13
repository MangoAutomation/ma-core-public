/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.provider.Providers;
import com.serotonin.util.properties.ReloadingProperties;

/**
 * 
 * Helper class for testing that helps bring up 
 * an entire or partial Mango instance
 * 
 * @author Terry Packer
 *
 */
public class MangoTestInstance extends Main{

	
	public static void startModules() throws Exception{
		loadModules();
	}
	
	
	public static void start(String envPropertiesName) throws Exception{
        Providers.add(ICoreLicense.class, new CoreLicenseDefinition());

        Common.MA_HOME = System.getProperty("ma.home");

        // Remove the restart flag if it exists.
        new File(Common.MA_HOME, "RESTART").delete();

        // Ensure the environment profile is available.
        Common.envProps = new ReloadingProperties(envPropertiesName);
        Map<String, Boolean> installMap = new HashMap<String, Boolean>();
        openZipFiles(installMap);
        ClassLoader moduleClassLoader = loadModules();

        //Reload the translations here because we will have more from the modules now
        //Clear the Translations cache, be aware that if anything has accessed the Common.systemTranslations this will not change those!
        Translations.clearCache();
        
        Lifecycle lifecycle = new Lifecycle();
        Providers.add(ILifecycle.class, lifecycle);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Providers.get(ILifecycle.class).terminate();
            }
        });

        try {
            lifecycle.initialize(moduleClassLoader, installMap);
            //Moved Browser open into Lifecycle.initialize
        }
        catch (Exception e) {
            lifecycle.terminate();
            LOG.error("Error during initialization", e);
        }
	}
	
}