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
public enum Parity implements ReverseEnum<Integer> {
    
    NONE (0, new TranslatableMessage("dsEdit.serial.parity.none")),
    ODD(1, new TranslatableMessage("dsEdit.serial.parity.odd")),
    EVEN(2, new TranslatableMessage("dsEdit.serial.parity.even")),
    MARK(3, new TranslatableMessage("dsEdit.serial.parity.mark")),
    SPACE(4, new TranslatableMessage("dsEdit.serial.parity.space"));

    private static ReverseEnumMap<Integer, Parity> map = new ReverseEnumMap<>(Parity.class);
    private final int value;
    private final TranslatableMessage description;

    private Parity(int value, TranslatableMessage description) {
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

    public static Parity fromValue(Integer value) {
        return map.get(value);
    }

    public static Parity fromName(String name) {
        return Enum.valueOf(Parity.class, name);
    }

    public static <X> Map<X, Parity> convertMap(Map<X, Integer> input) {
        return input.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Parity.fromValue(e.getValue())));
    }

}
