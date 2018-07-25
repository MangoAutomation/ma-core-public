package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class FloatMonitor extends ValueMonitor<Float> {

    /**
     * Create monitor with initial value of 0
     * @param id
     * @param name
     * @param owner
     */
    public FloatMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner) {
        super(id, name, owner, 0F);
    }

    public FloatMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, Float initialValue) {
        super(id, name, owner, initialValue);
    }

    public void addValue(Float value) {
        this.value += value;
    }

    public void setValueIfGreater(Float value) {
        if (this.value < value)
            this.value = value;
    }

}
