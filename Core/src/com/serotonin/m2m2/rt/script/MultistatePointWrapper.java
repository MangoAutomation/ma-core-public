/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
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
    public MultistatePointWrapper(IDataPointValueSource point, ScriptEngine engine, PointValueSetter setter) {
        super(point, engine, setter);
    }

    public int getValue() {
        DataValue value = getValueImpl();
        if (value == null)
            return 0;
        return value.getIntegerValue();
    }

    public int ago(int periodType) {
        return ago(periodType, 1);
    }

    public int ago(int periodType, int count) {
        long from = DateUtils.minus(getContext().getRuntime(), periodType, count);
        PointValueTime pvt = point.getPointValueBefore(from);
        if (pvt == null)
            return 0;
        return pvt.getIntegerValue();
    }
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.script.AbstractPointWrapper#helpImpl(java.lang.StringBuilder)
	 */
	@Override
	protected void helpImpl(StringBuilder builder) {
		builder.append("ago(periodType): int,\n ");		
    	builder.append("ago(periodType, periods): int,\n ");
    	super.helpImpl(builder);
	}
}
