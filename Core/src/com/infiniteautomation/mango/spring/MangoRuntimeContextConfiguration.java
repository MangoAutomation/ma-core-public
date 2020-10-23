/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import javax.script.ScriptEngineManager;
import javax.sql.DataSource;

import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.tools.StopWatchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.infiniteautomation.mango.monitor.MonitoredValues;
import com.infiniteautomation.mango.spring.components.RegisterModuleElementDefinitions;
import com.infiniteautomation.mango.spring.components.executors.MangoExecutors;
import com.infiniteautomation.mango.spring.eventMulticaster.EventMulticasterRegistry;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.infiniteautomation.mango.spring.session.MangoCachingSessionDataStore;
import com.infiniteautomation.mango.spring.session.MangoJdbcSessionDataStore;
import com.infiniteautomation.mango.spring.session.MangoSessionDataMap;
import com.infiniteautomation.mango.spring.session.MangoSessionDataStore;
import com.infiniteautomation.mango.spring.session.NullMangoSessionDataStore;
import com.serotonin.db.SpringConnectionProvider;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.MangoSessionDataDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.JacksonModuleDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsEventDispatcher;
import com.serotonin.m2m2.web.mvc.spring.MangoRootWebContextConfiguration;
import com.serotonin.provider.Providers;

/**
 *
 * @author Terry Packer
 */
@Configuration
@Import({MangoCommonConfiguration.class, RegisterModuleElementDefinitions.class})
@ComponentScan(basePackages = {
        "com.infiniteautomation.mango.spring",  //General Runtime Spring Components
        "com.serotonin.m2m2.db.dao" //DAOs
})
public class MangoRuntimeContextConfiguration {
    public static final String CONTEXT_ID = "runtimeContext";
    private static final CompletableFuture<ApplicationContext> RUNTIME_CONTEXT_FUTURE = new CompletableFuture<>();
    private static final CompletableFuture<WebApplicationContext> ROOT_WEB_CONTEXT_FUTURE = new CompletableFuture<>();

    /**
     * <p>Gets the spring runtime application context, only set after the context is refreshed (started).
     * If its not null, its safe to use.</p>
     *
     * @return the Spring runtime application context if it has been refreshed, otherwise null
     */
    public static ApplicationContext getRuntimeContext() {
        return RUNTIME_CONTEXT_FUTURE.getNow(null);
    }

    /**
     * <p>Gets the spring runtime application context as a future that is completed when the context is refreshed</p>
     *
     * @return future which is completed when runtime context has refreshed
     */
    public static CompletableFuture<ApplicationContext> getFutureRuntimeContext() {
        // thenApply() prevents the user of this API from completing the original CompletableFuture
        return RUNTIME_CONTEXT_FUTURE.thenApply(Function.identity());
    }

    /**
     * Gets the spring root web application context, only set after the context is refreshed (started).
     * If its not null, its safe to use.
     *
     * Prefer getting the application context from the request/servlet context if possible:
     * <p><code>WebApplicationContext webApplicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());</code></p>
     *
     * @return the Spring root web application context if it has been refreshed, otherwise null
     */
    public static WebApplicationContext getRootWebContext() {
        return ROOT_WEB_CONTEXT_FUTURE.getNow(null);
    }

    /**
     * <p>Gets the spring root web application context as a future that is completed when the context is refreshed</p>
     *
     * @return future which is completed when the root web application context has refreshed
     */
    public static CompletableFuture<WebApplicationContext> getFutureRootWebContext() {
        // thenApply() prevents the user of this API from completing the original CompletableFuture
        return ROOT_WEB_CONTEXT_FUTURE.thenApply(Function.identity());
    }

