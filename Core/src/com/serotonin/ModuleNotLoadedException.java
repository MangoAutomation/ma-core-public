/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin;

import java.io.IOException;

/**
 * Exception to help with handling Missing classes when deserializing from the database.
 *
 * @author Terry Packer
 */
public class ModuleNotLoadedException extends IOException {

    private static final long serialVersionUID = 1L;

    public ModuleNotLoadedException(String classname, Exception e) {
        super("Class " + classname + " not loaded, is a module missing?", e);
    }
    
    public ModuleNotLoadedException(String moduleName, String classname,  Exception e) {
        super("Module " + moduleName + " missing, unable to load " + classname, e);
    }
    
}
