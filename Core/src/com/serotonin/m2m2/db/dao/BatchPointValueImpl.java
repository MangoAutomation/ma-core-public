/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.DataPointVO;

public class BatchPointValueImpl implements BatchPointValue {
    private final DataPointVO vo;
    private final PointValueTime pointValue;
    private final @Nullable SetPointSource source;

    public BatchPointValueImpl(DataPointVO vo, PointValueTime pointValue) {
        this(vo, pointValue, null);
    }

    public BatchPointValueImpl(DataPointVO vo, PointValueTime pointValue, @Nullable SetPointSource source) {
        this.vo = vo;
        this.pointValue = pointValue;
        this.source = source;
    }

    @Override
    public DataPointVO getVo() {
        return vo;
    }

    @Override
    public PointValueTime getPointValue() {
        return pointValue;
    }

    @Override
    public @Nullable SetPointSource getSource() {
        return source;
    }
}
