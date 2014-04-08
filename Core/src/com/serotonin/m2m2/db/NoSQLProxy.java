package com.serotonin.m2m2.db;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoMetrics;

abstract public class NoSQLProxy {
    public abstract void initialize();
    public abstract void shutdown();

    public abstract PointValueDao createPointValueDao();
    
    public PointValueDao createPointValueDaoMetrics(){
    	return new PointValueDaoMetrics(this.createPointValueDao());
    }
    
	/**
	 * Helper to get the database directory
	 * 
	 * @return Absolute path to databases directory ending in a slash
	 */
	public static String getDatabasePath() {
		return Common.MA_HOME+ "/databases/";
	}

}
