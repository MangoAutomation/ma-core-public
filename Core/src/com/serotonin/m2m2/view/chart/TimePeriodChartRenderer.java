/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.chart;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.i18n.ProcessResult;

/**
 *
 * WARNING: The ChartRenderer classes are serializable and are present in blob columns of data points. Do not remove.
 *
 * Base Class for Time Period rendering Charts
 * 
 * @author Matthew Lohbihler, Terry Packer
 */
abstract public class TimePeriodChartRenderer extends BaseChartRenderer {
    
	private int timePeriod;
    @JsonProperty
    private int numberOfPeriods;

	//
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(timePeriod);
        out.writeInt(numberOfPeriods);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            timePeriod = in.readInt();
            numberOfPeriods = in.readInt();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        throw new UnsupportedOperationException();
    }

	@Override
	public void validate(ProcessResult result) {
        throw new UnsupportedOperationException();
	}
    
    
}
