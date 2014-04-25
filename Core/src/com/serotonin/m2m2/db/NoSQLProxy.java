package com.serotonin.m2m2.db;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoMetrics;
import com.serotonin.m2m2.db.dao.nosql.NoSQLDao;
import com.serotonin.m2m2.db.dao.nosql.NoSQLDataSerializer;

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
	/**
	 * @return
	 */
	public abstract NoSQLDao createNoSQLDao(NoSQLDataSerializer serializer, String storeName);

}
