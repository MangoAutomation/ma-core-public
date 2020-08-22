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
import java.util.concurrent.Callable;
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

                //Delete existing so we can re-create it using the dump script
                Files.delete(legacy);

                String user = Common.envProps.getString(propertyPrefix + "db.username", null);
                String password = Common.envProps.getString(propertyPrefix + "db.password", null);

                String url = getUrl(propertyPrefix);
                //Open a connection and import the dump script
                LOG.info("Importing existing H2 database...");
                String runScript = url + ";init=RUNSCRIPT FROM '" + dumpPath.toString().replaceAll("\\\\", "/") + "' COMPRESSION ZIP";
                try(Connection conn = DriverManager.getConnection(runScript, user, password)){
                    Statement stat = conn.createStatement();
                    //Might as well do a compaction here
                    stat.execute("SHUTDOWN COMPACT");
                    stat.close();
                }

                try {
                    LOG.info("Cleaning up H2 initialization tests...");
                    Files.deleteIfExists(dumpPath);
                } catch (IOException e) {
                    LOG.warn("Unable to delete un-necessary H2 dump file " + dumpPath.toString(), e);
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
        String urlUpperCase = upgradeUrl.toUpperCase(Locale.ROOT);
        if (urlUpperCase.contains(";MV_STORE=FALSE")) {
            // user has explicitly configured their URL, leave alone
            return;
        }

        List<H2FileInfo> files = getFilesForURL(upgradeUrl).collect(Collectors.toList());
        if (files.stream().anyMatch(file -> file.storeType == StoreType.MV_STORE)) {
            // there is already a MV store DB, leave alone
            return;
        }
        if (files.isEmpty()) {
            // no DB file found, probably first start
            return;
        }

        // sort by version
        files.sort(Comparator.<H2FileInfo>comparingInt(a -> a.version).reversed());

        H2FileInfo dbToUpgrade = files.get(0);
        Path dumpPath = Common.getBackupPath().resolve(dbToUpgrade.filePath.getFileName() + ".zip");
        if (Files.exists(dumpPath)) {
            throw new RuntimeException("Found existing backup for this database, aborting upgrade. Backup file: " + dumpPath);
        }

        String url = "jdbc:h2:" + dbToUpgrade.filePath.getParent().resolve(dbToUpgrade.prefix + "." + dbToUpgrade.version);
        List<String> userOptions = extractOptions(upgradeUrl);
        if (!userOptions.isEmpty()) {
            url += ";" + String.join(";", userOptions);
        }

        String configuredUrl = configureURL(url, StoreType.PAGE_STORE);
        Properties connectionProperties = getConnectionProperties(propertyPrefix);
        dumpToFile(dumpPath, () -> Driver.load().connect(configuredUrl, connectionProperties));

        // remove old database file, have a backup at this point
        Files.delete(dbToUpgrade.filePath);

        LOG.info("Importing H2 database from " + dumpPath);
        String destinationUrl = "jdbc:h2:" + dbToUpgrade.filePath.getParent().resolve(dbToUpgrade.prefix + "." + getCurrentVersion());
        if (!userOptions.isEmpty()) {
            destinationUrl += ";" + String.join(";", userOptions);
        }
        destinationUrl = configureURL(destinationUrl, StoreType.MV_STORE);

        try (Connection conn = Driver.load().connect(destinationUrl, connectionProperties)) {
            try (Statement stat = conn.createStatement()) {
                stat.execute("RUNSCRIPT FROM '" + dumpPath.toString().replaceAll("\\\\", "/") + "' COMPRESSION ZIP");
                stat.execute("SHUTDOWN COMPACT");
            }
        }
        LOG.info("H2 database imported successfully");

        try {
            LOG.info("Removing H2 database backup from " + dumpPath);
            Files.delete(dumpPath);
        } catch (IOException e) {
            LOG.info("Unable to delete H2 database backup from " + dumpPath, e);
        }
    }

    private List<String> extractOptions(String url) {
        List<String> options = Arrays.asList(url.split(";"));
        return options.subList(1, options.size());
    }

    private Properties getConnectionProperties(String propertyPrefix) {
        Properties connectionProperties = new Properties();
        connectionProperties.compute("user", (a, b) -> Common.envProps.getString(propertyPrefix + "db.username", null));
        connectionProperties.compute("password", (a, b) -> Common.envProps.getString(propertyPrefix + "db.password", null));
        return connectionProperties;
    }

    private String configureURL(String url, StoreType storeType) {
        String urlUpperCase = url.toUpperCase(Locale.ROOT);
        if (!urlUpperCase.contains(";DB_CLOSE_ON_EXIT=")) {
            url += ";DB_CLOSE_ON_EXIT=FALSE";
        }
        if (!urlUpperCase.contains(";MV_STORE=")) {
            url += ";MV_STORE=" + (storeType == StoreType.MV_STORE ? "TRUE" : "FALSE");
        }
        if (!urlUpperCase.contains(";IGNORECASE=")) {
            url += ";IGNORECASE=TRUE";
        }
        return url;
    }

    private Stream<H2FileInfo> getFilesForURL(String h2Url) throws IOException {
        Path path = getDbPathFromUrl(h2Url);
        String fileNamePrefix = path.getFileName().toString();
        Pattern filePattern = Pattern.compile("^" + Pattern.quote(fileNamePrefix) + "\\.(\\d+)\\.(h2|mv)\\.db$", Pattern.CASE_INSENSITIVE);

        return Files.list(path.getParent())
                .map(H2FileInfo::new)
                .filter(info -> {
                    Matcher matcher = filePattern.matcher(info.filePath.getFileName().toString());
                    if (matcher.matches() && Files.isRegularFile(info.filePath)) {
                        info.prefix = fileNamePrefix;
                        info.version = Integer.parseInt(matcher.group(1));
                        info.storeType = StoreType.fromString(matcher.group(2));
                        return true;
                    }
                    return false;
                });
    }

    private static class H2FileInfo {
        Path filePath;
        String prefix;
        Integer version;
        StoreType storeType;

        H2FileInfo(Path filePath) {
            this.filePath = filePath;
        }
    }

    private enum StoreType {
        PAGE_STORE,
        MV_STORE;

        public static StoreType fromString(String extension) {
            switch(extension.toLowerCase(Locale.ROOT)) {
                case "h2": return PAGE_STORE;
                case "mv": return MV_STORE;
                default: throw new IllegalArgumentException("Unknown store type: " + extension);
            }
        }
    }

    private String getUrl(String propertyPrefix) {
        String url = Common.envProps.getString(propertyPrefix + "db.url");
        String [] jdbcParts = url.split("jdbc:h2:");
        String [] commandParts = jdbcParts[1].split(";");
        Path dbPath = Common.MA_HOME_PATH.resolve(commandParts[0]).toAbsolutePath().normalize();

        //Determine the version info
        Path dbFolder = dbPath.getParent();

        //Make sure we have a db folder, create if not
        try {
            Files.createDirectories(dbFolder);
        } catch (IOException e) {
            throw new ShouldNeverHappenException("Could not create databases directory at " + dbFolder.toString());
        }

        String[] matchingDbs = dbFolder.toFile().list((dir, filename) -> {
            File possibleDb = new File(dir, filename);
            return possibleDb.isFile() && filename.endsWith(".db") && filename.startsWith(dbPath.getFileName().toString());
        });

        Objects.requireNonNull(matchingDbs);

        //Check to see if we have an existing db with a version number in it
        // if there are more than 1, select the latest version number in the list
        int version = 0;
        for(String match : matchingDbs) {
            try {
                int newVersion = getVersion(match);
                if(newVersion > version)
                    version = newVersion;
            } catch (IOException e) {
                //Ignore
            }
        }

        //If version is 0 then there is not an existing database we can open,
        // create a new one with our current version.
        if(version == 0)
            version = getCurrentVersion();

        //Rebuild URL
        StringBuilder builder = new StringBuilder();
        builder.append("jdbc:h2:");

        //Put in the db path
        builder.append(dbPath.toString()).append(".").append(version);

        //Add back on any command parts
        for(int i=1; i<commandParts.length; i++) {
            builder.append(";");
            builder.append(commandParts[i]);
        }
        url = builder.toString();

        // Force MV store
        return configureURL(url, StoreType.MV_STORE);
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

        File dbFile;
        String urlUpperCase = url.toUpperCase(Locale.ROOT);
        if (urlUpperCase.contains(";MV_STORE=FALSE")) {
            dbFile = new File(dataDir + ".h2.db");
        } else {
            dbFile = new File(dataDir + ".mv.db");
        }

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
     * Get the current version of the database.
     */
    public int getCurrentVersion() {
        return Constants.BUILD_ID;
    }

    /**
     * Parse the version out of a database name, the format should be
     * *.[version].h2|mv.db
     *   This method takes into account that there may be extra periods in the name
     */
    public int getVersion(String databaseName) throws IOException {
        String[] parts = databaseName.split("\\.");
        try{
            return getVersion(parts);
        }catch(NumberFormatException e) {
            throw new IOException("Invalid h2 database name format: " + databaseName);
        }
    }

    private static int getVersion(String[] parts) throws NumberFormatException {
        return Integer.parseInt(parts[parts.length - 3]);
    }

    /**
     * Check the name of the database file to see if it is a legacy db.
     *
     * *.h2.db = legacy
     * *.[version].h2.db = current
     */
    public static boolean isLegacy(String databaseName) {
        String[] parts = databaseName.split("\\.");
        if(parts.length < 4)
            return true;
        else {
            //Try to parse version
            try{
                getVersion(parts);
                return false;
            }catch(NumberFormatException e) {
                return true;
            }
        }
    }

    /**
     * Dump legacy database to SQL using legacy driver from separate classloader
     */
    public void dumpLegacy(String propertyPrefix, Path dumpPath) throws Exception {
        String url = Common.envProps.getString(propertyPrefix + "db.url");
        String urlUpperCase = url.toUpperCase(Locale.ROOT);
        if (!urlUpperCase.contains(";DB_CLOSE_ON_EXIT=")) {
            url += ";DB_CLOSE_ON_EXIT=FALSE";
        }
        if (!urlUpperCase.contains(";MV_STORE=")) {
            url += ";MV_STORE=FALSE";
        }
        if (!urlUpperCase.contains(";IGNORECASE=")) {
            url += ";IGNORECASE=TRUE";
        }

        Properties connectionProperties = new Properties();
        connectionProperties.compute("user", (a, b) -> Common.envProps.getString(propertyPrefix + "db.username", null));
        connectionProperties.compute("password", (a, b) -> Common.envProps.getString(propertyPrefix + "db.password", null));

        try (URLClassLoader jarLoader = loadLegacyJar()) {
            Class<?> driverManager = Class.forName("org.h2.Driver", false, jarLoader);
            Method connect = driverManager.getMethod("connect", String.class, Properties.class);
            //Get the INSTANCE to work on
            Field instance = driverManager.getDeclaredField("INSTANCE");
            instance.setAccessible(true);

            final String finalUrl = url;
            dumpToFile(dumpPath, () -> (Connection) connect.invoke(instance.get(driverManager), finalUrl, connectionProperties));
        }
    }

    public void dumpToFile(Path dumpPath, Callable<Connection> connectionSupplier) throws Exception {
        try (Connection conn = connectionSupplier.call()) {
            try (Statement stat = conn.createStatement()) {
                ResultSet rs = stat.executeQuery(H2_CREATE_VERSION_SELECT);
                int version = rs.next() ? rs.getInt(1) : -1;
                LOG.info("Dumping H2 database version " + version + " to SQL file at " + dumpPath);
                stat.executeQuery("SCRIPT DROP TO '" + dumpPath.toString() + "' COMPRESSION ZIP");
            }
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