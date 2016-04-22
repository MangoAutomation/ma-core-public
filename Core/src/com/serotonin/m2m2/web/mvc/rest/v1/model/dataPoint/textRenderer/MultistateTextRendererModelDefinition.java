/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.textRenderer;

import com.serotonin.m2m2.module.ModelDefinition;
import com.serotonin.m2m2.view.text.MultistateRenderer;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;

/**
 * @author Terry Packer
 *
 */
public class MultistateTextRendererModelDefinition extends ModelDefinition{

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ModelDefinition#getModelKey()
	 */
	@Override
	public String getModelKey() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ModelDefinition#getModelTypeName()
	 */
	@Override
	public String getModelTypeName() {
		return MultistateRenderer.getDefinition().getName();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ModelDefinition#createModel()
	 */
	@Override
	public AbstractRestModel<?> createModel() {
		return new MultistateTextRendererModel();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ModelDefinition#supportsClass(java.lang.Class)
	 */
	@Override
	public boolean supportsClass(Class<?> clazz) {
		return MultistateTextRendererModel.class.equals(clazz);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ModelDefinition#getModelClass()
	 */
	@Override
	public Class<? extends AbstractRestModel<?>> getModelClass() {
		return MultistateTextRendererModel.class;
	}

}
