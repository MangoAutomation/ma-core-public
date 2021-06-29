/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.dataSource;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.serotonin.json.JsonException;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonEntity;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;

//Required to prevent properties from being written
@JsonEntity
abstract public class AbstractPointLocatorVO<VO extends AbstractPointLocatorVO<VO>> implements PointLocatorVO<VO> {


    @Override
    public TranslatableMessage getDataTypeMessage() {
        return DataTypes.getDataTypeMessage(getDataTypeId());
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
        writer.writeEntry("dataType", DataTypes.CODES.getCode(getDataTypeId()));
    }

    protected Integer readDataType(JsonObject json, int... excludeIds) throws JsonException {
        String text = json.getString("dataType");
        if (text == null)
            return null;

        int dataType = DataTypes.CODES.getId(text);
        if (!DataTypes.CODES.isValidId(dataType, excludeIds))
            throw new TranslatableJsonException("emport.error.invalid", "dataType", text,
                    DataTypes.CODES.getCodeList(excludeIds));

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
