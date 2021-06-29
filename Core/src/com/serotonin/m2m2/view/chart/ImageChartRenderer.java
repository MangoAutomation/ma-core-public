/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.chart;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import com.serotonin.json.spi.JsonEntity;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.view.ImplDefinition;
import com.serotonin.m2m2.vo.DataPointVO;

@JsonEntity
public class ImageChartRenderer extends TimePeriodChartRenderer {
    private static ImplDefinition definition = new ImplDefinition("chartRendererImage", "IMAGE", "chartRenderer.image",
            new int[] { DataTypes.BINARY, DataTypes.MULTISTATE, DataTypes.NUMERIC });

    public static ImplDefinition getDefinition() {
        return definition;
    }

    @Override
    public String getTypeName() {
        return definition.getName();
    }

    public ImageChartRenderer() {
        // no op
    }

    public ImageChartRenderer(int timePeriod, int numberOfPeriods) {
        super(timePeriod, numberOfPeriods);
    }
    
    @Override
    public void addDataToModel(Map<String, Object> model, DataPointVO point) {
        // Nothing to do.
    }

    @Override
    public ImplDefinition getDef() {
        return definition;
    }

    @Override
    public String getChartSnippetFilename() {
        return "imageChart.jsp";
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
