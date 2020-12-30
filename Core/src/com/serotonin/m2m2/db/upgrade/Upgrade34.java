/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.transaction.support.TransactionTemplate;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;

/**
 * Adds seriesId to all data points
 *
 * @author Terry Packer
 */
public class Upgrade34 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {

        try (OutputStream out = createUpdateLogOutputStream()) {
            //Create timeSeriesTable
            HashMap<String, String[]> createTimeSeries = new HashMap<>();
            createTimeSeries.put(DatabaseProxy.DatabaseType.MYSQL.name(), createTimeSeriesTableMySQL);
            createTimeSeries.put(DatabaseProxy.DatabaseType.H2.name(), createTimeSeriesTableSQL);
            createTimeSeries.put(DatabaseProxy.DatabaseType.POSTGRES.name(), createTimeSeriesTableSQL);
            createTimeSeries.put(DatabaseProxy.DatabaseType.MSSQL.name(), createTimeSeriesTableMSSQL);
            runScript(createTimeSeries, out);

            //Add column
            runScript(Collections.singletonMap(DEFAULT_DATABASE_TYPE, new String[] {
                    "ALTER TABLE dataPoints ADD COLUMN seriesId INT;",
            }), out);

            //Insert/Update ids
            runScript(Collections.singletonMap(DEFAULT_DATABASE_TYPE, new String[] {
                    "INSERT INTO timeSeries (id) SELECT id FROM dataPoints;",
                    "UPDATE dataPoints SET seriesId = id;"
            }), out);

            //Add in constraint
            runScript(Collections.singletonMap(DEFAULT_DATABASE_TYPE, new String[] {
                    "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk5 FOREIGN KEY (seriesId) REFERENCES timeSeries(id);"
            }), out);

            //Make NON-NULL
            HashMap<String, String[]> scripts = new HashMap<>();
            scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), new String[] {"ALTER TABLE dataPoints MODIFY COLUMN seriesId INT NOT NULL;"});
            scripts.put(DEFAULT_DATABASE_TYPE,  new String[] {"ALTER TABLE dataPoints ALTER COLUMN seriesId INT NOT NULL;"});
            runScript(scripts, out);

        }
    }

    private final String[] createTimeSeriesTableMySQL = new String[] {
            "CREATE TABLE timeSeries (id INT NOT NULL auto_increment, PRIMARY KEY (id)) engine=InnoDB;",
    };

    private final String[] createTimeSeriesTableSQL = new String[] {
            "CREATE TABLE timeSeries (id INT NOT NULL auto_increment, PRIMARY KEY (id));",
    };

    private final String[] createTimeSeriesTableMSSQL = new String[] {
            "CREATE TABLE timeSeries (id INT NOT NULL IDENTITY, PRIMARY KEY (id));",
    };


    @Override
    protected String getNewSchemaVersion() {
        return "35";
    }

    @Override
    public TransactionTemplate getTransactionTemplate() {
        return super.getTransactionTemplate();
    }
}
