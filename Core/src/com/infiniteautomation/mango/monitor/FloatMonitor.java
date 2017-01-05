package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class FloatMonitor extends ValueMonitor<Float> {
    private float value;

    public FloatMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner) {
        this(id, name, owner, 0);
    }

    public FloatMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, float initialValue) {
        super(id, name, owner);
        value = initialValue;
    }

    @Override
    public Float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public void addValue(float value) {
        this.value += value;
    }

    public void setValueIfGreater(float value) {
        if (this.value < value)
            this.value = value;
    }

    @Override
    public float floatValue() {
        return value;
    }
}
