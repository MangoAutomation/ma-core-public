/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jooq.Field;
import org.jooq.InsertValuesStep6;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.db.tables.PublishedPoints;
import com.infiniteautomation.mango.db.tables.records.PublishedPointsRecord;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.db.dao.BaseDao;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.rt.GroupProcessor;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.util.SerializationHelper;

/**
 * Break published points out into new table
 *
 * @author Terry Packer
 */
public class Upgrade42 extends DBUpgrade {

    private final Logger LOG = LoggerFactory.getLogger(Upgrade42.class);

    @Override
    protected void upgrade() throws Exception {
        //Create table without Name NOT NULL constraint
        runScript(Collections.singletonMap(DEFAULT_DATABASE_TYPE, new String[] {
                "CREATE TABLE publishedPoints (id INT NOT NULL AUTO_INCREMENT, xid VARCHAR(100) NOT NULL, name VARCHAR(255), enabled CHAR(1), publisherId INT NOT NULL, dataPointId INT NOT NULL, data LONGTEXT, jsonData LONGTEXT, PRIMARY KEY (id));",
                "ALTER TABLE publishedPoints ADD CONSTRAINT publishedPointsUn1 UNIQUE (xid);",
                "ALTER TABLE publishedPoints ADD CONSTRAINT publishedPointsFk1 FOREIGN KEY (publisherId) REFERENCES publishers (id) ON DELETE CASCADE;",
                "ALTER TABLE publishedPoints ADD CONSTRAINT publishedPointsFk2 FOREIGN KEY (dataPointId) REFERENCES dataPoints (id) ON DELETE CASCADE;",
                "CREATE INDEX publishedPointNameIndex on publishedPoints (name ASC);",
                "CREATE INDEX publishedPointEnabledIndex on publishedPoints (enabled ASC);",
                "CREATE INDEX publishedPointXidNameIndex on publishedPoints (xid ASC, name ASC);"
        }));

        //Convert existing to new table
        Table<Record> publishersTable = DSL.table(DSL.name("publishers"));

        Field<Integer> idField = DSL.field(DSL.name("id"), Integer.class);
        Field<String> publisherTypeField = DSL.field(DSL.name("publisherType"), String.class);
        Field<byte[]> dataField = DSL.field(DSL.name("data"), byte[].class);
        PublishedPoints publishedPoints = PublishedPoints.PUBLISHED_POINTS;

        //Map of data point id to Name to ensure existence of point
        Map<Integer, String> dataPointNameMap = new HashMap<>();

        this.create.select(idField, publisherTypeField, dataField).from(publishersTable).forEach(row -> {
            int publisherId = row.get(idField);
            String publisherType = row.get(publisherTypeField);
            PublisherVO p = (PublisherVO) SerializationHelper
                    .readObjectInContextFromArray(row.get(dataField));
            p.setId(publisherId);

            PublisherDefinition<?> definition = ModuleRegistry.getPublisherDefinition(publisherType);
            if(definition == null) {
                LOG.warn("Module missing for publisher {} with id {}, dropping all published points", p.getName(), p.getId());
            }else {

                int maxConcurrency = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
                //TODO Is there an executor service available now?
                PublishedPointGroupUpgrader upgrader = new PublishedPointGroupUpgrader(publisherId, definition, Executors.newFixedThreadPool(maxConcurrency), maxConcurrency);
                upgrader.initialize((List<PublishedPointVO>) p.deletedProperties().get("points"), databaseProxy.maxInParameters());
            }

            //update publisher data to re-serialize and remove points
            this.create.update(publishersTable)
                    .set(dataField, SerializationHelper.writeObjectToArray(p))
                    .where(idField.eq(publisherId)).execute();
        });

        //Finally add the NOT NULL constraint to name column
        Map<String, String[]> makeNonNull = new HashMap<>();
        makeNonNull.put(DatabaseType.MYSQL.name(), new String[] {"ALTER TABLE publishedPoints MODIFY COLUMN name VARCHAR(255) NOT NULL;"});
        makeNonNull.put(DatabaseType.H2.name(), new String[] {"ALTER TABLE publishedPoints ALTER COLUMN name VARCHAR(255) NOT NULL;"});
        makeNonNull.put(DatabaseType.MSSQL.name(), new String[] {"ALTER TABLE publishedPoints ALTER COLUMN name NVARCHAR(255) NOT NULL;"});
        makeNonNull.put(DatabaseType.POSTGRES.name(), new String[] {"ALTER TABLE publishedPoints ALTER COLUMN name VARCHAR(255) NOT NULL;"});
        runScript(makeNonNull);
    }

