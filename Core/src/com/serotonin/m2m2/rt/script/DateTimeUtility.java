/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Class for use in Javascript context for Date/Time Manipulation
 * 
 * 
 * @author tpacker
 *
 */
public class DateTimeUtility {

	
	public static final String CONTEXT_KEY = "DateTimeUtility";

	/**
	 * Using a timezoneId such as Europe/Rome
	 * return the Milliseconds Offset for UTC for the given timestamp
	 * 
	 * @param timezoneId - Example "Europe/Rome"
	 * @param timestamp - ms timestamp
     */
	public int getOffset(String timezoneId, long timestamp){
		
		DateTimeZone tz = DateTimeZone.forID(timezoneId);
		
		return tz.getOffset(timestamp);
	}
	

	/**
	 * Get the Timezone ID for where the script is being run
	 * @return Timezone ID such as "Europe/Rome"
	 */
	public String getTimezone(){
		DateTimeZone tz = DateTimeZone.getDefault();
		return tz.getID();
	}
	
	/**
	 * Get the UTC Offset of where the script is being run in Milliseconds for a given time
     */
	public int getUtcOffset(long timestamp){
		DateTimeZone tz = DateTimeZone.getDefault();
		return tz.getOffset(timestamp);
	}

	/**
	 * Parse a string date with the given format into Milliseconds since epoch
	 * 
	 * @param format - Format of Date String see @DateTimeFormatter
	 * @param dateString - Input Date String
	 * @param timezoneId - Id of timezone ie. "Europe/Rome"
     */
	public long parseDate(String format, String dateString, String timezoneId){
		DateTimeFormatter formatter = DateTimeFormat.forPattern(format);
		
		DateTimeZone tz = DateTimeZone.forID(timezoneId);
		DateTime thisDate = formatter.parseDateTime(dateString).withZone(tz);
		
		return thisDate.getMillis();
		
	}
	
	/**
	 * Create a Date String using the given timestamp, format and timezone ID
	 * @param format - Format of Date String see @DateTimeFormatter
	 * @param timestamp - ms since epoch
	 * @param timezoneId - Id of timezone ie. "Europe/Rome"
     */
	public String formatDate(String format, long timestamp, String timezoneId){
		DateTimeZone tz = DateTimeZone.forID(timezoneId);
		DateTimeFormatter formatter = DateTimeFormat.forPattern(format).withZone(tz);
		return formatter.print(timestamp);
		
	}
	
	public String getHelp(){
		return toString();
	}
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		builder.append("offset(timezoneId, timestamp): UTC offset millis, \n");
		builder.append("timezone(): ").append(getTimezone()).append(", \n");
		builder.append("utcOffset(timestamp): UTC offset millis, \n");
		builder.append("parseDate(format, dateString, timezoneId): timestamp, \n");
		builder.append("formatDate(format, timestamp, timezoneId): dateString, \n");
		builder.append(" }\n");
		return builder.toString();
	}
}
