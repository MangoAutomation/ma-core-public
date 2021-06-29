/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.export;

/**
 * @author Matthew Lohbihler
 */
public interface ExportDataStreamHandler {
    /**
     * Called before the data for the given point is provided. A point may not have any data, so calls to setData are
     * not guaranteed.
     * 
     * @param pointInfo
     */
    void startPoint(ExportPointInfo pointInfo);

    /**
     * Provides a single data value for the current point.
     * 
     * @param rdv
     */
    void pointData(ExportDataValue rdv);

    /**
     * Indicates that the last of the information has been sent, i.e. the other methods will no longer be called. Useful
     * for cleanup operations.
     */
    void done();
}
