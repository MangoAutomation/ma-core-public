/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;

import com.infiniteautomation.mango.db.query.BaseSqlQuery;
import com.infiniteautomation.mango.db.query.QueryModel;
import com.infiniteautomation.mango.db.query.RqlQueryParser;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * 
 * Scripting Utility to query data points
 * 
 * @author Terry Packer
 *
 */
public class DataPointQuery extends RqlQueryParser{
	
	public static final String CONTEXT_KEY = "DataPointQuery";

	private ScriptPermissions permissions;
	private ScriptEngine engine;
	private PointValueSetter setter;
	
	public DataPointQuery(ScriptPermissions permissions, ScriptEngine engine, PointValueSetter setter){
		this.permissions = permissions;
		this.engine = engine;
		this.setter = setter;
	}
	
	/**
	 * Perform a query on the data points table
	 * @param query
	 * @return
	 */
	public List<DataPointWrapper> query(String query){
		QueryModel model = this.parseRQL(query);
		
		BaseSqlQuery<DataPointVO> sqlQuery = DataPointDao.instance.createQuery(model.getOrComparisons(),
				model.getAndComparisons(), model.getSort(), model.getOffset(), model.getLimit());
		
		List<DataPointVO> dataPoints = sqlQuery.immediateQuery();
		
		List<DataPointWrapper> results = new ArrayList<DataPointWrapper>();
		//Filter on permissions
		for(DataPointVO dp : dataPoints){
			try{
				if(Permissions.hasDataPointReadPermission(permissions.getDataPointReadPermissions(), dp)){
					DataPointRT rt = null;
					AbstractPointWrapper wrapper = null;
					try{
						if(Permissions.hasDataPointSetPermission(permissions.getDataPointSetPermissions(), dp)){
							rt = Common.runtimeManager.getDataPoint(dp.getId());
							if(rt != null)
								wrapper = ScriptUtils.wrapPoint(engine, rt, setter);
						}
					}catch(PermissionException e){
						//No Write but read
						rt = Common.runtimeManager.getDataPoint(dp.getId());
						if(rt != null)
							wrapper = ScriptUtils.wrapPoint(engine, rt);
					}
					results.add(new DataPointWrapper(dp, wrapper));
				}
			}catch(PermissionException e){ }
				
				
		}
		return results;
	}
	
    public String getHelp(){
    	return toString();
    }
    
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		builder.append("query(rql): List<DataPointWrapper>()");
		builder.append(" }\n");
		return builder.toString();
	}
	
}
