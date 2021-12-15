/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import com.serotonin.m2m2.view.stats.ITime;

/**
 *
 * @author Terry Packer
 */
public interface SeriesIdTime extends ITime {

    /**
     * Get the Series ID for the VO in Question
     */
    public int getSeriesId();

}
