/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util.log;

import org.apache.logging.log4j.Level;

import com.infiniteautomation.mango.util.ReverseEnum;
import com.infiniteautomation.mango.util.ReverseEnumMap;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Terry Packer
 *
 */
public enum LogLevel implements ReverseEnum<Integer> {
        
    TRACE(1, new TranslatableMessage("common.logging.trace"), Level.TRACE),
    DEBUG(2, new TranslatableMessage("common.logging.debug"), Level.DEBUG),
    INFO(3, new TranslatableMessage("common.logging.info"), Level.INFO),
    WARN(4, new TranslatableMessage("common.logging.warn"), Level.WARN),
    ERROR(5, new TranslatableMessage("common.logging.error"), Level.ERROR),
    FATAL(6, new TranslatableMessage("common.logging.fatal"), Level.FATAL),
    NONE(10, new TranslatableMessage("common.logging.none"), Level.OFF);

    private static ReverseEnumMap<Integer, LogLevel> map = new ReverseEnumMap<>(LogLevel.class);
    private final int value;
    private final TranslatableMessage description;
    private final Level log4jLevel;

    LogLevel(int value, TranslatableMessage description, Level log4jLevel) {
        this.value = value;
        this.description = description;
        this.log4jLevel = log4jLevel;
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

    public Level getLog4jLevel() {
        return log4jLevel;
    }
}
