/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DaoRegistry;
import com.serotonin.m2m2.rt.script.ScriptContextVariable;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class ScriptContextVariableModel {

	private String dataPointXid;
	private String variableName;
	private boolean contextUpdate;
	
	/**
	 * @param dataPointId
	 * @param variableName
	 * @param contextUpdate
	 */
	public ScriptContextVariableModel(String dataPointXid, String variableName,
			boolean contextUpdate) {
		super();
		this.dataPointXid = dataPointXid;
		this.variableName = variableName;
		this.contextUpdate = contextUpdate;
	}

	public ScriptContextVariableModel(){
		
	}
	
	/**
	 * Create a model from a variable
	 * @param variable
	 */
	public ScriptContextVariableModel(ScriptContextVariable variable){
		DataPointVO vo = DaoRegistry.dataPointDao.get(variable.getDataPointId());
		if(vo != null)
			this.dataPointXid = vo.getXid();
		this.variableName = variable.getVariableName();
		this.contextUpdate = variable.isContextUpdate();
	}
	
	public String getDataPointXid() {
		return dataPointXid;
	}

	public void setDataPointXid(String dataPointXid) {
		this.dataPointXid = dataPointXid;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public boolean isContextUpdate() {
		return contextUpdate;
	}

	public void setContextUpdate(boolean contextUpdate) {
		this.contextUpdate = contextUpdate;
	}

	public ScriptContextVariable createVariable(){
		int dataPointId = Common.NEW_ID;
		if(this.dataPointXid != null){
			DataPointVO vo = DaoRegistry.dataPointDao.getByXid(this.dataPointXid);
			if(vo != null)
				dataPointId = vo.getId();
		}
		return new ScriptContextVariable(dataPointId, variableName, contextUpdate);
	}
}
