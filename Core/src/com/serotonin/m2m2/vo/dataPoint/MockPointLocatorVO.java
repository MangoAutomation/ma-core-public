/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.dataPoint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataSource.MockPointLocatorRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.AbstractPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;

/**
 * Mock Point Locator, useful for testing.
 * 
 * 
 * @author Terry Packer
 *
 */
public class MockPointLocatorVO extends AbstractPointLocatorVO<MockPointLocatorVO> implements JsonSerializable {

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
	public MockPointLocatorRT createRuntime() {
		return new MockPointLocatorRT(this);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.JsonSerializable#jsonRead(com.serotonin.json.JsonReader, com.serotonin.json.type.JsonObject)
	 */
	@Override
	public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
	    Integer value = readDataType(jsonObject, DataTypes.IMAGE);
        if (value != null)
            dataTypeId = value;
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.JsonSerializable#jsonWrite(com.serotonin.json.ObjectWriter)
	 */
	@Override
	public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
	    writeDataType(writer);
	}

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.dataSource.AbstractPointLocatorVO#validateImpl(com.serotonin.m2m2.i18n.ProcessResult, com.serotonin.m2m2.vo.DataPointVO, com.serotonin.m2m2.vo.dataSource.DataSourceVO)
     */
    @Override
    public void validate(ProcessResult response, DataPointVO dpvo, DataSourceVO<?> dsvo) {
        if(!(dsvo instanceof MockDataSourceVO))
            response.addContextualMessage("dataSourceId", "dpEdit.validate.invalidDataSourceType");
    }
    
    private static final long serialVersionUID = -1;
    private static final int version = 2;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(dataTypeId);
        out.writeBoolean(settable);
    }
    private void readObject(ObjectInputStream in) throws IOException {
        int version = in.readInt();
        if(version == 1) {
            
        }else if(version == 2) {
            dataTypeId = in.readInt();
            settable = in.readBoolean();
        }
    }
    

}
