/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.infiniteautomation.mango.spring.TestConfigUtility;
import com.infiniteautomation.mango.spring.service.CachingService;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.serotonin.m2m2.db.BasePooledProxy;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleComparator;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.rt.EventManager;
import com.serotonin.m2m2.rt.maint.BackgroundProcessing;
import com.serotonin.m2m2.shared.ModuleUtils;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherDefinition;
import com.serotonin.provider.Providers;
import com.serotonin.timer.AbstractTimer;
import com.serotonin.timer.SimulationTimer;
import com.serotonin.util.properties.MangoProperties;

/**
 * Base class for all Mango tests using the JUnit 5 Jupiter framework.
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 *
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class MangoTest implements SupplierPoller {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final List<Module> modules = new ArrayList<>();
    protected MockMangoLifecycle lifecycle;
    protected MockMangoProperties properties;
    protected Path dataDirectory;
    protected TestConfigUtility configUtil;

    @BeforeAll
    protected void prepareForLifecycle() {
        // initialize the Spring Security context
        initializeSecurityContext();

        // create a temporary data directory
        try {
            dataDirectory = Files.createTempDirectory("mango_test_data");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // initialize the Mango properties
        properties = new MockMangoProperties();
        properties.setProperty("paths.data", dataDirectory.toString());
        initializeDatabaseProperties();
        Providers.add(MangoProperties.class, properties);

        Providers.add(ICoreLicense.class, new TestLicenseDefinition());
        Common.releaseProps = new Properties();

        // Add definitions for the mock data source and publisher
        addModule("mangoTestModule", new MockDataSourceDefinition(), new MockPublisherDefinition());
        // Find and load modules on the classpath
        loadModules();
    }

    /**
     * Initializes the test properties with appropriate settings for the configured database type.
     */
    protected void initializeDatabaseProperties() {
        String dbName = "t_" + RandomStringUtils.random(19, true, false ).toLowerCase();
        properties.setProperty("db.test.name", dbName);
        properties.setProperty("db.username", "root");
        properties.setProperty("db.password", "root");

        String testDbType = properties.getProperty("db.test.type");
        switch (testDbType != null? testDbType : "h2") {
            case "mysql": {
                properties.setProperty("db.test.url", "jdbc:mysql://0.0.0.0/mango");
                properties.setProperty("db.url", "jdbc:mysql://0.0.0.0/" + dbName);
            }
            case "postgres": {
                properties.setProperty("db.test.url", "jdbc:postgresql://0.0.0.0/mango");
                properties.setProperty("db.url", "jdbc:postgresql://0.0.0.0/" + dbName);
            }
            case "h2:tcp": {
                testDbType = "h2";
                properties.setProperty("db.url", "jdbc:h2:tcp://0.0.0.0/mem:" + dbName + ";DB_CLOSE_DELAY=-1;LOCK_MODE=0");
            }
            case "h2:file": {
                testDbType = "h2";
                properties.setProperty("db.url", "jdbc:h2:databases/" + dbName);
            }
            default: {
                testDbType = "h2";
                properties.setProperty("db.url", "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;LOCK_MODE=0");
            }
        }
        properties.setProperty("db.type", testDbType);
    }

    /**
     * Executes the CREATE DATABASE statement.
     */
    protected void createTestDatabase() {
        try {
            String dbName = properties.getProperty("db.test.name");
            String dbType = properties.getProperty("db.type");
            String dbUser = properties.getProperty("db.username");
            String dbPassword = properties.getProperty("db.password");

            String message;
            final String createQuery;
            switch (dbType) {
                case "mysql": {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    message = "CREATE DATABASE `%s`";
                }
                case "postgres": {
                    Class.forName("org.postgresql.Driver");
                    message = "CREATE DATABASE %s";
                }
                // H2 doesn't require a CREATE DATABASE statement
                default: { message = null; };
            };
            createQuery = message;

            if (createQuery != null) {
                String testUrl = properties.getProperty("db.test.url");
                try (Connection conn = DriverManager.getConnection(testUrl, dbUser, dbPassword)){
                    Statement stmt = conn.createStatement();
                    //noinspection SqlSourceToSinkFlow
                    stmt.executeUpdate(String.format(createQuery, dbName));
                }
            }

        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Initializes, or re-initializes the Mango lifecycle (before each test).
     */
    @BeforeEach
    protected void initializeLifecycle() {
        createTestDatabase();

        if (lifecycle == null) {
            lifecycle = createLifecycle();
            try {
                lifecycle.initialize();
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            }
        } else {
            // Re-initialize database
            getBean(DatabaseProxy.class).initialize();

            // re-initialize background processing, uses SystemSettingsDao so must be initialized after database proxy
            BackgroundProcessing backgroundProcessing = getBean(BackgroundProcessing.class);
            backgroundProcessing.initialize(false);

            //Lifecycle hook for things that need to run to install into a new database
            for (Module module : modules) {
                module.postDatabase();
            }

            // Re-populate user cache with users from the newly initialized database
            getBean(UsersService.class).clearCaches(false);
        }
        this.configUtil = getBean(TestConfigUtility.class);
    }

    /**
     * Partially terminate Mango between tests.
     */
    @AfterEach
    protected void terminateMango() {
        //Tear down to simulate some form of shutdown
        for (Module module : modules) {
            module.postRuntimeManagerTerminate(false);
        }

        // terminate background processing
        BackgroundProcessing backgroundProcessing = getBean(BackgroundProcessing.class);
        backgroundProcessing.terminate();
        backgroundProcessing.joinTermination();

        for (Module module : modules) {
            module.postTerminate(false);
        }

        EventManager eventManager = getBean(EventManager.class);
        eventManager.purgeAllEvents();

        AbstractTimer timer = getBean(AbstractTimer.class);
        if (timer instanceof SimulationTimer) {
            SimulationTimer simulationTimer = (SimulationTimer) timer;
            simulationTimer.reset();
        }

        //Clean the noSQL database
        if (properties.getBoolean("tests.after.deleteAllPointData", true)) {
            try {
                getBean(PointValueDao.class).deleteAllPointData();
            } catch (UnsupportedOperationException e) {
                log.warn("PointValueDao does not support deleteAllPointData() method. Your tests may be effected if you reuse this Mango lifecycle");
            }
        }

        // Clear all caches in services
        getApplicationContext().getBeansOfType(CachingService.class).values()
                .forEach(s -> s.clearCaches(true));

        DatabaseProxy databaseProxy = getBean(DatabaseProxy.class);
        // Try to release active connections if possible
        if (databaseProxy instanceof BasePooledProxy) {
            BasePooledProxy pooledProxy = (BasePooledProxy) databaseProxy;
            pooledProxy.softEvictConnections();
        }
        // Clean database
        databaseProxy.clean();
    }

    /**
     * Fully terminate the Mango lifecycle and delete the data directory.
     */
    @AfterAll
    protected void terminateLifecycle() throws IOException {
        if (lifecycle != null) {
            lifecycle.terminate(TerminationReason.SHUTDOWN);
        }

        if (dataDirectory != null) {
            FileUtils.deleteDirectory(dataDirectory.toFile());
        }
    }

    /**
     * Add a module to the test.  Before the before() method is called.
     */
    protected MangoTestModule addModule(String name, ModuleElementDefinition... definitions) {
        MangoTestModule module = new MangoTestModule(name);
        Arrays.stream(definitions).forEach(module::addDefinition);
        modules.add(module);
        return module;
    }

    protected List<MangoTestModule> loadModules() {
        return loadModules(List.of());
    }

    protected List<MangoTestModule> loadModules(List<ModuleElementDefinition> overrides) {
        try {
            List<MangoTestModule> loaded = new ArrayList<>();
            Enumeration<URL> urls = MangoTest.class.getClassLoader().getResources("module.properties");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                Properties properties = new Properties();
                try (InputStream is = url.openStream()) {
                    properties.load(is);
                }
                String moduleName = properties.getProperty(ModuleUtils.Constants.PROP_NAME);
                String dependencies = properties.getProperty(ModuleUtils.Constants.PROP_DEPENDENCIES);
                String loadOrder = properties.getProperty(ModuleUtils.Constants.PROP_LOAD_ORDER);
                loaded.add(new MangoTestModule(moduleName, dependencies, loadOrder == null ? 1 : Integer.parseInt(loadOrder)));
            }

            loaded.sort(ModuleComparator.INSTANCE);

            for (MangoTestModule module : loaded) {
                module.loadDefinitions(MangoTest.class.getClassLoader());
                for (var override : overrides) {
                    module.overrideDefinition(override);
                }
            }

            modules.addAll(loaded);
            return loaded;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected MockMangoLifecycle createLifecycle() {
        return new MockMangoLifecycle(modules);
    }

    protected ApplicationContext getApplicationContext() {
        return lifecycle.getRuntimeContext();
    }

    protected <T> T getBean(Class<T> requiredType) {
        return getApplicationContext().getBean(requiredType);
    }

    protected void initializeSecurityContext() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(new PreAuthenticatedAuthenticationToken(PermissionHolder.SYSTEM_SUPERADMIN, null));
    }

}
