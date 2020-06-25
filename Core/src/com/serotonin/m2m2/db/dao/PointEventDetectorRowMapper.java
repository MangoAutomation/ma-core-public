/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.sql.Clob;

import com.fasterxml.jackson.databind.JsonNode;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.PointEventDetectorDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

/**
 * When returning data point detectors this will fill the VO with the data point
 *
 * @author Terry Packer
 *
 */
public class PointEventDetectorRowMapper extends EventDetectorRowMapper<AbstractPointEventDetectorVO> {

    private final DataPointVO dp;

    public PointEventDetectorRowMapper(DataPointVO dp, ExtractJson<Clob, JsonNode> extractJson, EventDetectorDao dao) {
        super(extractJson, dao);
        this.dp = dp;
    }

    public PointEventDetectorRowMapper(int firstColumn, ExtractJson<Clob, JsonNode> extractJson, DataPointVO dp, EventDetectorDao dao){
        super(firstColumn, extractJson, dao);
        this.dp = dp;
    }

    @Override
    protected AbstractPointEventDetectorVO createEventDetector(int sourceId, EventDetectorDefinition<?> definition) {
        PointEventDetectorDefinition<?> pedDef = (PointEventDetectorDefinition<?>)definition;
        return pedDef.baseCreateEventDetectorVO(dp);
    }

}
