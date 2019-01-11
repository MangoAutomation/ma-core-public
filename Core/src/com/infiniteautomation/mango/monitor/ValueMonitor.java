package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
abstract public class ValueMonitor<T> {
    
    private final String id;
    private final TranslatableMessage name;
    protected final ValueMonitorOwner owner;
    protected volatile T value;
    protected boolean uploadUsageToStore;
    
    public ValueMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner) {
        this(id, name, owner, null);
    }
    
    public ValueMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, boolean uploadUsageToStore) {
        this(id, name, owner, null, uploadUsageToStore);
    }
    
    public ValueMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, T value) {
        this(id, name, owner, value, false);
    }
    
    public ValueMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, T value, boolean uploadUsageToStore) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.value = value;
        this.uploadUsageToStore = uploadUsageToStore;
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
    
    public void setValue(T value) {
        this.value = value;
    }
    
    public T getValue() {
        return value;
    }
    
    /**
     * Should this monitor relay data to the Mango Store?
     * @return
     */
    public boolean uploadUsageToStore() {
        return uploadUsageToStore;
    }

 }
