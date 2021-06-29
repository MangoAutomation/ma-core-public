/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util.log;

import java.io.PrintWriter;

import com.serotonin.io.NullWriter;

/**
 * For place holding loggers at level NONE
 * 
 * @author Terry Packer
 *
 */
public class NullPrintWriter extends PrintWriter {

    public NullPrintWriter() {
        super(new NullWriter());
    }
    
    public NullPrintWriter(NullWriter writer) {
        super(writer);
    }
}
