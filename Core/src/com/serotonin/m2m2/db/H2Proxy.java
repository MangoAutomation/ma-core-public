/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.h2.Driver;
import org.h2.engine.Constants;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infiniteautomation.mango.spring.DatabaseProxyConfiguration;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.util.DirectoryInfo;
import com.serotonin.util.DirectoryUtils;

public class H2Proxy extends AbstractDatabaseProxy {
    private static final Logger LOG = LoggerFactory.getLogger(H2Proxy.class);

    //Select the build number of H2
    public static final String H2_LEGACY_SELECT_BUILD_NUMBER = "SELECT value FROM information_schema.settings WHERE name='CREATE_BUILD' LIMIT 1";
    public static final String H2_SELECT_BUILD_NUMBER = "SELECT setting_value FROM information_schema.settings WHERE setting_name='CREATE_BUILD' LIMIT 1";

    public static final String IN_MEMORY_URL_PREFIX = "jdbc:h2:mem:";
    public static final String TCP_URL_PREFIX = "jdbc:h2:tcp:";

    private JdbcConnectionPool dataSource;
    private Server web; //web UI

    public static final Map<String, String> DEFAULT_OPTIONS;
    static {
        Map<String, String> options = new HashMap<>();
        options.put("DB_CLOSE_ON_EXIT", "FALSE");
        options.put("IGNORECASE", "TRUE");
        options.put("MV_STORE", "TRUE");
        DEFAULT_OPTIONS = Collections.unmodifiableMap(options);
    }

    public H2Proxy(DatabaseProxyFactory factory, DatabaseProxyConfiguration configuration, String propertyPrefix) {
        super(factory, configuration, propertyPrefix);
    }

    @Override
    protected void initializeImpl() {
        LOG.info("Initializing H2 connection manager");

        try {
            upgradePageStoreToMvStore();
        } catch (Exception e) {
            throw new RuntimeException("Error upgrading H2 page store to MV store", e);
        }

        try {
            upgradeLegacyMVStore();
        } catch (Exception e) {
            throw new RuntimeException("Error upgrading H2 database", e);
        }

        JdbcDataSource jds = new JdbcDataSource();
        jds.setURL(getUrl(null));
        jds.setDescription("maDataSource");

        String user = env.getProperty(propertyPrefix + "db.username");
        if (user != null) {
            jds.setUser(user);

            String password = env.getProperty(propertyPrefix + "db.password");
            if (password != null)
                jds.setPassword(password);
        }
        dataSource = JdbcConnectionPool.create(jds);
        dataSource.setMaxConnections(env.getProperty(propertyPrefix + "db.pool.maxActive", int.class, 100));
    }

    public static Path getDbPathFromUrl(String url) {
        String [] jdbcParts = url.split("jdbc:h2:");
        String [] commandParts = jdbcParts[1].split(";");
        return Common.MA_DATA_PATH.resolve(Paths.get(commandParts[0])).normalize();
    }

