/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.util;

import java.util.HashMap;
import java.util.Map;

import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;

/**
 * @deprecated
 * These codes are no longer used.
 * Use UnitUtil.convertToUnit() to convert to the Unit<?> type
 */
@Deprecated
public class EngineeringUnits {
    public static final int numberOfUnits = 197;
    
    // Acceleration
    public static final int metersPerSecondPerSecond = 166;
    // Area
    public static final int squareMeters = 0;
    public static final int squareCentimeters = 116;
    public static final int squareFeet = 1;
    public static final int squareInches = 115;
    // Currency
    public static final int currency1 = 105;
    public static final int currency2 = 106;
    public static final int currency3 = 107;
    public static final int currency4 = 108;
    public static final int currency5 = 109;
    public static final int currency6 = 110;
    public static final int currency7 = 111;
    public static final int currency8 = 112;
    public static final int currency9 = 113;
    public static final int currency10 = 114;
    // Electrical
    public static final int milliamperes = 2;
    public static final int amperes = 3;
    public static final int amperesPerMeter = 167;
    public static final int amperesPerSquareMeter = 168;
    public static final int ampereSquareMeters = 169;
    public static final int farads = 170;
    public static final int henrys = 171;
    public static final int ohms = 4;
    public static final int ohmMeters = 172;
    public static final int milliohms = 145;
    public static final int kilohms = 122;
    public static final int megohms = 123;
    public static final int siemens = 173; // 1 mho equals 1 siemens
    public static final int siemensPerMeter = 174;
    public static final int teslas = 175;
    public static final int volts = 5;
    public static final int millivolts = 124;
    public static final int kilovolts = 6;
    public static final int megavolts = 7;
    public static final int voltAmperes = 8;
    public static final int kilovoltAmperes = 9;
    public static final int megavoltAmperes = 10;
    public static final int voltAmperesReactive = 11;
    public static final int kilovoltAmperesReactive = 12;
    public static final int megavoltAmperesReactive = 13;
    public static final int voltsPerDegreeKelvin = 176;
    public static final int voltsPerMeter = 177;
    public static final int degreesPhase = 14;
    public static final int powerFactor = 15;
    public static final int webers = 178;
    public static final int coulombs = 195;
    // Energy
    public static final int joules = 16;
    public static final int kilojoules = 17;
    public static final int kilojoulesPerKilogram = 125;
    public static final int megajoules = 126;
    public static final int wattHours = 18;
    public static final int kilowattHours = 19;
    public static final int megawattHours = 146;
    public static final int btus = 20;
    public static final int kiloBtus = 147;
    public static final int megaBtus = 148;
    public static final int therms = 21;
    public static final int tonHours = 22;
    // Enthalpy
    public static final int joulesPerKilogramDryAir = 23;
    public static final int kilojoulesPerKilogramDryAir = 149;
    public static final int megajoulesPerKilogramDryAir = 150;
    public static final int btusPerPoundDryAir = 24;
    public static final int btusPerPound = 117;
    // Entropy
    public static final int joulesPerDegreeKelvin = 127;
    public static final int kilojoulesPerDegreeKelvin = 151;
    public static final int megajoulesPerDegreeKelvin = 152;
    public static final int joulesPerKilogramDegreeKelvin = 128;
    // Force
    public static final int newton = 153;
    // Frequency
    public static final int cyclesPerHour = 25;
    public static final int cyclesPerMinute = 26;
    public static final int hertz = 27;
    public static final int kilohertz = 129;
    public static final int megahertz = 130;
    public static final int perHour = 131;
    // Humidity
    public static final int gramsOfWaterPerKilogramDryAir = 28;
    public static final int percentRelativeHumidity = 29;
    // Length
    public static final int millimeters = 30;
    public static final int centimeters = 118;
    public static final int meters = 31;
    public static final int kilometers = 196;
    public static final int inches = 32;
    public static final int feet = 33;
    // Light
    public static final int candelas = 179;
    public static final int candelasPerSquareMeter = 180;
    public static final int wattsPerSquareFoot = 34;
    public static final int wattsPerSquareMeter = 35;
    public static final int lumens = 36;
    public static final int luxes = 37;
    public static final int footCandles = 38;
    // Mass
    public static final int grams = 190;
    public static final int kilograms = 39;
    public static final int tonnes = 191;
    public static final int poundsMass = 40;
    public static final int tons = 41;
    // Mass Flow
    public static final int gramsPerSecond = 154;
    public static final int gramsPerMinute = 155;
    public static final int kilogramsPerSecond = 42;
    public static final int kilogramsPerMinute = 43;
    public static final int kilogramsPerHour = 44;
    public static final int tonnesPerSecond = 192;
    public static final int tonnesPerMinute = 193;
    public static final int tonnesPerHour = 194;
    public static final int poundsMassPerSecond = 119;
    public static final int poundsMassPerMinute = 45;
    public static final int poundsMassPerHour = 46;
    public static final int tonsPerHour = 156;
    // Power
    public static final int milliwatts = 132;
    public static final int watts = 47;
    public static final int kilowatts = 48;
    public static final int megawatts = 49;
    public static final int btusPerHour = 50;
    public static final int kiloBtusPerHour = 157;
    public static final int horsepower = 51;
    public static final int tonsRefrigeration = 52;
    // Pressure
    public static final int pascals = 53;
    public static final int hectopascals = 133;
    public static final int kilopascals = 54;
    public static final int millibars = 134;
    public static final int bars = 55;
    public static final int poundsForcePerSquareInch = 56;
    public static final int centimetersOfWater = 57;
    public static final int inchesOfWater = 58;
    public static final int millimetersOfMercury = 59;
    public static final int centimetersOfMercury = 60;
    public static final int inchesOfMercury = 61;
    // Temperature
    public static final int degreesCelsius = 62;
    public static final int degreesKelvin = 63;
    public static final int degreesKelvinPerHour = 181;
    public static final int degreesKelvinPerMinute = 182;
    public static final int degreesFahrenheit = 64;
    public static final int degreeDaysCelsius = 65;
    public static final int degreeDaysFahrenheit = 66;
    public static final int deltaDegreesFahrenheit = 120;
    public static final int deltaDegreesKelvin = 121;
    // Time
    public static final int years = 67;
    public static final int months = 68;
    public static final int weeks = 69;
    public static final int days = 70;
    public static final int hours = 71;
    public static final int minutes = 72;
    public static final int seconds = 73;
    public static final int hundredthsSeconds = 158;
    public static final int milliseconds = 159;
    // Torque
    public static final int newtonMeters = 160;
    // Velocity
    public static final int millimetersPerSecond = 161;
    public static final int millimetersPerMinute = 162;
    public static final int metersPerSecond = 74;
    public static final int metersPerMinute = 163;
    public static final int metersPerHour = 164;
    public static final int kilometersPerSecond = 197;
    public static final int kilometersPerHour = 75;
    public static final int feetPerSecond = 76;
    public static final int feetPerMinute = 77;
    public static final int milesPerHour = 78;
    // Volume
    public static final int cubicFeet = 79;
    public static final int cubicMeters = 80;
    public static final int imperialGallons = 81;
    public static final int liters = 82;
    public static final int usGallons = 83;
    // Volumetric Flow
    public static final int cubicFeetPerSecond = 142;
    public static final int cubicFeetPerMinute = 84;
    public static final int cubicMetersPerSecond = 85;
    public static final int cubicMetersPerMinute = 165;
    public static final int cubicMetersPerHour = 135;
    public static final int imperialGallonsPerMinute = 86;
    public static final int litersPerSecond = 87;
    public static final int litersPerMinute = 88;
    public static final int litersPerHour = 136;
    public static final int usGallonsPerMinute = 89;
    // Other
    public static final int degreesAngular = 90;
    public static final int degreesCelsiusPerHour = 91;
    public static final int degreesCelsiusPerMinute = 92;
    public static final int degreesFahrenheitPerHour = 93;
    public static final int degreesFahrenheitPerMinute = 94;
    public static final int jouleSeconds = 183;
    public static final int kilogramsPerCubicMeter = 186;
    public static final int kilowattHoursPerSquareMeter = 137;
    public static final int kilowattHoursPerSquareFoot = 138;
    public static final int megajoulesPerSquareMeter = 139;
    public static final int megajoulesPerSquareFoot = 140;
    public static final int noUnits = 95;
    public static final int newtonSeconds = 187;
    public static final int newtonsPerMeter = 188;
    public static final int partsPerMillion = 96;
    public static final int partsPerBillion = 97;
    public static final int percent = 98;
    public static final int percentObscurationPerFoot = 143;
    public static final int percentObscurationPerMeter = 144;
    public static final int percentPerSecond = 99;
    public static final int perMinute = 100;
    public static final int perSecond = 101;
    public static final int psiPerDegreeFahrenheit = 102;
    public static final int radians = 103;
    public static final int radiansPerSecond = 184;
    public static final int revolutionsPerMinute = 104;
    public static final int squareMetersPerNewton = 185;
    public static final int wattsPerMeterPerDegreeKelvin = 189;
    public static final int wattsPerSquareMeterDegreeKelvin = 141;

