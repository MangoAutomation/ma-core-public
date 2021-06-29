/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import java.util.Map;
import java.util.stream.Collectors;

import com.infiniteautomation.mango.util.ReverseEnum;
import com.infiniteautomation.mango.util.ReverseEnumMap;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Terry Packer
 *
 */
public enum StopBits implements ReverseEnum<Integer> {

    STOP_BITS_1(1, new TranslatableMessage("dsEdit.serial.StopBits1")),
    STOP_BITS_1_5(3, new TranslatableMessage("dsEdit.serial.StopBits15")),
    STOP_BITS_2(2, new TranslatableMessage("dsEdit.serial.StopBits2"));
    
    private static ReverseEnumMap<Integer, StopBits> map = new ReverseEnumMap<>(StopBits.class);
    private final int value;
    private final TranslatableMessage description;

    private StopBits(int value, TranslatableMessage description) {
        this.value = value;
        this.description = description;
    }

    @Override
    public Integer value() {
        return this.value;
    }

    public TranslatableMessage getDescription() {
        return description;
    }

    public static StopBits fromValue(Integer value) {
        return map.get(value);
    }

    public static StopBits fromName(String name) {
        return Enum.valueOf(StopBits.class, name);
    }

    public static <X> Map<X, StopBits> convertMap(Map<X, Integer> input) {
        return input.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> StopBits.fromValue(e.getValue())));
    }

    
}
