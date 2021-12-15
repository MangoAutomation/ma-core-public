/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.dataSource;

import java.io.Serializable;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataSource.PointLocatorRT;

public interface PointLocatorVO<VO extends PointLocatorVO<VO>> extends Serializable {
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
    public Boolean isRelinquishable();

    /**
     * Create a runtime version of the locator
     */
    public PointLocatorRT<VO> createRuntime();

    /**
     * Return the type of data source to look up its definition
     */
    public String getDataSourceType();
}
