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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import com.serotonin.util.StringUtils;

public class H2Proxy extends AbstractDatabaseProxy {
    private static final Log LOG = LogFactory.getLog(H2Proxy.class);

    private JdbcConnectionPool dataSource;
    private Server web; //web UI
    
    @Override
    protected void initializeImpl(String propertyPrefix) {
        LOG.info("Initializing H2 connection manager");
        JdbcDataSource jds = new JdbcDataSource();
        jds.setURL(getUrl(propertyPrefix));
        jds.setDescription("maDataSource");
        
        String user = Common.envProps.getString(propertyPrefix + "db.username", null);
	    if(user != null){
	    	jds.setUser(user);
	    
	        String password = Common.envProps.getString(propertyPrefix + "db.password", null);
	        if(password != null)
	        	jds.setPassword(password);
        }
        dataSource = JdbcConnectionPool.create(jds);
        dataSource.setMaxConnections(Common.envProps.getInt(propertyPrefix + "db.pool.maxActive", 100));
        
    	if(Common.envProps.getBoolean(propertyPrefix + "db.web.start", false)){
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

            // Replace macros in the line
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "blob", replacement("blob"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "char", replacement("char"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "clob", replacement("clob"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "double", replacement("double"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "identity", replacement("identity"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "int", replacement("int"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "varchar", replacement("varchar"));
            //
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "ALTER COLUMN", replacement("ALTER COLUMN"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "DROP FOREIGN KEY",
            //                    replacement("DROP FOREIGN KEY"));

            statement.append(line);
            statement.append(" ");
            if (line.endsWith(";")) {
                // Execute the statement
                ejt.execute(statement.toString());
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
        String dataDir = ejt.queryForObject("call DATABASE_PATH()", new Object[]{}, String.class, null);
        if(dataDir == null)
        	return null;
    	return new File(dataDir);
    	
    }
    
    @Override
    public Long getDatabaseSizeInBytes(){
    	ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(this.getDataSource());
        String dataDir = ejt.queryForObject("call DATABASE_PATH()", new Object[]{}, String.class, null);
        if(dataDir == null){
        	return null;
        }
        File dbData = new File(dataDir + ".h2.db"); //Good until we change to MVStore
    	if(dbData.exists()){
    		DirectoryInfo dbInfo = DirectoryUtils.getSize(dbData);
    		return dbInfo.getSize();
    	}else
    		return null;
    }

    @Override
    public void terminateImpl() {
        if (dataSource != null)
            dataSource.dispose();
        if(web != null){
        	if(web.isRunning(true)){
        		web.stop();
        		web.shutdown();
        	}
        }
    }

    @Override
    public boolean tableExists(ExtendedJdbcTemplate ejt, String tableName) {
    	return ejt.queryForObject("SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES WHERE table_name='"
                + tableName.toUpperCase() + "' AND table_schema='PUBLIC'", new Object[]{}, Integer.class, 0) > 0;
    }
    
}