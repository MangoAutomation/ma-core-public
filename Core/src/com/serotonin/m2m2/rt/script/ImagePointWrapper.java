package com.serotonin.m2m2.rt.script;

import javax.script.ScriptEngine;

import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.ImageValue;
import com.serotonin.m2m2.util.DateUtils;

public class ImagePointWrapper extends AbstractPointWrapper {

    ImagePointWrapper(IDataPointValueSource point, ScriptEngine engine,
            ScriptPointValueSetter setter) {
        super(point, engine, setter);
    }
    
    public byte[] getValue() {
        PointValueTime value = point.getPointValue();
        if(value == null)
            return null;
        return ((ImageValue)value.getValue()).getData();
    }
    
    public byte[] ago(int periodType) {
        return ago(periodType, 1);
    }
    
    public byte[] ago(int periodType, int periods) {
        long from = DateUtils.minus(getContext().getRuntime(), periodType, periods);
        PointValueTime pvt = point.getPointValueBefore(from);
        if (pvt == null)
            return null;
        return ((ImageValue)pvt.getValue()).getData();
    }

    @Override
    protected void helpImpl(StringBuilder builder) {
        builder.append("ago(periodType): byte[],\n ");      
        builder.append("ago(periodType, periods): byte[],\n ");
    }

}
