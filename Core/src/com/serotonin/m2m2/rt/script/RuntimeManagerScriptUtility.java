/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * @author Terry Packer
 *
 */
public class RuntimeManagerScriptUtility{
	
	public static final String CONTEXT_KEY = "RuntimeManager";
	
	private static final Log LOG = LogFactory.getLog(RuntimeManagerScriptUtility.class);
	
	private static final int DOES_NOT_EXIST = -1;  //Point or Data Soure Does not exist
	private static final int OPERATION_NO_CHANGE = 0; //Operation didn't have any effect, it was already in that state
	private static final int OPERATION_SUCCESSFUL = 1; //Operation worked
	
	private ScriptPermissions permissions;
	public RuntimeManagerScriptUtility(ScriptPermissions permissions){
		this.permissions = permissions;
	}
	
	/**
	 * Refresh a data point with the given XID.
	 * 
	 * @param xid
	 * @return status of operation
	 * 0 - Point not enabled
	 * -1 - Point does not exist
	 * 1 - Refresh performed 
	 */
	public int refreshDataPoint(String xid){
		
		DataPointVO vo = DataPointDao.instance.getByXid(xid);
		
		if(vo != null){
			
			if(!vo.isEnabled())
				return OPERATION_NO_CHANGE;
			
			DataSourceRT dsRt = Common.runtimeManager.getRunningDataSource(vo.getDataSourceId());
			if(dsRt == null)
				return OPERATION_NO_CHANGE;
			
			Common.runtimeManager.forcePointRead(vo.getId());
			return OPERATION_SUCCESSFUL;
			
		}else
			return DOES_NOT_EXIST;
		
	}
	
	/**
	 * Is a data source enabled?
	 * @param xid
	 * @return true if it is, false if it is not
	 */
	public boolean isDataSourceEnabled(String xid){
		
		DataSourceVO<?> vo = DataSourceDao.instance.getByXid(xid);
		
		if(vo == null)
			return false;
		else{
			//This will throw an exception if there is no permission
			if(Permissions.hasDataSourcePermission(permissions.getDataSourcePermissions(), vo))
				return vo.isEnabled();
			else
				return false;
		}

	}
	
	/**
	 * Start a Data Source via its XID
	 * @param xid
	 * @return -1 if DS DNE, 0 if it was already enabled, 1 if it was sent to RuntimeManager
	 */
	public int enableDataSource(String xid){
		DataSourceVO<?> vo = DataSourceDao.instance.getByXid(xid);
		if(vo == null)
			return DOES_NOT_EXIST;
		else if(!vo.isEnabled()){
			if(Permissions.hasDataSourcePermission(permissions.getDataSourcePermissions(), vo)){
				vo.setEnabled(true);
				try{
					Common.runtimeManager.saveDataSource(vo);
				}catch(Exception e){
					LOG.error(e.getMessage(), e);
					throw e;
				}
				return OPERATION_SUCCESSFUL;
			}else
				return DOES_NOT_EXIST; //No permissions
		}else
			return OPERATION_NO_CHANGE;
	}

	/**
	 * Stop a Data Source via its XID
	 * @param xid
	 * @return -1 if DS DNE, 0 if it was already disabled, 1 if it was sent to RuntimeManager
	 */
	public int disableDataSource(String xid){
		DataSourceVO<?> vo = DataSourceDao.instance.getByXid(xid);
		if(vo == null)
			return DOES_NOT_EXIST;
		else if(vo.isEnabled()){
			if(Permissions.hasDataSourcePermission(permissions.getDataSourcePermissions(), vo)){
				vo.setEnabled(false);
				try{
					Common.runtimeManager.saveDataSource(vo);
				}catch(Exception e){
					LOG.error(e.getMessage(), e);
					throw e;
				}
				return OPERATION_SUCCESSFUL;
			}else
				return DOES_NOT_EXIST; //No permissions
		}else
			return OPERATION_NO_CHANGE;
	}
	
	/**
	 * Is a data point enabled?
	 * 
	 * A point is enabled if both the data source and point are enabled.
	 * 
	 * @param xid
	 * @return true if it is, false if it is not
	 */
	public boolean isDataPointEnabled(String xid){
		DataPointVO vo = DataPointDao.instance.getByXid(xid);
		if(vo == null)
			return false;
		else{
			if(Permissions.hasDataPointSetPermission(permissions.getDataPointSetPermissions(), vo) || Permissions.hasDataPointReadPermission(permissions.getDataPointReadPermissions(), vo)){
				DataSourceVO<?> ds = DataSourceDao.instance.get(vo.getDataSourceId());
				if(ds == null)
					return false;
				return (ds.isEnabled() && vo.isEnabled());
			}else
				return false;
		}
	}
	
	/**
	 * Start a Data Point via its XID
	 * @param xid
	 * @return -1 if DS DNE, 0 if it was already enabled, 1 if it was sent to RuntimeManager
	 */
	public int enableDataPoint(String xid){
		DataPointVO vo = DataPointDao.instance.getByXid(xid);
		if(vo == null)
			return DOES_NOT_EXIST;
		else if(!vo.isEnabled()){
			if(Permissions.hasDataPointSetPermission(permissions.getDataPointSetPermissions(), vo)){
				DataPointDao.instance.setEventDetectors(vo);
				vo.setEnabled(true);
				try{
					Common.runtimeManager.saveDataPoint(vo);
				}catch(Exception e){
					LOG.error(e.getMessage(), e);
					throw e;
				}
				return OPERATION_SUCCESSFUL;
			}else
				return DOES_NOT_EXIST; //No permissions
		}else
			return OPERATION_NO_CHANGE;
	}

	/**
	 * Stop a Data Point via its XID
	 * @param xid
	 * @return -1 if DS DNE, 0 if it was already disabled, 1 if it was sent to RuntimeManager
	 */
	public int disableDataPoint(String xid){
		DataPointVO vo = DataPointDao.instance.getByXid(xid);
		if(vo == null)
			return DOES_NOT_EXIST;
		else if(vo.isEnabled()){
			if(Permissions.hasDataPointSetPermission(permissions.getDataPointSetPermissions(), vo)){
				DataPointDao.instance.setEventDetectors(vo);
				vo.setEnabled(false);
				try{
					Common.runtimeManager.saveDataPoint(vo);
				}catch(Exception e){
					LOG.error(e.getMessage(), e);
					throw e;
				}
				return OPERATION_SUCCESSFUL;
			}else
				return DOES_NOT_EXIST; //No permissions
		}else
			return OPERATION_NO_CHANGE;
	}
	
	public String getHelp(){
		return toString();
	}
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		builder.append("dataSourceEnabled(xid): boolean, \n");
		builder.append("enableDataSource(xid): -1 0 1, \n");
		builder.append("disableDataSource(xid): -1 0 1, \n");
		builder.append("dataPointEnabled(xid): boolean, \n");
		builder.append("enableDataPoint(xid): -1 0 1, \n");
		builder.append("disableDataPoint(xid): -1 0 1, \n");
		builder.append(" }");
		return builder.toString();
	}
}
