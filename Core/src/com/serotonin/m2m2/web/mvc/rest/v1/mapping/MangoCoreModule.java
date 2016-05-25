/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import java.util.List;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.RestJsonDeserializerDefinition;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractDataSourceModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.SuperclassModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.PointLocatorModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.email.EmailRecipientModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.publisher.AbstractPublishedPointModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.publisher.AbstractPublisherModel;

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
		//Add the core deserializers
		this.addDeserializer(AbstractDataSourceModel.class, new DataSourceModelDeserializer());
		this.addDeserializer(PointLocatorModel.class, new PointLocatorModelDeserializer());
		this.addDeserializer(SuperclassModel.class, new SuperclassModelDeserializer());
		this.addDeserializer(EmailRecipientModel.class, new EmailRecipientModelDeserializer());
		this.addDeserializer(AbstractPublisherModel.class, new PublisherModelDeserializer());
		this.addDeserializer(AbstractPublishedPointModel.class, new PublishedPointModelDeserializer());
		
		//Setup the Deserializer's from the Module Registry
		List<RestJsonDeserializerDefinition> defs = ModuleRegistry.getDefinitions(RestJsonDeserializerDefinition.class);
		for(RestJsonDeserializerDefinition def : defs){
			this.addDeserializer(def.getType(), def.getDeserializer());
		}
		
		super.setupModule(context);
	}
}
