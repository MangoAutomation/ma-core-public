/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.dataSource;

import java.io.Serializable;

import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataSource.PointLocatorRT;
import com.serotonin.m2m2.util.ChangeComparableObject;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.PointLocatorModel;

public interface PointLocatorVO extends Serializable, ChangeComparableObject {
    /**
     * One of the com.serotonin.m2m2.DataTypes
     */
    public int getDataTypeId();

    /**
     * The text representation of the data type
     */
    public TranslatableMessage getDataTypeMessage();

    /**
     * An arbitrary description of the point location configuration for human consumption.
     */
    public TranslatableMessage getConfigurationDescription();

    /**
     * Can the value be set in the data source?
     */
    public boolean isSettable();

    /**
     * Supplemental to being settable, can the set value be relinquished?
     */
    public boolean isRelinquishable();

    /**
     * Create a runtime version of the locator
     */
    public PointLocatorRT createRuntime();

    /**
     * Validate. What else?
     */
    public void validate(ProcessResult response, DataPointVO dpvo);

    public void validate(ProcessResult response);

    public DataPointSaveHandler getDataPointSaveHandler();
    
    /**
     * Get the Model of this Point Locator
     * @return PointLocatorModel implementation
     */
    public PointLocatorModel<?> asModel();
}
