/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.Field;
import org.jooq.Table;

import com.infiniteautomation.mango.db.DefaultSchema;
import com.serotonin.m2m2.module.DatabaseSchemaDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

/**
 * @author Matthew Lohbihler
 */
public class DBConvert {
    private static final Log LOG = LogFactory.getLog(DBConvert.class);

    private DatabaseProxy source;
    private DatabaseProxy target;

    public void setSource(DatabaseProxy source) {
        this.source = source;
    }

    public void setTarget(DatabaseProxy target) {
        this.target = target;
    }

    public void execute() throws SQLException {
        LOG.warn("Running database conversion from " + source.getType().name() + " to " + target.getType().name());

        // Create the connections
        try (Connection sourceConn = source.getDataSource().getConnection()) {
            sourceConn.setAutoCommit(true);
            try (Connection targetConn = target.getDataSource().getConnection()) {
                targetConn.setAutoCommit(false);

                List<Table<?>> tables = new ArrayList<>(DefaultSchema.DEFAULT_SCHEMA.getTables());

                for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class)) {
                    tables.addAll(def.getTablesForConversion());
                }

                int constraintViolations = 0;
                for (int i = 0; i < tables.size(); i++) {
                    Table<?> table = tables.get(i);
                    try {
                        copyTable(sourceConn, targetConn, table);
                    } catch (SQLIntegrityConstraintViolationException e) {
                        // TODO either pre-order the tables or look up foreign key reference to check if table exists yet
                        // table must have a constraint that references a table we haven't created yet
                        if (constraintViolations++ < 1000) {
                            LOG.warn("Constraint violation while converting table " + table.getName() + ", will reattempt");
                            tables.add(table);
                        } else {
                            throw e;
                        }
                    }
                }
            }
        }

        LOG.warn("Completed database conversion");
    }

    private void copyTable(Connection sourceConn, Connection targetConn, Table<?> table) throws SQLException {
        String tableName = table.getName();
        LOG.warn("Converting table " + tableName + "...");

        // Get the source data
        Statement sourceStmt = sourceConn.createStatement();

        // only copy fields explicitly listed in our schema
        String fields = table.fieldStream().map(Field::getName).collect(Collectors.joining(","));
        ResultSet rs = sourceStmt.executeQuery("select  " + fields + " from " + tableName);

        // Create the insert statement from the meta data of the source.
        StringBuilder sb = new StringBuilder();
        ResultSetMetaData meta = rs.getMetaData();
        int columns = meta.getColumnCount();
        sb.append("insert into ").append(tableName).append(" (");
        for (int i = 1; i <= columns; i++) {
            if (i > 1)
                sb.append(",");
            sb.append(meta.getColumnName(i));
        }
        sb.append(") values (");
        for (int i = 1; i <= columns; i++) {
            if (i > 1)
                sb.append(",");
            sb.append("?");
        }
        sb.append(")");
        String insert = sb.toString();

        // Do the inserts. Commit every now and then so that transaction logs don't get huge.
        int cnt = 0;
        int total = 0;
        int maxCnt = 1000;
        while (rs.next()) {
            PreparedStatement targetStmt = targetConn.prepareStatement(insert);
            for (int i = 1; i <= columns; i++)
                targetStmt.setObject(i, rs.getObject(i), meta.getColumnType(i));
            targetStmt.executeUpdate();

            cnt++;
            total++;
            if (cnt >= maxCnt) {
                targetConn.commit();
                cnt = 0;
            }

            targetStmt.close();
        }
        targetConn.commit();

        rs.close();
        sourceStmt.close();

        LOG.warn("Finished converting table " + tableName + ". " + total + " records copied.");
    }
}
