/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import java.util.List;

/**
 * The class allows a module to declare a SQL statement (with no parameters) that provides a list of file data IDs (i.e.
 * images) that are still referenced, and therefore should not be removed during the purge process.
 * 
 * @author Matthew Lohbihler
 */
abstract public class FiledataDefinition extends ModuleElementDefinition {
    /**
     * @return the list of image ids
     */
    abstract public List<Long> getFiledataImageIds();
}
