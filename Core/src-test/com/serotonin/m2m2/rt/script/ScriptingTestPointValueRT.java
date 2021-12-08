/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.List;

import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.test.data.RhinoScriptTestData;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class ScriptingTestPointValueRT implements IDataPointValueSource{
	
    final DataPointVO vo;
    
	public ScriptingTestPointValueRT(DataPointVO vo){
	    this.vo = vo;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.IDataPointValueSource#getLatestPointValues(int)
	 */
	@Override
	public List<PointValueTime> getLatestPointValues(int limit) {
		return RhinoScriptTestData.getLatestPointValues(vo.getPointLocator().getDataType(), vo.getId(), limit);
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
		return RhinoScriptTestData.getLatestPointValue(vo.getPointLocator().getDataType(), vo.getId());

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
		return RhinoScriptTestData.getPointValuesBetween(vo.getPointLocator().getDataType(), vo.getId(), from, to);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.IDataPointValueSource#getDataTypeId()
	 */
	@Override
	public DataTypes getDataType() {
		return vo.getPointLocator().getDataType();
	}

	@Override
	public PointValueTime getPointValueAt(long time) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataPointWrapper getDataPointWrapper(AbstractPointWrapper wrapper) {
		return null;
	}

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.dataImage.IDataPointValueSource#getVO()
     */
    @Override
    public DataPointVO getVO() {
        return vo;
    }

}
