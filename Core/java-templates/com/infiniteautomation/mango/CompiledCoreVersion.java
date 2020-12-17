/*
 * Copyright (C) 2020 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango;

import com.github.zafarkhaja.semver.Version;

/**
 * @author Jared Wiltshire
 */
public final class CompiledCoreVersion {
    public static final Version VERSION = Version.valueOf("${project.version}");
}
