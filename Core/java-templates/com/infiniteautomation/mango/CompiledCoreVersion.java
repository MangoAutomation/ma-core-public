/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango;

import com.github.zafarkhaja.semver.Version;

/**
 * @author Jared Wiltshire
 */
public class CompiledCoreVersion {
    public static final Version VERSION = Version.valueOf("${project.version}");
}
