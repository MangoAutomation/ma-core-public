/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.template;

import com.serotonin.m2m2.module.TemplateDefinition;

/**
 * @author Terry Packer
 *
 */
public class DataPointPropertiesTemplateDefinition extends TemplateDefinition{

	public static final String TEMPLATE_TYPE = "DATA_POINT_PROPERTIES";

	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.TemplateDefinition#getTemplateTypeName()
	 */
	@Override
	public String getTemplateTypeName() {
		return TEMPLATE_TYPE;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.TemplateDefinition#createTemplateVO()
	 */
	@Override
	protected BaseTemplateVO<?> createTemplateVO() {
		return new DataPointPropertiesTemplateVO();
	}

}
