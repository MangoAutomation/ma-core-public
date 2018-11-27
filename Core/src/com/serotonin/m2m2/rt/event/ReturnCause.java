/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event;

import com.infiniteautomation.mango.util.ReverseEnum;
import com.infiniteautomation.mango.util.ReverseEnumMap;

public enum ReturnCause implements ReverseEnum<Integer> {

    /**
     * Should only be present in DB where rtnApplicable = N
     *
     * TODO Mango 3.6 EventInstance.rtnCause used to default to 0 and this was stored in the DB
     * add an upgrade script and change these to NULL?
     */
    HAS_NOT_RETURNED(0),

    RETURN_TO_NORMAL(1),
    SOURCE_DISABLED(4);

    private final int value;
    private static ReverseEnumMap<Integer, ReturnCause> map = new ReverseEnumMap<>(ReturnCause.class);

    private ReturnCause(int value) {
        this.value = value;
    }

    @Override
    public Integer value() {
        return this.value;
    }

    public static ReturnCause enumFor(int value) {
        return map.get(value);
    }
}