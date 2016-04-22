/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint;

import com.serotonin.m2m2.module.ModelDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractVoModel;

/**
 * @author Terry Packer
 *
 */
public class DataPointModelDefinition extends ModelDefinition{
	
	public static final String TYPE_NAME = "DATA_POINT";

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
		return TYPE_NAME;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ModelDefinition#createModel()
	 */
	@Override
	public AbstractVoModel<?> createModel() {
		DataPointVO vo = new DataPointVO();
	    return new DataPointModel(vo);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ModelDefinition#supportsClass(java.lang.Class)
	 */
	@Override
	public boolean supportsClass(Class<?> clazz) {
		return DataPointModel.class.equals(clazz);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ModelDefinition#getModelClass()
	 */
	@Override
	public Class<? extends AbstractRestModel<?>> getModelClass() {
		return DataPointModel.class;
	}

}
