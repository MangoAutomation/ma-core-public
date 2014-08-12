/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

/**
 * Module to extend Jackson JSON rendering
 * 
 * @author Terry Packer
 * 
 */
public class MangoCoreModule extends SimpleModule {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MangoCoreModule() {
		super("MangoCore", new Version(0, 0, 1, "SNAPSHOT", "com.infiniteautomation",
				"mango"));
	}
	
	@Override
	public void setupModule(SetupContext context) {
	
		//this.setMixInAnnotation(DataPointVO.class, DataPointVoMixin.class);
		this.setMixInAnnotation(DataSourceVO.class, DataSourceVoMixin.class);
		this.addSerializer(JsonSerializable.class, new SeroJsonSerializer());
		//this.addDeserializer(JsonSerializable.class, new SeroJsonDeSerializer());
		super.setupModule(context);
	}
}
