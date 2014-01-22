package com.serotonin.m2m2.db;

import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoMetrics;
import com.serotonin.m2m2.db.dao.PointValueDaoMetrics.TimeScale;

abstract public class NoSQLProxy {
    public abstract void initialize();
    public abstract void shutdown();

    public abstract PointValueDao createPointValueDao();
    
    public PointValueDao createPointValueDaoMetrics(TimeScale scale){
    	return new PointValueDaoMetrics(this.createPointValueDao(),scale);
    }

}
