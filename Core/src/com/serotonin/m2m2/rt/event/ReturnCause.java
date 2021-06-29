/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event;

import com.infiniteautomation.mango.util.ReverseEnum;
import com.infiniteautomation.mango.util.ReverseEnumMap;

public enum ReturnCause implements ReverseEnum<Integer> {

    RETURN_TO_NORMAL(1),
    SOURCE_DISABLED(4);

    private static ReverseEnumMap<Integer, ReturnCause> map = new ReverseEnumMap<>(ReturnCause.class);
    private final int value;

    private ReturnCause(int value) {
        this.value = value;
    }

    @Override
    public Integer value() {
        return this.value;
    }

    public static ReturnCause fromValue(Integer value) {
        // EventInstance.rtnCause used to default to 0 and this was stored in the DB
        // should only be present in DB where rtnApplicable = N
        // also if the DB column contains null then ResultSet.getInt() returns 0
        if (value == 0) {
            return null;
        }

        return map.get(value);
    }

    public static ReturnCause fromName(String name) {
        return Enum.valueOf(ReturnCause.class, name);
    }
}