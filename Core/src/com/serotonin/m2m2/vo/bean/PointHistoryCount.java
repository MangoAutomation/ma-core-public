/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.bean;

import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Matthew Lohbihler
 */
public class PointHistoryCount {
    private final DataPointVO point;
    private final int count;

    public PointHistoryCount(DataPointVO point, int count) {
        this.point = point;
        this.count = count;
    }

    public String getPointXid() {
        return point.getXid();
    }

    public int getPointId() {
        return point.getId();
    }

    public String getPointName() {
        return point.getExtendedName();
    }

    public int getCount() {
        return count;
    }
}
