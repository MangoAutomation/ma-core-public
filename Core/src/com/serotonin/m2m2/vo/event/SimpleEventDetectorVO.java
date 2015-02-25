/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.event;

/**
 * @author Matthew Lohbihler
 */
abstract public class SimpleEventDetectorVO {
    public static final String POINT_EVENT_DETECTOR_PREFIX = "P";
    public static final String SCHEDULED_EVENT_PREFIX = "S";

    abstract public String getEventDetectorKey();
}