    public static final String REST_OBJECT_MAPPER_NAME = "restObjectMapper";
    public static final String COMMON_OBJECT_MAPPER_NAME = "commonObjectMapper";
    public static final String DAO_OBJECT_MAPPER_NAME = "daoObjectMapper";
    public static final String SCHEDULED_EXECUTOR_SERVICE_NAME = "scheduledExecutorService";
    public static final String EXECUTOR_SERVICE_NAME = "executorService";
    public static final String SYSTEM_SUPERADMIN_PERMISSION_HOLDER = "systemSuperadminPermissionHolder";
    public static final String ANONYMOUS_PERMISSION_HOLDER = "anonymousPermissionHolder";
    public static final String SYSTEM_SETTING_DAO_NAME = "systemSettingsDao";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * ContextStartedEvent is not fired for root web context, only ContextRefreshedEvent.
     * Initialize our static holders in this event handler.
     *
     * @param event
     */
    @EventListener
    private void contextRefreshed(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();

        if (log.isInfoEnabled()) {
            log.info("Spring context '" + context.getId() +"' refreshed: " + context.getDisplayName());
        }

        if (CONTEXT_ID.equals(context.getId())) {
            RUNTIME_CONTEXT_FUTURE.complete(context);
        } else if (MangoRootWebContextConfiguration.CONTEXT_ID.equals(context.getId()) && context instanceof WebApplicationContext) {
            ROOT_WEB_CONTEXT_FUTURE.complete((WebApplicationContext) context);
        }
    }

    @EventListener
    private void contextStarted(ContextStartedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        if (log.isInfoEnabled()) {
            log.info("Spring context '" + context.getId() +"' started: " + context.getDisplayName());
        }
    }

    @Bean(REST_OBJECT_MAPPER_NAME)
    public static ObjectMapper getObjectMapper() {
        // For raw Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        if(Common.envProps.getBoolean("rest.indentJSON", false))
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        //Setup Module Defined JSON Modules
        List<JacksonModuleDefinition> defs = ModuleRegistry.getDefinitions(JacksonModuleDefinition.class);
        for(JacksonModuleDefinition def : defs) {
            if(def.getSourceMapperType() == JacksonModuleDefinition.ObjectMapperSource.REST)
                objectMapper.registerModule(def.getJacksonModule());
        }

        //Always output dates in ISO 8601
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        objectMapper.setDateFormat(dateFormat);

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setTimeZone(TimeZone.getDefault()); //Set to system tz

        //This will allow messy JSON to be imported even if all the properties in it are part of the POJOs
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Bean(COMMON_OBJECT_MAPPER_NAME)
    public ObjectMapper getCommonObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setTimeZone(TimeZone.getDefault());

        //Setup Module Defined JSON Modules
        List<JacksonModuleDefinition> defs = ModuleRegistry.getDefinitions(JacksonModuleDefinition.class);
        for(JacksonModuleDefinition def : defs) {
            if(def.getSourceMapperType() == JacksonModuleDefinition.ObjectMapperSource.COMMON)
                mapper.registerModule(def.getJacksonModule());
        }

        //This will allow messy JSON to be imported even if all the properties in it are part of the POJOs
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }

    @Bean(DAO_OBJECT_MAPPER_NAME)
    public ObjectMapper getDaoObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setTimeZone(TimeZone.getTimeZone("UTC")); //Set to UTC in case timezone change while data is in database

        //Setup Module Defined JSON Modules
        List<JacksonModuleDefinition> defs = ModuleRegistry.getDefinitions(JacksonModuleDefinition.class);
        for(JacksonModuleDefinition def : defs) {
            if(def.getSourceMapperType() == JacksonModuleDefinition.ObjectMapperSource.DATABASE)
                mapper.registerModule(def.getJacksonModule());
        }

