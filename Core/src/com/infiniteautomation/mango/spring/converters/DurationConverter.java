/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.converters;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Jared Wiltshire
 */
public class DurationConverter implements Converter<String, Duration> {

    @Override
    public Duration convert(String source) {
        if (source.isBlank()) return null;

        String[] parts = source.trim().split("\\s+");
        if (parts.length != 2) throw new IllegalArgumentException("Should have a number followed by a unit");

        long numberPart = Long.parseLong(parts[0]);
        ChronoUnit unit = ChronoUnit.valueOf(parts[1]);

        try {
            return Duration.of(numberPart, unit);
        } catch (DateTimeException | ArithmeticException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
