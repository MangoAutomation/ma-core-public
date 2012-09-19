package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;

public interface PointValueSetter {
    void set(IDataPointValueSource point, Object value, long timestamp);
}
