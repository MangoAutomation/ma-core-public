/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;

/**
 * Most basic script permission allowing you to eval a script / list script engines.
 *
 * @author Jared Wiltshire
 */
public class ScriptPermissionDefinition extends PermissionDefinition {

    public static final String PERMISSION = "script.eval";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("permission." + PERMISSION);
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

}
