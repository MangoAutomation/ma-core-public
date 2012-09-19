/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.util;

import org.directwebremoting.create.NewCreator;

import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.web.dwr.ModuleDwr;

public class ModuleDwrCreator extends NewCreator {
    private final Module module;

    public ModuleDwrCreator(Module module) {
        this.module = module;
    }

    @Override
    public ModuleDwr getInstance() throws InstantiationException {
        ModuleDwr o = (ModuleDwr) super.getInstance();
        o.setModule(module);
        return o;
    }
}
