/*
    Copyright (C) 2013 Deltamation Software All rights reserved.
    @author Terry Packer
 */
package com.serotonin.m2m2;

import java.awt.Font;
import java.io.File;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import com.infiniteautomation.mango.db.query.SortOption;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.timer.CronTimerTrigger;

/**
 * Copyright (C) 2013 Deltamation Software. All Rights Reserved.
 * @author Terry Packer
 *
 */
public class DeltamationCommon {
    public static final String decimalFormat = "0.00%";
    public static final String dateFormat = "yyyy-MM-dd HH:mm:ss";
    public static final String hourlyDurationFormat = "HH:mm:ss";
    public static final String dailyDurationFormat = "0.00";
    
    private static final DecimalFormat percentageFormat = new DecimalFormat(decimalFormat);
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
    private static final SimpleDateFormat durationFormatter = new SimpleDateFormat(hourlyDurationFormat);
    {
        durationFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    private static final DecimalFormat dayFormatter = new DecimalFormat(dailyDurationFormat);

    /**
     * Format a Number as x.xx%
     * @param value
     * @return
     */
    public static String formatPercentage(Number value){
        return percentageFormat.format(value);
    }
    
    /**
     * Format a Duration as a Translatable Message:
     * HH:mm:ss or
     * if > 1 Day x.x Days
     * @param value
     * @return
     */
    public static TranslatableMessage formatDurationIntoTranslatableMessage(Long value) {
        //TODO Implement this using JodaTime (It's already a dep for the core).
        Period duration = new Period((long)value);
    	if (value < 86400000) {
    		PeriodFormatter formatter = new PeriodFormatterBuilder()
    			.minimumPrintedDigits(2)
    			.printZeroAlways()
    			.appendHours().appendSuffix(":")
    			.appendMinutes().appendSuffix(":")
    			.appendSeconds()
    			.toFormatter();
    		return new TranslatableMessage("common.durationStd",formatter.print(duration));
    		//return new TranslatableMessage("common.durationStd",String.format("%d:%02d:%02d", value/3600000L, (value%3600000L)/60000L, ((value%3600000L)%60000L)/1000L));
            //This seems buggy? The spreadsheets don't match the screen? return new TranslatableMessage("delta.util.view.durationStd", durationFormatter.format(new Date(value)));
        }else {
            return new TranslatableMessage("common.durationDays", dayFormatter.format(value/86400000D));
        }
    }
    
    /**
     * Format a duration as a string: HH:mm:ss or
     * if > 1 Day x.x Days
     * @param value
     * @return
     */
    public static String formatDuration(Long value) {
        TranslatableMessage message = formatDurationIntoTranslatableMessage(value);
        return TranslatableMessage.translate(Common.getTranslations(), message.getKey(), message.getArgs());
    }
    
	/**
	 * HH:mm:ss or
	 * if > 1 Day x.x Days
	 * 
	 * @param durationString
	 * @return
	 */
	public static long unformatDuration(String durationString) throws NumberFormatException{
		if(durationString.contains(":")){
			String parts[] = durationString.split(":");
			Double ms = Double.parseDouble(parts[2]) * 1000; //Seconds
			ms += Double.parseDouble(parts[1]) * 60000; //Minutes
			ms += Double.parseDouble(parts[0]) * 3600000; //Hrs
			return ms.longValue();
		}else if(durationString.contains("Days")){
			String parts[] = durationString.split("Days");
			Double days = Double.parseDouble(parts[0]);
			days = days * 86400000;
			return days.longValue();
		}else{
			throw new NumberFormatException("Invalid Format for Duration. HH:mm:ss or x.x Days"); //Not possible
		}
	}
	
	
	
	
    /**
     * Format a Unix Date as: yyyy-MM-dd HH:mm:ss
     * @param value
     * @return
     */
    public static String formatDate(Long value){
        return dateFormatter.format(new Date(value));
    }

    /**
     * Get the Default Font for the Module
     * @return
     */
    public static Font getDefaultFont() {
        return new Font("SansSerif",Font.PLAIN,10);
    }
    
    /**
     * Returns the override path if it exists, otherwise returns the base path
     * 
     * @param path relative to Mango base directory
     * @param webPath if true then relative to the Mango web directory
     * @return
     */
    public static String getOverriddenPath(String path, boolean webPath) {
        path = path.startsWith("/") ? path : "/" + path;
        if (webPath) {
            path = "/web" + path;
        }
        
        File location = new File(Common.MA_HOME + "/overrides" + path);
        if (!location.exists()) {
            location = new File(Common.MA_HOME + path);
        }
        if (!location.exists()) {
            return null;
        }
        
        return location.getAbsolutePath();
    }
    
    /**
     * Returns the override path if it exists, otherwise returns the base path
     * 
     * @param path relative to Mango base directory
     * @return
     */
    public static String getOverriddenPath(String path) {
        return getOverriddenPath(path, false);
    }
    
    /**
     * Sorts a list of Bean objects by multiple SortOptions
     * @param list
     * @param sort
     */
    @SuppressWarnings("unchecked")
    public static void beanSort(List<?> list, SortOption... sort) {
        ComparatorChain cc = new ComparatorChain();
        
        // always sort so that the offset/limit work as intended
        if (sort.length == 0) {
            sort = new SortOption[] {new SortOption("id", false)};
        }
        
        // TODO catch exceptions?
        NullComparator nullComparator = new NullComparator();
        for (SortOption option : sort) {
            if (option == null)
                continue;
            cc.addComparator(new BeanComparator(option.getAttribute(), nullComparator), option.isDesc());
        }
        Collections.sort(list, cc);
    }
    
    /**
     * Limit and offset a list
     * @param list
     * @param start
     * @param count
     * @return
     */
    public static <T> List<T> limitList(List<T> list, Integer start, Integer count) {
        List<T> result;
        try {
            List<T> offsetList = list.subList(start, list.size());
            if (offsetList.size() <= count)
                return offsetList;
            
            result = offsetList.subList(0, count);
        }
        catch (Exception e) {
            result = new ArrayList<T>();
        }
        
        return result;
    }
    
    public static CronTimerTrigger getCronTrigger(int periodType) {
        return getCronTrigger(periodType, 0);
    }
    
    public static CronTimerTrigger getCronTrigger(int periodType, int delaySeconds) {
        return getCronTrigger(1, periodType, 0);
    }
    
    /**
     * Delta implementation of com.serotonin.m2m2.Common.getCronTrigger()
     * Adds an "every" parameter and overloaded methods
     * 
     * @param every
     * @param periodType
     * @param delaySeconds
     * @return
     */
    public static CronTimerTrigger getCronTrigger(int every, int periodType, int delaySeconds) {
        // could use constraint
        if (every <= 0) {
            every = 1;
        }
        
        int delayMinutes = 0;
        if (delaySeconds >= 60) {
            delayMinutes = delaySeconds / 60;
            delaySeconds %= 60;

            if (delayMinutes >= 60)
                delayMinutes = 59;
        }

        try {
            switch (periodType) {
            case TimePeriods.MILLISECONDS:
                throw new ShouldNeverHappenException("Can't create a cron trigger for milliseconds");
            case TimePeriods.SECONDS:
                return new CronTimerTrigger("*/" + Integer.toString(every) +" * * * * ?");
            case TimePeriods.MINUTES:
                return new CronTimerTrigger(delaySeconds + " */" + Integer.toString(every) + " * * * ?");
            case TimePeriods.HOURS:
                return new CronTimerTrigger(delaySeconds + " " + delayMinutes + " */" + Integer.toString(every) + " * * ?");
            case TimePeriods.DAYS:
                return new CronTimerTrigger(delaySeconds + " " + delayMinutes + " 0 */" + Integer.toString(every) + " * ?");
            case TimePeriods.WEEKS:
                return new CronTimerTrigger(delaySeconds + " " + delayMinutes + " 0 ? * MON"); // TODO cant do every on weeks
            case TimePeriods.MONTHS:
                return new CronTimerTrigger(delaySeconds + " " + delayMinutes + " 0 1 */" + Integer.toString(every) + " ?");
            case TimePeriods.YEARS:
                return new CronTimerTrigger(delaySeconds + " " + delayMinutes + " 0 1 JAN ? */" + Integer.toString(every));
            default:
                throw new ShouldNeverHappenException("Invalid cron period type: " + periodType);
            }
        }
        catch (ParseException e) {
            throw new ShouldNeverHappenException(e);
        }
    }
    
    /**
     * @param period
     * @param endTime
     * @return
     */
    public static long calcStartTime(int period, long endTime) {
        // Create the date range
        long startTime = endTime - (long) (86400000L * (long) period); //24Hrs
        if (startTime < 0)
            startTime = 0;
        return startTime;
    }
    
    public static Interval getInterval(TimePeriodDescriptor periodEnum) {
        return getInterval(periodEnum, 0 , 0);
    }
    
    public static Interval getInterval(TimePeriodDescriptor periodEnum, long startIn, long endIn) {
        long startOut, endOut;
        
        switch (periodEnum) {
        case FIXED_TO_FIXED:
            startOut = startIn;
            endOut = endIn;
            break;
        case FIXED_TO_NOW:
            startOut = startIn;
            endOut = Common.backgroundProcessing.currentTimeMillis();
            break;
        case INCEPTION_TO_FIXED:
            startOut = 0;
            endOut = endIn;
            break;
        case INCEPTION_TO_NOW:
            startOut = 0;
            endOut = Common.backgroundProcessing.currentTimeMillis();
            break;
        default:
            endOut = Common.backgroundProcessing.currentTimeMillis();
            startOut = endOut - (long) (86400000L * (long) periodEnum.getCode()); //24Hrs
            break;
        }
        
        return new Interval(startOut, endOut);
    }
    
    public static int parsePoint(JsonObject jsonObject, String name) throws TranslatableJsonException {
        DataPointDao points = DataPointDao.instance;
        
        String xid = jsonObject.getString(name);
        DataPointVO point = points.getDataPoint(xid);
        if (point == null) {
            throw new TranslatableJsonException("validate.pointMissing", name, xid);
        }
        return point.getId();
    }
    
    public static DataPointVO validatePoint(int pointId, String name, ProcessResult response) {
        return validatePoint(pointId, name, response, null, false);
    }
    
    public static DataPointVO validatePoint(int pointId, String name, ProcessResult response, Integer dataType) {
        return validatePoint(pointId, name, response, dataType, false);
    }
    
    public static DataPointVO validatePoint(int pointId, String name, ProcessResult response, Integer dataType, boolean requireSettable) {
        DataPointDao points = DataPointDao.instance;
        
        DataPointVO point = points.getDataPoint(pointId);
        if (point == null) {
            response.addContextualMessage(name, "validate.noPoint");
            return null;
        }
        
        if (requireSettable && !point.getPointLocator().isSettable()) {
            response.addContextualMessage(name, "validate.pointNotSettable",
                    point.getName());
        }
        
        if (dataType != null && point.getPointLocator().getDataTypeId() != dataType) {
            response.addContextualMessage(name, "validate.pointWrongType",
                    point.getName());
        }
        
        return point;
    }

}
