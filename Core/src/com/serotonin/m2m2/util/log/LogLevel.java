/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.util.log;

import com.infiniteautomation.mango.util.ReverseEnum;
import com.infiniteautomation.mango.util.ReverseEnumMap;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Terry Packer
 *
 */
public enum LogLevel implements ReverseEnum<Integer> {
        
    TRACE(1, new TranslatableMessage("common.logging.trace")),
    DEBUG(2, new TranslatableMessage("common.logging.debug")),
    INFO(3, new TranslatableMessage("common.logging.info")),
    WARN(4, new TranslatableMessage("common.logging.warn")),
    ERROR(5, new TranslatableMessage("common.logging.error")),
    FATAL(6, new TranslatableMessage("common.logging.fatal")),
    NONE(10, new TranslatableMessage("common.logging.none"));
    

    private static ReverseEnumMap<Integer, LogLevel> map = new ReverseEnumMap<>(LogLevel.class);
    private final int value;
    private final TranslatableMessage description;

    private LogLevel(int value, TranslatableMessage description) {
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

    public static LogLevel fromValue(Integer value) {
        return map.get(value);
    }

    public static LogLevel fromName(String name) {
        return Enum.valueOf(LogLevel.class, name);
    }
}
