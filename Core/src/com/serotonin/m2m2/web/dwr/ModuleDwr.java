/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

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
        return module.getDirectoryPath();
    }
}
