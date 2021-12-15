/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.chart;

import java.io.Serializable;
import java.util.Map;

import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.view.ImplDefinition;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * WARNING: The ChartRenderer classes are serializable and are present in blob columns of data points. Do not remove.
 */
public interface ChartRenderer extends Serializable {

    public String getTypeName();

    public void addDataToModel(Map<String, Object> model, DataPointVO point);

    public ImplDefinition getDef();

    public String getChartSnippetFilename();
    
    /**
     * Validate the settings
     */
    public void validate(ProcessResult result);
}
