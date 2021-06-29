/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event;

import com.serotonin.m2m2.vo.AbstractVO;

/**
 * @author Matthew Lohbihler
 */
abstract public class SimpleEventDetectorVO<T extends AbstractVO> extends AbstractVO {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static final String POINT_EVENT_DETECTOR_PREFIX = "P";
    public static final String SCHEDULED_EVENT_PREFIX = "S";

    abstract public String getEventDetectorKey();
}
