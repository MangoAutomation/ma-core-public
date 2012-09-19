/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.directwebremoting.WebContextFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.util.DateUtils;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.pair.LongPair;
import com.serotonin.m2m2.web.dwr.beans.EventExportDefinition;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

public class EventsDwr extends BaseDwr {
    private static final int PAGE_SIZE = 50;
    private static final int PAGINATION_RADIUS = 3;

    public static final String STATUS_ALL = "*";
    public static final String STATUS_ACTIVE = "A";
    public static final String STATUS_RTN = "R";
    public static final String STATUS_NORTN = "N";

    public static final int DATE_RANGE_TYPE_NONE = 1;
    public static final int DATE_RANGE_TYPE_RELATIVE = 2;
    public static final int DATE_RANGE_TYPE_SPECIFIC = 3;

    public static final int RELATIVE_DATE_TYPE_PREVIOUS = 1;
    public static final int RELATIVE_DATE_TYPE_PAST = 2;

    @DwrPermission(user = true)
    public ProcessResult search(int eventId, String eventType, String status, int alarmLevel, String keywordStr,
            int dateRangeType, int relativeDateType, int previousPeriodCount, int previousPeriodType,
            int pastPeriodCount, int pastPeriodType, boolean fromNone, int fromYear, int fromMonth, int fromDay,
            int fromHour, int fromMinute, int fromSecond, boolean toNone, int toYear, int toMonth, int toDay,
            int toHour, int toMinute, int toSecond, int page, Date date) {
        ProcessResult response = new ProcessResult();
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        User user = Common.getUser(request);

        int from = PAGE_SIZE * page;
        int to = from + PAGE_SIZE;

        // This date is for the "jump to date" functionality. The date is set for the top of the day, which will end up 
        // excluding all of the events for that day. So, // we need to add 1 day to it.
        if (date != null)
            date = DateUtils.minus(new DateTime(date.getTime()), Common.TimePeriods.DAYS, -1).toDate();

        LongPair dateRange = getDateRange(dateRangeType, relativeDateType, previousPeriodCount, previousPeriodType,
                pastPeriodCount, pastPeriodType, fromNone, fromYear, fromMonth, fromDay, fromHour, fromMinute,
                fromSecond, toNone, toYear, toMonth, toDay, toHour, toMinute, toSecond, user.getDateTimeZoneInstance());

        EventDao eventDao = new EventDao();
        List<EventInstance> results = eventDao.search(eventId, eventType, status, alarmLevel, getKeywords(keywordStr),
                dateRange.getL1(), dateRange.getL2(), user.getId(), getTranslations(), from, to, date);

        Map<String, Object> model = new HashMap<String, Object>();
        int searchRowCount = eventDao.getSearchRowCount();
        int pages = (int) Math.ceil(((double) searchRowCount) / PAGE_SIZE);

        if (date != null) {
            int startRow = eventDao.getStartRow();
            if (startRow == -1)
                page = pages - 1;
            else
                page = eventDao.getStartRow() / PAGE_SIZE;
        }

        if (pages > 1) {
            model.put("displayPagination", true);

            if (page - PAGINATION_RADIUS > 1)
                model.put("leftEllipsis", true);
            else
                model.put("leftEllipsis", false);

            int linkFrom = page + 1 - PAGINATION_RADIUS;
            if (linkFrom < 2)
                linkFrom = 2;
            model.put("linkFrom", linkFrom);
            int linkTo = page + 1 + PAGINATION_RADIUS;
            if (linkTo >= pages)
                linkTo = pages - 1;
            model.put("linkTo", linkTo);

            if (page + PAGINATION_RADIUS < pages - 2)
                model.put("rightEllipsis", true);
            else
                model.put("rightEllipsis", false);

            model.put("numberOfPages", pages);
        }
        else
            model.put("displayPagination", false);

        model.put("events", results);
        model.put("page", page);
        model.put("pendingEvents", false);

        response.addData("content", generateContent(request, "eventList.jsp", model));
        response.addData("resultCount", new TranslatableMessage("events.search.resultCount", searchRowCount));

        return response;
    }

    @DwrPermission(user = true)
    public void exportEvents(int eventId, String eventType, String status, int alarmLevel, String keywordStr,
            int dateRangeType, int relativeDateType, int previousPeriodCount, int previousPeriodType,
            int pastPeriodCount, int pastPeriodType, boolean fromNone, int fromYear, int fromMonth, int fromDay,
            int fromHour, int fromMinute, int fromSecond, boolean toNone, int toYear, int toMonth, int toDay,
            int toHour, int toMinute, int toSecond) {
        User user = Common.getUser();
        LongPair dateRange = getDateRange(dateRangeType, relativeDateType, previousPeriodCount, previousPeriodType,
                pastPeriodCount, pastPeriodType, fromNone, fromYear, fromMonth, fromDay, fromHour, fromMinute,
                fromSecond, toNone, toYear, toMonth, toDay, toHour, toMinute, toSecond, user.getDateTimeZoneInstance());

        EventExportDefinition def = new EventExportDefinition(eventId, eventType, status, alarmLevel,
                getKeywords(keywordStr), dateRange.getL1(), dateRange.getL2(), user.getId());

        Common.getUser().setEventExportDefinition(def);
    }

    private String[] getKeywords(String keywordStr) {
        String[] keywordArr = keywordStr.split("\\s+");
        List<String> keywords = new ArrayList<String>();
        for (String s : keywordArr) {
            if (!StringUtils.isBlank(s))
                keywords.add(s);
        }

        if (keywords.isEmpty())
            keywordArr = null;
        else {
            keywordArr = new String[keywords.size()];
            keywords.toArray(keywordArr);
        }

        return keywordArr;
    }

    private LongPair getDateRange(int dateRangeType, int relativeDateType, int previousPeriodCount,
            int previousPeriodType, int pastPeriodCount, int pastPeriodType, boolean fromNone, int fromYear,
            int fromMonth, int fromDay, int fromHour, int fromMinute, int fromSecond, boolean toNone, int toYear,
            int toMonth, int toDay, int toHour, int toMinute, int toSecond, DateTimeZone dtz) {
        LongPair range = new LongPair(-1, -1);

        if (dateRangeType == DATE_RANGE_TYPE_RELATIVE) {
            if (relativeDateType == RELATIVE_DATE_TYPE_PREVIOUS) {
                DateTime dt = DateUtils.truncateDateTime(new DateTime(dtz), previousPeriodType);
                range.setL2(dt.getMillis());
                dt = DateUtils.minus(dt, previousPeriodType, previousPeriodCount);
                range.setL1(dt.getMillis());
            }
            else {
                DateTime dt = new DateTime(dtz);
                range.setL2(dt.getMillis());
                dt = DateUtils.minus(dt, pastPeriodType, pastPeriodCount);
                range.setL1(dt.getMillis());
            }
        }
        else if (dateRangeType == DATE_RANGE_TYPE_SPECIFIC) {
            if (!fromNone) {
                DateTime dt = new DateTime(fromYear, fromMonth, fromDay, fromHour, fromMinute, fromSecond, 0, dtz);
                range.setL1(dt.getMillis());
            }

            if (!toNone) {
                DateTime dt = new DateTime(toYear, toMonth, toDay, toHour, toMinute, toSecond, 0, dtz);
                range.setL2(dt.getMillis());
            }
        }

        return range;
    }
}
