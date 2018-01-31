package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class ObjectMonitor<T> extends ValueMonitor<T> {
    private volatile T value;

    public ObjectMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner) {
        this(id, name, owner, null);
    }

    public ObjectMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, T initialValue) {
        super(id, name, owner);
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
