/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue.generator;

import java.util.function.Function;
import java.util.stream.Stream;

import com.serotonin.m2m2.db.dao.BatchPointValue;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.vo.DataPointVO;

public interface PointValueGenerator extends Function<DataPointVO, Stream<BatchPointValue>> {

    default PointValueInserter createInserter(PointValueDao pointValueDao, int chunkSize) {
        return new PointValueInserter(this, chunkSize, pointValueDao);
    }

}
