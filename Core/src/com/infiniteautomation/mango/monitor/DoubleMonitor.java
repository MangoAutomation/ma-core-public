package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class DoubleMonitor extends ValueMonitor<Double> {
    private double value;

    public DoubleMonitor(String id, TranslatableMessage name) {
        this(id, name, 0);
    }

    public DoubleMonitor(String id, TranslatableMessage name, double initialValue) {
        super(id, name);
        value = initialValue;
    }

    @Override
    public Double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void addValue(double value) {
        this.value += value;
    }

    public void setValueIfGreater(double value) {
        if (this.value < value)
            this.value = value;
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
