/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.textRenderer;

import com.serotonin.m2m2.view.text.NoneRenderer;

/**
 * @author Terry Packer
 *
 */
public class NoneTextRendererModel extends BaseTextRendererModel<NoneTextRendererModel>{

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.SuperclassModel#getType()
	 */
	@Override
	public String getType() {
		return NoneRenderer.getDefinition().getName();
	}

}
