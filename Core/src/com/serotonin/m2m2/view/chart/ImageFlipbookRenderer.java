/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.chart;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.view.ImplDefinition;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * WARNING: The ChartRenderer classes are serializable and are present in blob columns of data points. Do not remove.
 * @author Matthew Lohbihler
 */
public class ImageFlipbookRenderer extends BaseChartRenderer {

    @Override
    public String getTypeName() {
        throw new UnsupportedOperationException();
    }

    @JsonProperty
    private int limit;

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

    @Override
    public void validate(ProcessResult result) {
        throw new UnsupportedOperationException();
    }

    //
    // /
    // / Serialization
    // /
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(limit);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            limit = in.readInt();
        }
    }
}
