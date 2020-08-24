/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.DaoUtils;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.util.DirectoryInfo;
import com.serotonin.util.DirectoryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.Driver;
import org.h2.engine.Constants;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class H2Proxy extends AbstractDatabaseProxy {
    private static final Log LOG = LogFactory.getLog(H2Proxy.class);

    //The version of h2 at which we upgrade the page store could also do this each time h2 is upgraded via Constants.getVersion()
    public static final int H2_PAGE_STORE_UPGRADE_VERSION = 196;

    //Select the version of H2 that created this database.
    public static final String H2_CREATE_VERSION_SELECT = "SELECT value FROM information_schema.settings WHERE name='CREATE_BUILD' LIMIT 1";

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

    @Override
    protected void initializeImpl(String propertyPrefix) {
        LOG.info("Initializing H2 connection manager");

        upgradeLegacyPageStore(propertyPrefix);
        try {
            upgradePageStoreToMvStore(propertyPrefix);
        } catch (Exception e) {
            throw new RuntimeException("Error upgrading H2 page store to MV store", e);
        }

        JdbcDataSource jds = new JdbcDataSource();
        jds.setURL(getUrl(propertyPrefix));
        jds.setDescription("maDataSource");

        String user = Common.envProps.getString(propertyPrefix + "db.username", null);
        if (user != null) {
            jds.setUser(user);

            String password = Common.envProps.getString(propertyPrefix + "db.password", null);
            if (password != null)
                jds.setPassword(password);
        }
        dataSource = JdbcConnectionPool.create(jds);
        dataSource.setMaxConnections(
                Common.envProps.getInt(propertyPrefix + "db.pool.maxActive", 100));


        if (Common.envProps.getBoolean(propertyPrefix + "db.web.start", false)) {
            LOG.info("Initializing H2 web server");
            String[] webArgs = new String[4];
            webArgs[0] = "-webPort";
            webArgs[1] = Common.envProps.getString(propertyPrefix + "db.web.port");
            webArgs[2] = "-ifExists";
            webArgs[3] = "-webAllowOthers";
            try {
                this.web = Server.createWebServer(webArgs);
                this.web.start();
            } catch (SQLException e) {
                LOG.error(e);
            }
        }
    }

    public static Path getDbPathFromUrl(String url) {
        String [] jdbcParts = url.split("jdbc:h2:");
        String [] commandParts = jdbcParts[1].split(";");
        return Common.MA_HOME_PATH.resolve(Paths.get(commandParts[0])).normalize();
    }

    /**
     * Potentially upgrade the h2 pagestore database from v196
     */
    private void upgradeLegacyPageStore(String propertyPrefix) {
        //Parse out the useful sections of the url
        String dbUrl = Common.envProps.getString(propertyPrefix + "db.url");
        Path dbPath = getDbPathFromUrl(dbUrl);

        //Check to see if we are a legacy database, legacy DB does not have H2 version in filename
        Path legacy = dbPath.getParent().resolve(dbPath.getFileName().toString() + ".h2.db");
        if (legacy.toFile().exists()) {
            LOG.info("Converting legacy h2 database...");
            //We will need to convert it.
            //Create a reference for the dump file
            String dumpFileName = "mah2.h2.196.sql.zip";
            Path dumpPath = Common.getBackupPath().resolve(dumpFileName);

            //Check dump file existence, if so abort startup
            if(dumpPath.toFile().exists())
                throw new ShouldNeverHappenException("Found upgrade database backup, aborting startup.  Likely corrupt database, a clean backup can be found here: " + dumpPath.toString());

            try {
                LOG.info("Dumping legacy database to file " + dumpPath.toString());
                dumpLegacy(propertyPrefix, dumpPath);

                String url = getUrl(propertyPrefix);
                //Open a connection and import the dump script
                LOG.info("Importing H2 database from " + dumpPath);
                Properties connectionProperties = getConnectionProperties(propertyPrefix);
                try (Connection connection = Driver.load().connect(url, connectionProperties)) {
                    restoreFromFile(dumpPath, connection);
                }
                LOG.info("H2 database imported successfully");

                try {
                    LOG.info("Removing old H2 database from " + legacy);
                    Files.delete(legacy);
                } catch (IOException e) {
                    LOG.warn("Unable to delete old H2 database from " + dumpPath, e);
                }

                try {
                    LOG.info("Removing H2 database backup from " + dumpPath);
                    Files.delete(dumpPath);
                } catch (IOException e) {
                    LOG.warn("Unable to delete H2 database backup from " + dumpPath, e);
                }
            } catch(Exception e) {
                if(e.getCause() instanceof JdbcSQLIntegrityConstraintViolationException) {
                    //This is very likely a db that failed to open due to it being a legacy DB that was already opened 1x by a later H2 driver
                    throw new ShouldNeverHappenException("H2 Failed to start. Likely corrupt database, a clean backup can be found here: " + dumpPath.toString());
                }if(e instanceof InvocationTargetException) {
                    throw new ShouldNeverHappenException(e.getCause());
                }else {
                    throw new ShouldNeverHappenException(e);
                }
            }
        }
    }

    private void upgradePageStoreToMvStore(String propertyPrefix) throws Exception {
        String upgradeUrl = Common.envProps.getString(propertyPrefix + "db.url");
        Map<String, String> userOptions = extractOptions(upgradeUrl);
        if ("FALSE".equals(userOptions.get("MV_STORE"))) {
            // user has explicitly configured their URL to be page store, leave alone
            return;
        }

        List<H2FileInfo> files = getFilesForURL(upgradeUrl)
                .sorted(Comparator.<H2FileInfo>comparingInt(a -> a.version).reversed())
                .collect(Collectors.toList());

        if (files.stream().anyMatch(file -> file.storeType == StoreType.MV_STORE)) {
            // there is already a MV store DB, leave alone
            return;
        }
        if (files.isEmpty()) {
            // no DB file found, probably first start
            return;
        }

        H2FileInfo dbToUpgrade = files.get(0);
        Path dumpPath = Common.getBackupPath().resolve(dbToUpgrade.filePath.getFileName() + ".zip");
        if (Files.exists(dumpPath)) {
            throw new RuntimeException("Found existing backup for this database, aborting upgrade. Backup file: " + dumpPath);
        }

        String sourceUrl = "jdbc:h2:" + dbToUpgrade.filePath.getParent().resolve(dbToUpgrade.baseName + "." + dbToUpgrade.version);
        Map<String, String> sourceOptions = new HashMap<>(userOptions);
        // force page store for source
        sourceOptions.put("MV_STORE", "FALSE");
        sourceOptions.put("IFEXISTS", "TRUE");
        String sourceUrlWithOptions = configureURL(sourceUrl, sourceOptions);
        Properties connectionProperties = getConnectionProperties(propertyPrefix);
        try (Connection connection = Driver.load().connect(sourceUrlWithOptions, connectionProperties)) {
            dumpToFile(dumpPath, connection);
        }

        LOG.info("Importing H2 database from " + dumpPath);
        String destinationUrl = "jdbc:h2:" + dbToUpgrade.filePath.getParent().resolve(dbToUpgrade.baseName + "." + Constants.BUILD_ID);
        String destinationUrlWithOptions = configureURL(destinationUrl, userOptions);
        try (Connection connection = Driver.load().connect(destinationUrlWithOptions, connectionProperties)) {
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

    private Properties getConnectionProperties(String propertyPrefix) {
        Properties connectionProperties = new Properties();
        connectionProperties.compute("user", (a, b) -> Common.envProps.getString(propertyPrefix + "db.username", null));
        connectionProperties.compute("password", (a, b) -> Common.envProps.getString(propertyPrefix + "db.password", null));
        return connectionProperties;
    }

    private String configureURL(String url, Map<String, String> options) {
        StringBuilder urlWithOptions = new StringBuilder(url);
        for (Map.Entry<String, String> entry: options.entrySet()) {
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
        Pattern filePattern = Pattern.compile("^" + Pattern.quote(fileNamePrefix) + "\\.(\\d+)(\\.(?:h2|mv)\\.db)$", Pattern.CASE_INSENSITIVE);

        return Files.list(path.getParent())
                .map(H2FileInfo::new)
                .filter(info -> {
                    Matcher matcher = filePattern.matcher(info.filePath.getFileName().toString());
                    if (matcher.matches() && Files.isRegularFile(info.filePath)) {
                        info.baseName = fileNamePrefix;
                        info.version = Integer.parseInt(matcher.group(1));
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

    private String getUrl(String propertyPrefix) {
        String url = Common.envProps.getString(propertyPrefix + "db.url");
        Map<String, String> options = extractOptions(url);
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
            version = file.version;
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
    public double applyBounds(double value) {
        return value;
    }

    @Override
    public void executeCompress(ExtendedJdbcTemplate ejt) {
        // no op
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
    public void runScript(String[] script, final OutputStream out) {
        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(getDataSource());

        StringBuilder statement = new StringBuilder();

        for (String line : script) {
            // Trim whitespace
            line = line.trim();

            // Skip comments
            if (line.startsWith("--"))
                continue;

            statement.append(line);
            statement.append(" ");
            if (line.endsWith(";")) {
                // Execute the statement
                ejt.execute(statement.toString());
                if(out != null) {
                    try {
                        out.write((statement.toString() + "\n").getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        //Don't really care
                    }
                }
                statement.delete(0, statement.length() - 1);
            }
        }
    }

    @Override
    public void runScript(InputStream input, final OutputStream out) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(input));

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null)
                lines.add(line);

            String[] script = new String[lines.size()];
            lines.toArray(script);
            runScript(script, out);
        }
        catch (IOException ioe) {
            throw new ShouldNeverHappenException(ioe);
        }
        finally {
            try {
                if (in != null)
                    in.close();
            }
            catch (IOException ioe) {
                LOG.warn("", ioe);
            }
        }
    }

    @Override
    public <T> List<T> doLimitQuery(DaoUtils dao, String sql, Object[] args, RowMapper<T> rowMapper, int limit) {
        if (limit > 0)
            sql += " LIMIT " + limit;
        return dao.query(sql, args, rowMapper);
    }

    @Override
    protected String getLimitDelete(String sql, int chunkSize) {
        return sql + " LIMIT " + chunkSize;
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
        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(this.getDataSource());
        String dataDir =
                ejt.queryForObject("call DATABASE_PATH()", new Object[] {}, String.class, null);
        if (dataDir == null)
            return null;

        String url = getUrl("");
        Map<String, String> options = extractOptions(url);
        StoreType desiredStoreType = "FALSE".equals(options.get("MV_STORE")) ? StoreType.PAGE_STORE : StoreType.MV_STORE;
        File dbFile = new File(dataDir + desiredStoreType.extension);
        return dbFile.exists() ? dbFile : null;
    }

    @Override
    public Long getDatabaseSizeInBytes() {
        File dbFile = getDataDirectory();
        if (dbFile != null) {
            DirectoryInfo dbInfo = DirectoryUtils.getSize(dbFile);
            return dbInfo.getSize();
        }
        return null;
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
            if (Common.envProps.getBoolean("db.h2.shutdownCompact", false)) {
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
    public boolean tableExists(ExtendedJdbcTemplate ejt, String tableName) {
        return ejt.queryForInt(
                "SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES WHERE table_name='"
                        + tableName.toUpperCase() + "' AND table_schema='PUBLIC'",
                        new Object[] {}, 0) > 0;
    }

    /**
     * Check the name of the database file to see if it is a legacy db.
     *
     * *.h2.db = legacy
     * *.[version].h2.db = current
     */
    public static boolean isLegacy(String databaseName) {
        return !databaseName.matches("\\.\\d+\\.h2\\.db$");
    }

    /**
     * Dump legacy database to SQL using legacy driver from separate classloader
     */
    private void dumpLegacy(String propertyPrefix, Path dumpPath) throws Exception {
        String url = Common.envProps.getString(propertyPrefix + "db.url");
        Map<String, String> options = extractOptions(url);
        options.put("MV_STORE", "FALSE");
        options.put("IFEXISTS", "TRUE");

        Properties connectionProperties = getConnectionProperties(propertyPrefix);

        try (URLClassLoader jarLoader = loadLegacyJar()) {
            Class<?> driverManager = Class.forName("org.h2.Driver", false, jarLoader);
            Method connect = driverManager.getMethod("connect", String.class, Properties.class);
            //Get the INSTANCE to work on
            Field instance = driverManager.getDeclaredField("INSTANCE");
            instance.setAccessible(true);

            String configuredUrl = configureURL(url, options);
            try (Connection c = (Connection) connect.invoke(instance.get(driverManager), configuredUrl, connectionProperties)){
                dumpToFile(dumpPath, c);
            }
        }
    }

    private void dumpToFile(Path dumpPath, Connection connection) throws Exception {
        try (Statement stat = connection.createStatement()) {
            ResultSet rs = stat.executeQuery(H2_CREATE_VERSION_SELECT);
            int version = rs.next() ? rs.getInt(1) : -1;
            LOG.info("Dumping H2 database version " + version + " to SQL file at " + dumpPath);
            stat.executeQuery("SCRIPT DROP TO '" + dumpPath.toString() + "' COMPRESSION ZIP");
        }
    }

    private void restoreFromFile(Path restorePath, Connection connection) throws Exception {
        try (Statement stat = connection.createStatement()) {
            stat.execute("RUNSCRIPT FROM '" + restorePath.toString().replaceAll("\\\\", "/") + "' COMPRESSION ZIP");
            stat.execute("SHUTDOWN COMPACT");
        }
    }

    /**
     * Load the legacy H2 Driver into an isolated class loader
     */
    public static URLClassLoader loadLegacyJar() throws MalformedURLException {
        return new ParentLastURLClassLoader(new URL[] {Common.MA_HOME_PATH.resolve("boot/h2-1.4.196.jar").toUri().toURL()});
    }

    /**
     * A parent-last classloader that will try the child classloader first and then the parent.
     * This takes a fair bit of doing because java really prefers parent-first.
     *
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

        public ParentLastURLClassLoader(URL[] urls) {
            super(new URL[0], Thread.currentThread().getContextClassLoader());
            childClassLoader = new ChildURLClassLoader(urls, new FindClassClassLoader(this.getParent()) );
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