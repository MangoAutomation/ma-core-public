/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import com.serotonin.m2m2.vo.DataPointVO;

public interface BatchPointValue<T> {
    DataPointVO getPoint();
    T getValue();
}
