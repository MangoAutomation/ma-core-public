package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class IntegerMonitor extends ValueMonitor<Integer> {

    /**
     * Create monitor with initial value of 0
     * @param id
     * @param name
     * @param owner
     */
    public IntegerMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner) {
        super(id, name, owner, 0);
    }
    
    public IntegerMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, boolean uploadUsageToStore) {
        super(id, name, owner, 0, uploadUsageToStore);
    }

    public IntegerMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, Integer initialValue) {
        super(id, name, owner, initialValue);
    }

    public void addValue(Integer value) {
        this.value += value;
    }

    public void setValueIfGreater(Integer value) {
        if (this.value < value)
            this.value = value;
    }
}