    /**
     * Potentially upgrade the h2 database from older versions
     */
    private void upgradeLegacyMVStore() throws IOException {
        String upgradeUrl = env.getRequiredProperty(propertyPrefix + "db.url");
        if (upgradeUrl.startsWith(IN_MEMORY_URL_PREFIX) || upgradeUrl.startsWith(TCP_URL_PREFIX)) {
            return;
        }

        List<H2FileInfo> files = getFilesForURL(upgradeUrl)
                .sorted(Comparator.<H2FileInfo>comparingInt(a -> a.version).reversed())
                .collect(Collectors.toList());

        if (files.stream().anyMatch(file -> file.storeType == StoreType.PAGE_STORE)) {
            // Page store DB need to converted MV Store first
            throw new ShouldNeverHappenException("Page store needs to converted to MV Store");
        }

        if (files.isEmpty()) {
            // no DB file found, probably first start
            return;
        }

        H2FileInfo dbToUpgrade = files.get(0);
        if (dbToUpgrade.version == Constants.BUILD_ID) {
            // Already upgraded
            return;
        }

        LOG.info("Converting legacy h2 database...");
        Path dumpPath = Common.getBackupPath().resolve(dbToUpgrade.filePath.getFileName() + ".zip");

        //Check dump file existence, if so abort startup
        if(dumpPath.toFile().exists())
            throw new ShouldNeverHappenException("Found upgrade database backup, aborting startup. Likely corrupt database, a clean backup can be found here: " + dumpPath);

        try {
            LOG.info("Dumping legacy database to file " + dumpPath);
            dumpLegacy(dumpPath, dbToUpgrade.version, "TRUE");
            restoreLegacy(dumpPath, dbToUpgrade);
        } catch(Exception e) {
            if(e.getCause() instanceof JdbcSQLIntegrityConstraintViolationException) {
                //This is very likely a db that failed to open due to it being a legacy DB that was already opened 1x by a later H2 driver
                throw new ShouldNeverHappenException("H2 Failed to start. Likely corrupt database, a clean backup can be found here: " + dumpPath);
            }if(e instanceof InvocationTargetException) {
                throw new ShouldNeverHappenException(e.getCause());
            }else {
                throw new ShouldNeverHappenException(e);
            }
        }
    }

    private void upgradePageStoreToMvStore() throws Exception {
        String upgradeUrl = env.getRequiredProperty(propertyPrefix + "db.url");
        if (upgradeUrl.startsWith(IN_MEMORY_URL_PREFIX) || upgradeUrl.startsWith(TCP_URL_PREFIX)) {
            return;
        }

        List<H2FileInfo> files = getFilesForURL(upgradeUrl)
                .sorted(Comparator.<H2FileInfo>comparingInt(a -> a.version).reversed())
                .collect(Collectors.toList());

        if (files.stream().anyMatch(file -> file.storeType == StoreType.MV_STORE)) {
            // there is already an MV store DB, leave alone
            return;
        }

        if (files.isEmpty()) {
            // no DB file found, probably first start
            return;
        }

        LOG.info("Converting legacy h2 database...");
        H2FileInfo dbToUpgrade = files.get(0);
        Path dumpPath = Common.getBackupPath().resolve(dbToUpgrade.filePath.getFileName() + ".zip");
        if (Files.exists(dumpPath)) {
            throw new RuntimeException("Found existing backup for this database, aborting upgrade. Backup file: " + dumpPath);
        }

        try {
            LOG.info("Dumping legacy database to file " + dumpPath);
            dumpLegacy(dumpPath, dbToUpgrade.version, "FALSE");
            restoreLegacy(dumpPath, dbToUpgrade);
        } catch(Exception e) {
            if(e.getCause() instanceof JdbcSQLIntegrityConstraintViolationException) {
                //This is very likely a db that failed to open due to it being a legacy DB that was already opened 1x by a later H2 driver
                throw new ShouldNeverHappenException("H2 Failed to start. Likely corrupt database, a clean backup can be found here: " + dumpPath);
            }if(e instanceof InvocationTargetException) {
                throw new ShouldNeverHappenException(e.getCause());
            }else {
                throw new ShouldNeverHappenException(e);
            }
        }
    }

    private Map<String, String> extractOptions(String url) {
        Map<String, String> combinedOptions = new HashMap<>(DEFAULT_OPTIONS);

        Arrays.stream(url.split(";"))
                .map(s -> s.toUpperCase(Locale.ROOT).split("="))
                .filter(o -> o.length > 1)
                .forEach(o -> {
                    combinedOptions.put(o[0], o[1]);
                });

        return combinedOptions;
    }

    private Properties getConnectionProperties() {
        Properties connectionProperties = new Properties();
        connectionProperties.compute("user", (a, b) -> env.getProperty(propertyPrefix + "db.username"));
        connectionProperties.compute("password", (a, b) -> env.getProperty(propertyPrefix + "db.password"));
        return connectionProperties;
    }

    private String configureURL(String url, Map<String, String> options) {
        StringBuilder urlWithOptions = new StringBuilder(url);
        for (Entry<String, String> entry: options.entrySet()) {
            urlWithOptions.append(";")
                    .append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
        }
        return urlWithOptions.toString();
    }

