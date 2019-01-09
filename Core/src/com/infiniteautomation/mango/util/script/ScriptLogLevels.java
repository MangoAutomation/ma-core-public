/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import java.util.Map;
import java.util.stream.Collectors;

import com.infiniteautomation.mango.util.ReverseEnum;
import com.infiniteautomation.mango.util.ReverseEnumMap;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Terry Packer
 *
 */
public enum ScriptLogLevels implements ReverseEnum<Integer> {
    
    TRACE(1, new TranslatableMessage("common.logging.trace")),
    DEBUG(2, new TranslatableMessage("common.logging.debug")),
    INFO(3, new TranslatableMessage("common.logging.info")),
    WARN(4, new TranslatableMessage("common.logging.warn")),
    ERROR(5, new TranslatableMessage("common.logging.error")),
    FATAL(6, new TranslatableMessage("common.logging.fatal")),
    NONE(10, new TranslatableMessage("common.logging.none"));

    private static ReverseEnumMap<Integer, ScriptLogLevels> map = new ReverseEnumMap<>(ScriptLogLevels.class);
    private final int value;
    private final TranslatableMessage description;

    private ScriptLogLevels(int value, TranslatableMessage description) {
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

    public static ScriptLogLevels fromValue(Integer value) {
        return map.get(value);
    }

    public static ScriptLogLevels fromName(String name) {
        return Enum.valueOf(ScriptLogLevels.class, name);
    }

    public static <X> Map<X, ScriptLogLevels> convertMap(Map<X, Integer> input) {
        return input.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> ScriptLogLevels.fromValue(e.getValue())));
    }
    
}
