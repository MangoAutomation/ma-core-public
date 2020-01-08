/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.engine.Constants;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.springframework.jdbc.core.RowMapper;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.DaoUtils;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.util.DirectoryInfo;
import com.serotonin.util.DirectoryUtils;

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

        upgradePageStore(propertyPrefix);

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
            String webArgs[] = new String[4];
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
        return Paths.get(commandParts[0]);
    }

    /**
     * Potentially upgrade the h2 pagestore database from v196
     * @param propertyPrefix
     */
    private void upgradePageStore(String propertyPrefix) {

        //Parse out the useful sections of the url
        String dbUrl = Common.envProps.getString(propertyPrefix + "db.url");
        Path dbPath = getDbPathFromUrl(dbUrl);

        //Check to see if we are a legacy database
        Path legacy = dbPath.getParent().resolve(dbPath.getFileName().toString() + ".h2.db");
        if(legacy.toFile().exists()) {
            LOG.info("Converting legacy h2 database...");
            //We will need to convert it.
            //Create a reference for the dump file
            String dumpFileName = "mah2.h2.196.sql.zip";
            Path dumpPath = Common.MA_HOME_PATH.resolve("backup").resolve(dumpFileName);

            //Check dump file existence, if so abort startup
            if(dumpPath.toFile().exists())
                throw new ShouldNeverHappenException("Found upgrade database backup, aborting startup.  Likely corrupt database, a clean backup can be found here: " + dumpPath.toString());

            try {
                LOG.info("Dumping legacy database to file " + dumpPath.toString());
                dump(legacy, dumpPath);

                //Delete existing so we can re-create it using the dump script
                Files.delete(legacy);

                String user = Common.envProps.getString(propertyPrefix + "db.username", null);
                String password = Common.envProps.getString(propertyPrefix + "db.password", null);

                String url = getUrl(propertyPrefix);
                //Open a connection and import the dump script
                LOG.info("Importing existing H2 database...");
                String runScript = url + ";init=RUNSCRIPT FROM '" + dumpPath.toString().replaceAll("\\\\", "/") + "' COMPRESSION ZIP";
                try(Connection conn = DriverManager.getConnection(runScript, user, password);){
                    Statement stat = conn.createStatement();
                    //Might as well do a compaction here
                    stat.execute("SHUTDOWN COMPACT");
                    stat.close();
                }

                try {
                    LOG.info("Cleaning up H2 initializataion tests...");
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

    private String getUrl(String propertyPrefix) {
        String url = Common.envProps.getString(propertyPrefix + "db.url");
        String [] jdbcParts = url.split("jdbc:h2:");
        String [] commandParts = jdbcParts[1].split(";");
        Path dbPath = Paths.get(commandParts[0]);

        //Determine the version info
        Path dbFolder = dbPath.getParent();

        //Make sure we have a db folder, create if not
        if(!dbFolder.toFile().exists()) {
            if(!dbFolder.toFile().mkdirs()) {
                throw new ShouldNeverHappenException("Could not create databases directory at " + dbFolder.toString());
            }
        }

        String[] matchingDbs = dbFolder.toFile().list((dir, filename) -> {
            File possibleDb = new File(dir, filename);
            if(possibleDb.isFile() && filename.endsWith(".db") && filename.startsWith(dbPath.getFileName().toString()))
                return true;
            else
                return false;
        });

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
        builder.append(commandParts[0]);
        builder.append("." + version);

        //Add back on any command parts
        for(int i=1; i<commandParts.length; i++) {
            builder.append(";");
            builder.append(commandParts[i]);
        }
        url = builder.toString();

        //Force page store
        if (!url.contains(";MV_STORE=")) {
            url += ";MV_STORE=FALSE";
        }
        if (!url.contains(";DB_CLOSE_ON_EXIT=")) {
            url += ";DB_CLOSE_ON_EXIT=FALSE";
        }
        if (!url.contains(";IGNORECASE=")) {
            url += ";IGNORECASE=TRUE";
        }
        return url;
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
                        out.write((statement.toString() + "\n").getBytes(Common.UTF8_CS));
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
        if(url.toLowerCase().contains("mv_store=false")) {
            return new File(dataDir + ".h2.db"); // Good until we change to MVStore
        }else {
            return new File(dataDir + ".mv.db");
        }
    }

    @Override
    public Long getDatabaseSizeInBytes(){
        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(this.getDataSource());
        String dataDir =
                ejt.queryForObject("call DATABASE_PATH()", new Object[] {}, String.class, null);
        if (dataDir == null) {
            return null;
        }

        File dbData;
        //Detect page store or mv store
        String url = getUrl("");
        if(url.toLowerCase().contains("mv_store=false"))
            dbData = new File(dataDir + ".h2.db"); // Good until we change to MVStore
        else
            dbData = new File(dataDir + ".mv.db");

        if (dbData.exists()) {
            DirectoryInfo dbInfo = DirectoryUtils.getSize(dbData);
            return dbInfo.getSize();
        } else
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
        return ejt.queryForObject(
                "SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES WHERE table_name='"
                        + tableName.toUpperCase() + "' AND table_schema='PUBLIC'",
                        new Object[] {}, Integer.class, 0) > 0;
    }

    /**
     * Get the current version of the database.
     * @return
     */
    public int getCurrentVersion() {
        return Constants.BUILD_ID;
    }

    /**
     * Get the expected runtime database name
     * @return
     */
    public String getDatabaseFileSuffix() {
        return Constants.BUILD_ID + ".h2.db";
    }

    /**
     * Parse the version out of a database name, the format should be
     * *.[version].h2.db
     *   This method takes into account that there may be extra periods in the name
     * @param databaseName
     * @return
     * @throws NumberFormatException
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
     *
     * @return
     */
    public static boolean isLegacy(String databaseName) throws IOException {
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
     * Dump a database to SQL using a legacy driver
     * @param legacy
     * @param dumpPath
     * @throws Exception
     */
    public void dump(Path legacy, Path dumpPath) throws Exception {

        //First load in the 196 Driver
        Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"), H2Proxy.class.getName());
        File tempDirectoryFile = tempDirectory.toFile();
        tempDirectoryFile.mkdirs();
        tempDirectoryFile.deleteOnExit();

        ClassLoader jarLoader = loadLegacyJar();
        Class<?> driverManager = Class.forName("org.h2.Driver", false, jarLoader);

        String url = Common.envProps.getString("db.url");
        if (!url.contains(";DB_CLOSE_ON_EXIT=")) {
            url += ";DB_CLOSE_ON_EXIT=FALSE";
        }
        if (!url.contains(";MV_STORE=")) {
            url += ";MV_STORE=FALSE";
        }
        if (!url.contains(";IGNORECASE=")) {
            url += ";IGNORECASE=TRUE";
        }
        String user = Common.envProps.getString("db.username", null);
        String password = Common.envProps.getString("db.password", null);
        Properties connectionProperties = new Properties();
        if(user != null) {
            connectionProperties.put("user", user);
        }
        if(password != null) {
            connectionProperties.put("password", password);
        }

        Method connect = driverManager.getMethod("connect", String.class, Properties.class);
        //Get the INSTANCE to work on
        Field instance = driverManager.getDeclaredField("INSTANCE");
        instance.setAccessible(true);

        try(Connection conn = (Connection)connect.invoke(instance.get(driverManager), url, connectionProperties)){
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery(H2_CREATE_VERSION_SELECT);
            if(rs.next()) {
                int version = rs.getInt(1);
                LOG.info("H2 database is version " + version + " , will upgrade to " + Constants.BUILD_ID + ".");
            }
            LOG.info("Exporting existing H2 database...");
            stat.executeQuery("SCRIPT DROP TO '" + dumpPath.toString() + "' COMPRESSION ZIP");
            stat.close();
        }
    }

    /**
     * Load the legacy H2 Driver into an isolated class loader
     * @return
     * @throws MalformedURLException
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
        private ChildURLClassLoader childClassLoader;

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
            private FindClassClassLoader realParent;

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