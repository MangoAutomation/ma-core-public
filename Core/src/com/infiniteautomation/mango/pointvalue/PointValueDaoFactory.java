/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue;

import com.serotonin.m2m2.db.dao.PointValueDao;

@FunctionalInterface
public interface PointValueDaoFactory {
    PointValueDao getPointValueDao();
}
