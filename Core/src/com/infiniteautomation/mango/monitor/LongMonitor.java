package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class LongMonitor extends ValueMonitor<Long> {
    private volatile long value;

    public LongMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner) {
        this(id, name, owner, 0);
    }

    public LongMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, long initialValue) {
        super(id, name, owner);
        value = initialValue;
    }
    
    @Override
    public Long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public void addValue(long value) {
        this.value += value;
    }

    public void setValueIfGreater(long value) {
        if (this.value < value)
            this.value = value;
    }

    @Override
    public long longValue() {
        return value;
    }
}
