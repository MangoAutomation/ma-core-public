/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class BatchPointValueImpl implements BatchPointValue {
    private final DataPointVO vo;
    private final PointValueTime pointValue;

    public BatchPointValueImpl(DataPointVO vo, PointValueTime pointValue) {
        this.vo = vo;
        this.pointValue = pointValue;
    }

    @Override
    public DataPointVO getVo() {
        return vo;
    }

    @Override
    public PointValueTime getPointValue() {
        return pointValue;
    }
}
