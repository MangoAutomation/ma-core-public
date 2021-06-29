/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util;

import java.time.ZoneId;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * This class was ported from the legacy UI as it is used in a few areas still
 * @author Terry Packer
 *
 */
public class Functions {

    public static final String decimalFormat = "0.00%";
    public static final String WHITESPACE_REGEX = "\\s+?";
    public static final Pattern WHITESPACE_PATTERN = Pattern.compile(WHITESPACE_REGEX);

    public static String quotEncode(String s) {
        if (s == null)
            return null;
        return s.replaceAll("'", "\\\\'");
    }

    public static String dquotEncode(String s) {
        if (s == null)
            return null;
        return s.replaceAll("\"", "\\\\\"");
    }

    public static String scriptEncode(String s) {
        if (s == null)
            return null;
        return s.replaceAll("</script>", "&lt;/script>");
    }

    public static String crlfToBr(String s) {
        return s.replaceAll("\r\n", "<br/>");
    }

    public static String lfToBr(String s) {
        return s.replaceAll("\n", "<br/>");
    }

    public static String escapeWhitespace(String s) {
        if (s == null)
            return null;
        return WHITESPACE_PATTERN.matcher(s).replaceAll("&nbsp;");
    }

    public static String escapeLessThan(String s) {
        if (s == null)
            return null;
        s = s.replaceAll("&", "&amp;");
        return s.replaceAll("<", "&lt;");
    }

    public static String escapeQuotes(String s) {
        if (s == null)
            return null;
        return s.replaceAll("\\'", "\\\\'");
    }

    public static String escapeHash(String s) {
        if (s == null)
            return null;
        return s.replaceAll("#", "%23");
    }


    public static String getHtmlText(DataPointVO point, PointValueTime pointValue) {
        if (point == null)
            return "-";
        String text = point.getTextRenderer().getText(pointValue, TextRenderer.HINT_FULL);
        String colour = point.getTextRenderer().getColour(pointValue);
        return getHtml(colour, text,
                point.getPointLocator().getDataTypeId() == DataTypes.ALPHANUMERIC);
    }

    public static String getRenderedText(DataPointVO point, PointValueTime pointValue) {
        if (point == null)
            return "-";
        return point.getTextRenderer().getText(pointValue, TextRenderer.HINT_FULL);
    }

    public static String getIntegralText(DataPointVO point, double integralValue) {
        if (point == null || Double.valueOf(integralValue).isNaN())
            return "-";
        String result =
                point.createIntegralRenderer().getText(integralValue, TextRenderer.HINT_SPECIFIC);
        if (!StringUtils.isBlank(result))
            return encodeDQuot(result);
        return result;
    }

    public static String getRawText(DataPointVO point, PointValueTime pointValue) {
        if (point == null)
            return "-";
        String result = point.getTextRenderer().getText(pointValue, TextRenderer.HINT_RAW);
        if (!StringUtils.isBlank(result))
            return encodeDQuot(result);
        return result;
    }

    public static String getHtmlTextValue(DataPointVO point, DataValue value) {
        if (point == null)
            return "-";
        return getHtmlTextValue(point, value, TextRenderer.HINT_FULL);
    }

    public static String getSpecificHtmlTextValue(DataPointVO point, double value) {
        if (point == null)
            return "-";
        return getHtmlTextValue(point, new NumericValue(value), TextRenderer.HINT_SPECIFIC);
    }

    private static String getHtmlTextValue(DataPointVO point, DataValue value, int hint) {
        if (point == null)
            return "-";
        String text = point.getTextRenderer().getText(value, hint);
        String colour = point.getTextRenderer().getColour(value);
        return getHtml(colour, text,
                point.getPointLocator().getDataTypeId() == DataTypes.ALPHANUMERIC);
    }

