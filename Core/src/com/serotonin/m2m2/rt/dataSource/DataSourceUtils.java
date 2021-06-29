/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataSource;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.view.text.MultistateRenderer;
import com.serotonin.m2m2.view.text.MultistateValue;
import com.serotonin.m2m2.view.text.TextRenderer;

/**
 * Utility for common methods with  Point Value Time String manipulations
 * 
 * @author Matthew Lohbihler
 */
public class DataSourceUtils {
    
	/**
	 * Get a value from string data via pattern matching on group 1
	 * @param valuePattern
	 * @param data
	 * @param dataTypeId
	 * @param binary0Value
	 * @param textRenderer
	 * @param valueFormat
	 * @param pointName
	 * @return
	 * @throws TranslatableException
	 */
	public static DataValue getValue(Pattern valuePattern, String data, int dataTypeId, String binary0Value,
            TextRenderer textRenderer, DecimalFormat valueFormat, String pointName) throws TranslatableException {
		return getValue(valuePattern, 1, data, dataTypeId, binary0Value,
	            textRenderer, valueFormat, pointName);
    }

	/**
	 * Get DataValue from String using group number
	 * @param valuePattern
	 * @param group
	 * @param data
	 * @param dataTypeId
	 * @param binary0Value
	 * @param textRenderer
	 * @param valueFormat
	 * @param pointName
	 * @return
	 * @throws TranslatableException
	 */
	public static DataValue getValue(Pattern valuePattern, int group, String data, int dataTypeId, String binary0Value,
            TextRenderer textRenderer, DecimalFormat valueFormat, String pointName) throws TranslatableException {
        if (data == null)
            throw new TranslatableException(new TranslatableMessage("event.valueParse.noData", pointName));

        Matcher matcher = valuePattern.matcher(data);
        if (matcher.find()) {
            String valueStr = matcher.group(group);
            if (valueStr == null)
                valueStr = "";

            return getValue(valueStr, dataTypeId, binary0Value, textRenderer, valueFormat, pointName);
        }

        throw new NoMatchException(new TranslatableMessage("event.valueParse.noValue", pointName));
    }
	
	/**
	 * Get the timestamp from a string using the pattern by matching group 1
	 * @param time
	 * @param timePattern
	 * @param data
	 * @param timeFormat
	 * @param pointName
	 * @return
	 * @throws TranslatableException
	 */
    public static long getValueTime(long time, Pattern timePattern, String data, DateFormat timeFormat, String pointName)
            throws TranslatableException {
    	return getValueTime(time, timePattern, 1, data, timeFormat, pointName);
    }

    /**
     * Get the timestamp from a string using the pattern by matching the provided group number
     * @param time - default time if none found
     * @param timePattern
     * @param group
     * @param data
     * @param timeFormat
     * @param pointName
     * @return
     * @throws TranslatableException
     */
    public static long getValueTime(long time, Pattern timePattern, int group, String data, DateFormat timeFormat, String pointName)
            throws TranslatableException {
        if (data == null)
            throw new TranslatableException(new TranslatableMessage("event.valueParse.noData", pointName));

        // Get the time.
        long valueTime = time;
        if (timePattern != null) {
            Matcher matcher = timePattern.matcher(data);
            if (matcher.find()) {
                String timeStr = matcher.group(group);
                try {
                    valueTime = timeFormat.parse(timeStr).getTime();
                }
                catch (ParseException e) {
                    if (pointName == null)
                        throw new TranslatableException(new TranslatableMessage("event.valueParse.timeParse", timeStr));
                    throw new TranslatableException(new TranslatableMessage("event.valueParse.timeParsePoint", timeStr,
                            pointName));
                }
            }
            else
                throw new TranslatableException(new TranslatableMessage("event.valueParse.noTime", pointName));
        }

        return valueTime;
    }
    
    /**
     * Create Data Value From String
     * @param valueStr
     * @param dataTypeId
     * @param binary0Value
     * @param textRenderer
     * @param valueFormat
     * @param pointName
     * @return
     * @throws TranslatableException
     */
    public static DataValue getValue(String valueStr, int dataTypeId, String binary0Value, TextRenderer textRenderer,
            DecimalFormat valueFormat, String pointName) throws TranslatableException {
        if (dataTypeId == DataTypes.ALPHANUMERIC)
            return new AlphanumericValue(valueStr);

        if (dataTypeId == DataTypes.BINARY)
            return new BinaryValue(!valueStr.equals(binary0Value));

        if (dataTypeId == DataTypes.MULTISTATE) {
            if (textRenderer instanceof MultistateRenderer) {
                List<MultistateValue> multistateValues = ((MultistateRenderer) textRenderer).getMultistateValues();
                for (MultistateValue multistateValue : multistateValues) {
                    if (multistateValue.getText().equalsIgnoreCase(valueStr))
                        return new com.serotonin.m2m2.rt.dataImage.types.MultistateValue(multistateValue.getKey());
                }
            }

            try {
                return com.serotonin.m2m2.rt.dataImage.types.MultistateValue.parseMultistate(valueStr);
            }
            catch (NumberFormatException e) {
                if (pointName == null)
                    throw new TranslatableException(new TranslatableMessage("event.valueParse.textParse", valueStr));
                throw new TranslatableException(new TranslatableMessage("event.valueParse.textParsePoint", valueStr,
                        pointName));
            }
        }

        if (dataTypeId == DataTypes.NUMERIC) {
            try {
                if (valueFormat != null)
                    return new NumericValue(valueFormat.parse(valueStr).doubleValue());
                return NumericValue.parseNumeric(valueStr);
            }
            catch (NumberFormatException e) {
                if (pointName == null)
                    throw new TranslatableException(new TranslatableMessage("event.valueParse.numericParse", valueStr));
                throw new TranslatableException(new TranslatableMessage("event.valueParse.numericParsePoint", valueStr,
                        pointName));
            }
            catch (ParseException e) {
                if (pointName == null)
                    throw new TranslatableException(new TranslatableMessage("event.valueParse.generalParse",
                            e.getMessage(), valueStr));
                throw new TranslatableException(new TranslatableMessage("event.valueParse.generalParsePoint",
                        e.getMessage(), valueStr, pointName));
            }
        }

        return null;
    }
}
