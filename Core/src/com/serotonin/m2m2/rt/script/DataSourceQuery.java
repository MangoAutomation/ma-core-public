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

import com.infiniteautomation.mango.db.query.BaseSqlQuery;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * 
 * Scripting Utility to query data points
 * 
 * @author Terry Packer
 *
 */
public class DataSourceQuery{
	
	public static final String CONTEXT_KEY = "DataSourceQuery";

	private ScriptPermissions permissions;
	private ScriptEngine engine;
	private PointValueSetter setter;
	private RQLParser parser;
	
	public DataSourceQuery(ScriptPermissions permissions, ScriptEngine engine, PointValueSetter setter){
		this.permissions = permissions;
		this.engine = engine;
		this.setter = setter;
		this.parser = new RQLParser();
	}
	
	public List<DataSourceWrapper> query(String query){
		ASTNode root = parser.parse(query);
		BaseSqlQuery<DataSourceVO<?>> sqlQuery = DataSourceDao.instance.createQuery(root, true);
		
		List<DataSourceVO<?>> dataSources = sqlQuery.immediateQuery();
		List<DataSourceWrapper> results = new ArrayList<DataSourceWrapper>();

		//Filter on permissions
		for(DataSourceVO<?> ds : dataSources){
			if(Permissions.hasDataSourcePermission(permissions.getDataSourcePermissions(), ds)){
				List<DataPointWrapper> points = getPointsForSource(ds);
				results.add(new DataSourceWrapper(ds, points));
			}
		}
		return results;
	}
	
    /**
     * Helper to extract points for a source
	 * @param ds
	 * @return
	 */
	private List<DataPointWrapper> getPointsForSource(DataSourceVO<?> ds) {
		List<DataPointWrapper> points = new ArrayList<DataPointWrapper>();
		
		List<DataPointVO> dataPoints = DataPointDao.instance.getDataPoints(ds.getId(), null, false);
		
		for(DataPointVO vo : dataPoints){
			DataPointRT rt = Common.runtimeManager.getDataPoint(vo.getId());
			AbstractPointWrapper wrapper = null;
			if(rt != null)
				wrapper = ScriptUtils.wrapPoint(engine, rt, setter);
			points.add(new DataPointWrapper(vo, wrapper));	
		}
		return points;
	}

	public String getHelp(){
    	return toString();
    }
    
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		builder.append("query(rql): List<DataSourceWrapper>()");
		builder.append(" }\n");
		return builder.toString();
	}
	
}
