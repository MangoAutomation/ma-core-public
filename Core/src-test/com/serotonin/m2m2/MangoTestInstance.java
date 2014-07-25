/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2;

import java.io.File;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.CoreLicenseDefinition;
import com.serotonin.m2m2.ICoreLicense;
import com.serotonin.m2m2.ILifecycle;
import com.serotonin.m2m2.Lifecycle;
import com.serotonin.m2m2.Main;
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
	
	
	public static void start(String envPropertiesName, String maHome) throws Exception{
        Providers.add(ICoreLicense.class, new CoreLicenseDefinition());

        Common.MA_HOME = maHome; //System.getProperty("ma.home");

        // Remove the restart flag if it exists.
        new File(Common.MA_HOME, "RESTART").delete();

        // Ensure the environment profile is available.
        Common.envProps = new ReloadingProperties(envPropertiesName);

        openZipFiles();
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
            lifecycle.initialize(moduleClassLoader);
            //Moved Browser open into Lifecycle.initialize
        }
        catch (Exception e) {
            lifecycle.terminate();
            LOG.error("Error during initialization", e);
        }
	}
	
}
