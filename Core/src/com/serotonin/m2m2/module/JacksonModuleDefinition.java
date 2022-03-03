/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.spring.annotations.CommonMapper;
import com.infiniteautomation.mango.spring.annotations.DatabaseMapper;
import com.infiniteautomation.mango.spring.annotations.RestMapper;

/**
 * Define Jackson Modules to be applied to the various core mappers.
 *
 * Add one or more annotations such as {@link CommonMapper}, {@link DatabaseMapper}, and {@link RestMapper} to
 * apply these modules to a specific {@link ObjectMapper}.
 * 
 * @author Terry Packer
 *
 */
abstract public class JacksonModuleDefinition extends ModuleElementDefinition {

	/**
	 * Get the Jackson Module to apply to the Mapper
	 */
	public abstract Iterable<? extends Module> getJacksonModules();

    protected Version createJacksonVersion() {
        com.github.zafarkhaja.semver.Version version = getModule().getVersion();

        return new Version(version.getMajorVersion(), version.getMinorVersion(), version.getPatchVersion(),
                version.getPreReleaseVersion());
    }

}
