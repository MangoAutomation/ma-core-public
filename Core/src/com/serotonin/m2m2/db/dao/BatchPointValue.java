/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public interface BatchPointValue<T extends IValueTime<?>> {
    DataPointVO getPoint();
    T getValue();
}
