/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.infiniteautomation.mango.io.serial.SerialPortManager;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfig;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfigResolver;
import com.infiniteautomation.mango.spring.MangoPropertySource;
import com.infiniteautomation.mango.spring.MangoTestRuntimeContextConfiguration;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.H2InMemoryDatabaseProxy;
import com.serotonin.m2m2.module.EventManagerListenerDefinition;
import com.serotonin.m2m2.module.FreemarkerTemplateLoaderDefinition;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.SystemSettingsListenerDefinition;
import com.serotonin.m2m2.module.license.ITimedLicenseRegistrar;
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
import com.serotonin.m2m2.vo.mailingList.MailingListRecipient;
import com.serotonin.m2m2.vo.mailingList.MailingListRecipientResolver;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;
import com.serotonin.timer.AbstractTimer;
import com.serotonin.util.LifecycleException;

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
    protected List<Module> modules;
    private final List<Runnable> STARTUP_TASKS = new ArrayList<>();
    private final List<Runnable> SHUTDOWN_TASKS = new ArrayList<>();

    //Members to use for non defaults
    protected TimerProvider<AbstractTimer> timer;
    protected EventManager eventManager;
    protected DatabaseProxy db;
    protected RuntimeManager runtimeManager;
    protected SerialPortManager serialPortManager;
    protected MockBackgroundProcessing backgroundProcessing;
    protected ApplicationContext runtimeContext;

    public MockMangoLifecycle(List<Module> modules) {
        this.modules = modules;
    }

    /**
     * Startup a dummy Mango with a basic infrastructure
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws IOException
     */
    public void initialize() throws InterruptedException, ExecutionException {
        //See if we have any translations to load
        try {
            ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
            ClassLoader urlCl = URLClassLoader.newInstance(new URL[]{Paths.get("classes").toUri().toURL()}, prevCl);
            Thread.currentThread().setContextClassLoader(urlCl);
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        }

        Security.addProvider(new BouncyCastleProvider());

        Common.setModuleClassLoader(MockMangoLifecycle.class.getClassLoader());

        Providers.add(ITimedLicenseRegistrar.class, new MockTimedLicenseRegistrar());

        Providers.add(IMangoLifecycle.class, this);

        //TODO Licensing Providers.add(ITimedLicenseRegistrar.class, new TimedLicenseRegistrar());
        Common.free = false;

        //Add in modules
        for(Module module : modules) {
            ModuleRegistry.addModule(module);
        }

        //Startup a simulation timer provider
        Providers.add(TimerProvider.class, getSimulationTimerProvider());

        Common.JSON_CONTEXT.addResolver(new EventTypeResolver(), EventType.class);
        Common.JSON_CONTEXT.addResolver(new BaseTextRenderer.Resolver(), TextRenderer.class);
        Common.JSON_CONTEXT.addResolver(new MailingListRecipientResolver(), MailingListRecipient.class);
        Common.JSON_CONTEXT.addResolver(new VirtualSerialPortConfigResolver(), VirtualSerialPortConfig.class);
        Common.JSON_CONTEXT.addConverter(new MapWrapConverter(), MapWrap.class);

        for (Module module : ModuleRegistry.getModules()) {
            module.preInitialize();
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
        this.runtimeContext = springRuntimeContextInitialize(MockMangoLifecycle.class.getClassLoader()).get();

        //Ensure we start with the proper timer
        Common.backgroundProcessing = getBackgroundProcessing();
        Common.backgroundProcessing.initialize(false);

        for (Module module : ModuleRegistry.getModules()) {
            module.postDatabase();
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

        for (Module module : ModuleRegistry.getModules()) {
            module.postEventManager();
        }

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
            module.postInitialize();
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

        try {
            List<TemplateLoader> loaders = new ArrayList<>();

            // Add the overrides directory.
            Path override = Common.OVERRIDES.resolve(Constants.DIR_FTL);
            if (Files.isDirectory(override)) {
                loaders.add(new FileTemplateLoader(override.toFile()));
            }

            // Add the default template dir
            Path ftlDirectory = Common.MA_HOME_PATH.resolve(Constants.DIR_FTL);
            if (Files.isDirectory(ftlDirectory)) {
                loaders.add(new FileTemplateLoader(ftlDirectory.toFile()));
            }

            // Add template loaders defined by modules.
            for (FreemarkerTemplateLoaderDefinition def : ModuleRegistry.getDefinitions(FreemarkerTemplateLoaderDefinition.class)) {
                try {
                    loaders.add(0, def.getTemplateLoader());
                } catch (IOException e) {
                    LOG.error("Error getting FTL template loader for module " + def.getModule().getName(), e);
                }
            }

            cfg.setTemplateLoader(new MultiTemplateLoader(loaders.toArray(new TemplateLoader[0])));
        } catch (IOException e) {
            LOG.error("Exception defining Freemarker template directory: " + e.getMessage(), e);
        }

        cfg.setObjectWrapper(new DefaultObjectWrapper(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS));
        Common.freemarkerConfiguration = cfg;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public void terminate(TerminationReason reason) {
        Common.runtimeManager.terminate();
        Common.runtimeManager.joinTermination();

        ConfigurableApplicationContext runtimeContext = (ConfigurableApplicationContext) this.runtimeContext;
        // can be null if the lifecycle didn't start completely
        if (runtimeContext != null) {
            runtimeContext.close();
        }

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
    public int getStartupProgress() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getShutdownProgress() {
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

    protected EventManager getEventManager() {
        if(this.eventManager == null)
            return new MockEventManager(false);
        else
            return this.eventManager;
    }

    protected DatabaseProxy getDatabaseProxy() {
        if(this.db == null) {
            boolean enableWebConsole = Common.envProps.getBoolean("db.web.start");
            int webPort = Common.envProps.getInt("db.web.port");
            return new H2InMemoryDatabaseProxy(enableWebConsole, webPort);
        }
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

    public void setTimer(TimerProvider<AbstractTimer> timer) {
        this.timer = timer;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void setDb(DatabaseProxy db) {
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

    @Override
    public void addListener(Consumer<LifecycleState> listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeListener(Consumer<LifecycleState> listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public TerminationReason getTerminationReason() {
        // TODO Auto-generated method stub
        return null;
    }

}
