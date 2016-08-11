package com.infiniteautomation.mango.monitor;

import com.serotonin.NotImplementedException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
abstract public class ValueMonitor<T> {
    private final String id;
    private final TranslatableMessage name;

    public ValueMonitor(String id, TranslatableMessage name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public TranslatableMessage getName() {
        return name;
    }

    abstract public T getValue();

    public int intValue() {
        throw new NotImplementedException();
    }

    public long longValue() {
        throw new NotImplementedException();
    }

    public float floatValue() {
        throw new NotImplementedException();
    }

    public double doubleValue() {
        throw new NotImplementedException();
    }

    public String stringValue() {
        throw new NotImplementedException();
    }
}
