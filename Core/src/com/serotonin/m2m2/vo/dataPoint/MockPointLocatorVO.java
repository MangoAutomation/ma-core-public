/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataSource.MockPointLocatorRT;
import com.serotonin.m2m2.vo.dataSource.AbstractPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceDefinition;

/**
 * Mock Point Locator, useful for testing.
 *
 *
 * @author Terry Packer
 *
 */
public class MockPointLocatorVO extends AbstractPointLocatorVO<MockPointLocatorVO> implements JsonSerializable {

    private DataType dataType = DataType.NUMERIC;
    private boolean settable = false;

    public MockPointLocatorVO(DataType dataType, boolean settable){
        this.dataType = dataType;
        this.settable = settable;
    }

    public MockPointLocatorVO() {}

    @Override
    public DataType getDataType() {
        return this.dataType;
    }

    public void setDataType(DataType type) {
        this.dataType = type;
    }

    @Override
    public TranslatableMessage getConfigurationDescription() {
        return new TranslatableMessage("literal", "Mock Point Locator");
    }

    @Override
    public boolean isSettable() {
        return this.settable;
    }

    public void setSettable(boolean settable) {
        this.settable = settable;
    }

    @Override
    public MockPointLocatorRT createRuntime() {
        return new MockPointLocatorRT(this);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        if (jsonObject.containsKey("dataType")) {
            this.dataType = readDataType(jsonObject);
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writeDataType(writer);
    }

    private static final long serialVersionUID = -1;
    private static final int version = 2;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(dataType.getId());
        out.writeBoolean(settable);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int version = in.readInt();
        if(version == 1) {
            dataType = DataType.NUMERIC;
        }else if(version == 2) {
            dataType = DataType.fromId(in.readInt());
            settable = in.readBoolean();
        }
    }

    @Override
    public String getDataSourceType() {
        return MockDataSourceDefinition.TYPE_NAME;
    }
}
