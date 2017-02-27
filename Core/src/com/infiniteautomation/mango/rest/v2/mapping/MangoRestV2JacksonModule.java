/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.mapping;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Jackson Module to extend JSON rendering
 * @author Terry Packer
 */
public class MangoRestV2JacksonModule extends SimpleModule{

	private static final long serialVersionUID = 1L;

	public MangoRestV2JacksonModule() {
		super("MangoCoreRestV2", new Version(0, 0, 1, "SNAPSHOT", "com.infiniteautomation",
				"mango"));
	}
	
	@Override
	public void setupModule(SetupContext context) {
		this.addSerializer(TranslatableMessage.class, new TranslatableMessageSerializer());
		super.setupModule(context);
	}
}