    private String convertData(JsonNode data) {
        return null;
    }

    @Override
    protected String getNewSchemaVersion() {
        return "43";
    }

    class PublishedPointGroupUpgrader extends GroupProcessor<List<PublishedPointVO>, Void> {

        private final PublishedPoints publishedPoints = PublishedPoints.PUBLISHED_POINTS;
        private final Table<Record> dataPointsTable;
        private final Field<Integer> idField;
        private final Field<String> nameField;
        private final int publisherId;
        private final PublisherDefinition<?> definition;

        public PublishedPointGroupUpgrader(int publisherId, PublisherDefinition<?> definition, ExecutorService executor, int maxConcurrency) {
            super(executor, maxConcurrency);
            this.dataPointsTable = DSL.table(DSL.name("dataPoints"));
            this.idField = DSL.field(DSL.name("id"), Integer.class);
            this.nameField = DSL.field(DSL.name("name"), String.class);
            this.publisherId = publisherId;
            this.definition = definition;
        }

        public void initialize(List<PublishedPointVO> items, int groupSize) {
            long startTs = 0L;
            if (log.isInfoEnabled()) {
                startTs = Common.timer.currentTimeMillis();
                log.info("Upgrading {} published points in {} threads",
                        items.size(), maxConcurrency);
            }

            // break into subgroups
            List<List<PublishedPointVO>> subgroups = new ArrayList<>();
            int numPoints = items.size();
            for (int from = 0; from < numPoints; from += groupSize) {
                int to = Math.min(from + groupSize, numPoints);
                subgroups.add(items.subList(from, to));
            }
            process(subgroups);

            if (log.isInfoEnabled()) {
                log.info("Upgrade of {} published points in {} threads took {} ms",
                        items.size(), maxConcurrency, Common.timer.currentTimeMillis() - startTs);
            }
        }

        @Override
        protected Void processItem(List<PublishedPointVO> subgroup, int itemId) throws Exception {
            long startTs = 0L;
            if (log.isInfoEnabled()) {
                startTs = Common.timer.currentTimeMillis();
                log.info("Upgrading group {} of {} published points",
                        itemId,
                        subgroup.size());
            }

            int failedCount = 0;

            //Collect the names for the points we intend to process
            Map<Integer, String> dataPointNameMap = new HashMap<>(subgroup.size());
            for (PublishedPointVO point : subgroup) {
                dataPointNameMap.put(point.getDataPointId(), null);
            }
            create.select(idField, nameField)
                    .from(dataPointsTable)
                    .where(idField.in(dataPointNameMap.keySet())).forEach((r -> {
                        dataPointNameMap.put(r.get(idField), r.get(nameField));
                    }));

            //For all published points, insert them into new table
            InsertValuesStep6<PublishedPointsRecord, String, String, String, Integer, Integer, String> insertPoints = create.insertInto(publishedPoints).columns(
                    publishedPoints.xid,
                    publishedPoints.name,
                    publishedPoints.enabled,
                    publishedPoints.publisherId,
                    publishedPoints.dataPointId,
                    publishedPoints.data);
            try (insertPoints) {
                for (PublishedPointVO point : subgroup) {
                    String name = dataPointNameMap.get(point.getDataPointId());
                    if(name != null) {
                        try {
                            insertPoints.values(
                                    PublishedPointVO.XID_PREFIX + UUID.randomUUID().toString(),
                                    name,
                                    BaseDao.boolToChar(true),
                                    publisherId,
                                    point.getDataPointId(),
                                    definition.createPublishedPointDbData(point)
                            );
                        } catch (JsonProcessingException e) {
                            failedCount++;
                            LOG.error("Failed to write published point JSON for data point with id {} on publisher with id {}, published point removed", point.getDataPointId(), publisherId, e);
                        } catch (Exception e) {
                            failedCount++;
                            log.error("Failed to upgrade published point for data point with id {} on publisher with id {},  published point removed", point.getDataPointId(), publisherId, e);
                        }
                    }else {
                        failedCount++;
                        LOG.info("Discarding published point because data point with id {} no longer exists.", point.getDataPointId());
                    }
                }
            }

            insertPoints.execute();

            if (log.isInfoEnabled()) {
                log.info("Group {} successfully upgraded {} of {} published points in {} ms",
                        itemId, subgroup.size() - failedCount, subgroup.size(), Common.timer.currentTimeMillis() - startTs);
            }
            return null;
        }
    }
}
