package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class IntegerMonitor extends ValueMonitor<Integer> {
    protected int value;

    public IntegerMonitor(String id, TranslatableMessage name) {
        this(id, name, 0);
    }

    public IntegerMonitor(String id, TranslatableMessage name, int initialValue) {
        super(id, name);
        value = initialValue;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void addValue(int value) {
        this.value += value;
    }

    public void setValueIfGreater(int value) {
        if (this.value < value)
            this.value = value;
    }

    @Override
    public int intValue() {
        return value;
    }
}
