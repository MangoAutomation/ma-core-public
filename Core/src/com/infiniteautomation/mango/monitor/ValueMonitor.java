package com.infiniteautomation.mango.monitor;

import com.serotonin.NotImplementedException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
abstract public class ValueMonitor<T> {
    private final String id;
    private final TranslatableMessage name;
    protected final ValueMonitorOwner owner;

    public ValueMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
    }

    public String getId() {
        return id;
    }

    public TranslatableMessage getName() {
        return name;
    }

    /**
     * Reset the value from its external source.
     * Useful for counters that can get out of sync with their external source.
     */
    public void reset(){
    	this.owner.reset(this.id);
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
