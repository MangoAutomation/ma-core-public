/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import java.util.List;

import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.test.data.RhinoScriptTestData;

/**
 * @author Terry Packer
 *
 */
public class RhinoScriptingTestPointValueRT implements IDataPointValueSource{

	private int id; //Index of data in Test Data Matrix
	private int dataTypeId; //Type of data this point will return
	
	public RhinoScriptingTestPointValueRT(int id, int dataTypeId){
		this.id = id;
		this.dataTypeId = dataTypeId;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.IDataPointValueSource#getLatestPointValues(int)
	 */
	@Override
	public List<PointValueTime> getLatestPointValues(int limit) {
		return RhinoScriptTestData.getLatestPointValues(dataTypeId, id, limit);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.IDataPointValueSource#updatePointValue(com.serotonin.m2m2.rt.dataImage.PointValueTime)
	 */
	@Override
	public void updatePointValue(PointValueTime newValue) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.IDataPointValueSource#updatePointValue(com.serotonin.m2m2.rt.dataImage.PointValueTime, boolean)
	 */
	@Override
	public void updatePointValue(PointValueTime newValue, boolean async) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.IDataPointValueSource#setPointValue(com.serotonin.m2m2.rt.dataImage.PointValueTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
	 */
	@Override
	public void setPointValue(PointValueTime newValue, SetPointSource source) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.IDataPointValueSource#getPointValue()
	 */
	@Override
	public PointValueTime getPointValue() {
		return RhinoScriptTestData.getLatestPointValue(dataTypeId, id);

	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.IDataPointValueSource#getPointValueBefore(long)
	 */
	@Override
	public PointValueTime getPointValueBefore(long time) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.IDataPointValueSource#getPointValueAfter(long)
	 */
	@Override
	public PointValueTime getPointValueAfter(long time) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.IDataPointValueSource#getPointValues(long)
	 */
	@Override
	public List<PointValueTime> getPointValues(long since) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.IDataPointValueSource#getPointValuesBetween(long, long)
	 */
	@Override
	public List<PointValueTime> getPointValuesBetween(long from, long to) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.IDataPointValueSource#getDataTypeId()
	 */
	@Override
	public int getDataTypeId() {
		return dataTypeId;
	}

	@Override
	public PointValueTime getPointValueAt(long time) {
		// TODO Auto-generated method stub
		return null;
	}

}