    private Stream<H2FileInfo> getFilesForURL(String h2Url) throws IOException {
        Path path = getDbPathFromUrl(h2Url);
        String fileNamePrefix = path.getFileName().toString();
        Pattern filePattern = Pattern.compile("^" + Pattern.quote(fileNamePrefix) + "\\.?(\\d+)?(\\.(?:h2|mv)\\.db)$", Pattern.CASE_INSENSITIVE);

        if (!Files.isDirectory(path.getParent())) {
            return Stream.empty();
        }

        return Files.list(path.getParent())
                .map(H2FileInfo::new)
                .filter(info -> {
                    Matcher matcher = filePattern.matcher(info.filePath.getFileName().toString());
                    if (matcher.matches() && Files.isRegularFile(info.filePath)) {
                        info.baseName = fileNamePrefix;
                        info.version = matcher.group(1) == null ? 196 : Integer.parseInt(matcher.group(1));
                        info.storeType = StoreType.fromExtension(matcher.group(2));
                        return true;
                    }
                    return false;
                });
    }

    private static class H2FileInfo {
        Path filePath;
        String baseName;
        Integer version;
        StoreType storeType;

        H2FileInfo(Path filePath) {
            this.filePath = filePath;
        }
    }

    private enum StoreType {
        PAGE_STORE(".h2.db"),
        MV_STORE(".mv.db");

        final String extension;

        StoreType(String extension) {
            this.extension = extension;
        }

        public static StoreType fromExtension(String extension) {
            String lower = extension.toLowerCase(Locale.ROOT);
            return Arrays.stream(StoreType.values())
                    .filter(t -> t.extension.equals(lower))
                    .findAny()
                    .orElseThrow(NoSuchElementException::new);
        }
    }

