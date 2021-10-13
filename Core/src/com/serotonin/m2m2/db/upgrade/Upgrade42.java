/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.Collections;
import java.util.UUID;

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
import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.db.dao.BaseDao;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PublisherDefinition;
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
        //TODO Published Points add all database types
        runScript(Collections.singletonMap(DatabaseType.H2.name(), new String[] {
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

        this.create.select(idField, publisherTypeField, dataField).from(publishersTable).forEach(row -> {
            int publisherId = row.get(idField);
            String publisherType = row.get(publisherTypeField);
            PublisherVO<? extends PublishedPointVO> p = (PublisherVO<? extends PublishedPointVO>) SerializationHelper
                    .readObjectInContextFromArray(row.get(dataField));
            p.setId(publisherId);
            //TODO Published Points ensure definition exists otherwise ...????
            PublisherDefinition<?> definition = ModuleRegistry.getPublisherDefinition(publisherType);

            PublishedPoints publishedPoints = PublishedPoints.PUBLISHED_POINTS;

            //TODO Published Points - auto closeable insert?
            //For all published points, insert them into new table
            InsertValuesStep6<PublishedPointsRecord, String, String, String, Integer, Integer, String> insertPoint = create.insertInto(publishedPoints).columns(
                    publishedPoints.xid,
                    publishedPoints.name,
                    publishedPoints.enabled,
                    publishedPoints.publisherId,
                    publishedPoints.dataPointId,
                    publishedPoints.data);

            for(PublishedPointVO point : p.getPoints()) {
                //TODO Published Points Batch insert points
                // - decide on a decent name or remove the field?
                // - ensure data point exists before inserting new point
                try {
                    insertPoint.values(
                            PublishedPointVO.XID_PREFIX + UUID.randomUUID().toString(),
                            "default name",
                            BaseDao.boolToChar(true),
                            p.getId(),
                            point.getDataPointId(),
                            definition.createPublishedPointDbData(point)
                    );
                } catch (JsonProcessingException e) {
                    LOG.error("Failed to write published point JSON for data point with id {}, published point removed", point.getDataPointId(), e);
                }
            }
            insertPoint.execute();

            //update publisher data to re-serialize and remove points
            this.create.update(publishersTable)
                    .set(dataField, SerializationHelper.writeObjectToArray(p))
                    .where(idField.eq(publisherId)).execute();
        });
    }

    private String convertData(JsonNode data) {
        return null;
    }

    @Override
    protected String getNewSchemaVersion() {
        return "43";
    }
}
