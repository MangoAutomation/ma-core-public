/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        runScript(Collections.singletonMap(DEFAULT_DATABASE_TYPE, new String[] {
                "CREATE TABLE publishedPoints (id INT NOT NULL AUTO_INCREMENT, xid VARCHAR(100) NOT NULL, name VARCHAR(255), enabled CHAR(1), publisherId INT NOT NULL, dataPointId INT NOT NULL, data LONGTEXT, jsonData LONGTEXT, PRIMARY KEY (id));",
                "ALTER TABLE publishedPoints ADD CONSTRAINT publishedPointsUn1 UNIQUE (xid);",
                "ALTER TABLE publishedPoints ADD CONSTRAINT publishedPointsUn2 UNIQUE (publisherId, dataPointId);",
                "ALTER TABLE publishedPoints ADD CONSTRAINT publishedPointsFk1 FOREIGN KEY (publisherId) REFERENCES publishers (id) ON DELETE CASCADE;",
                "ALTER TABLE publishedPoints ADD CONSTRAINT publishedPointsFk2 FOREIGN KEY (dataPointId) REFERENCES dataPoints (id) ON DELETE CASCADE;",
                "CREATE INDEX publishedPointNameIndex on publishedPoints (name ASC);",
                "CREATE INDEX publishedPointEnabledIndex on publishedPoints (enabled ASC);",
                "CREATE INDEX publishedPointXidNameIndex on publishedPoints (xid ASC, name ASC);"
        }));

        //Convert existing to new table
        Table<Record> publishersTable = DSL.table(DSL.name("publishers"));
        Table<Record> dataPointsTable = DSL.table(DSL.name("dataPoints"));
        Field<String> nameField = DSL.field(DSL.name("name"), String.class);
        Field<Integer> idField = DSL.field(DSL.name("id"), Integer.class);
        Field<String> publisherTypeField = DSL.field(DSL.name("publisherType"), String.class);
        Field<byte[]> dataField = DSL.field(DSL.name("data"), byte[].class);

        //Map of data point id to Name to ensure existence of point
        Map<Integer, String> dataPointNameMap = new HashMap<>();

        this.create.select(idField, publisherTypeField, dataField).from(publishersTable).forEach(row -> {
            int publisherId = row.get(idField);
            String publisherType = row.get(publisherTypeField);
            PublisherVO p = (PublisherVO) SerializationHelper
                    .readObjectInContextFromArray(row.get(dataField));
            p.setId(publisherId);
            //TODO Published Points ensure definition exists otherwise ...????
            PublisherDefinition<?> definition = ModuleRegistry.getPublisherDefinition(publisherType);

            PublishedPoints publishedPoints = PublishedPoints.PUBLISHED_POINTS;

            //For all published points, insert them into new table
            InsertValuesStep6<PublishedPointsRecord, String, String, String, Integer, Integer, String> insertPoint = create.insertInto(publishedPoints).columns(
                    publishedPoints.xid,
                    publishedPoints.name,
                    publishedPoints.enabled,
                    publishedPoints.publisherId,
                    publishedPoints.dataPointId,
                    publishedPoints.data);
            try (insertPoint) {
                List<PublishedPointVO> points = (List<PublishedPointVO>) p.deletedProperties().get("points");
                for (PublishedPointVO point : points) {
                    // Optimize the access to data point name to ensure it exists as the blob can contain deleted points
                    // - use the data point name as the published point's name
                    // - ensure data point exists before inserting new point
                    // - this is sub-optimal as multiple requests for points that DNE will make DB calls but
                    // the assumption is this isn't going to happen a lot
                    String name = dataPointNameMap.computeIfAbsent(point.getDataPointId(), (k) -> {
                       return this.create.select(nameField)
                               .from(dataPointsTable)
                               .where(idField.eq(point.getDataPointId()))
                               .limit(1)
                               .fetchOneInto(String.class);
                    });
                    if(name != null) {
                        try {
                            insertPoint.values(
                                    PublishedPointVO.XID_PREFIX + UUID.randomUUID().toString(),
                                    name,
                                    BaseDao.boolToChar(true),
                                    p.getId(),
                                    point.getDataPointId(),
                                    definition.createPublishedPointDbData(point)
                            );
                        } catch (JsonProcessingException e) {
                            LOG.error("Failed to write published point JSON for data point with id {}, published point removed", point.getDataPointId(), e);
                        }
                    }
                }
                insertPoint.execute();
            }

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
