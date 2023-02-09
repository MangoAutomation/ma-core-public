/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.rt.maint.work.BackupWorkItem;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.joda.time.DateTime;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Matthew Lohbihler
 */
public class DateUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DateUtils.class);
    public static long minus(long time, int periodType, int periods) {
        return minus(new DateTime(time), periodType, periods).getMillis();
    }

    public static DateTime minus(DateTime time, int periodType, int periods) {
        return time.minus(Common.getPeriod(periodType, periods));
    }

    public static long plus(long time, int periodType, int periods) {
        return plus(new DateTime(time), periodType, periods).getMillis();
    }

    public static DateTime plus(DateTime time, int periodType, int periods) {
        return time.plus(Common.getPeriod(periodType, periods));
    }

    public static long truncate(long time, int periodType) {
        return truncateDateTime(new DateTime(time), periodType, 1).getMillis();
    }

    public static long truncate(long time, int periodType, int periods) {
        return truncateDateTime(new DateTime(time), periodType, periods).getMillis();
    }

    public static DateTime truncateDateTime(DateTime time, int periodType) {
        return truncateDateTime(time, periodType, 1);
    }

    public static DateTime truncateDateTime(DateTime time, int periodType, int periods) {
        if (periodType == TimePeriods.MILLISECONDS) {
            if (periods > 1)
                time = time.minusMillis(time.getMillisOfSecond() % periods);
        }
        else if (periodType == TimePeriods.SECONDS) {
            if (periods > 1)
                time = time.minusSeconds(time.getSecondOfMinute() % periods);
            time = time.millisOfSecond().withMinimumValue();
        }
        else if (periodType == TimePeriods.MINUTES) {
            if (periods > 1)
                time = time.minusMinutes(time.getMinuteOfHour() % periods);
            time = time.secondOfMinute().withMinimumValue();
            time = time.millisOfSecond().withMinimumValue();
        }
        else if (periodType == TimePeriods.HOURS) {
            if (periods > 1)
                time = time.minusHours(time.getHourOfDay() % periods);
            time = time.minuteOfHour().withMinimumValue();
            time = time.secondOfMinute().withMinimumValue();
            time = time.millisOfSecond().withMinimumValue();
        }
        else if (periodType == TimePeriods.DAYS) {
            if (periods > 1)
                time = time.minusDays(time.getDayOfYear() % periods);
            time = time.withTimeAtStartOfDay();
        }
        else if (periodType == TimePeriods.WEEKS) {
            if (periods > 1)
                time = time.minusWeeks(time.getWeekOfWeekyear() % periods);
            time = time.dayOfWeek().withMinimumValue();
            time = time.withTimeAtStartOfDay();
        }
        else if (periodType == TimePeriods.MONTHS) {
            if (periods > 1)
                time = time.minusMonths(time.getMonthOfYear() % periods);
            time = time.dayOfMonth().withMinimumValue();
            time = time.withTimeAtStartOfDay();
        }
        else if (periodType == TimePeriods.YEARS) {
            if (periods > 1)
                time = time.minusYears(time.getYearOfCentury() % periods);
            time = time.dayOfYear().withMinimumValue();
            time = time.withTimeAtStartOfDay();
        }
        return time;
    }

    public static long next(long time, int periodType) {
        return minus(truncateDateTime(new DateTime(time), periodType), periodType, -1).getMillis();
    }

    public static TranslatableMessage getDuration(long duration) {
        if (duration < 1000)
            return new TranslatableMessage("common.duration.millis", duration);

        if (duration < 10000) {
            String s = "" + (duration / 1000) + '.';
            s += (int) (((double) (duration % 1000)) / 10 + 0.5);
            return new TranslatableMessage("common.duration.seconds", s);
        }

        if (duration < 60000) {
            String s = "" + (duration / 1000) + '.';
            s += (int) (((double) (duration % 1000)) / 100 + 0.5);
            return new TranslatableMessage("common.duration.seconds", s);
        }

        // Convert to seconds
        duration /= 1000;

        if (duration < 600)
            return new TranslatableMessage("common.duration.minSec", duration / 60, duration % 60);

        // Convert to minutes
        duration /= 60;

        if (duration < 60)
            return new TranslatableMessage("common.duration.minutes", duration);

        if (duration < 1440)
            return new TranslatableMessage("common.duration.hourMin", duration / 60, duration % 60);

        // Convert to hours
        duration /= 60;

        return new TranslatableMessage("common.duration.hours", duration);
    }

    public static TranslatableMessage getDurationCourse(long duration) {
        if (duration < 60000)
            return new TranslatableMessage("common.tp.ltDescription", 1, new TranslatableMessage("common.tp.minute"));

        // Convert to minutes
        duration /= 60000;

        if (duration < 60)
            return new TranslatableMessage("common.tp.description", duration, new TranslatableMessage(
                    "common.tp.minutes"));

        // Convert to hours
        duration /= 60;

        if (duration < 24)
            return new TranslatableMessage("common.tp.gtDescription", duration, new TranslatableMessage(
                    "common.tp.hours"));

        // Convert to days
        duration /= 24;
        return new TranslatableMessage("common.tp.gtDescription", duration, new TranslatableMessage("common.tp.days"));
    }

    /**
     * Ticket RAD-2443 This is trying to fix the issue cause by a system language change so some dates were store in some language
     * and when those are read a parse exceptions occurred. So the solution is to store lastRunDate as long number and to read it we have 3 options:
     * 1) Try to parse with the current system language.
     * 2) Try to read the date from a long.
     * 3) Try to parse the date, base on java available locales. In this case it will store the date as long so it will be faster to find it next time.
     * @param lastRunDateString this field is the last execution date stored into the database.
     * 1) lastRunDateString could come like Nov-07-2023_214128
     * 2) lastRunDateString could come like 1699414888000
     * 3) lastRunDateString could come like Kas-07-2023_214128
     * @return parsed date.
     */
    public static Date getLastRunDate(String lastRunDateString) {
        try {
            // first try will be trying to parse lastRunDate with current system language; example: Nov-07-2023_214128 to Date
            return new SimpleDateFormat(BackupWorkItem.BACKUP_DATE_FORMAT).parse(lastRunDateString);
        } catch (ParseException parseException) {
            try {
                // second try will be trying to parse Date from long; example: 1699414888000
                return new Date(Long.parseLong(lastRunDateString));
            } catch (Exception e) {
                // third try will be trying to parse lastRunDate from different languages; example: Kas-07-2023_214128 to Date
                Date date = figureOutDateLanguage(lastRunDateString);
                if(date == null) {
                    LOG.warn("Failed to parse last backup date, using Jan 1 1970.", e);
                    date = new Date();
                }
                return date;
            }
        }
    }

    private static Date figureOutDateLanguage(String dateString) {
        Locale locales[] = DateFormat.getAvailableLocales();
        Date date = null;
        for (Locale locale : locales) {
            date = formatStringDate(dateString, locale);
            if (date != null) {
                // Store the last successful backup time
                SystemSettingsDao.getInstance().setValue(SystemSettingsDao.BACKUP_LAST_RUN_SUCCESS,
                    String.valueOf(date.getTime()));
                break;
            }
        }
        return date;
    }

    private static Date formatStringDate(String dateString, Locale locale) {
        try {
            return new SimpleDateFormat(BackupWorkItem.BACKUP_DATE_FORMAT, locale).parse(dateString);
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        long time = Common.timer.currentTimeMillis();
        System.out.println(new DateTime(time));
        System.out.println(new DateTime(next(time, Common.TimePeriods.SECONDS)));
        System.out.println(new DateTime(next(time, Common.TimePeriods.MINUTES)));
        System.out.println(new DateTime(next(time, Common.TimePeriods.HOURS)));
        System.out.println(new DateTime(next(time, Common.TimePeriods.DAYS)));
        System.out.println(new DateTime(next(time, Common.TimePeriods.WEEKS)));
        System.out.println(new DateTime(next(time, Common.TimePeriods.MONTHS)));
        System.out.println(new DateTime(next(time, Common.TimePeriods.YEARS)));
        System.out.println();

        time = next(time, Common.TimePeriods.YEARS);
        System.out.println(new DateTime(time));
        System.out.println(new DateTime(next(time, Common.TimePeriods.SECONDS)));
        System.out.println(new DateTime(next(time, Common.TimePeriods.MINUTES)));
        System.out.println(new DateTime(next(time, Common.TimePeriods.HOURS)));
        System.out.println(new DateTime(next(time, Common.TimePeriods.DAYS)));
        System.out.println(new DateTime(next(time, Common.TimePeriods.WEEKS)));
        System.out.println(new DateTime(next(time, Common.TimePeriods.MONTHS)));
        System.out.println(new DateTime(next(time, Common.TimePeriods.YEARS)));
    }
}
