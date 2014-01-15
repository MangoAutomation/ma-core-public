package com.serotonin.m2m2.db;

import com.serotonin.m2m2.db.dao.PointValueDao;

abstract public class NoSQLProxy {
    public abstract void initialize();

    public abstract PointValueDao createPointValueDao();

}
