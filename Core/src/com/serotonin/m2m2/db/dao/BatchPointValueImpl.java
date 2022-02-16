/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import com.serotonin.m2m2.vo.DataPointVO;

public class BatchPointValueImpl<T> implements BatchPointValue<T> {
    private final DataPointVO vo;
    private final T pointValue;

    public BatchPointValueImpl(DataPointVO vo, T pointValue) {
        this.vo = vo;
        this.pointValue = pointValue;
    }

    @Override
    public DataPointVO getPoint() {
        return vo;
    }

    @Override
    public T getValue() {
        return pointValue;
    }
}
