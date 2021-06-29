/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * A set point source is anything that can cause a set point to occur. For example, a user can use the interface to
 * explicitly set a point, in which case the user is the set point source. A program could reset a value to 0 every
 * date, making that program the set point source. This information is stored in the database as a point value
 * annotation.
 * 
 * @author Matthew Lohbihler
 */
public interface SetPointSource {
    public String getSetPointSourceType();

    public int getSetPointSourceId();

    public TranslatableMessage getSetPointSourceMessage();

    public void raiseRecursionFailureEvent();
}
