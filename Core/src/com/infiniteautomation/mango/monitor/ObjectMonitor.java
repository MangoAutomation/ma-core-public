package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class ObjectMonitor<T> extends ValueMonitor<T> {
    private T value;

    public ObjectMonitor(String id, TranslatableMessage name) {
        this(id, name, null);
    }

    public ObjectMonitor(String id, TranslatableMessage name, T initialValue) {
        super(id, name);
        this.value = initialValue;
    }

    @Override
    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public String stringValue() {
        if (value == null)
            return null;
        return value.toString();
    }
}