    private final int value;

    public EngineeringUnits(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String getKey() {
        return getKey(value);
    }

    public String getAbbrevKey() {
        return getAbbrevKey(value);
    }
    
    public static String getKey(int value) {
        return "engUnit." + value;
    }

    public static String getAbbrevKey(int value) {
        return "engUnit.abbr." + value;
    }
    
    protected static final Map<Integer, Unit<?>> conversionMap;
    static {
        conversionMap = new HashMap<Integer, Unit<?>>();
        
        // Acceleration
        conversionMap.put(metersPerSecondPerSecond, SI.METRES_PER_SQUARE_SECOND);
        // Area
        conversionMap.put(squareMeters, SI.SQUARE_METRE);
        conversionMap.put(squareCentimeters, SI.CENTIMETRE.pow(2));
        conversionMap.put(squareFeet, NonSI.FOOT.pow(2));
        conversionMap.put(squareInches, NonSI.INCH.pow(2));
        // Currency -- NOT possible, no set currencies
        // Electrical
        conversionMap.put(milliamperes, SI.MILLI(SI.AMPERE));
        conversionMap.put(amperes, SI.AMPERE);
        conversionMap.put(amperesPerMeter, SI.AMPERE.divide(SI.METER));
        conversionMap.put(amperesPerSquareMeter, SI.AMPERE.divide(SI.SQUARE_METRE));
        conversionMap.put(ampereSquareMeters, SI.AMPERE.times(SI.SQUARE_METRE));
        conversionMap.put(farads, SI.FARAD);
        conversionMap.put(henrys, SI.HENRY);
        conversionMap.put(ohms, SI.OHM);
        conversionMap.put(ohmMeters, SI.OHM.times(SI.METER));
        conversionMap.put(milliohms, SI.MILLI(SI.OHM));
        conversionMap.put(kilohms, SI.KILO(SI.OHM));
        conversionMap.put(megohms, SI.MEGA(SI.OHM));
        conversionMap.put(siemens, SI.SIEMENS);
        conversionMap.put(siemensPerMeter, SI.SIEMENS.divide(SI.METER));
        conversionMap.put(teslas, SI.TESLA);
        conversionMap.put(volts, SI.VOLT);
        conversionMap.put(millivolts, SI.MILLI(SI.VOLT));
        conversionMap.put(kilovolts, SI.KILO(SI.VOLT));
        conversionMap.put(megavolts, SI.MEGA(SI.VOLT));
        conversionMap.put(voltAmperes, SI.VOLT.times(SI.AMPERE));
        conversionMap.put(kilovoltAmperes, SI.KILO(SI.VOLT).times(SI.AMPERE));
        conversionMap.put(megavoltAmperes, SI.MEGA(SI.VOLT).times(SI.AMPERE));
        conversionMap.put(voltAmperesReactive, SI.VOLT.times(SI.AMPERE)); // same units as VA
        conversionMap.put(kilovoltAmperesReactive, SI.KILO(SI.VOLT).times(SI.AMPERE)); // same units as VA
        conversionMap.put(megavoltAmperesReactive, SI.MEGA(SI.VOLT).times(SI.AMPERE)); // same units as VA
        conversionMap.put(voltsPerDegreeKelvin, SI.VOLT.divide(SI.KELVIN));
        conversionMap.put(voltsPerMeter, SI.VOLT.divide(SI.METER));
        conversionMap.put(degreesPhase, NonSI.DEGREE_ANGLE);
        conversionMap.put(powerFactor, Unit.ONE); // dimensionless
        conversionMap.put(webers, SI.WEBER);
        conversionMap.put(coulombs, SI.COULOMB);
        // Energy
        conversionMap.put(joules, SI.JOULE);
        conversionMap.put(kilojoules, SI.KILO(SI.JOULE));
        conversionMap.put(kilojoulesPerKilogram, SI.KILO(SI.JOULE).divide(SI.KILOGRAM));
        conversionMap.put(megajoules, SI.MEGA(SI.JOULE));
        conversionMap.put(wattHours, SI.WATT.times(NonSI.HOUR));
        conversionMap.put(kilowattHours, SI.KILO(SI.WATT).times(NonSI.HOUR));
        conversionMap.put(megawattHours, SI.MEGA(SI.WATT).times(NonSI.HOUR));
        conversionMap.put(btus, JUnitUtil.BTU);
        conversionMap.put(kiloBtus, SI.KILO(JUnitUtil.BTU));
        conversionMap.put(megaBtus, SI.MEGA(JUnitUtil.BTU));
        conversionMap.put(therms, JUnitUtil.THERM);
        conversionMap.put(tonHours, NonSI.TON_US.times(NonSI.HOUR));
        // Enthalpy
        conversionMap.put(joulesPerKilogramDryAir, SI.JOULE.divide(SI.KILOGRAM));
        conversionMap.put(kilojoulesPerKilogramDryAir, SI.KILO(SI.JOULE).divide(SI.KILOGRAM));
        conversionMap.put(megajoulesPerKilogramDryAir, SI.MEGA(SI.JOULE).divide(SI.KILOGRAM));
        conversionMap.put(btusPerPoundDryAir, JUnitUtil.BTU.divide(NonSI.POUND));
        conversionMap.put(btusPerPound, JUnitUtil.BTU.divide(NonSI.POUND));
        // Entropy
        conversionMap.put(joulesPerDegreeKelvin, SI.JOULE.divide(SI.KELVIN));
        conversionMap.put(kilojoulesPerDegreeKelvin, SI.KILO(SI.JOULE).divide(SI.KELVIN));
        conversionMap.put(megajoulesPerDegreeKelvin, SI.MEGA(SI.JOULE).divide(SI.KELVIN));
        conversionMap.put(joulesPerKilogramDegreeKelvin, SI.JOULE.divide(SI.KILOGRAM.times(SI.KELVIN)));
        // Force
        conversionMap.put(newton, SI.NEWTON);
        // Frequency
        conversionMap.put(cyclesPerHour, Unit.ONE.divide(NonSI.HOUR));
        conversionMap.put(cyclesPerMinute, Unit.ONE.divide(NonSI.MINUTE));
        conversionMap.put(hertz, SI.HERTZ);
        conversionMap.put(kilohertz, SI.KILO(SI.HERTZ));
        conversionMap.put(megahertz, SI.MEGA(SI.HERTZ));
        conversionMap.put(perHour, Unit.ONE.divide(NonSI.HOUR));
        // Humidity
        conversionMap.put(gramsOfWaterPerKilogramDryAir, SI.GRAM.divide(SI.KILOGRAM));
        conversionMap.put(percentRelativeHumidity, NonSI.PERCENT);
        // Length
        conversionMap.put(millimeters, SI.MILLIMETRE);
        conversionMap.put(centimeters, SI.CENTIMETRE);
        conversionMap.put(meters, SI.METRE);
        conversionMap.put(kilometers, SI.KILOMETRE);
        conversionMap.put(inches, NonSI.INCH);
        conversionMap.put(feet, NonSI.FOOT);
        // Light
        conversionMap.put(candelas, SI.CANDELA);
        conversionMap.put(candelasPerSquareMeter, SI.CANDELA.divide(SI.SQUARE_METRE));
        conversionMap.put(wattsPerSquareFoot, SI.WATT.divide(NonSI.FOOT.pow(2)));
        conversionMap.put(wattsPerSquareMeter, SI.WATT.divide(SI.SQUARE_METRE));
        conversionMap.put(lumens, SI.LUMEN);
        conversionMap.put(luxes, SI.LUX);
        conversionMap.put(footCandles, SI.LUMEN.divide(NonSI.FOOT.pow(2)));
        // Mass
        conversionMap.put(grams, SI.GRAM);
        conversionMap.put(kilograms, SI.KILOGRAM);
        conversionMap.put(tonnes, NonSI.METRIC_TON);
        conversionMap.put(poundsMass, NonSI.POUND);
        conversionMap.put(tons, NonSI.TON_US);
        // Mass Flow
        conversionMap.put(gramsPerSecond, SI.GRAM.divide(SI.SECOND));
        conversionMap.put(gramsPerMinute, SI.GRAM.divide(NonSI.MINUTE));
        conversionMap.put(kilogramsPerSecond, SI.KILO(SI.GRAM).divide(SI.SECOND));
        conversionMap.put(kilogramsPerMinute, SI.KILO(SI.GRAM).divide(NonSI.MINUTE));
        conversionMap.put(kilogramsPerHour, SI.KILO(SI.GRAM).divide(NonSI.HOUR));
        conversionMap.put(tonnesPerSecond, NonSI.METRIC_TON.divide(SI.SECOND));
        conversionMap.put(tonnesPerMinute, NonSI.METRIC_TON.divide(NonSI.MINUTE));
        conversionMap.put(tonnesPerHour, NonSI.METRIC_TON.divide(NonSI.HOUR));
        conversionMap.put(poundsMassPerSecond, NonSI.POUND);
        conversionMap.put(poundsMassPerMinute, NonSI.POUND.divide(NonSI.MINUTE));
        conversionMap.put(poundsMassPerHour, NonSI.POUND.divide(NonSI.MINUTE));
        conversionMap.put(tonsPerHour, NonSI.TON_US.divide(NonSI.HOUR));
        // Power
        conversionMap.put(milliwatts, SI.MILLI(SI.WATT));
        conversionMap.put(watts, SI.WATT);
        conversionMap.put(kilowatts, SI.KILO(SI.WATT));
        conversionMap.put(megawatts, SI.MEGA(SI.WATT));
        conversionMap.put(btusPerHour, JUnitUtil.BTU.divide(NonSI.HOUR));
        conversionMap.put(kiloBtusPerHour, SI.KILO(JUnitUtil.BTU).divide(NonSI.HOUR));
        conversionMap.put(horsepower, NonSI.HORSEPOWER);
        conversionMap.put(tonsRefrigeration, NonSI.TON_US);
        // Pressure
        conversionMap.put(pascals, SI.PASCAL);
        conversionMap.put(hectopascals, SI.HECTO(SI.PASCAL));
        conversionMap.put(kilopascals, SI.KILO(SI.PASCAL));
        conversionMap.put(millibars, SI.MILLI(NonSI.BAR));
        conversionMap.put(bars, NonSI.BAR);
        conversionMap.put(poundsForcePerSquareInch, JUnitUtil.PSI);
        conversionMap.put(centimetersOfWater, SI.PASCAL.times(98.0665));
        conversionMap.put(inchesOfWater, SI.PASCAL.times(248.84));
        conversionMap.put(millimetersOfMercury, NonSI.MILLIMETER_OF_MERCURY);
        conversionMap.put(centimetersOfMercury, NonSI.MILLIMETER_OF_MERCURY.divide(10));
        conversionMap.put(inchesOfMercury, NonSI.INCH_OF_MERCURY);
        // Temperature
        conversionMap.put(degreesCelsius, SI.CELSIUS);
        conversionMap.put(degreesKelvin, SI.KELVIN);
        conversionMap.put(degreesKelvinPerHour, SI.KELVIN.divide(NonSI.HOUR));
        conversionMap.put(degreesKelvinPerMinute, SI.KELVIN.divide(NonSI.MINUTE));
        conversionMap.put(degreesFahrenheit, NonSI.FAHRENHEIT);
        conversionMap.put(degreeDaysCelsius, NonSI.DAY.times(SI.CELSIUS));
        conversionMap.put(degreeDaysFahrenheit, NonSI.DAY.times(NonSI.FAHRENHEIT));
        conversionMap.put(deltaDegreesFahrenheit, NonSI.FAHRENHEIT);
        conversionMap.put(deltaDegreesKelvin, SI.KELVIN);
        // Time
        conversionMap.put(years, NonSI.YEAR);
        conversionMap.put(months, NonSI.MONTH);
        conversionMap.put(weeks, NonSI.WEEK);
        conversionMap.put(days, NonSI.DAY);
        conversionMap.put(hours, NonSI.HOUR);
        conversionMap.put(minutes, NonSI.MINUTE);
        conversionMap.put(seconds, SI.SECOND);
        conversionMap.put(hundredthsSeconds, SI.CENTI(SI.SECOND));
        conversionMap.put(milliseconds, SI.MILLI(SI.SECOND));
        // Torque
        conversionMap.put(newtonMeters, SI.NEWTON.times(SI.METER));
        // Velocity
        conversionMap.put(millimetersPerSecond, SI.MILLIMETRE.divide(SI.SECOND));
        conversionMap.put(millimetersPerMinute, SI.MILLIMETRE.divide(NonSI.MINUTE));
        conversionMap.put(metersPerSecond, SI.METRE.divide(SI.SECOND));
        conversionMap.put(metersPerMinute, SI.METRE.divide(NonSI.MINUTE));
        conversionMap.put(metersPerHour, SI.METRE.divide(NonSI.HOUR));
        conversionMap.put(kilometersPerSecond, SI.KILOMETRE.divide(SI.SECOND));
        conversionMap.put(kilometersPerHour, NonSI.KILOMETRES_PER_HOUR);
        conversionMap.put(feetPerSecond, NonSI.FOOT.divide(SI.SECOND));
        conversionMap.put(feetPerMinute, NonSI.FOOT.divide(NonSI.MINUTE));
        conversionMap.put(milesPerHour, NonSI.MILES_PER_HOUR);
        // Volume
        conversionMap.put(cubicFeet, NonSI.FOOT.pow(3));
        conversionMap.put(cubicMeters, SI.CUBIC_METRE);
        conversionMap.put(imperialGallons, NonSI.GALLON_UK);
        conversionMap.put(liters, NonSI.LITRE);
        conversionMap.put(usGallons, NonSI.GALLON_LIQUID_US);
        // Volumetric Flow
        conversionMap.put(cubicFeetPerSecond, NonSI.FOOT.pow(3).divide(SI.SECOND));
        conversionMap.put(cubicFeetPerMinute, NonSI.FOOT.pow(3).divide(NonSI.MINUTE));
        conversionMap.put(cubicMetersPerSecond, SI.CUBIC_METRE.divide(SI.SECOND));
        conversionMap.put(cubicMetersPerMinute, SI.CUBIC_METRE.divide(NonSI.MINUTE));
        conversionMap.put(cubicMetersPerHour, SI.CUBIC_METRE.divide(NonSI.HOUR));
        conversionMap.put(imperialGallonsPerMinute, NonSI.GALLON_UK.divide(NonSI.MINUTE));
        conversionMap.put(litersPerSecond, NonSI.LITRE.divide(SI.SECOND));
        conversionMap.put(litersPerMinute, NonSI.LITRE.divide(NonSI.MINUTE));
        conversionMap.put(litersPerHour, NonSI.LITRE.divide(NonSI.HOUR));
        conversionMap.put(usGallonsPerMinute, NonSI.GALLON_LIQUID_US.divide(NonSI.MINUTE));
        // Other
        conversionMap.put(degreesAngular, NonSI.DEGREE_ANGLE);
        conversionMap.put(degreesCelsiusPerHour, SI.CELSIUS.divide(NonSI.HOUR));
        conversionMap.put(degreesCelsiusPerMinute, SI.CELSIUS.divide(NonSI.MINUTE));
        conversionMap.put(degreesFahrenheitPerHour, NonSI.FAHRENHEIT.divide(NonSI.HOUR));
        conversionMap.put(degreesFahrenheitPerMinute, NonSI.FAHRENHEIT.divide(NonSI.MINUTE));
        conversionMap.put(jouleSeconds, SI.JOULE.times(SI.SECOND));
        conversionMap.put(kilogramsPerCubicMeter, SI.KILOGRAM.divide(SI.CUBIC_METRE));
        conversionMap.put(kilowattHoursPerSquareMeter, SI.KILO(SI.WATT).divide(SI.SQUARE_METRE));
        conversionMap.put(kilowattHoursPerSquareFoot, SI.KILO(SI.WATT).divide(NonSI.FOOT.pow(2)));
        conversionMap.put(megajoulesPerSquareMeter, SI.MEGA(SI.JOULE).divide(SI.SQUARE_METRE));
        conversionMap.put(megajoulesPerSquareFoot, SI.MEGA(SI.JOULE).divide(NonSI.FOOT.pow(2)));
        conversionMap.put(noUnits, Unit.ONE);
        conversionMap.put(newtonSeconds, SI.NEWTON.times(SI.SECOND));
        conversionMap.put(newtonsPerMeter, SI.NEWTON.divide(SI.METRE));
        conversionMap.put(partsPerMillion, Unit.ONE.divide(1000000));
        conversionMap.put(partsPerBillion, Unit.ONE.divide(1000000000));
        conversionMap.put(percent, NonSI.PERCENT);
        conversionMap.put(percentObscurationPerFoot, NonSI.PERCENT.divide(NonSI.FOOT));
        conversionMap.put(percentObscurationPerMeter, NonSI.PERCENT.divide(SI.METRE));
        conversionMap.put(percentPerSecond, NonSI.PERCENT.divide(SI.SECOND));
        conversionMap.put(perMinute, Unit.ONE.divide(NonSI.MINUTE));
        conversionMap.put(perSecond, Unit.ONE.divide(SI.SECOND));
        conversionMap.put(psiPerDegreeFahrenheit, JUnitUtil.PSI.divide(NonSI.FAHRENHEIT));
        conversionMap.put(radians, SI.RADIAN);
        conversionMap.put(radiansPerSecond, SI.RADIAN.divide(SI.SECOND));
        conversionMap.put(revolutionsPerMinute, NonSI.REVOLUTION.divide(NonSI.MINUTE));
        conversionMap.put(squareMetersPerNewton, SI.SQUARE_METRE.divide(SI.NEWTON));
        conversionMap.put(wattsPerMeterPerDegreeKelvin, SI.WATT.divide(SI.METRE).divide(SI.KELVIN));
        conversionMap.put(wattsPerSquareMeterDegreeKelvin, SI.WATT.divide(SI.SQUARE_METRE).divide(SI.KELVIN));
    }
}
