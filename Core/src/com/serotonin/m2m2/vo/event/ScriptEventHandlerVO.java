/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.event.handlers.ScriptEventHandlerRT;
import com.serotonin.util.SerializationHelper;

/**
 * @author Jared Wiltshire
 */
public class ScriptEventHandlerVO extends AbstractEventHandlerVO {

    @JsonProperty
    String engineName;
    @JsonProperty
    String script;

    @Override
    public EventHandlerRT<ScriptEventHandlerVO> createRuntime() {
        return new ScriptEventHandlerRT(this);
    }

    private static final long serialVersionUID = -1L;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        SerializationHelper.writeSafeUTF(out, engineName);
        SerializationHelper.writeSafeUTF(out, script);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();
        if (ver == 1) {
            engineName = SerializationHelper.readSafeUTF(in);
            script = SerializationHelper.readSafeUTF(in);
        }
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }
}
