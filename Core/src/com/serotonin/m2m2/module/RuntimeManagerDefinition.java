/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

/**
 * This definition is notified upon initialization and termination of the application.
 * 
 * @author Matthew Lohbihler
 */
abstract public class RuntimeManagerDefinition extends ModuleElementDefinition {
    /**
     * Determines where in the system initialization this class's initialize method will be called. Values less than or
     * equal to 4 will run before the data sources start. Values grater than or equal to 5 will run after the data
     * sources start. At termination the priorities are reversed.
     * 
     * @return this class's initialization priority
     */
    abstract public int getInitializationPriority();

    /**
     * Run code that initializes things within the module.
     * 
     * @param safe
     *            true if the system is starting in "safe mode".
     */
    abstract public void initialize(boolean safe);

    /**
     * Run code that terminates things within the module/
     */
    abstract public void terminate();
}
