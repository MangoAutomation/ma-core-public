/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.infiniteautomation.mango.io.serial.SerialPortManager;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfig;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfigResolver;
import com.infiniteautomation.mango.spring.MangoPropertySource;
import com.infiniteautomation.mango.spring.MangoTestRuntimeContextConfiguration;
import com.serotonin.m2m2.db.H2InMemoryDatabaseProxy;
import com.serotonin.m2m2.module.EventManagerListenerDefinition;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.SystemSettingsListenerDefinition;
import com.serotonin.m2m2.rt.EventManager;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.EventTypeResolver;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.BackgroundProcessing;
import com.serotonin.m2m2.util.MapWrap;
import com.serotonin.m2m2.util.MapWrapConverter;
import com.serotonin.m2m2.view.text.BaseTextRenderer;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.vo.mailingList.EmailRecipientResolver;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;
import com.serotonin.timer.AbstractTimer;
import com.serotonin.util.LifecycleException;
import com.serotonin.util.properties.MangoProperties;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;

/**
 * Dummy implementation for Mango Lifecycle for use in testing,
 *   override as necessary.
 *
 * @author Terry Packer
 */
public class MockMangoLifecycle implements IMangoLifecycle {

    static Log LOG = LogFactory.getLog(MockMangoLifecycle.class);
    protected boolean enableWebConsole;
    protected int webPort;
    protected List<Module> modules;
    private final List<Runnable> STARTUP_TASKS = new ArrayList<>();
    private final List<Runnable> SHUTDOWN_TASKS = new ArrayList<>();

    //Members to use for non defaults
    protected TimerProvider<AbstractTimer> timer;
    protected EventManager eventManager;
    protected H2InMemoryDatabaseProxy db;
    protected RuntimeManager runtimeManager;
    protected SerialPortManager serialPortManager;
    protected MockBackgroundProcessing backgroundProcessing;

    Path tempPath;
    Path filedataPath;
    Path logsPath;
    Path filestorePath;
    Path moduleDataPath;

    /**
     * Create a default lifecycle with an H2 web console on port 9001
     *   to view the in-memory database.
     */
    public MockMangoLifecycle(List<Module> modules) {
        this(modules, true, 9001);
    }

    public MockMangoLifecycle(List<Module> modules, boolean enableWebConsole, int webPort) {
        this.enableWebConsole = enableWebConsole;
        this.webPort = webPort;
        this.modules = modules;
    }


