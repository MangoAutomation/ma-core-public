/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.chart;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import com.serotonin.json.spi.JsonEntity;
import com.serotonin.m2m2.view.ImplDefinition;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * WARNING: The ChartRenderer classes are serializable and are present in blob columns of data points. Do not remove.
 */
@JsonEntity
public class ImageChartRenderer extends TimePeriodChartRenderer {

    @Override
    public String getTypeName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDataToModel(Map<String, Object> model, DataPointVO point) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImplDefinition getDef() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getChartSnippetFilename() {
        throw new UnsupportedOperationException();
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
        in.readInt();
    }

}
