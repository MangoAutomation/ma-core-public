/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.util;

import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Matthew Lohbihler
 */
public class BackgroundContext {
    /**
     * The ThreadLocal instance that will contain the various BackgroundContext objects.
     */
    private static ThreadLocal<BackgroundContext> contextStore = new ThreadLocal<BackgroundContext>();

    /**
     * Creates the BackgroundContext instance for this thread and adds it to the store.
     */
    public static void set(PermissionHolder user) {
        contextStore.set(new BackgroundContext(user));
    }

    public static void set(String processDescriptionKey) {
        contextStore.set(new BackgroundContext(processDescriptionKey));
    }
    
    public static void set(BackgroundContext context) {
        contextStore.set(context);
    }

    /**
     * Used within user code to access the context objects.
     * 
     * @return the BackgroundContext object found for this thread.
     */
    public static BackgroundContext get() {
        return contextStore.get();
    }

    /**
     * Removes the BackgroundContext object from this thread once we are done with it.
     */
    public static void remove() {
        contextStore.remove();
    }

    private final PermissionHolder user;
    private final String processDescriptionKey;

    /**
     * Constructor
     */
    private BackgroundContext(PermissionHolder user) {
        this.user = user;
        this.processDescriptionKey = null;
    }

    private BackgroundContext(String processDescriptionKey) {
        this.user = null;
        this.processDescriptionKey = processDescriptionKey;
    }

    public User getUser() {
        return (User)user;
    }
    
    public PermissionHolder getPermissionHolder() {
        return user;
    }
    
    public String getProcessDescriptionKey() {
        return processDescriptionKey;
    }
}
