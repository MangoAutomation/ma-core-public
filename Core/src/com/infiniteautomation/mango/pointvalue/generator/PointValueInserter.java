/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue.generator;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.serotonin.m2m2.db.dao.BatchPointValue;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class PointValueInserter implements Consumer<DataPointVO> {
    private final Function<DataPointVO, Stream<BatchPointValue<PointValueTime>>> generator;
    private final int chunkSize;
    private final PointValueDao pointValueDao;

    public PointValueInserter(Function<DataPointVO, Stream<BatchPointValue<PointValueTime>>> generator, int chunkSize, PointValueDao pointValueDao) {
        this.generator = generator;
        this.chunkSize = chunkSize;
        this.pointValueDao = pointValueDao;
    }

    @Override
    public void accept(DataPointVO point) {
        pointValueDao.savePointValues(generator.apply(point), chunkSize);
    }
}
