/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockMangoProperties;
import com.serotonin.provider.Providers;
import com.serotonin.util.properties.MangoProperties;

/**
 *
 * @author Terry Packer
 */
public class MySQLDatabaseUpgradeTest {


    private final String createScript = "/db/version1/createTables-MySQL.sql";
    private final String dataScript = "/db/version1/defaultData-MySQL.sql";

    @Test
    public void doUpgrade() throws Exception {
        Properties props = new Properties();
        try(InputStream input = getClass().getClassLoader().getResourceAsStream("mysqlUpgradeTest.properties")){
            props.load(input);
        }
        if(!props.getProperty("enabled", "false").equals("true"))
            return;

        //Dummy for registering the insert user startup task
        Providers.add(IMangoLifecycle.class, new MockMangoLifecycle(null));

        //Setup MySQL db properties

        MockMangoProperties properties = new MockMangoProperties();
        properties.setProperty("db.type", "mysql");
        properties.setProperty("db.url", props.getProperty("db.url") + props.getProperty("db.name"));
        properties.setProperty("db.username", props.getProperty("db.username"));
        properties.setProperty("db.password", props.getProperty("db.password"));
        Providers.add(MangoProperties.class, properties);

        //Load the driver
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(props.getProperty("db.url"), props.getProperty("db.username"), props.getProperty("db.password"))){
            Statement stmt = conn.createStatement();
            stmt.executeUpdate( "DROP DATABASE IF EXISTS `" + props.getProperty("db.name") + "`;");
            stmt.executeUpdate("CREATE DATABASE `" + props.getProperty("db.name") + "`;");
            stmt.execute("USE `" + props.getProperty("db.name") + "`;");
            //Create the database at version 1
            runScript(stmt, this.getClass().getResourceAsStream(createScript), System.out);
        }

        //Insert the test data

        //Start the proxy and let it upgrade
        String type = Common.envProps.getString("db.type", "h2");
        DatabaseType databaseType = DatabaseType.valueOf(type.toUpperCase());
        DatabaseProxyFactory factory = new DefaultDatabaseProxyFactory();
        Common.databaseProxy = factory.createDatabaseProxy(databaseType);
        Common.databaseProxy.initialize(this.getClass().getClassLoader());
    }



    protected void runScript(Statement stmt, InputStream input, OutputStream out) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(input));

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null)
                lines.add(line);

            String[] script = new String[lines.size()];
            lines.toArray(script);
            runScript(stmt, script, out);
        }
        catch (Exception ioe) {
            throw new ShouldNeverHappenException(ioe);
        }
        finally {
            try {
                if (in != null)
                    in.close();
            }
            catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    public void runScript(Statement stmt, String[] script, OutputStream out) throws Exception {
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
                stmt.executeUpdate(statement.toString());
                statement.delete(0, statement.length() - 1);
            }
        }

    }
}
