/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * A license definition allows the module to check the validity of its license, take action as necessary. It also
 * provides a means to present messages to users about license conditions, either as errors or warnings.
 * 
 * @author Matthew Lohbihler
 */
abstract public class LicenseDefinition extends ModuleElementDefinition {
    /**
     * Allows the license definition to provide error messages to the user regarding the module's license.
     * 
     * @param errors
     *            a list of translatable messages that can be appended to
     */
    abstract public void addLicenseErrors(List<TranslatableMessage> errors);

    /**
     * Allows the license definition to provide warning messages to the user regarding the module's license.
     * 
     * @param warnings
     *            a list of translatable messages that can be appended to
     */
    abstract public void addLicenseWarnings(List<TranslatableMessage> warnings);

    /**
     * Provides the module an opportunity to check it's license. All license values are available in the Common.license
     * License instance. This method is called once upon instantiation, and thereafter regularly during the lifetime of
     * the instance. Exceptions should not be thrown. Instead, the module should take the opportunity to change settings
     * as necessary that change the behavior or the module.
     * 
     * @param initialization
     *            whether the method is being called at system startup (true), or during a scheduled re-check (false).
     */
    abstract public void licenseCheck(boolean initialization);
}
