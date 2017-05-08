package com.serotonin.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.db.pair.StringStringPair;

public class TimeZoneUtils {
    public static final String[] TZ_CONTINENTS = { "Africa", "America", "Antarctica", "Arctic", "Asia", "Atlantic",
            "Australia", "Europe", "Indian", "Pacific", };
    private static final String REGEX;
    static {
        StringBuilder sb = null;
        for (String continent : TZ_CONTINENTS) {
            if (sb == null)
                sb = new StringBuilder("^(");
            else
                sb.append("|");
            sb.append(continent);
        }
        sb.append(")/[^/]*");

        REGEX = sb.toString();
    }

    public static List<String> getTimeZoneIds() {
        List<String> result = new ArrayList<String>();
        String[] ids = TimeZone.getAvailableIDs();
        for (String id : ids) {
            if (id.matches(REGEX))
                result.add(id);
        }

        Collections.sort(result);

        return result;
    }

    public static List<StringStringPair> getTimeZoneIdsWithOffset() {
        List<TzSort> tzs = new ArrayList<TimeZoneUtils.TzSort>();

        String[] ids = TimeZone.getAvailableIDs();
        for (String id : ids) {
            if (id.matches(REGEX)) {
                TzSort t = new TzSort();
                t.offset = TimeZone.getTimeZone(id).getRawOffset();
                t.info = new StringStringPair(id, formatOffset(t.offset) + " " + id);
                tzs.add(t);
            }
        }

        Collections.sort(tzs);

        List<StringStringPair> result = new ArrayList<StringStringPair>();
        for (TzSort t : tzs)
            result.add(t.info);
        return result;
    }

    static class TzSort implements Comparable<TzSort> {
        int offset;
        StringStringPair info;

        public int compareTo(TzSort o) {
            if (offset != o.offset)
                return offset - o.offset;
            return info.getValue().compareTo(o.info.getValue());
        }
    }

    public static String formatOffset(int offset) {
        if (offset == 0)
            return "(UTC)";

        String s = "";

        boolean minus = false;
        if (offset < 0) {
            minus = true;
            offset = -offset;
        }

        // Ignore millis and seconds
        offset /= 60000;
        s = ":" + StringUtils.leftPad(Integer.toString(offset % 60), 2, '0');

        offset /= 60;
        s = "(UTC" + (minus ? "-" : "+") + Integer.toString(offset) + s + ")";

        return s;
    }
}
