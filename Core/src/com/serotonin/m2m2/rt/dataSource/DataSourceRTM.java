/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.dataSource;

import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.rt.AbstractRTM;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

/**
 * This class is dead and can be removed when we remove DWR
 * 
 * @author Terry Packer
 *
 */
@Deprecated
public class DataSourceRTM<T extends DataSourceVO<?>> extends AbstractRTM<T,DataSourceRT<T>,DataSourceDao<T>>{

	public static DataSourceRTM<?> instance = new DataSourceRTM<>();
	
	/**
	 * @param initializationPriority
	 */
	public DataSourceRTM() {
		super(0);
		instance = this;
		LOG = LogFactory.getLog(DataSourceRTM.class);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.AbstractRTM#getRt(com.serotonin.m2m2.vo.AbstractActionVO)
	 */
	@Override
	public DataSourceRT<T> getRt(T vo) {
		return (DataSourceRT<T>) vo.createDataSourceRT();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.AbstractRTM#getDao()
	 */
	@Override
	public DataSourceDao<T> getDao() {
		return (DataSourceDao<T>) DataSourceDao.getInstance();
	}

	@Override 
	public void save(T vo){
		Common.runtimeManager.saveDataSource(vo);
	}
	
	@Override
	public void delete(int dataSourceId){
		Common.runtimeManager.deleteDataSource(dataSourceId);
	}
	
}