    /**
     * Startup a dummy Mango with a basic infrastructure
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws IOException
     */
    public void initialize() throws InterruptedException, ExecutionException {
        Common.setModuleClassLoader(Thread.currentThread().getContextClassLoader());

        Providers.add(ICoreLicense.class, new TestLicenseDefinition());
        Providers.add(IMangoLifecycle.class, this);

        //TODO Licensing Providers.add(ITimedLicenseRegistrar.class, new TimedLicenseRegistrar());
        Common.free = false;

        // create temporary paths for data
        try {
            tempPath = Files.createTempDirectory("mango-mock-temp-").toAbsolutePath();
            filedataPath = Files.createTempDirectory("mango-mock-filedata-").toAbsolutePath();
            logsPath = Files.createTempDirectory("mango-mock-logs-").toAbsolutePath();
            filestorePath = Files.createTempDirectory("mango-mock-filestore-").toAbsolutePath();
            moduleDataPath = Files.createTempDirectory("mango-mock-moduleData-").toAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Make sure that Common and other classes are properly loaded
        Common.envProps = getEnvProps();

        //Add in modules
        for(Module module : modules)
            ModuleRegistry.addModule(module);

        //Startup a simulation timer provider
        Providers.add(TimerProvider.class, getSimulationTimerProvider());

        Common.JSON_CONTEXT.addResolver(new EventTypeResolver(), EventType.class);
        Common.JSON_CONTEXT.addResolver(new BaseTextRenderer.Resolver(), TextRenderer.class);
        Common.JSON_CONTEXT.addResolver(new EmailRecipientResolver(), EmailRecipient.class);
        Common.JSON_CONTEXT.addResolver(new VirtualSerialPortConfigResolver(), VirtualSerialPortConfig.class);
        Common.JSON_CONTEXT.addConverter(new MapWrapConverter(), MapWrap.class);

        for (Module module : ModuleRegistry.getModules()) {
            module.preInitialize(true, false);
        }

        freemarkerInitialize();

        //TODO This must be done only once because we have a static
        // final referece to the PointValueDao in the PointValueCache class
        // and so if you try to restart the database it doesn't get the new connection
        // for each new test.
        //Start the Database so we can use Daos (Base Dao requires this)
        if(Common.databaseProxy == null) {
            Common.databaseProxy = getDatabaseProxy();
        }

        if(Common.databaseProxy != null)
            Common.databaseProxy.initialize(null);

        //Setup the Spring Context
        springRuntimeContextInitialize(Thread.currentThread().getContextClassLoader()).get();

        //Ensure we start with the proper timer
        Common.backgroundProcessing = getBackgroundProcessing();
        Common.backgroundProcessing.initialize(false);

        for (Module module : ModuleRegistry.getModules()) {
            module.postDatabase(true, false);
        }

        //Utilities
        //So we can add users etc.
        EventType.initialize();
        SystemEventType.initialize();
        AuditEventType.initialize();

        //Do this last as Event Types have listeners
        for (SystemSettingsListenerDefinition def : ModuleRegistry.getSystemSettingListenerDefinitions())
            def.registerListener();

        //Event Manager
        Common.eventManager = getEventManager();
        Common.eventManager.initialize(false);
        for (EventManagerListenerDefinition def : ModuleRegistry.getDefinitions(EventManagerListenerDefinition.class))
            Common.eventManager.addListener(def);

        Common.runtimeManager = getRuntimeManager();
        Common.runtimeManager.initialize(false);

        if(Common.serialPortManager == null) {
            Common.serialPortManager = getSerialPortManager();
            try{
                Common.serialPortManager.initialize(false);
            }catch(Exception e){
                fail(e.getMessage());
            }
        }

        for (Module module : ModuleRegistry.getModules()) {
            module.postInitialize(true, false);
        }

        for (Runnable task : STARTUP_TASKS){
            try{
                task.run();
            }catch(Exception e){
                fail(e.getMessage());
            }
        }

    }

    protected CompletableFuture<ApplicationContext>  springRuntimeContextInitialize(ClassLoader classLoader) {
        @SuppressWarnings("resource")
        AnnotationConfigApplicationContext runtimeContext = new AnnotationConfigApplicationContext();
        runtimeContext.setClassLoader(classLoader);
        runtimeContext.setId(MangoTestRuntimeContextConfiguration.CONTEXT_ID);
        runtimeContext.getEnvironment().getPropertySources().addLast(new MangoPropertySource("envProps", Common.envProps));
        runtimeContext.register(MangoTestRuntimeContextConfiguration.class);
        runtimeContext.refresh();
        runtimeContext.start();

        return MangoTestRuntimeContextConfiguration.getFutureRuntimeContext();
    }

    private void freemarkerInitialize() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_28);
        File baseTemplateDir = Common.MA_HOME_PATH.resolve(Constants.DIR_FTL).toFile();
        if(!baseTemplateDir.exists()) {
            LOG.info("Not initializing Freemarker, this test is not running in Core source tree.  Requires ./ftl directory to initialize.");
            return;
        }

        try {
            List<TemplateLoader> loaders = new ArrayList<>();

            // Add the overrides directory.
            File override = Common.OVERRIDES.resolve(Constants.DIR_FTL).toFile();
            if (override.exists())
                loaders.add(new FileTemplateLoader(override));

            // Add the default template dir
            loaders.add(new FileTemplateLoader(baseTemplateDir));

            // Add template dirs defined by modules.
            for (Module module : ModuleRegistry.getModules()) {
                if (module.getEmailTemplateDirs() != null) {
                    for(String templateDir : module.getEmailTemplateDirs()) {
                        loaders.add(0, new FileTemplateLoader(module.modulePath().resolve(templateDir).toFile()));
                    }
                }
            }

            cfg.setTemplateLoader(new MultiTemplateLoader(loaders.toArray(new TemplateLoader[loaders.size()])));
        }
        catch (IOException e) {
            throw new RuntimeException("Exception defining Freemarker template directory", e);
        }

