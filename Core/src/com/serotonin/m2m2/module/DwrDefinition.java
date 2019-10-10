/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.web.dwr.ModuleDwr;

/**
 * A DWR definition registers a DWR proxy.
 *
 * @author Matthew Lohbihler
 */
abstract public class DwrDefinition extends ModuleElementDefinition implements DwrClassHolder {
    @Override
    abstract public Class<? extends ModuleDwr> getDwrClass();
}
