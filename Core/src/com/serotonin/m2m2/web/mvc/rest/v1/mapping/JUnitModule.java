/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Module to extend Jackson JSON rendering for JUnit
 * 
 * @author Terry Packer
 * 
 */
public class JUnitModule extends SimpleModule {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JUnitModule() {
		super("ModuleName", new Version(0, 0, 1, "snapshotInfo", "groupId",
				"artifactId"));
	}

	@Override
	public void setupModule(SetupContext context) {
		context.setMixInAnnotations(javax.measure.unit.Unit.class, UnitMixin.class);
	}
}
