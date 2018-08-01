package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class LongMonitor extends ValueMonitor<Long> {

    /**
     * Create monitor with initial value of 0
     * @param id
     * @param name
     * @param owner
     */
    public LongMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner) {
        super(id, name, owner, 0L);
    }

    public LongMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, Long initialValue) {
        super(id, name, owner, initialValue);
        value = initialValue;
    }

    public void addValue(long value) {
        this.value += value;
    }

    public void setValueIfGreater(long value) {
        if (this.value < value)
            this.value = value;
    }
}
