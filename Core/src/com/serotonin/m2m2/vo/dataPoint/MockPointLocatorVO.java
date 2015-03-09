/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.dataPoint;

import java.io.IOException;
import java.util.List;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataSource.MockPointLocatorRT;
import com.serotonin.m2m2.rt.dataSource.PointLocatorRT;
import com.serotonin.m2m2.vo.dataSource.AbstractPointLocatorVO;

/**
 * Mock Point Locator, useful for testing.
 * 
 * 
 * @author Terry Packer
 *
 */
public class MockPointLocatorVO extends AbstractPointLocatorVO implements JsonSerializable {

	private int dataTypeId = DataTypes.NUMERIC;
	private boolean settable = false;
	
	public MockPointLocatorVO(int dataTypeId, boolean settable){
		this.dataTypeId = dataTypeId;
		this.settable = settable;
	}
	
	
	/**
	 * 
	 */
	public MockPointLocatorVO() {
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.dataSource.PointLocatorVO#getDataTypeId()
	 */
	@Override
	public int getDataTypeId() {
		return this.dataTypeId;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.dataSource.PointLocatorVO#getConfigurationDescription()
	 */
	@Override
	public TranslatableMessage getConfigurationDescription() {
		return new TranslatableMessage("common.default", "Mock Point Locator");
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.dataSource.PointLocatorVO#isSettable()
	 */
	@Override
	public boolean isSettable() {
		return this.settable;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.dataSource.PointLocatorVO#createRuntime()
	 */
	@Override
	public PointLocatorRT createRuntime() {
		return new MockPointLocatorRT(this);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.dataSource.PointLocatorVO#validate(com.serotonin.m2m2.i18n.ProcessResult)
	 */
	@Override
	public void validate(ProcessResult response) {
		// TODO Implement when needed for testing
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.ChangeComparableObject#addProperties(java.util.List)
	 */
	@Override
	public void addProperties(List<TranslatableMessage> list) {
		// TODO Implement when needed for testing
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.ChangeComparableObject#addPropertyChanges(java.util.List, java.lang.Object)
	 */
	@Override
	public void addPropertyChanges(List<TranslatableMessage> list, Object o) {
		// TODO Implement when needed for testing
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.JsonSerializable#jsonRead(com.serotonin.json.JsonReader, com.serotonin.json.type.JsonObject)
	 */
	@Override
	public void jsonRead(JsonReader arg0, JsonObject arg1) throws JsonException {
		// TODO Implement when needed for testing
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.JsonSerializable#jsonWrite(com.serotonin.json.ObjectWriter)
	 */
	@Override
	public void jsonWrite(ObjectWriter arg0) throws IOException, JsonException {
		//TODO Implement when needed for testing
		
	}

}
