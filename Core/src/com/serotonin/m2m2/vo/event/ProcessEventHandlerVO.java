/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonNumber;
import com.serotonin.json.type.JsonObject;
import com.serotonin.util.SerializationHelper;

/**
 * @author Terry Packer
 *
 */
public class ProcessEventHandlerVO extends AbstractEventHandlerVO {

    private String activeProcessCommand;
    private int activeProcessTimeout = 15;
    private String inactiveProcessCommand;
    private int inactiveProcessTimeout = 15;

    @JsonProperty
    private boolean interpolateCommands;

    public boolean isInterpolateCommands() {
        return interpolateCommands;
    }

    public void setInterpolateCommands(boolean interpolateCommands) {
        this.interpolateCommands = interpolateCommands;
    }

    public String getActiveProcessCommand() {
        return activeProcessCommand;
    }

    public void setActiveProcessCommand(String activeProcessCommand) {
        this.activeProcessCommand = activeProcessCommand;
    }

    public int getActiveProcessTimeout() {
        return activeProcessTimeout;
    }

    public void setActiveProcessTimeout(int activeProcessTimeout) {
        this.activeProcessTimeout = activeProcessTimeout;
    }

    public String getInactiveProcessCommand() {
        return inactiveProcessCommand;
    }

    public void setInactiveProcessCommand(String inactiveProcessCommand) {
        this.inactiveProcessCommand = inactiveProcessCommand;
    }

    public int getInactiveProcessTimeout() {
        return inactiveProcessTimeout;
    }

    public void setInactiveProcessTimeout(int inactiveProcessTimeout) {
        this.inactiveProcessTimeout = inactiveProcessTimeout;
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 2;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        SerializationHelper.writeSafeUTF(out, activeProcessCommand);
        out.writeInt(activeProcessTimeout);
        SerializationHelper.writeSafeUTF(out, inactiveProcessCommand);
        out.writeInt(inactiveProcessTimeout);
        out.writeBoolean(interpolateCommands);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();
        if(ver == 1){
            activeProcessCommand = SerializationHelper.readSafeUTF(in);
            activeProcessTimeout = in.readInt();
            inactiveProcessCommand = SerializationHelper.readSafeUTF(in);
            inactiveProcessTimeout = in.readInt();
            interpolateCommands = false;
        } else if(ver == 2) {
            activeProcessCommand = SerializationHelper.readSafeUTF(in);
            activeProcessTimeout = in.readInt();
            inactiveProcessCommand = SerializationHelper.readSafeUTF(in);
            inactiveProcessTimeout = in.readInt();
            interpolateCommands = in.readBoolean();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("activeProcessCommand", activeProcessCommand);
        writer.writeEntry("activeProcessTimeout", activeProcessTimeout);
        writer.writeEntry("inactiveProcessCommand", inactiveProcessCommand);
        writer.writeEntry("inactiveProcessTimeout", inactiveProcessTimeout);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        String text = jsonObject.getString("activeProcessCommand");
        if (text != null)
            activeProcessCommand = text;

        JsonNumber i = jsonObject.getJsonNumber("activeProcessTimeout");
        if (i != null)
            activeProcessTimeout = i.intValue();

        text = jsonObject.getString("inactiveProcessCommand");
        if (text != null)
            inactiveProcessCommand = text;

        i = jsonObject.getJsonNumber("inactiveProcessTimeout");
        if (i != null)
            inactiveProcessTimeout = i.intValue();

    }

}
