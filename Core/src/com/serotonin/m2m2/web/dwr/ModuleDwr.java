/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import com.serotonin.m2m2.Constants;
import com.serotonin.m2m2.module.Module;

abstract public class ModuleDwr extends BaseDwr {
    private Module module;

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public String getModulePath() {
        return "/" + Constants.DIR_WEB + "/" + Constants.DIR_MODULES + "/" + module.getName();
    }
}
