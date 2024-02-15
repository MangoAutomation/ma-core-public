/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.annotation.HandlesTypes;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.context.WebApplicationContext;

import com.infiniteautomation.mango.io.serial.SerialPortManager;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfig;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfigResolver;
import com.infiniteautomation.mango.spring.MangoCommonConfiguration;
import com.infiniteautomation.mango.spring.MangoPropertySource;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.MangoTestRuntimeContextConfiguration;
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
import com.serotonin.m2m2.view.text.BaseTextRenderer.Resolver;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.mailingList.MailingListRecipient;
import com.serotonin.m2m2.vo.mailingList.MailingListRecipientResolver;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.web.OverridingWebAppContext;
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

    static Logger LOG = LoggerFactory.getLogger(MockMangoLifecycle.class);
    protected List<Module> modules;
    private final List<Runnable> STARTUP_TASKS = new ArrayList<>();
    private final List<Runnable> SHUTDOWN_TASKS = new ArrayList<>();

    //Members to use for non defaults
    protected TimerProvider<AbstractTimer> timer;
    protected EventManager eventManager;
    protected RuntimeManager runtimeManager;
    protected SerialPortManager serialPortManager;
    protected MockBackgroundProcessing backgroundProcessing;
    protected ApplicationContext runtimeContext;
    private final CompletableFuture<Void> runningFuture = new CompletableFuture<>();

    private boolean initializeWebServer = false;
    private final ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
    private Server webServer;
    private OverridingWebAppContext webAppContext;

    public MockMangoLifecycle(List<Module> modules) {
        this.modules = modules;
    }

    /**
     * Startup a dummy Mango with a basic infrastructure
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
        Common.JSON_CONTEXT.addResolver(new Resolver(), TextRenderer.class);
        Common.JSON_CONTEXT.addResolver(new MailingListRecipientResolver(), MailingListRecipient.class);
        Common.JSON_CONTEXT.addResolver(new VirtualSerialPortConfigResolver(), VirtualSerialPortConfig.class);
        Common.JSON_CONTEXT.addConverter(new MapWrapConverter(), MapWrap.class);

        for (Module module : ModuleRegistry.getModules()) {
            module.preInitialize();
        }

        freemarkerInitialize();

        //To ensure the EventManager bean is created with the right reference
        Common.eventManager = getEventManager();

        //Setup the Spring Context
        var classLoader = MockMangoLifecycle.class.getClassLoader();
        this.runtimeContext = springRuntimeContextInitialize(classLoader).get();

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

        //Event Manager init
        Common.eventManager.initialize(false);
        for (EventManagerListenerDefinition def : ModuleRegistry.getDefinitions(EventManagerListenerDefinition.class))
            Common.eventManager.addListener(def);

        for (Module module : ModuleRegistry.getModules()) {
            module.postEventManager();
        }

        Common.runtimeManager = runtimeContext.getBean(RuntimeManager.class);
        Common.runtimeManager.initialize(false);

        if (initializeWebServer) {
            initializeWebServer();
            initializeWebAppContext(classLoader);
        }

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

        // Run in current thread so that security context is set
        runningFuture.complete(null);
    }

    private final Set<Class<?>> runtimeContextConfigurations = new LinkedHashSet<>();
    {
        runtimeContextConfigurations.add(MangoTestRuntimeContextConfiguration.class);
    }
    private final Map<String, BeanDefinition> beanDefinitions = new HashMap<>();

    public MockMangoLifecycle addRuntimeContextConfiguration(Class<?> clazz) {
        runtimeContextConfigurations.add(clazz);
        return this;
    }

    public MockMangoLifecycle addBeanDefinition(Class<?> clazz) {
        GenericBeanDefinition def = new GenericBeanDefinition();
        def.setBeanClass(clazz);
        return addBeanDefinition(def.getBeanClassName(), def);
    }

    public MockMangoLifecycle addBeanDefinition(String name, BeanDefinition beanDefinition) {
        beanDefinitions.put(name, beanDefinition);
        return this;
    }

    protected CompletableFuture<ApplicationContext> springRuntimeContextInitialize(ClassLoader classLoader) {
        AnnotationConfigApplicationContext runtimeContext = new AnnotationConfigApplicationContext();
        runtimeContext.setClassLoader(classLoader);
        runtimeContext.setId(MangoRuntimeContextConfiguration.CONTEXT_ID);
        runtimeContext.getEnvironment().getPropertySources().addLast(new MangoPropertySource("envProps", Common.envProps));
        runtimeContext.register(runtimeContextConfigurations.toArray(new Class<?>[] {}));
        for (var entry : beanDefinitions.entrySet()) {
            runtimeContext.registerBeanDefinition(entry.getKey(), entry.getValue());
        }
        runtimeContext.refresh();
        runtimeContext.start();
        return MangoRuntimeContextConfiguration.getFutureRuntimeContext();
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

        // set the authentication for the terminate thread's security context, enables the use of services requiring a user in the
        // module lifecycle hooks
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(new PreAuthenticatedAuthenticationToken(PermissionHolder.SYSTEM_SUPERADMIN, null));

        // Pre-terminate
        for (Module module : ModuleRegistry.getModules()) {
            try {
                module.preTerminate(module.isMarkedForDeletion());
            } catch (Throwable e) {
                LOG.error("Error in preTerminate of module '" + module.getName() + "': " + e.getMessage(), e);
            }
        }

        if (webAppContext != null) {
            try {
                webAppContext.stop();
            } catch (Exception e) {
                LOG.error("Failed to stop web app context", e);
            }
        }
        if (webServer != null) {
            try {
                webServer.stop();
            } catch (Exception e) {
                LOG.error("Failed to stop web server", e);
            }
        }

        try {
            Common.runtimeManager.terminate();
            Common.runtimeManager.joinTermination();
        } catch (DataAccessException e) {
            // database has been cleared by the time the lifecycle terminates
            // see https://radixiot.atlassian.net/browse/MANGO-317
        }

        for (Module module : ModuleRegistry.getModules()) {
            try {
                module.postRuntimeManagerTerminate(module.isMarkedForDeletion());
            } catch (Throwable e) {
                LOG.error("Error in postRuntimeManagerTerminate of module '" + module.getName() + "': " + e.getMessage(), e);
            }
        }

        Common.backgroundProcessing.terminate();
        Common.backgroundProcessing.joinTermination();

        Common.eventManager.terminate();
        Common.eventManager.joinTermination();

        ConfigurableApplicationContext runtimeContext = (ConfigurableApplicationContext) this.runtimeContext;
        // can be null if the lifecycle didn't start completely
        if (runtimeContext != null) {
            runtimeContext.close();
        }

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
    public Thread scheduleShutdown(Long timeout, boolean restart, PermissionHolder user) {
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

    public ApplicationContext getRuntimeContext() {
        return runtimeContext;
    }

    public MockMangoLifecycle setInitializeWebServer(boolean initializeWebServer) {
        this.initializeWebServer = initializeWebServer;
        return this;
    }

    private void initializeWebAppContext(ClassLoader classLoader) {
        this.webAppContext = new OverridingWebAppContext();
        this.webAppContext.setClassLoader(classLoader);

        var servletContainerInitializers = MangoCommonConfiguration.beansOfTypeIncludingAncestors(this.runtimeContext,
            ServletContainerInitializer.class);
        var containerInitializers = servletContainerInitializers.stream().map(initializer -> {
            HandlesTypes types = initializer.getClass().getAnnotation(HandlesTypes.class);
            return new ContainerInitializer(initializer, types != null ? types.value() : null);
        }).collect(Collectors.toList());

        webAppContext.setAttribute(AnnotationConfiguration.CONTAINER_INITIALIZERS, containerInitializers);
        webAppContext.addBean(new ServletContainerInitializersStarter(webAppContext), true);
        webAppContext.setThrowUnavailableOnStartupException(true);

        try {
            handlerCollection.addHandler(webAppContext);
            webAppContext.start();
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't initialize web application context", e);
        }
    }

    private void initializeWebServer() {
        this.webServer = new Server();

        //General Http Configuration
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendXPoweredBy(false);
        httpConfig.setOutputAggregationSize(httpConfig.getOutputBufferSize());

        //Add Http 1.1 and 2
        ServerConnector conn = new ServerConnector(webServer, new HttpConnectionFactory(httpConfig), new HTTP2CServerConnectionFactory(httpConfig));
        conn.setPort(0);
        conn.setHost("localhost");
        conn.setIdleTimeout(Duration.ofSeconds(70).toMillis());

        //Add the HTTP Connector
        webServer.addConnector(conn);
        webServer.setHandler(handlerCollection);
        try {
            webServer.start();
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't initialize web server", e);
        }
    }

    public ServerConnector getWebServerConnector() {
        return (ServerConnector) Objects.requireNonNull(webServer).getConnectors()[0];
    }

    public Server getWebServer() {
        return webServer;
    }

    /**
     * @return The Jetty web application context
     */
    public OverridingWebAppContext getWebAppContext() {
        return webAppContext;
    }

    /**
     * @return The Spring root web application context
     */
    public WebApplicationContext getRootWebAppContext() {
        return MangoRuntimeContextConfiguration.getRootWebContext();
    }

    @Override
    public ServerStatus getServerStatus() {
        return IMangoLifecycle.super.getServerStatus();
    }

    @Override
    public boolean isSafeMode() {
        return IMangoLifecycle.super.isSafeMode();
    }
}
