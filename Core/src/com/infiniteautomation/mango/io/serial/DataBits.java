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
public enum DataBits implements ReverseEnum<Integer> {

    DATA_BITS_5(5, new TranslatableMessage("dsEdit.serial.dataBits5")),
    DATA_BITS_6(6, new TranslatableMessage("dsEdit.serial.dataBits6")),
    DATA_BITS_7(7, new TranslatableMessage("dsEdit.serial.dataBits7")),
    DATA_BITS_8(8, new TranslatableMessage("dsEdit.serial.dataBits8"));
    
    private static ReverseEnumMap<Integer, DataBits> map = new ReverseEnumMap<>(DataBits.class);
    private final int value;
    private final TranslatableMessage description;

    private DataBits(int value, TranslatableMessage description) {
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

    public static DataBits fromValue(Integer value) {
        return map.get(value);
    }

    public static DataBits fromName(String name) {
        return Enum.valueOf(DataBits.class, name);
    }

    public static <X> Map<X, DataBits> convertMap(Map<X, Integer> input) {
        return input.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> DataBits.fromValue(e.getValue())));
    }
    
}