        cfg.setObjectWrapper(new DefaultObjectWrapper());
        Common.freemarkerConfiguration = cfg;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public void terminate() {
        //        H2InMemoryDatabaseProxy proxy = (H2InMemoryDatabaseProxy) Common.databaseProxy;
        //        try {
        //            proxy.clean();
        //        } catch (Exception e) {
        //            throw new ShouldNeverHappenException(e);
        //        }
        if(Common.databaseProxy != null)
            Common.databaseProxy.terminate(true);

        if(Common.serialPortManager != null) {
            try {
                Common.serialPortManager.terminate();
            } catch (LifecycleException e) {
                fail(e.getMessage());
            }
            Common.serialPortManager.joinTermination();
        }

        Exception failed = null;
        if (tempPath != null) {
            try {
                FileUtils.deleteDirectory(tempPath.toFile());
            } catch (IOException e) {
                failed = e;
            }
        }
        if (filedataPath != null) {
            try {
                FileUtils.deleteDirectory(filedataPath.toFile());
            } catch (IOException e) {
                failed = e;
            }
        }
        if (logsPath != null) {
            try {
                FileUtils.deleteDirectory(logsPath.toFile());
            } catch (IOException e) {
                failed = e;
            }
        }
        if (filestorePath != null) {
            try {
                FileUtils.deleteDirectory(filestorePath.toFile());
            } catch (IOException e) {
                failed = e;
            }
        }
        if (moduleDataPath != null) {
            try {
                FileUtils.deleteDirectory(moduleDataPath.toFile());
            } catch (IOException e) {
                failed = e;
            }
        }
        if (failed != null) {
            throw new RuntimeException(failed);
        }
    }

    @Override
    public void addStartupTask(Runnable task) {
        STARTUP_TASKS.add(task);
    }

    @Override
    public void addShutdownTask(Runnable task) {
        SHUTDOWN_TASKS.add(task);
    }

    @Override
    public LifecycleState getLifecycleState() {
        return LifecycleState.RUNNING;
    }

    @Override
    public float getStartupProgress() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getShutdownProgress() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void loadLic() {
        // TODO Auto-generated method stub

    }

    @Override
    public Integer dataPointLimit() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Thread scheduleShutdown(Long timeout, boolean b, PermissionHolder user) {

        return null;
    }

    @Override
    public boolean isRestarting() {
        return false;
    }

    @Override
    public void reloadSslContext() {
    }

    protected TimerProvider<AbstractTimer>  getSimulationTimerProvider() {
        if(this.timer == null)
            return new SimulationTimerProvider();
        else
            return this.timer;
    }

    protected MangoProperties getEnvProps() {
        MockMangoProperties props = new MockMangoProperties();
        props.setDefaultValue("paths.temp", tempPath.toString());
        props.setDefaultValue("paths.filedata", filedataPath.toString());
        props.setDefaultValue("filestore.location", filestorePath.toString());
        props.setDefaultValue("moduleData.location", moduleDataPath.toString());
        props.setDefaultValue("paths.logs", logsPath.toString());
        return props;
    }

    protected EventManager getEventManager() {
        if(this.eventManager == null)
            return new MockEventManager(false);
        else
            return this.eventManager;
    }

    protected H2InMemoryDatabaseProxy getDatabaseProxy() {
        if(this.db == null)
            return new H2InMemoryDatabaseProxy(enableWebConsole, webPort);
        else
            return this.db;
    }

    protected RuntimeManager getRuntimeManager() {
        if(this.runtimeManager == null)
            return new MockRuntimeManager(true);
        else
            return this.runtimeManager;
    }

    protected SerialPortManager getSerialPortManager() {
        if(this.serialPortManager == null)
            return new MockSerialPortManager();
        else
            return this.serialPortManager;
    }

    protected BackgroundProcessing getBackgroundProcessing() {
        if(this.backgroundProcessing == null)
            return new MockBackgroundProcessing(Providers.get(TimerProvider.class).getTimer());
        else
            return this.backgroundProcessing;
    }

    public boolean isEnableWebConsole() {
        return enableWebConsole;
    }

    public void setEnableWebConsole(boolean enableWebConsole) {
        this.enableWebConsole = enableWebConsole;
    }

    public void setWebPort(int webPort) {
        this.webPort = webPort;
    }

    public void setTimer(TimerProvider<AbstractTimer> timer) {
        this.timer = timer;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void setDb(H2InMemoryDatabaseProxy db) {
        this.db = db;
    }

    public void setRuntimeManager(RuntimeManager runtimeManager) {
        this.runtimeManager = runtimeManager;
    }

    public void setSerialPortManager(SerialPortManager serialPortManager) {
        this.serialPortManager = serialPortManager;
    }

    public void setBackgroundProcessing(MockBackgroundProcessing backgroundProcessing) {
        this.backgroundProcessing = backgroundProcessing;
    }

    @Override
    public boolean verifyProperties(InputStream in, boolean signed, Map<String, String> verify) {
        return true;
    }

}
