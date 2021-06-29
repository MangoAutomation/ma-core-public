/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo;

import java.io.IOException;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonBoolean;
import com.serotonin.json.type.JsonObject;

/**
 *
 * Class that has an Enable/Disable Member
 *
 * @author Terry Packer
 *
 */
public abstract class AbstractActionVO extends AbstractVO implements JsonSerializable {
    private static final long serialVersionUID = -1;
    public static final String ENABLED_KEY = "enabled";

    protected boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        JsonBoolean enabled = jsonObject.getJsonBoolean(ENABLED_KEY);
        if(enabled != null)
            this.enabled = enabled.toBoolean();
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException,
    JsonException {
        super.jsonWrite(writer);
        writer.writeEntry(ENABLED_KEY, enabled);
    }
}