        //This will allow messy JSON to be imported even if all the properties in it are part of the POJOs
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }

    // ScheduledExecutorService cannot be annotated with @Primary as it is also an ExecutorService
    @Bean(SCHEDULED_EXECUTOR_SERVICE_NAME)
    public ScheduledExecutorService scheduledExecutorService(MangoExecutors executors) {
        return executors.getScheduledExecutor();
    }

    @Primary
    @Bean(EXECUTOR_SERVICE_NAME)
    public ExecutorService executorService(MangoExecutors executors) {
        return executors.getExecutor();
    }

    /**
     * TaskExecutor that is used by Spring @Async annotated methods, propagates the SecurityContext to the new thread (and clears it when done)
     */
    @Primary
    @Bean
    public TaskExecutor taskExecutor(ExecutorService executor) {
        return new DelegatingSecurityContextAsyncTaskExecutor(new TaskExecutorAdapter(executor));
    }

    /**
     * J.W. This is used to convert properties from strings into lists of integers etc when they are injected by Spring
     * @return
     */
    @Bean
    public static ConfigurableConversionService conversionService() {
        return new DefaultConversionService();
    }

    @Bean
    public EventMulticasterRegistry eventMulticasterRegistry() {
        return new EventMulticasterRegistry();
    }

    @Bean
    public IMangoLifecycle lifecycle() {
        return Providers.get(IMangoLifecycle.class);
    }

    @Bean(SYSTEM_SUPERADMIN_PERMISSION_HOLDER)
    public PermissionHolder systemSuperadminPermissionHolder() {
        return PermissionHolder.SYSTEM_SUPERADMIN;
    }

    @Bean(ANONYMOUS_PERMISSION_HOLDER)
    public PermissionHolder systemAnonymousPermissionHolder() {
        return PermissionHolder.ANONYMOUS;
    }

    @Bean(SYSTEM_SETTING_DAO_NAME)
    @DependsOn(DAO_OBJECT_MAPPER_NAME)
    public SystemSettingsDao systemSettingsDao() {
        return SystemSettingsDao.instance;
    }

    @Bean
    public MonitoredValues monitoredValues() {
        return Common.MONITORED_VALUES;
    }

    /**
     * The SystemSettingsEventDispatcher is used inside SystemSettingsListenerProcessor which means it is instantiated early and cannot use dependency injection.
     * @return
     */
    @Bean
    public SystemSettingsEventDispatcher systemSettingsEventDispatcher() {
        return SystemSettingsEventDispatcher.INSTANCE;
    }

    @Bean
    public ScriptEngineManager scriptEngineManager() {
        return new ScriptEngineManager();
    }

    @Bean
    public MangoSessionDataStore sessionDataStore(@Value("${sessionCookie.persistent:true}") boolean persistentSessions,
                                                  UsersService usersService, MangoSessionDataDao sessionDao,
                                                  ApplicationEventPublisher publisher) {
        if(persistentSessions) {
            return new MangoCachingSessionDataStore(new MangoSessionDataMap(), new MangoJdbcSessionDataStore(usersService, sessionDao, publisher));
        }else {
            return new NullMangoSessionDataStore();
        }
    }


    @Bean
    public DatabaseProxy.DatabaseType dataBaseType() {
        return Common.databaseProxy.getType();
    }

    @Bean
    public DataSource dataSource() {
        return Common.databaseProxy.getDataSource();
    }

    @Bean
    public PlatformTransactionManager platformTransactionManager() {
        return Common.databaseProxy.getTransactionManager();
    }

    @Bean
    public ExtendedJdbcTemplate extendedJdbcTemplate() {
        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(dataSource());
        return ejt;
    }

    @Bean
    public org.jooq.Configuration jooqConfiguration(@Value("${db.useMetrics:false}") boolean useMetrics) {
        org.jooq.Configuration configuration = new DefaultConfiguration();
        configuration.set(new SpringConnectionProvider(dataSource()));

        configuration.settings().setExecuteLogging(useMetrics);
        if (useMetrics) {
            configuration.set(StopWatchListener::new);
        }

        switch(dataBaseType()) {
            case DERBY:
                configuration.set(SQLDialect.DERBY);
                break;
            case H2:
                configuration.set(SQLDialect.H2);
                configuration.settings().setRenderNameStyle(RenderNameStyle.UPPER);
                break;
            case MYSQL:
                configuration.set(SQLDialect.MYSQL);
                break;
            case POSTGRES:
                configuration.set(SQLDialect.POSTGRES);
                break;
            case MSSQL:
            default:
                configuration.set(SQLDialect.DEFAULT);
                break;
        }

        return configuration;
    }
}
