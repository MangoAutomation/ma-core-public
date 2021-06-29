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
public enum FlowControl implements ReverseEnum<Integer> {

    NONE(0 ,new TranslatableMessage("dsEdit.serial.flow.none")),
    RTSCTS(1 ,new TranslatableMessage("dsEdit.serial.flow.rtsCts")),
    XONXOFF(2 ,new TranslatableMessage("dsEdit.serial.flow.xonXoff"));
    
    private static ReverseEnumMap<Integer, FlowControl> map = new ReverseEnumMap<>(FlowControl.class);
    private final int value;
    private final TranslatableMessage description;

    private FlowControl(int value, TranslatableMessage description) {
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

    public static FlowControl fromValue(Integer value) {
        return map.get(value);
    }

    public static FlowControl fromName(String name) {
        return Enum.valueOf(FlowControl.class, name);
    }

    public static <X> Map<X, FlowControl> convertMap(Map<X, Integer> input) {
        return input.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> FlowControl.fromValue(e.getValue())));
    }
}
