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

            runScript(Collections.singletonMap(DEFAULT_DATABASE_TYPE, new String[] {
                    "ALTER TABLE dataPoints ADD COLUMN seriesId INT;",
                    "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk5 FOREIGN KEY (seriesId) REFERENCES timeSeries(id);",
            }), out);

            //Do for all data points, insert timeSeries and set on data point
            //Add seriesId ID column to dataPoints
            final int batchSize = Common.envProps.getInt("db.in.maxOperands", 1000);
            final String update = "UPDATE dataPoints SET seriesId=? WHERE id=?";
            final List<Object[]> batchArgs = new ArrayList<>();
            final AtomicInteger currentBatchSize = new AtomicInteger();

            ejt.query("SELECT id FROM dataPoints", rch -> {
                int id = rch.getInt(1);
                int seriesId = ejt.doInsert("INSERT INTO timeSeries VALUES(?)", new Object[] {id});
                batchArgs.add(new Object[]{seriesId, id});
                if(currentBatchSize.incrementAndGet() >= batchSize) {
                    ejt.batchUpdate(update, batchArgs);
                    batchArgs.clear();
                    currentBatchSize.set(0);
                }
            });

            //Finish the batch
            if(currentBatchSize.get() > 0) {
                ejt.batchUpdate(update, batchArgs);
            }

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
