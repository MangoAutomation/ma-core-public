package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class LongMonitor extends ValueMonitor<Long> {
    private long value;

    public LongMonitor(String id, TranslatableMessage name) {
        this(id, name, 0);
    }

    public LongMonitor(String id, TranslatableMessage name, long initialValue) {
        super(id, name);
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
