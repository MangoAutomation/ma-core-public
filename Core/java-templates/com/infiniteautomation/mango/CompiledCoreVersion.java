/*
 * Copyright (C) 2020 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango;

import java.time.Instant;
import java.util.Date;

import com.github.zafarkhaja.semver.Version;

/**
 * @author Jared Wiltshire
 */
public final class CompiledCoreVersion {
    public static final Version VERSION = Version.valueOf("${project.version}");
    public static final Date BUILD_DATE = Date.from(Instant.parse("${buildTimestamp}"));
}
