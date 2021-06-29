/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event;

import java.util.Map;
import java.util.stream.Collectors;

import com.infiniteautomation.mango.util.ReverseEnum;
import com.infiniteautomation.mango.util.ReverseEnumMap;
import com.serotonin.m2m2.i18n.TranslatableMessage;

public enum AlarmLevels implements ReverseEnum<Integer> {
    NONE(0, new TranslatableMessage("common.alarmLevel.none")),
    INFORMATION(1, new TranslatableMessage("common.alarmLevel.info")),
    IMPORTANT(2, new TranslatableMessage("common.alarmLevel.important")),
    WARNING(3, new TranslatableMessage("common.alarmLevel.warning")),
    URGENT(4, new TranslatableMessage("common.alarmLevel.urgent")),
    CRITICAL(5, new TranslatableMessage("common.alarmLevel.critical")),
    LIFE_SAFETY(6, new TranslatableMessage("common.alarmLevel.lifeSafety")),
    DO_NOT_LOG(-2, new TranslatableMessage("common.alarmLevel.doNotLog")),
    IGNORE(-3, new TranslatableMessage("common.alarmLevel.ignore"));

    private static ReverseEnumMap<Integer, AlarmLevels> map = new ReverseEnumMap<>(AlarmLevels.class);
    private final int value;
    private final TranslatableMessage description;

    private AlarmLevels(int value, TranslatableMessage description) {
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

    public static AlarmLevels fromValue(Integer value) {
        return map.get(value);
    }

    public static AlarmLevels fromName(String name) {
        return Enum.valueOf(AlarmLevels.class, name);
    }

    public static <X> Map<X, AlarmLevels> convertMap(Map<X, Integer> input) {
        return input.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> AlarmLevels.fromValue(e.getValue())));
    }
}
