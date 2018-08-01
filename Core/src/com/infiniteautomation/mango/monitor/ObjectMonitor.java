package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class ObjectMonitor<T> extends ValueMonitor<T> {
    private volatile T value;

    public ObjectMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner) {
        super(id, name, owner);
    }

    public ObjectMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, T initialValue) {
        super(id, name, owner, initialValue);
    }

    public String stringValue() {
        if (value == null)
            return null;
        return value.toString();
    }
}
