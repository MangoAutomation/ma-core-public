/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.dataSource.mock;

import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.MockDataSourceModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataSource.AbstractDataSourceModel;

/**
 * @author Terry Packer
 *
 */
public class MockDataSourceDefinition extends DataSourceDefinition{

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.DataSourceDefinition#getDataSourceTypeName()
	 */
	@Override
	public String getDataSourceTypeName() {
		return "MOCK";
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.DataSourceDefinition#getDescriptionKey()
	 */
	@Override
	public String getDescriptionKey() {
		return ""; //
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.DataSourceDefinition#createDataSourceVO()
	 */
	@Override
	protected DataSourceVO<?> createDataSourceVO() {
		return new MockDataSourceVO();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.DataSourceDefinition#getEditPagePath()
	 */
	@Override
	public String getEditPagePath() {
		return "";
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.DataSourceDefinition#getDwrClass()
	 */
	@Override
	public Class<?> getDwrClass() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.DataSourceDefinition#getModelClass()
	 */
	@Override
	public Class<? extends AbstractDataSourceModel<?>> getModelClass() {
		return MockDataSourceModel.class;
	}

}
