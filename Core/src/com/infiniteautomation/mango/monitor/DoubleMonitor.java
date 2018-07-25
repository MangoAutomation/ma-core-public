package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class DoubleMonitor extends ValueMonitor<Double> {

    /**
     * Create monitor with initial value of 0.0
     * @param id
     * @param name
     * @param owner
     */
    public DoubleMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner) {
        super(id, name, owner, 0D);
    }

    public DoubleMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, Double initialValue) {
        super(id, name, owner, initialValue);
    }

    public void addValue(double value) {
        this.value += value;
    }

    public void setValueIfGreater(double value) {
        if (this.value < value)
            this.value = value;
    }
}
