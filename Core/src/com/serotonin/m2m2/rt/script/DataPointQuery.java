/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;

import com.infiniteautomation.mango.db.query.ConditionSortLimitWithTagKeys;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * 
 * Scripting Utility to query data points
 * 
 * @author Terry Packer
 *
 */
public class DataPointQuery{
	
	public static final String CONTEXT_KEY = "DataPointQuery";

	private ScriptPermissions permissions;
	private ScriptEngine engine;
	private ScriptPointValueSetter setter;
	private RQLParser parser;
	
	public DataPointQuery(ScriptPermissions permissions, ScriptEngine engine, ScriptPointValueSetter setter){
		this.permissions = permissions;
		this.engine = engine;
		this.setter = setter;
		this.parser = new RQLParser();
	}
	
	/**
	 * Perform a query on the data points table
	 * @param query
	 * @return
	 */
	public List<DataPointWrapper> query(String query){
		ASTNode root = parser.parse(query);
		List<DataPointVO> dataPoints = new ArrayList<>();
		ConditionSortLimitWithTagKeys conditions = DataPointDao.getInstance().rqlToCondition(root);
		DataPointDao.getInstance().customizedQuery(conditions, new MappedRowCallback<DataPointVO>() {
            @Override
            public void row(DataPointVO item, int index) {
                dataPoints.add(item);
            }
		});
		
		List<DataPointWrapper> results = new ArrayList<DataPointWrapper>();

		//Filter on permissions
		DataPointRT rt = null;
		AbstractPointWrapper wrapper = null;
		
		for(DataPointVO dp : dataPoints){
			
			//Can we read or write to this point?
			if(Permissions.hasDataPointSetPermission(permissions, dp) || Permissions.hasDataPointReadPermission(permissions, dp)){
				rt = Common.runtimeManager.getDataPoint(dp.getId());
				if(rt != null)
					wrapper = ScriptUtils.wrapPoint(engine, rt, setter);
				else
					wrapper = null;
				results.add(new DataPointWrapper(dp, wrapper));
			}
		}
		return results;
	}
	
	public DataPointWrapper byXid(String xid) {
	    DataPointVO dp = DataPointDao.getInstance().getByXid(xid);
	    if(dp == null)
	        return null;
	    
	    if(Permissions.hasDataPointSetPermission(permissions, dp) || Permissions.hasDataPointReadPermission(permissions, dp)) {
	        DataPointRT rt = null;
	        AbstractPointWrapper wrapper = null;
	        rt = Common.runtimeManager.getDataPoint(dp.getId());
	        if(rt != null)
	            wrapper = ScriptUtils.wrapPoint(engine, rt, setter);
	        else
	            wrapper = null;
	        return new DataPointWrapper(dp, wrapper);
	    } else
	        return null;
	    
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
