/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.converters;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Jared Wiltshire
 */
public class TemporalAmountConverter implements Converter<String, TemporalAmount> {

    @Override
    public TemporalAmount convert(String source) {
        if (source.isBlank()) return null;

        String[] parts = source.trim().split("\\s+");
        if (parts.length != 2) throw new IllegalArgumentException("Should have a number followed by a unit");

        long numberPart = Long.parseLong(parts[0]);
        ChronoUnit unit = ChronoUnit.valueOf(parts[1]);
        if (unit.isTimeBased()) {
            return Duration.of(numberPart, unit);
        }

        try {
            switch (unit) {
                case DAYS: return Period.ofDays(Math.toIntExact(numberPart));
                case WEEKS: return Period.ofWeeks(Math.toIntExact(numberPart));
                case MONTHS: return Period.ofMonths(Math.toIntExact(numberPart));
                case YEARS: return Period.ofYears(Math.toIntExact(numberPart));
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(e);
        }

        throw new IllegalArgumentException("Unsupported unit: " + unit);
    }

}
