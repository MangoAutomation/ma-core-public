package com.serotonin.m2m2.db;

import java.io.File;
import java.text.DecimalFormat;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoMetrics;
import com.serotonin.m2m2.db.dao.logging.LoggingDao;
import com.serotonin.m2m2.db.dao.nosql.NoSQLDao;
import com.serotonin.m2m2.db.dao.nosql.NoSQLDataSerializer;
import com.serotonin.util.DirectoryUtils;

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
	 * Create a Dao for general NoSQL Storage
	 * 
	 * @return
	 */
	public abstract NoSQLDao createNoSQLDao(NoSQLDataSerializer serializer, String storeName);
	
	
	/**
	 * return this size of the database in bytes
	 * @return
	 */
	public long getDatabaseSizeInBytes(){
		return DirectoryUtils.getSize(new File(getDatabasePath())).getSize();
	}
	/**
	 * Create an instance of a LoggingDao
	 * 
	 * @return
	 */
	public abstract LoggingDao createLoggingDao();
	

}
