/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.util.exception;

import com.github.zafarkhaja.semver.Version;

/**
 *
 * @author Terry Packer
 */
public class ModuleUpgradeException extends Exception {

    private static final long serialVersionUID = 1L;

    public ModuleUpgradeException(Throwable cause, String moduleName, Version previousVersion, Version version) {
        super("Module " + moduleName + " failed to upgrade from " + previousVersion.toString() + " to " + version.toString(), cause);
    }
}
