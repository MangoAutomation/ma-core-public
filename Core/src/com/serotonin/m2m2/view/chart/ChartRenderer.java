/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.chart;

import java.io.Serializable;
import java.util.Map;

import com.serotonin.m2m2.view.ImplDefinition;
import com.serotonin.m2m2.vo.DataPointVO;

public interface ChartRenderer extends Serializable {
    public static final int TYPE_NONE = 1;
    public static final int TYPE_TABLE = 2;
    public static final int TYPE_IMAGE = 3;
    public static final int TYPE_STATS = 4;

    public String getTypeName();

    public void addDataToModel(Map<String, Object> model, DataPointVO point);

    public ImplDefinition getDef();

    public String getChartSnippetFilename();
}
