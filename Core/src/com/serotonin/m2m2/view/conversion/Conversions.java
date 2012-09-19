/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.conversion;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.util.EngineeringUnits;

/**
 * @author Matthew Lohbihler
 * 
 */
public class Conversions {
    private static final Map<ConversionType, Conversion> availableConversions = new HashMap<ConversionType, Conversion>();

    private static final LinearConversion DEGREES_CELSIUS_TO_DEGREES_FAHRENHEIT = new LinearConversion(1.8, 32);
    private static final LinearConversion DEGREES_FAHRENHEIT_TO_DEGREES_CELSIUS = DEGREES_CELSIUS_TO_DEGREES_FAHRENHEIT
            .getInverse();

    static {
        availableConversions.put(
                new ConversionType(EngineeringUnits.degreesFahrenheit, EngineeringUnits.degreesCelsius),
                DEGREES_FAHRENHEIT_TO_DEGREES_CELSIUS);
        availableConversions.put(
                new ConversionType(EngineeringUnits.degreesCelsius, EngineeringUnits.degreesFahrenheit),
                DEGREES_CELSIUS_TO_DEGREES_FAHRENHEIT);
    }

    public static Conversion getConversion(EngineeringUnits from, EngineeringUnits to) {
        return getConversion(from.getValue(), to.getValue());
    }

    public static Conversion getConversion(Integer from, Integer to) {
        return availableConversions.get(new ConversionType(from, to));
    }

    public static DataValue convert(EngineeringUnits from, EngineeringUnits to, DataValue value) {
        return convert(from.getValue(), to.getValue(), value);
    }

    public static DataValue convert(Integer from, Integer to, DataValue value) {
        double d = convert(from, to, value.getDoubleValue());
        return new NumericValue(d);
    }

    public static double convert(EngineeringUnits from, EngineeringUnits to, double value) {
        return convert(from.getValue(), to.getValue(), value);
    }

    public static double convert(Integer from, Integer to, double value) {
        Conversion conversion = getConversion(from, to);
        if (conversion == null)
            return Double.NaN;
        return conversion.convert(value);
    }

    public static double fahrenheitToCelsius(double fahrenheit) {
        return DEGREES_FAHRENHEIT_TO_DEGREES_CELSIUS.convert(fahrenheit);
    }

    public static double celsiusToFahrenheit(double celsius) {
        return DEGREES_CELSIUS_TO_DEGREES_FAHRENHEIT.convert(celsius);
    }

    static class ConversionType {
        private final Integer from;
        private final Integer to;

        public ConversionType(EngineeringUnits from, EngineeringUnits to) {
            this(from.getValue(), to.getValue());
        }

        public ConversionType(Integer from, Integer to) {
            this.from = from;
            this.to = to;
        }

        public Integer getFrom() {
            return from;
        }

        public Integer getTo() {
            return to;
        }
    }

    private Conversions() {
        // Static methods only
    }
}
