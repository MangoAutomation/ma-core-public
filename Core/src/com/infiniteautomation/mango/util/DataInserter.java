/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.serotonin.m2m2.db.dao.BatchPointValue;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.vo.DataPointVO;

public class DataInserter implements Consumer<DataPointVO> {
    private final Function<DataPointVO, Stream<BatchPointValue>> generator;
    private final int chunkSize;
    private final PointValueDao pointValueDao;

    public DataInserter(Function<DataPointVO, Stream<BatchPointValue>> generator, int chunkSize, PointValueDao pointValueDao) {
        this.generator = generator;
        this.chunkSize = chunkSize;
        this.pointValueDao = pointValueDao;
    }

    @Override
    public void accept(DataPointVO point) {
        pointValueDao.savePointValues(generator.apply(point), chunkSize);
    }
}
