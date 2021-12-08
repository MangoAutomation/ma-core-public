/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.dataSource;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumSet;

import com.serotonin.json.JsonException;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonEntity;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;

//Required to prevent properties from being written
@JsonEntity
abstract public class AbstractPointLocatorVO<VO extends AbstractPointLocatorVO<VO>> implements PointLocatorVO<VO> {


    @Override
    public TranslatableMessage getDataTypeMessage() {
        return getDataType().getDescription();
    }

    protected String getMessage(Translations translations, String key, Object... args) {
        return new TranslatableMessage(key, args).translate(translations);
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        in.readInt(); // Read the version. Value is currently not used.
    }

    protected void writeDataType(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("dataType", getDataType().name());
    }


    protected DataType readDataType(JsonObject json) throws JsonException {
        return readDataType(json, EnumSet.noneOf(DataType.class));
    }

    protected DataType readDataType(JsonObject json, DataType excludeType) throws JsonException {
        return readDataType(json, EnumSet.of(excludeType));
    }

    protected DataType readDataType(JsonObject json, EnumSet<DataType> excludeTypes) throws JsonException {
        String text = json.getString("dataType");
        if (text == null) {
            throw new TranslatableJsonException("emport.error.missing", "dataType", DataType.formatNames(excludeTypes));
        }

        DataType dataType;
        try {
            dataType = DataType.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw new TranslatableJsonException("emport.error.invalid", "dataType", text, DataType.formatNames(excludeTypes));
        }

        if (excludeTypes.contains(dataType)) {
            throw new TranslatableJsonException("emport.error.invalid", "dataType", text, DataType.formatNames(excludeTypes));
        }

        return dataType;
    }

    /**
     * Defaults to returning null. Override to return something else.
     */
    @Override
    public Boolean isRelinquishable() {
        return null;
    }

}
