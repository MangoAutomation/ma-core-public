/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.dao.DataAccessException;

import com.infiniteautomation.mango.spring.DatabaseProxyConfiguration;
import com.serotonin.util.DirectoryInfo;
import com.serotonin.util.DirectoryUtils;

public class MySQLProxy extends BasePooledProxy {

    public MySQLProxy(DatabaseProxyFactory factory, DatabaseProxyConfiguration configuration, String propertyPrefix) {
        super(factory, configuration, propertyPrefix);
    }

    @Override
    protected String getUrl() {
        String url = super.getUrl();
        if (url.indexOf('?') > 0)
            url += "&";
        else
            url += "?";
        url += "useUnicode=yes&characterEncoding=" + StandardCharsets.UTF_8.name();
        return url;
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.MYSQL;
    }

    @Override
    protected String getDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public double applyBounds(double value) {
        if (Double.isNaN(value))
            return 0;
        if (value == Double.POSITIVE_INFINITY)
            return Double.MAX_VALUE;
        if (value == Double.NEGATIVE_INFINITY)
            return -Double.MAX_VALUE;

        return value;
    }

    @Override
    public boolean tableExists(String tableName) {
        try {
            getJdbcTemplate().execute("select count(*) from " + tableName);
        }
        catch (DataAccessException e) {
            if (e.getCause() instanceof SQLException) {
                SQLException se = (SQLException) e.getCause();
                // This state means a missing table.
                return !"42S02".equals(se.getSQLState());
            }
            throw e;
        }
        return true;
    }

    @Override
    public String getTableListQuery() {
        return "show tables";
    }

    @Override
    public File getDataDirectory() {
        String url = getUrl();
        if (!url.startsWith("jdbc:")) {
            throw new IllegalStateException("Invalid JDBC URL");
        }
        URI uri = URI.create(url.substring("jdbc:".length()));
        String host = uri.getHost();
        if (!(host == null || host.equals("localhost") || host.equals("[::1]") || host.startsWith("127."))) {
            throw new UnsupportedOperationException();
        }

        String dataDir = getJdbcTemplate().queryForObject("select @@DATADIR", new Object[]{}, String.class, null);
        if (dataDir == null) {
            throw new IllegalStateException("Couldn't get data directory from MySQL");
        }
        return new File(dataDir);
    }

    @Override
    public long getDatabaseSizeInBytes() {
        File dataDirectory = getDataDirectory();
        DirectoryInfo dbInfo = DirectoryUtils.getSize(dataDirectory);
        return dbInfo.getSize();
    }

    @Override
    public int defaultFetchSize() {
        // see https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-implementation-notes.html
        return Integer.MIN_VALUE;
    }

    @Override
    public void clean() {
        String testUrl = env.getProperty("db.test.url");
        String dbName = env.getProperty("db.test.name");
        String username = env.getProperty("db.username");
        String password = env.getProperty("db.password");
        try (Connection conn = DriverManager.getConnection(testUrl, username, password)) {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(String.format("DROP DATABASE `%s`", dbName));
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
