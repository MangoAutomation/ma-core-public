/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.dataSource;

import java.io.Serializable;

import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataSource.PointLocatorRT;

public interface PointLocatorVO<VO extends PointLocatorVO<VO>> extends Serializable {
    /**
     * @return data type of the point
     */
    DataTypes getDataType();

    /**
     * The text representation of the data type
     */
    TranslatableMessage getDataTypeMessage();

    /**
     * An arbitrary description of the point location configuration for human consumption.
     */
    TranslatableMessage getConfigurationDescription();

    /**
     * Can the value be set in the data source?
     */
    boolean isSettable();

    /**
     * Supplemental to being settable, can the set value be relinquished?
     */
    Boolean isRelinquishable();

    /**
     * Create a runtime version of the locator
     */
    PointLocatorRT<VO> createRuntime();

    /**
     * @return the type of data source to look up its definition
     */
    String getDataSourceType();
}
