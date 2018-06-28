/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceDefinition;
import com.serotonin.m2m2.web.dwr.emport.ImportTask;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;
import com.serotonin.timer.SimulationTimer;

/**
 * 
 * Base Class for all JUnit Tests
 * 
 * To enable the in-memory H2 database's web console 
 *   use the correct constructor OR supply the system propertites:
 *   
 *   mango.test.enableH2Web=true
 *   mango.test.h2WebPort=<port>
 *   
 *   Add any modules to load prior to the @Before method
 * 
 * @author Terry Packer
 *
 */
public class MangoTestBase {

    protected final Log LOG = LogFactory.getLog(MangoTestBase.class);
    
    protected static MockMangoLifecycle lifecycle;
    protected static List<Module> modules = new ArrayList<>();
    
    protected SimulationTimer timer;
    protected long testTime = 0l; //time during test

    protected boolean enableH2Web;
    protected int h2WebPort;
	
	public MangoTestBase() {
        enableH2Web = false;
        h2WebPort = 9001;
        String prop = System.getProperty("mango.test.enableH2Web");
        if(prop != null)
            enableH2Web = Boolean.parseBoolean(prop);
        else 
            enableH2Web = false;
        
        prop = System.getProperty("mango.test.h2WebPort");
        if(prop != null)
            h2WebPort = Integer.parseInt(prop);
        else
            h2WebPort = 9001;
	}
	
	public MangoTestBase(boolean enableH2Web, int h2WebPort) {
	    this.enableH2Web = enableH2Web;
	    this.h2WebPort = h2WebPort;
	}
	
	@BeforeClass
	public static void staticSetup() throws IOException{
	    
	    //Configure Log4j2
        ConfigurationSource source = new ConfigurationSource(ClassLoader.getSystemResource("test-log4j2.xml").openStream());
        Configurator.initialize(null, source);
        
        List<ModuleElementDefinition> definitions = new ArrayList<>();
        definitions.add(new MockDataSourceDefinition());
        addModule("BaseTest", definitions);
	}
	
	@Before
	public void before() {
	    //So it only happens once per class for now (problems with restarting lifecycle during a running JVM)
	    if(lifecycle == null) {
    	        lifecycle = getLifecycle();
            lifecycle.initialize();
	    }
	    
	    SimulationTimerProvider provider = (SimulationTimerProvider) Providers.get(TimerProvider.class);
	    this.timer = provider.getSimulationTimer();
	}
	
	@After
	public void after() {
        SimulationTimerProvider provider = (SimulationTimerProvider) Providers.get(TimerProvider.class);
        provider.reset();
        Common.runtimeManager.terminate();
        Common.runtimeManager.joinTermination();
	    H2InMemoryDatabaseProxy proxy = (H2InMemoryDatabaseProxy) Common.databaseProxy;
        try {
            proxy.clean();
        } catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
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
	
    /**
     * Add a module to the test.  Before the before() method is called.
     * @param name
     * @param definitions
     * @return
     */
    protected static void addModule(String name, List<ModuleElementDefinition> definitions) {
        
        MangoTestModule module = new MangoTestModule(name);
        
        for(ModuleElementDefinition definition : definitions)
            module.addDefinition(definition);
        
        modules.add(module);
    }
    
    /**
     * Load a default test JSON Configuration into Mango
     * @throws JsonException
     * @throws IOException
     * @throws URISyntaxException
     */
    protected void loadDefaultConfiguration() throws JsonException, IOException, URISyntaxException {
        File cfg = new File(MangoTestBase.class.getResource("/testMangoConfig.json").toURI());
        loadConfiguration(cfg);
    }
    
    protected void loadConfiguration(File jsonFile) throws JsonException, IOException, URISyntaxException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile), Common.UTF8_CS));
        JsonReader jr = new JsonReader(reader);
        JsonObject jo = jr.read(JsonObject.class);
        
        ImportTask task = new ImportTask(jo, Common.getTranslations(), null, false);
        task.run(Common.timer.currentTimeMillis());
        if(task.getResponse().getHasMessages()){
            for(ProcessMessage message : task.getResponse().getMessages()){
               switch(message.getLevel()) {
                   case error:
                   case warning:
                       fail(message.toString(Common.getTranslations()));
                   case info:
                       LOG.info(message.toString(Common.getTranslations()));
               }
            }
        }
    }
    
    /**
     * Create users with password=password and supplied permissions
     * @param count
     * @param permissions
     * @return
     */
    protected List<User> createUsers(int count, String permissions){
        List<User> users = new ArrayList<>();
        for(int i=0; i<count; i++) {
            User user = new User();
            user.setId(Common.NEW_ID);
            user.setName("User" + i);
            user.setUsername("user" + i);
            user.setPassword(Common.encrypt("password"));
            user.setEmail("user" + i + "@yourMangoDomain.com");
            user.setPhone("");
            user.setPermissions(permissions);
            user.setDisabled(false);
            validate(user);
            
            UserDao.instance.saveUser(user);
            users.add(user);
        }
        return users;
    }
    
    /**
     * Fast Forward the Simulation Timer in a separate thread and wait
     * for some time.
     * 
     * Useful when you have scheduled tasks that do not end, the sim timer
     * won't stop fast forwarding.
     * 
     * @param until
     * @param step
     */
    protected void waitAndExecute(final long until, final long step) {
        //TODO Could wait until sync completed event is fired here by creating a mock event handler
        //TODO Could add fastForwardToInOtherThread method to timer...
        new Thread() {
            /* (non-Javadoc)
             * @see java.lang.Thread#run()
             */
            @Override
            public void run() {
                long time = timer.currentTimeMillis();
                while(timer.currentTimeMillis() < until) {
                    time = time + step;
                    timer.fastForwardTo(time);
                }
            }
        }.start();

        
        while(timer.currentTimeMillis() < until) {
             try {
                 Thread.sleep(100);
             } catch (InterruptedException e) {
                 fail(e.getMessage());
             }
         }
    }
    
    /**
     * Override as necessary
     * @return
     */
    protected MockMangoLifecycle getLifecycle() {
        return new MockMangoLifecycle(modules, enableH2Web, h2WebPort);
    }
}
