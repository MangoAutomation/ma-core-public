/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module;

/**
 * @author Jared Wiltshire
 */
public interface DwrClassHolder {

    /**
     * The class of a DWR proxy. Called once upon startup, the class is then registered in the DWR controller. May be
     * null.
     *
     * @return the DWR proxy
     */
    Class<?> getDwrClass();
    
    /**
     * The module of the DWR used when creating the DWR.  Cannot be null.
     * @return
     */
    Module getModule();

}
