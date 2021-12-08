/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.infiniteautomation.mango.emport.ImportTask;
import com.infiniteautomation.mango.emport.ImportTaskDependencies;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.CachingService;
import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.spring.service.EventDetectorsService;
import com.infiniteautomation.mango.spring.service.EventHandlerService;
import com.infiniteautomation.mango.spring.service.JsonDataService;
import com.infiniteautomation.mango.spring.service.MailingListService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.spring.service.PublishedPointService;
import com.infiniteautomation.mango.spring.service.PublisherService;
import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.spring.service.SystemPermissionService;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.shared.ModuleUtils;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceDefinition;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherDefinition;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherVO;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;
import com.serotonin.timer.SimulationTimer;
import com.serotonin.util.properties.MangoProperties;

/**
 *
 * Base Class for all JUnit Tests
 *
 *   Add any modules to load prior to the @Before method
 *
 * @author Terry Packer
 *
 */
public class MangoTestBase {

    protected final Logger LOG = LoggerFactory.getLogger(MangoTestBase.class);

    protected static MockMangoLifecycle lifecycle;
    protected static List<Module> modules = new ArrayList<>();
    protected static MockMangoProperties properties;
    protected static Path dataDirectory;

    protected SimulationTimer timer;
    protected long testTime = 0L; //time during test

    @BeforeClass
    public static void staticSetup() throws IOException {
        setSuperadminAuthentication();

        dataDirectory = Files.createTempDirectory("MangoTestBase");

        properties = new MockMangoProperties();
        properties.setProperty("paths.data", dataDirectory.toString());
        properties.setProperty("db.url", "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_MODE=0");

        Providers.add(MangoProperties.class, properties);
        Providers.add(ICoreLicense.class, new TestLicenseDefinition());
        Common.releaseProps = new Properties();
        addModule("BaseTest", new MockDataSourceDefinition(), new MockPublisherDefinition());
    }

    @Before
    public void before() {
        //So it only happens once per class for now (problems with restarting lifecycle during a running JVM)
        if(lifecycle == null) {
            lifecycle = getLifecycle();
            try {
                lifecycle.initialize();
            } catch (InterruptedException | ExecutionException e) {
                fail(e.getMessage());
            }
        }else {
            //Lifecycle hook for things that need to run to install into a new database
            for (Module module : ModuleRegistry.getModules()) {
                module.postDatabase();
            }
        }
        SimulationTimerProvider provider = (SimulationTimerProvider) Providers.get(TimerProvider.class);
        this.timer = provider.getSimulationTimer();
    }

    @After
    public void after() {
        //Tear down to simulate some form of shutdown
        for (Module module : ModuleRegistry.getModules()) {
            module.postRuntimeManagerTerminate(false);
        }

        //Need to restart background processing and clear it out
        Common.backgroundProcessing.terminate();
        Common.backgroundProcessing.joinTermination();

        for (Module module : ModuleRegistry.getModules()) {
            module.postTerminate(false);
        }

        //Setup for next test in class
        Common.eventManager.purgeAllEvents();

        Common.backgroundProcessing = lifecycle.getBackgroundProcessing();
        Common.backgroundProcessing.initialize(false);

        SimulationTimerProvider provider = (SimulationTimerProvider) Providers.get(TimerProvider.class);
        provider.reset();

        //Clean the noSQL database
        if (properties.getBoolean("tests.after.deleteAllPointData", true)) {
            try {
                Common.getBean(PointValueDao.class).deleteAllPointData();
            } catch (UnsupportedOperationException e) {
                LOG.warn("PointValueDao does not support deleteAllPointData() method. Your tests may be effected if you reuse this Mango lifecycle");
            }
        }

        DatabaseProxy databaseProxy = Common.getBean(DatabaseProxy.class);
        databaseProxy.clean();
        databaseProxy.initialize();

        //clear all caches in services
        Common.getRuntimeContext().getBeansOfType(CachingService.class).values().stream()
                .filter(s -> !(s instanceof PermissionService))
                .forEach(CachingService::clearCaches);

        // We clear the permission service cache afterwards as every call to clearCaches() on another service will
        // repopulate the role hierarchy cache
        Common.getRuntimeContext().getBean(PermissionService.class).clearCaches();

    }

    @AfterClass
    public static void staticTearDown() throws IOException {
        if(lifecycle != null) {
            lifecycle.terminate(TerminationReason.SHUTDOWN);
        }

        if (dataDirectory != null) {
            FileUtils.deleteDirectory(dataDirectory.toFile());
        }
    }

