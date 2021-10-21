/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.DataPointVO;

public interface BatchPointValue {
    DataPointVO getVo();
    PointValueTime getPointValue();
    @Nullable SetPointSource getSource();
}
