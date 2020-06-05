package com.serotonin.db;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;

public class DeregisteringDataSource extends BasicDataSource {
    @Override
    public synchronized void close() throws SQLException {
        super.close();

        try {
            Driver driver = DriverManager.getDriver(getUrl());
            DriverManager.deregisterDriver(driver);
        }
        catch (SQLException e) {
            // Ignore "No suitable driver" exceptions. The driver may have already been deregistered.
            if (!"No suitable driver".equals(e.getMessage()))
                throw e;
        }
    }
}
