/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.chart;

import java.io.Serializable;
import java.util.Map;

import com.serotonin.m2m2.i18n.ProcessResult;
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
    
    /**
     * Validate the settings
     * @param result
     */
    public void validate(ProcessResult result);
}
