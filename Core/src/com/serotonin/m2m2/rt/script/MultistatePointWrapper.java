/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import javax.script.ScriptEngine;

import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.util.DateUtils;

/**
 * @author Matthew Lohbihler
 */
public class MultistatePointWrapper extends DistinctPointWrapper {
    public MultistatePointWrapper(IDataPointValueSource point, ScriptEngine engine, ScriptPointValueSetter setter) {
        super(point, engine, setter);
    }

    public Integer getValue() { //always use cache here, last() for no cache
        DataValue value = getValueImpl();
        if (value == null)
            return null;
        return value.getIntegerValue();
    }

    public Integer ago(int periodType) {
        return ago(periodType, 1, false);
    }
    
    public Integer ago(int periodType, boolean cache) {
        return ago(periodType, 1, cache);
    }

    public Integer ago(int periodType, int count) {
        return ago(periodType, count, false);
    }
    
    public Integer ago(int periodType, int count, boolean cache) {
        long from = DateUtils.minus(getContext().getComputeTime(), periodType, count);
        PointValueTime pvt;
        if(cache)
            pvt = point.getPointValueBefore(from);
        else
            pvt = valueFacade.getPointValueBefore(from);
        
        if (pvt == null)
            return null;
        return pvt.getIntegerValue();
    }
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.script.AbstractPointWrapper#helpImpl(java.lang.StringBuilder)
	 */
	@Override
	protected void helpImpl(StringBuilder builder) {
		builder.append("ago(periodType, cache): int,\n ");		
    	builder.append("ago(periodType, periods, cache): int,\n ");
    	super.helpImpl(builder);
	}
}
