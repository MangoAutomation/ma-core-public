/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.settings;

import java.util.List;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.SystemInfoDefinition;
import com.serotonin.m2m2.vo.bean.PointHistoryCount;

/**
 * 
 * @author Terry Packer
 */
public class PointHistoryCountInfoDefinition extends SystemInfoDefinition<List<PointHistoryCount>>{

	public final String KEY = "pointHistoryCounts";
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.SystemInfoDefinition#getKey()
	 */
	@Override
	public String getKey() {
		return KEY;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.SystemInfoDefinition#getValue()
	 */
	@Override
	public List<PointHistoryCount> getValue() {
		return DataPointDao.instance.getTopPointHistoryCounts();
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ModuleElementDefinition#getModule()
	 */
	@Override
	public Module getModule() {
		return ModuleRegistry.getCoreModule();
	}

}