    /**
     * Add a module to the test.  Before the before() method is called.
     */
    protected static MangoTestModule addModule(String name, ModuleElementDefinition... definitions) {
        MangoTestModule module = new MangoTestModule(name);
        Arrays.stream(definitions).forEach(module::addDefinition);
        modules.add(module);
        return module;
    }

    protected static List<MangoTestModule> loadModules() {
        try {
            List<MangoTestModule> loaded = new ArrayList<>();
            Enumeration<URL> urls = MangoTestBase.class.getClassLoader().getResources("module.properties");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                Properties properties = new Properties();
                try (InputStream is = url.openStream()) {
                    properties.load(is);
                }
                String moduleName = properties.getProperty(ModuleUtils.Constants.PROP_NAME);
                MangoTestModule module = new MangoTestModule(moduleName);
                module.loadDefinitions(MangoTestBase.class.getClassLoader());
                loaded.add(module);
            }
            modules.addAll(loaded);
            return loaded;
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load module.properties", e);
        }
    }

    /**
     * Load a default test JSON Configuration into Mango
     */
    protected void loadDefaultConfiguration() throws JsonException, IOException {
        try (InputStream is = getClass().getResourceAsStream("/testMangoConfig.json")) {
            loadConfiguration(is);
        }
    }

    protected void loadConfiguration(InputStream jsonFile) throws JsonException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(jsonFile, StandardCharsets.UTF_8));
        JsonReader jr = new JsonReader(reader);
        JsonObject jo = jr.read(JsonObject.class);
        loadConfiguration(jo);
    }

    protected void loadConfiguration(JsonObject jo) {
        ImportTaskDependencies dependencies = new ImportTaskDependencies(Common.getTranslations(),
                Common.getBean(RoleService.class),
                Common.getBean(UsersService.class),
                Common.getBean(MailingListService.class),
                Common.getBean(DataSourceService.class),
                Common.getBean(DataPointService.class),
                Common.getBean(PublisherService.class),
                Common.getBean(PublishedPointService.class),
                Common.getBean(EventHandlerService.class),
                Common.getBean(JsonDataService.class),
                Common.getBean(EventDetectorsService.class),
                Common.getBean(SystemPermissionService.class));
        ImportTask task = new ImportTask(jo,
                dependencies,
                null, false);
        task.run(Common.timer.currentTimeMillis());
        if (task.getResponse().getHasMessages()) {
            for (ProcessMessage message : task.getResponse().getMessages()) {
                switch (message.getLevel()) {
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
     * Convert an object to sero json
     */
    protected String convertToSeroJson(Object o) throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, stringWriter);
        writer.setPrettyOutput(true);
        writer.setPrettyIndent(2);
        try {
            writer.writeObject(o);
            writer.flush();
            return stringWriter.toString();
        }
        catch (JsonException e) {
            throw new IOException(e);
        }
    }

    /**
     * Convert from json string to Object
     */
    protected Object readSeroJson(Class<? extends Object> clazz, String json) throws IOException {
        JsonReader jr = new JsonReader(json);
        try {
            JsonValue jo = jr.read(JsonValue.class);
            JsonReader reader = new JsonReader(Common.JSON_CONTEXT, jo);
            return reader.read(clazz);
        }catch(JsonException e){
            throw new IOException(e);
        }
    }

    protected List<RoleVO> createRoles(int count) {
        return createRoles(count, UUID.randomUUID().toString());
    }

    protected List<RoleVO> createRoles(int count, String prefix) {
        List<RoleVO> roles = new ArrayList<>();
        for(int i=0; i<count; i++) {
            roles.add(createRole(prefix + i, prefix + i));
        }
        return roles;
    }

    /**
     * Create a role
     */
    protected RoleVO createRole(String xid, String name) {
        return createRole(xid, name, new Role[0]);
    }

    /**
     * Create a role with inherited roles (
     */
    protected RoleVO createRole(String xid, String name, Role... inherited) {
        RoleService service = Common.getBean(RoleService.class);
        RoleVO role = new RoleVO(Common.NEW_ID, xid, name, new HashSet<>(Arrays.asList(inherited)));
        return service.insert(role);
    }

    /**
     * Create users with password=password and supplied permissions
     */
    protected List<User> createUsers(int count, Role... roles){
        List<User> users = new ArrayList<>();
        for(int i=0; i<count; i++) {
            User user = createUser("User" + i,
                    "user" + i,
                    "password",
                    "user" + i + "@yourMangoDomain.com",
                    roles);
            users.add(user);
        }
        return users;
    }

    /**
     * Create a single user
     */
    protected User createUser(String name, String username, String password, String email, Role... roles) {
        return createUser(Common.NEW_ID, name, username, password, email, roles);
    }

    /**
     * Create a user with pre-assigned ID
     */
    protected User createUser(int id, String name, String username, String password, String email, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setUsername(username);
        user.setPassword(Common.encrypt(password));
        user.setEmail(email);
        user.setPhone("");
        user.setRoles(Collections.unmodifiableSet(new HashSet<>(Arrays.asList(roles))));
        user.setDisabled(false);
        UsersService service = Common.getBean(UsersService.class);

        return service.insert(user);
    }

    protected MockDataSourceVO createMockDataSource() {
        return createMockDataSource(UUID.randomUUID().toString(), UUID.randomUUID().toString(), false);
    }

    protected MockDataSourceVO createMockDataSource(boolean enabled) {
        return createMockDataSource(UUID.randomUUID().toString(), UUID.randomUUID().toString(), enabled);
    }

    protected MockDataSourceVO createMockDataSource(String name, String xid, boolean enabled) {
        return createMockDataSource(name, xid, enabled, new MangoPermission(), new MangoPermission());
    }

    protected MockDataSourceVO createMockDataSource(String name, String xid, boolean enabled, MangoPermission readPermission, MangoPermission editPermission) {
        DataSourceService service = Common.getBean(DataSourceService.class);
        MockDataSourceVO vo = (MockDataSourceVO) ModuleRegistry.getDataSourceDefinition(MockDataSourceDefinition.TYPE_NAME).baseCreateDataSourceVO();
        vo.setXid(name);
        vo.setName(xid);
        vo.setEnabled(enabled);
        vo.setReadPermission(readPermission);
        vo.setEditPermission(editPermission);

        try {
            return (MockDataSourceVO) service.insert(vo);
        } catch (ValidationException e) {
            fail(e.getValidationErrorMessage(Common.getTranslations()));
            return null;
        }
    }

    protected List<IDataPoint> createMockDataPoints(int count) {
        List<IDataPoint> points = new ArrayList<>(count);
        MockDataSourceVO ds = createMockDataSource();
        for(int i=0; i<count; i++) {
            points.add(createMockDataPoint(ds, new MockPointLocatorVO()));
        }
        return points;
    }

    protected List<IDataPoint> createMockDataPoints(int count, boolean enabled, MangoPermission readPermission, MangoPermission setPermission) {
        return createMockDataPoints(count, enabled, readPermission, setPermission, createMockDataSource(enabled));
    }

    protected List<IDataPoint> createMockDataPoints(int count, boolean enabled, MangoPermission readPermission, MangoPermission setPermission, DataSourceVO ds) {
        List<IDataPoint> points = new ArrayList<>(count);
        for(int i=0; i<count; i++) {
            String name = UUID.randomUUID().toString();
            points.add(createMockDataPoint(Common.NEW_ID,
                    UUID.randomUUID().toString(),
                    name,
                    ds.getName() + " " + name,
                    enabled,
                    ds.getId(),
                    ds.getXid(),
                    readPermission,
                    setPermission,
                    new MockPointLocatorVO()));
        }
        return points;
    }

    protected DataPointVO createMockDataPoint(MockDataSourceVO ds, MockPointLocatorVO vo) {
        return createMockDataPoint(ds, vo, false);
    }

    protected DataPointVO createMockDataPoint(MockDataSourceVO ds, MockPointLocatorVO vo, boolean enabled) {
        String name = UUID.randomUUID().toString();
        return createMockDataPoint(Common.NEW_ID,
                UUID.randomUUID().toString(),
                name,
                ds.getName() + " " + name,
                enabled,
                ds.getId(),
                ds.getXid(),
                vo);
    }

    protected DataPointVO createMockDataPoint(int id, String xid, String name,
            String deviceName, boolean enabled, int dataSourceId, String dataSourceXid, MockPointLocatorVO vo) {
        return createMockDataPoint(id, xid,
                name, deviceName, enabled, dataSourceId, dataSourceXid, new MangoPermission(), new MangoPermission(), vo);
    }

    protected DataPointVO createMockDataPoint(int id, String xid, String name, String deviceName, boolean enabled, int dataSourceId,
            String dataSourceXid, MangoPermission readPermission, MangoPermission setPermission, MockPointLocatorVO vo) {

        DataPointService service = Common.getBean(DataPointService.class);
        DataPointVO dp = new DataPointVO();
        dp.setId(id);
        dp.setXid(xid);
        dp.setName(name);
        dp.setDeviceName(deviceName);
        dp.setEnabled(enabled);
        dp.setPointLocator(vo);
        dp.setDataSourceId(dataSourceId);
        dp.setDataSourceXid(dataSourceXid);
        dp.setReadPermission(readPermission);
        dp.setSetPermission(setPermission);

        try {
            return service.insert(dp);
        } catch (ValidationException e) {
            String failureMessage = "";
            for (ProcessMessage m : e.getValidationResult().getMessages()) {
                String messagePart = m.getContextKey() + " -> " + m.getContextualMessage().translate(Common.getTranslations()) + "\n";
                failureMessage += messagePart;
            }
            fail(failureMessage);
            return null;
        }
    }

    protected DataPointVO createMockDataPoint(MockDataSourceVO ds, Consumer<DataPointVO> customizer) {
        DataPointService service = Common.getBean(DataPointService.class);
        DataPointVO dp = new DataPointVO();
        dp.setName(UUID.randomUUID().toString());
        dp.setDeviceName(ds.getName());
        dp.setPointLocator(new MockPointLocatorVO(DataType.NUMERIC, true));
        dp.setDataSourceId(ds.getId());

        customizer.accept(dp);

        try {
            return service.insert(dp);
        } catch (ValidationException e) {
            fail(e.getValidationErrorMessage(Common.getTranslations()));
            return null;
        }
    }

    /**
     * Create a publisher
     */
    public MockPublisherVO createMockPublisher(boolean enabled) {
        MockPublisherVO publisherVO = (MockPublisherVO) ModuleRegistry.getPublisherDefinition(MockPublisherDefinition.TYPE_NAME).baseCreatePublisherVO();
        publisherVO.setName(UUID.randomUUID().toString());
        publisherVO.setEnabled(enabled);
        PublisherService publisherService = Common.getBean(PublisherService.class);
        try {
            return (MockPublisherVO)publisherService.insert(publisherVO);
        } catch (ValidationException e) {
            fail(e.getValidationErrorMessage(Common.getTranslations()));
            return null;
        }
    }

    /**
     * Create a publisher with points
     */
    public MockPublisherVO createMockPublisher(boolean enabled, List<MockPublishedPointVO> points) {
        MockPublisherVO publisherVO = (MockPublisherVO) ModuleRegistry.getPublisherDefinition(MockPublisherDefinition.TYPE_NAME).baseCreatePublisherVO();
        publisherVO.setName(UUID.randomUUID().toString());
        publisherVO.setEnabled(enabled);
        PublisherService publisherService = Common.getBean(PublisherService.class);
        try {
            MockPublisherVO pub = (MockPublisherVO)publisherService.insert(publisherVO);
            PublishedPointService publishedPointService = Common.getBean(PublishedPointService.class);
            for(MockPublishedPointVO point : points) {
                publishedPointService.insert(point);
            }
            return pub;
        } catch (ValidationException e) {
            fail(e.getValidationErrorMessage(Common.getTranslations()));
            return null;
        }
    }

    /**
     * Create a published point
     */
    public MockPublishedPointVO createMockPublishedPoint(MockPublisherVO publisher, IDataPoint dataPoint, boolean enabled) {
        MockPublishedPointVO pp = publisher.getDefinition().createPublishedPointVO(publisher, dataPoint);
        pp.setName(dataPoint.getName());
        pp.setEnabled(true);

        PublishedPointService publishedPointService = Common.getBean(PublishedPointService.class);
        try {
            publishedPointService.insert(pp);
        }catch (ValidationException e) {
            fail(e.getValidationErrorMessage(Common.getTranslations()));
            return null;
        }

        return pp;
    }

    /**
     * Create a list of published points
     */
    public List<PublishedPointVO> createMockPublishedPoints(MockPublisherVO publisher, List<IDataPoint> dataPoints, boolean enabled) {
        List<PublishedPointVO> points = new ArrayList<>();
        for(IDataPoint dp : dataPoints) {
            points.add(createMockPublishedPoint(publisher, dp, enabled));
        }
        return points;
    }

    /**
     * Fast Forward the Simulation Timer in a separate thread and wait
     * for some time.
     *
     * Useful when you have scheduled tasks that do not end, the sim timer
     * won't stop fast forwarding.
     *
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
     */
    protected MockMangoLifecycle getLifecycle() {
        return new MockMangoLifecycle(modules);
    }

    public static void setSuperadminAuthentication() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(new PreAuthenticatedAuthenticationToken(PermissionHolder.SYSTEM_SUPERADMIN, null));
    }
}
