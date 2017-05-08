/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.db;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Matthew Lohbihler
 */
public class JndiDataSource implements DataSource {
    private static final Log LOG = LogFactory.getLog(JndiDataSource.class);

    private String resourceName;
    private DataSource dataSource;

    public JndiDataSource() {
        // no op
    }

    public JndiDataSource(String resourceName) {
        setResourceName(resourceName);
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Connection getConnection(String username, String password) {
        throw new RuntimeException("Don't use this method. Use the one without username and password instead.");
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            synchronized (this) {
                if (dataSource == null) {
                    try {
                        InitialContext ctx = new InitialContext();
                        dataSource = (DataSource) ctx.lookup(resourceName);
                    }
                    catch (NamingException e) {
                        LOG.error("Error while initializing data source", e);
                        throw new SQLException("Error while initializing data source: " + e.getMessage());
                    }

                    // Test to ensure we can get a connection via the datasource.
                    Connection c = dataSource.getConnection();
                    c.close();
                }
            }
        }

        return dataSource.getConnection();
    }

    public boolean isWrapperFor(Class<?> iface) {
        return DataSource.class.isAssignableFrom(iface);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("not implemented");
    }

    public int getLoginTimeout() {
        return 0;
    }

    public void setLoginTimeout(int timeout) {
        throw new UnsupportedOperationException("setLoginTimeout");
    }

    public PrintWriter getLogWriter() {
        throw new UnsupportedOperationException("getLogWriter");
    }

    public void setLogWriter(PrintWriter pw) {
        throw new UnsupportedOperationException("setLogWriter");
    }

	/* (non-Javadoc)
	 * @see javax.sql.CommonDataSource#getParentLogger()
	 */
	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}
}
