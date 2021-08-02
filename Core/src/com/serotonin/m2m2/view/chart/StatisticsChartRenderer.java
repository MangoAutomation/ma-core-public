/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.chart;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.view.ImplDefinition;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * WARNING: The ChartRenderer classes are serializable and are present in blob columns of data points. Do not remove.
 */
public class StatisticsChartRenderer extends TimePeriodChartRenderer {


    @Override
    public String getTypeName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImplDefinition getDef() {
        throw new UnsupportedOperationException();
    }
    
    @JsonProperty
    private boolean includeSum;

    @Override
    public void addDataToModel(Map<String, Object> model, DataPointVO point) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getChartSnippetFilename() {
        throw new UnsupportedOperationException();
    }
    
    //
    // /
    // / Serialization
    // /
    //
    private static final long serialVersionUID = -1;
    private static final int version = 2;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeBoolean(includeSum);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1)
            includeSum = true;
        else if (ver == 2)
            includeSum = in.readBoolean();
    }
}
