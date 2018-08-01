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
public class BinaryPointWrapper extends DistinctPointWrapper {
    public BinaryPointWrapper(IDataPointValueSource point, ScriptEngine engine, ScriptPointValueSetter setter) {
        super(point, engine, setter);
    }

    public Boolean getValue() {
        DataValue value = getValueImpl();
        if (value == null)
            return null;
        return value.getBooleanValue();
    }

    public Boolean ago(int periodType) {
        return ago(periodType, 1);
    }

    public Boolean ago(int periodType, int count) {
        long from = DateUtils.minus(getContext().getRuntime(), periodType, count);
        PointValueTime pvt = point.getPointValueBefore(from);
        if (pvt == null)
            return null;
        return pvt.getBooleanValue();
    }

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.script.AbstractPointWrapper#helpImpl(java.lang.StringBuilder)
	 */
	@Override
	protected void helpImpl(StringBuilder builder) {
		builder.append("ago(periodType): Boolean,\n ");		
    	builder.append("ago(periodType, periods): Boolean,\n ");
    	super.helpImpl(builder);
	}
}
