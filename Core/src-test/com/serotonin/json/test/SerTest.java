package com.serotonin.json.test;

import java.io.IOException;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;

public class SerTest implements JsonSerializable {
    private int dontShow = 1;
    private int doShow = 2;

    public int getDontShow() {
        return dontShow;
    }

    public void setDontShow(int dontShow) {
        this.dontShow = dontShow;
    }

    public int getDoShow() {
        return doShow;
    }

    public void setDoShow(int doShow) {
        this.doShow = doShow;
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("doShow", doShow);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        doShow = jsonObject.getInt("doShow", 0);
    }
}
