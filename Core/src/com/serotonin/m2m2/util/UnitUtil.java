/*
 * Copyright (C) 2013 Infinite Automation and Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

package com.serotonin.m2m2.util;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import com.serotonin.m2m2.Common;

/**
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * 
 * @author Jared Wiltshire
 */
public class UnitUtil {
    static final UnitFormat localFormat = UnitFormat.getInstance(Common.getLocale());
    static final UnitFormat ucumFormat = UnitFormat.getInstance();//UnitFormat.getUCUMInstance();
    static final List<String> addedUnitLabels = new ArrayList<String>();

    public static final Unit<Energy> BTU = SI.JOULE.times(1055.05585262D);
    public static final Unit<Energy> THERM = BTU.times(100000);
    public static final Unit<?> PSI = NonSI.POUND_FORCE.divide(NonSI.INCH.pow(2));

    static {
        // register some labels
        localFormat.label(BTU, "btu"); addedUnitLabels.add("btu");
        localFormat.label(THERM, "thm"); addedUnitLabels.add("thm");
        localFormat.label(PSI, "psi"); addedUnitLabels.add("psi");
        
        localFormat.label(NonSI.REVOLUTION.divide(NonSI.MINUTE), "rpm");
        addedUnitLabels.add("rpm");
        localFormat.label(Unit.ONE.divide(1000000), "ppm");
        addedUnitLabels.add("ppm");
        localFormat.label(Unit.ONE.divide(1000000000), "ppb");
        addedUnitLabels.add("ppb");
        localFormat.label(NonSI.GALLON_LIQUID_US.divide(NonSI.MINUTE), "gpm");
        addedUnitLabels.add("gpm");
        localFormat.label(SI.WATT.times(1000).divide(NonSI.HOUR), "kWh");
        addedUnitLabels.add("kWh");

        //Define any aliases
        localFormat.alias(SI.CELSIUS, "Celsius"); // easier to type
        addedUnitLabels.add("Celsius");
        localFormat.alias(NonSI.FAHRENHEIT, "Fahrenheit");
        addedUnitLabels.add("Fahrenheit");
    }

    @SuppressWarnings("deprecation")
    public static Unit<?> convertToUnit(int engineeringUnit) {
        Unit<?> unit = EngineeringUnits.conversionMap.get(engineeringUnit);
        return (unit == null) ? Unit.ONE : unit;
    }

    public static String formatLocal(Unit<?> unit) {
        return localFormat.format(unit);
    }

    public static Unit<?> parseLocal(String unit) throws IllegalArgumentException {
        try {
            return localFormat.parseProductUnit(unit, new ParsePosition(0));
        }
        catch (ParseException e) {
            try {
                return ucumFormat.parseProductUnit(unit, new ParsePosition(0));
            }
            catch (ParseException e2) {
            	//This has been changed to now throw an exception for better validation
                throw new IllegalArgumentException(e);
            }
        }
    }

    public static String formatUcum(Unit<?> unit) {
        return ucumFormat.format(unit);
    }

    public static Unit<?> parseUcum(String unit) throws IllegalArgumentException {
        try {
            return ucumFormat.parseProductUnit(unit, new ParsePosition(0));
        }
        catch (ParseException e) {
            try {
                return localFormat.parseProductUnit(unit, new ParsePosition(0));
            }
            catch (ParseException e2) {
            	//This has been changed to now throw an exception for better validation
                throw new IllegalArgumentException(e);
                //return Unit.ONE;
            }
        }
    }
    
    public static List<String> getAddedUnitLabels() {
    	return addedUnitLabels;
    }
}
