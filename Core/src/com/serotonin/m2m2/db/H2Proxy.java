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

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Recover;
import org.h2.tools.Server;
import org.springframework.jdbc.core.RowMapper;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.DaoUtils;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.util.DirectoryInfo;
import com.serotonin.util.DirectoryUtils;
import com.serotonin.util.StringUtils;

public class H2Proxy extends AbstractDatabaseProxy {
    private static final Log LOG = LogFactory.getLog(H2Proxy.class);

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

    /**
     * Potentially upgrade the h2 pagestore database from v196 
     * @param propertyPrefix
     */
    private void upgradePageStore(String propertyPrefix) {
        
        //Parse out the useful sections of the url
        String url = Common.envProps.getString(propertyPrefix + "db.url");
        url = StringUtils.replaceMacros(url, System.getProperties());
        String [] jdbcParts = url.split(":");
        String [] commandParts = jdbcParts[2].split(";");
        Path dbPath = Paths.get(commandParts[0]);
        Path existingDbPath = dbPath.getParent().resolve(dbPath.getFileName() + ".h2.db");
        
        //TODO Determine a good place for these files
        Path dumpPath = dbPath.getParent().resolve(dbPath.getFileName() + ".h2.196.sql.zip");
        Path bakDbPath = dbPath.getParent().resolve("mah2.h2.196.db");
        
        //If the backup database from a previous attempt is still hanging around we will 
        
        //We need to do this if a db exists and is version 196 or below
        if(existingDbPath.toFile().exists()) {
            
            try {
                //Did a previous attempt fail?
                if(bakDbPath.toFile().exists()){
                    //TODO is this logic ideal?
                    //Failed attempt assume the backup is the best one to keep so replace the existing
                    Files.copy(bakDbPath, existingDbPath);
                }else {
                    //Move existing db (for safety)
                    Files.copy(existingDbPath, bakDbPath);
                }

                //First we will try to get the version from the existing db
                org.h2.Driver.load();
                String fullUrl = getUrl(propertyPrefix);
                String user = Common.envProps.getString(propertyPrefix + "db.username", null);
                String password = Common.envProps.getString(propertyPrefix + "db.password", null);
                

                
                try(Connection conn = DriverManager.getConnection(fullUrl, user, password);){
                    Statement stat = conn.createStatement();
                    ResultSet rs = stat.executeQuery(H2_CREATE_VERSION_SELECT);
                    if(rs.next()) {
                        int version = rs.getInt(1);
                        if(version >= 197)
                            return;
                    }
                    stat.executeQuery("SCRIPT DROP TO '" + dumpPath.toString() + "' COMPRESSION ZIP");
                }
                
                //Delete existing so we can re-create it using the dump script
                Files.delete(existingDbPath);

                //Open a connection and import the dump script
                String runScript = fullUrl + ";init=RUNSCRIPT FROM '" + dumpPath.toString() + "' COMPRESSION ZIP";
                try(Connection conn = DriverManager.getConnection(runScript, user, password);){
                    Statement stat = conn.createStatement();
                    //Might as well do a compaction here
                    stat.execute("shutdown compact");
                    stat.close();
                }
                
                try {
                    Files.deleteIfExists(dumpPath);
                } catch (IOException e) {
                    LOG.warn("Unable to delete un-necessary h2 dump file " + dumpPath.toString(), e);
                }
                
                try {
                    Files.deleteIfExists(bakDbPath);
                } catch (IOException e) {
                    LOG.warn("Unable to delete un-necessary h2 backup file " + bakDbPath.toString(), e);
                }
            }catch(Exception e) {
                throw new ShouldNeverHappenException(e);
            }
        }
    }

    private String getUrl(String propertyPrefix) {
        String url = Common.envProps.getString(propertyPrefix + "db.url");
        url = StringUtils.replaceMacros(url, System.getProperties());
        if (!url.contains(";DB_CLOSE_ON_EXIT=")) {
            url += ";DB_CLOSE_ON_EXIT=FALSE";
        }
        if (!url.contains(";MV_STORE=")) {
            url += ";MV_STORE=FALSE";
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
        return new File(dataDir);
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
        File dbData = new File(dataDir + ".h2.db"); // Good until we change to MVStore
        if (dbData.exists()) {
            DirectoryInfo dbInfo = DirectoryUtils.getSize(dbData);
            return dbInfo.getSize();
        } else
            return null;
    }

    @Override
    public void terminateImpl() {
        if (Common.envProps.getBoolean("db.h2.shutdownCompact", false)) {
            runScript(new String[] {"SHUTDOWN COMPACT;"}, null);
        }
        if (dataSource != null)
            dataSource.dispose();
        if (web != null) {
            if (web.isRunning(true)) {
                web.stop();
                web.shutdown();
            }
        }
    }

    @Override
    public boolean tableExists(ExtendedJdbcTemplate ejt, String tableName) {
        return ejt.queryForObject(
                "SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES WHERE table_name='"
                        + tableName.toUpperCase() + "' AND table_schema='PUBLIC'",
                new Object[] {}, Integer.class, 0) > 0;
    }
    
}