    private static String getHtml(String colour, String text, boolean detectOverflow) {
        String result;
        if (text != null) {
            text = encodeHtml(text);
            if (detectOverflow && text.length() > 30) {
                text = encodeDQuot(text);
                if (StringUtils.isBlank(colour))
                    result = "<input type='text' readonly='readonly' class='ovrflw' value=\"" + text
                    + "\"/>";
                else
                    result = "<input type='text' readonly='readonly' class='ovrflw' style='color:"
                            + colour + ";' value=\"" + text + "\"/>";
            } else {
                if (StringUtils.isBlank(colour))
                    result = text;
                else
                    result = "<span style='color:" + colour + ";'>" + text + "</span>";
            }
            return result;
        }
        return null;
    }

    public static String getTime(PointValueTime pointValue) {
        if (pointValue != null)
            return getTime(pointValue.getTime());
        return null;
    }

    public static String padZeros(int i, int len) {
        return StringUtils.leftPad(Integer.toString(i), len, '0');
    }

    public static String encodeDQuot(String s) {
        return s.replaceAll("\"", "&quot;");
    }

    public static String encodeHtml(String s) {
        return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;");
    }

    public static String escapeScripts(String s) {
        String result = Pattern.compile("<script", Pattern.CASE_INSENSITIVE).matcher(s)
                .replaceAll("&lt;script");
        result = Pattern.compile("</script", Pattern.CASE_INSENSITIVE).matcher(result)
                .replaceAll("&lt;/script");
        return result;
    }

    public static String envString(String key, String defaultValue) {
        return Common.envProps.getString(key, defaultValue);
    }

    public static boolean envBoolean(String key, boolean defaultValue) {
        return Common.envProps.getBoolean(key, defaultValue);
    }

    private static String dtfFullMinute = "yyyy/MM/dd HH:mm";
    private static String dtfFullSecond = "yyyy/MM/dd HH:mm:ss";
    private static String dtfFullMilliSecond = "yyyy/MM/dd HH:mm:ss.SSS";
    private static String dtfLong = "yyyy/MM/dd";
    private static String dtfMed = "MMM dd HH:mm";
    private static String dtfShort = "HH:mm:ss";

    public static String getTime(long time) {
        DateTime valueTime = new DateTime(time);
        DateTime now = new DateTime();

        if (valueTime.getYear() != now.getYear())
            return dtfFormat(time, dtfLong, defaultDTZ());

        if (valueTime.getMonthOfYear() != now.getMonthOfYear()
                || valueTime.getDayOfMonth() != now.getDayOfMonth())
            return dtfFormat(time, dtfMed, defaultDTZ());

        return dtfFormat(time, dtfShort, defaultDTZ());
    }

    public static String getFullMinuteTime(long time) {
        return dtfFormat(time, dtfFullMinute, defaultDTZ());
    }

    public static String getFullSecondTime(long time) {
        return dtfFormat(time, dtfFullSecond, defaultDTZ());
    }

    public static String getFullMilliSecondTime(long time) {
        return dtfFormat(time, dtfFullMilliSecond, defaultDTZ());
    }

    public static String getTime(long time, DateTimeZone dtz) {
        DateTime valueTime = new DateTime(time);
        DateTime now = new DateTime();

        if (valueTime.getYear() != now.getYear())
            return dtfFormat(time, dtfLong, dtz);

        if (valueTime.getMonthOfYear() != now.getMonthOfYear()
                || valueTime.getDayOfMonth() != now.getDayOfMonth())
            return dtfFormat(time, dtfMed, dtz);

        return dtfFormat(time, dtfShort, dtz);
    }

    public static String getFullMinuteTime(long time, DateTimeZone dtz) {
        return dtfFormat(time, dtfFullMinute, dtz);
    }

    public static String getFullSecondTime(long time, DateTimeZone dtz) {
        return dtfFormat(time, dtfFullSecond, dtz);
    }

    private static DateTimeZone defaultDTZ() {
        try {
            PermissionHolder user = Common.getUser();
            return DateTimeZone.forID(user.getZoneId().getId());
        } catch(PermissionException e) {
            return DateTimeZone.forID(ZoneId.systemDefault().getId());
        }
    }

    private static String dtfFormat(long time, String pattern, DateTimeZone dtz) {
        DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
        if (dtz != null)
            dtf = dtf.withZone(dtz);
        return dtf.print(time);
    }
}