    private String getUrl(Integer destinationVersion) {
        String url = env.getRequiredProperty(propertyPrefix + "db.url");
        if (url.startsWith(IN_MEMORY_URL_PREFIX) || url.startsWith(TCP_URL_PREFIX)) {
            return url;
        }

        Map<String, String> options = extractOptions(url);

        // Ignore user input
        if ("FALSE".equals(options.get("MV_STORE"))) {
            options.put("MV_STORE", "TRUE");
        }

        StoreType desiredStoreType = "FALSE".equals(options.get("MV_STORE")) ? StoreType.PAGE_STORE : StoreType.MV_STORE;

        List<H2FileInfo> files;
        try {
            files = getFilesForURL(url).filter(file -> file.storeType == desiredStoreType)
                    .sorted(Comparator.<H2FileInfo>comparingInt(a -> a.version).reversed())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Couldn't list database files for URL: " + url, e);
        }

        Path parentDirectory;
        String baseName;
        int version;

        if (files.isEmpty()) {
            // new database
            Path dbPath = getDbPathFromUrl(url);
            parentDirectory = dbPath.getParent();
            baseName = dbPath.getFileName().toString();
            version = Constants.BUILD_ID;
        } else {
            // found existing database
            H2FileInfo file = files.get(0);
            parentDirectory = file.filePath.getParent();
            baseName = file.baseName;
            version = destinationVersion != null ? destinationVersion : file.version;
        }

        try {
            Files.createDirectories(parentDirectory);
        } catch (IOException e) {
            throw new ShouldNeverHappenException("Could not create databases directory at " + parentDirectory);
        }

        String sourceUrl = "jdbc:h2:" + parentDirectory.resolve(baseName + "." + version);
        return configureURL(sourceUrl, options);
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public String getTableListQuery() {
        return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE table_schema='PUBLIC'";
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.H2;
    }

    @Override
    public int getActiveConnections() {
        return dataSource.getActiveConnections();
    }

    @Override
    public int getIdleConnections() {
        return dataSource.getMaxConnections() - dataSource.getActiveConnections();
    }

    @Override
    public File getDataDirectory() {
        String url = getUrl(null);
        if (!url.startsWith(IN_MEMORY_URL_PREFIX) && !url.startsWith(TCP_URL_PREFIX)) {
            Map<String, String> options = extractOptions(url);
            StoreType storeType = "FALSE".equals(options.get("MV_STORE")) ? StoreType.PAGE_STORE : StoreType.MV_STORE;
            Path dbPath = getDbPathFromUrl(url);
            Path dbFile = dbPath.getParent().resolve(dbPath.getFileName().toString() + storeType.extension);
            if (Files.exists(dbFile)) {
                return dbFile.toFile();
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public long getDatabaseSizeInBytes() {
        File dbFile = getDataDirectory();
        DirectoryInfo dbInfo = DirectoryUtils.getSize(dbFile);
        return dbInfo.getSize();
    }

    @Override
    public void terminateImpl() {
        if (web != null) {
            if (web.isRunning(true)) {
                web.stop();
                web.shutdown();
            }
        }

        if (dataSource != null) {
            if (env.getProperty("db.h2.shutdownCompact", boolean.class, false)) {
                LOG.info("Terminating and Compacting database.");
                runScript(new String[] {"SHUTDOWN COMPACT;"}, null);
            }else {
                LOG.info("Terminating database.");
                runScript(new String[] {"SHUTDOWN;"}, null);
            }
            dataSource.dispose();
        }


    }

    @Override
    public boolean tableExists(String tableName) {
        return getJdbcTemplate().queryForInt(
                "SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES WHERE table_name='"
                        + tableName.toUpperCase() + "' AND table_schema='PUBLIC'",
                        new Object[] {}, 0) > 0;
    }

    @Override
    public void clean() {
        runScript(new String[] {"DROP ALL OBJECTS;"}, null);
    }

    /**
     * Check the name of the database file to see if it is a legacy db.
     * *.h2.db = legacy
     * *.[version].h2.db = current
     */
    public static boolean isLegacy(String databaseName) {
        return !databaseName.matches("\\.\\d+\\.h2\\.db$");
    }

    /**
     * Dump legacy database to SQL using legacy driver from separate classloader
     */
    private void dumpLegacy(Path dumpPath, Integer buildNumber, String mvStore) throws Exception {
        String url = env.getRequiredProperty(propertyPrefix + "db.url");
        Map<String, String> options = extractOptions(url);
        options.put("MV_STORE", mvStore);
        options.put("IFEXISTS", "TRUE");

        Properties connectionProperties = getConnectionProperties();
        Path dbPath = getDbPathFromUrl(url);

        try (URLClassLoader jarLoader = loadLegacyJar(buildNumber)) {
            Class<?> driverManager = Class.forName("org.h2.Driver", false, jarLoader);
            Method connect = driverManager.getMethod("connect", String.class, Properties.class);
            //Get the INSTANCE to work on
            Field instance = driverManager.getDeclaredField("INSTANCE");
            instance.setAccessible(true);

            String dbUrl = "jdbc:h2:" + dbPath;
            if (buildNumber != 196) dbUrl += "." + buildNumber;
            String configuredUrl = configureURL(dbUrl, options);
            try (Connection c = (Connection) connect.invoke(instance.get(driverManager), configuredUrl, connectionProperties)){
                dumpToFile(dumpPath, c);
            }
        }
    }

    private void dumpToFile(Path dumpPath, Connection connection) throws Exception {
        try (Statement stat = connection.createStatement()) {
            ResultSet rs = stat.executeQuery(H2_LEGACY_SELECT_BUILD_NUMBER);
            int version = rs.next() ? rs.getInt(1) : -1;
            LOG.info("Dumping H2 database version " + version + " to SQL file at " + dumpPath);
            stat.executeQuery("SCRIPT DROP TO '" + dumpPath.toString() + "' COMPRESSION ZIP");
        }
    }

    private void restoreLegacy(Path dumpPath, H2FileInfo dbToUpgrade) throws Exception {
        String url = getUrl(Constants.BUILD_ID);
        //Open a connection and import the dump script
        LOG.info("Importing H2 database from " + dumpPath);
        Properties connectionProperties = getConnectionProperties();
        try (Connection connection = Driver.load().connect(url, connectionProperties)) {
            restoreFromFile(dumpPath, connection);
        }
        LOG.info("H2 database imported successfully");

        try {
            LOG.info("Removing old H2 database from " + dbToUpgrade.filePath);
            Files.delete(dbToUpgrade.filePath);
        } catch (IOException e) {
            LOG.warn("Unable to delete old H2 database from " + dumpPath, e);
        }

        try {
            LOG.info("Removing H2 database backup from " + dumpPath);
            Files.delete(dumpPath);
        } catch (IOException e) {
            LOG.warn("Unable to delete H2 database backup from " + dumpPath, e);
        }
    }

    private void restoreFromFile(Path restorePath, Connection connection) throws Exception {
        try (Statement stat = connection.createStatement()) {
            ResultSet rs = stat.executeQuery(H2_SELECT_BUILD_NUMBER);
            int version = rs.next() ? rs.getInt(1) : -1;
            LOG.info("Restoring H2 database version " + version + " from SQL file at " + restorePath);
            stat.execute("RUNSCRIPT FROM '" + restorePath.toString().replaceAll("\\\\", "/") + "' COMPRESSION ZIP FROM_1X");
            stat.execute("SHUTDOWN COMPACT");
        }
    }

    /**
     * Load the legacy H2 Driver into an isolated class loader
     */
    public static URLClassLoader loadLegacyJar(Integer buildNumber) throws MalformedURLException {
        // fix legacy version for MangoNosql
        if (buildNumber == null) buildNumber = 196;

        String library;
        switch(buildNumber) {
            case 196:
                library = "h2-1.4.196.jar";
                break;
            case 199:
            case 200:
                library = "h2-1.4.200.jar";
                break;
            default:
                throw new ShouldNeverHappenException("Unknown legacy database version " + buildNumber);
        }

        URL[] urls = new URL[]{Common.MA_HOME_PATH.resolve("boot/" + library ).toUri().toURL()};
        return new ParentLastURLClassLoader(urls, H2Proxy.class.getClassLoader());
    }

    /**
     * A parent-last classloader that will try the child classloader first and then the parent.
     * This takes a fair bit of doing because java really prefers parent-first.
     * For those not familiar with class loading trickery, be wary
     */
    public static class ParentLastURLClassLoader extends URLClassLoader {
        private final ChildURLClassLoader childClassLoader;

        /**
         * This class allows me to call findClass on a classloader
         */
        private static class FindClassClassLoader extends ClassLoader {
            public FindClassClassLoader(ClassLoader parent) {
                super(parent);
            }

            @Override
            public Class<?> findClass(String name) throws ClassNotFoundException {
                return super.findClass(name);
            }
        }

        /**
         * This class delegates (child then parent) for the findClass method for a URLClassLoader.
         * We need this because findClass is protected in URLClassLoader
         */
        private static class ChildURLClassLoader extends URLClassLoader {
            private final FindClassClassLoader realParent;

            public ChildURLClassLoader(URL[] urls, FindClassClassLoader realParent ) {
                super(urls, null);

                this.realParent = realParent;
            }

            @Override
            public Class<?> findClass(String name) throws ClassNotFoundException {
                try {
                    // first try to use the URLClassLoader findClass
                    return super.findClass(name);
                } catch( ClassNotFoundException e ) {
                    // if that fails, we ask our real parent classloader to load the class (we give up)
                    return realParent.loadClass(name);
                }
            }
        }

        public ParentLastURLClassLoader(URL[] urls, ClassLoader parent) {
            super(new URL[0], parent);
            childClassLoader = new ChildURLClassLoader(urls, new FindClassClassLoader(parent) );
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            try {
                // first we try to find a class inside the child classloader
                return childClassLoader.findClass(name);
            } catch( ClassNotFoundException e ) {
                // didn't find it, try the parent
                return super.loadClass(name, resolve);
            }
        }
    }
}
