/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.taglib;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.jsp.PageContext;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.AuditEventTypeDefinition;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.SystemEventTypeDefinition;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.controller.ControllerUtils;

public class Functions {
    public static String getHtmlText(DataPointVO point, PointValueTime pointValue) {
        if (point == null)
            return "-";
        String text = point.getTextRenderer().getText(pointValue, TextRenderer.HINT_FULL);
        String colour = point.getTextRenderer().getColour(pointValue);
        return getHtml(colour, text, point.getPointLocator().getDataTypeId() == DataTypes.ALPHANUMERIC);
    }

    public static String getRenderedText(DataPointVO point, PointValueTime pointValue) {
        if (point == null)
            return "-";
        return point.getTextRenderer().getText(pointValue, TextRenderer.HINT_FULL);
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
        return getHtml(colour, text, point.getPointLocator().getDataTypeId() == DataTypes.ALPHANUMERIC);
    }

    private static String getHtml(String colour, String text, boolean detectOverflow) {
        String result;

        if (text != null && detectOverflow && text.length() > 30) {
            text = encodeDQuot(text);
            if (StringUtils.isBlank(colour))
                result = "<input type='text' readonly='readonly' class='ovrflw' value=\"" + text + "\"/>";
            else
                result = "<input type='text' readonly='readonly' class='ovrflw' style='color:" + colour + ";' value=\""
                        + text + "\"/>";
        }
        else {
            if (StringUtils.isBlank(colour))
                result = text;
            else
                result = "<span style='color:" + colour + ";'>" + text + "</span>";
        }

        return result;
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

    public static String escapeScripts(String s) {
        String result = Pattern.compile("<script", Pattern.CASE_INSENSITIVE).matcher(s).replaceAll("&lt;script");
        result = Pattern.compile("</script", Pattern.CASE_INSENSITIVE).matcher(result).replaceAll("&lt;/script");
        return result;
    }

    public static String contextualMessage(String context, ProcessResult result, PageContext page) {
        if (result != null) {
            for (ProcessMessage msg : result.getMessages()) {
                if (context.equals(msg.getContextKey()))
                    return msg.getContextualMessage().translate(ControllerUtils.getTranslations(page));
            }
        }
        return null;
    }

    public static List<String> contextualMessages(String context, ProcessResult result, PageContext page) {
        List<String> msgs = new ArrayList<String>();
        if (result != null) {
            for (ProcessMessage msg : result.getMessages()) {
                if (context.equals(msg.getContextKey()))
                    msgs.add(msg.getContextualMessage().translate(ControllerUtils.getTranslations(page)));
            }
        }
        return msgs;
    }

    public static String genericMessage(ProcessResult result, PageContext page) {
        if (result != null) {
            for (ProcessMessage msg : result.getMessages()) {
                if (msg.getContextKey() == null)
                    return msg.getGenericMessage().translate(ControllerUtils.getTranslations(page));
            }
        }
        return null;
    }

    public static List<String> genericMessages(ProcessResult result, PageContext page) {
        List<String> msgs = new ArrayList<String>();
        if (result != null) {
            for (ProcessMessage msg : result.getMessages()) {
                if (msg.getContextKey() == null)
                    msgs.add(msg.getGenericMessage().translate(ControllerUtils.getTranslations(page)));
            }
        }
        return msgs;
    }

    public static String systemEventTypeLink(String subtype, int ref1, int ref2, PageContext page) {
        SystemEventTypeDefinition def = ModuleRegistry.getSystemEventTypeDefinition(subtype);
        if (def != null)
            return def.getEventListLink(ref1, ref2, ControllerUtils.getTranslations(page));
        return null;
    }

    public static String auditEventTypeLink(String subtype, int ref1, int ref2, PageContext page) {
        AuditEventTypeDefinition def = ModuleRegistry.getAuditEventTypeDefinition(subtype);
        if (def != null)
            return def.getEventListLink(ref1, ref2, ControllerUtils.getTranslations(page));
        return null;
    }

    public static String eventTypeLink(String type, String subtype, int ref1, int ref2, PageContext page) {
        EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(type);
        if (def != null)
            return def.getEventListLink(subtype, ref1, ref2, ControllerUtils.getTranslations(page));
        return null;
    }

    public static String envString(String key, String defaultValue) {
        return Common.envProps.getString(key, defaultValue);
    }

    public static boolean envBoolean(String key, boolean defaultValue) {
        return Common.envProps.getBoolean(key, defaultValue);
    }

    private static String dtfFullMinute = "yyyy/MM/dd HH:mm";
    private static String dtfFullSecond = "yyyy/MM/dd HH:mm:ss";
    private static String dtfLong = "yyyy/MM/dd";
    private static String dtfMed = "MMM dd HH:mm";
    private static String dtfShort = "HH:mm:ss";

    public static String getTime(long time) {
        DateTime valueTime = new DateTime(time);
        DateTime now = new DateTime();

        if (valueTime.getYear() != now.getYear())
            return dtfFormat(time, dtfLong, defaultDTZ());

        if (valueTime.getMonthOfYear() != now.getMonthOfYear() || valueTime.getDayOfMonth() != now.getDayOfMonth())
            return dtfFormat(time, dtfMed, defaultDTZ());

        return dtfFormat(time, dtfShort, defaultDTZ());
    }

    public static String getFullMinuteTime(long time) {
        return dtfFormat(time, dtfFullMinute, defaultDTZ());
    }

    public static String getFullSecondTime(long time) {
        return dtfFormat(time, dtfFullSecond, defaultDTZ());
    }

    public static String getTime(long time, DateTimeZone dtz) {
        DateTime valueTime = new DateTime(time);
        DateTime now = new DateTime();

        if (valueTime.getYear() != now.getYear())
            return dtfFormat(time, dtfLong, dtz);

        if (valueTime.getMonthOfYear() != now.getMonthOfYear() || valueTime.getDayOfMonth() != now.getDayOfMonth())
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
        User user = Common.getUser();
        if (user != null && !StringUtils.isEmpty(user.getTimezone()))
            return DateTimeZone.forID(user.getTimezone());
        return null;
    }

    private static String dtfFormat(long time, String pattern, DateTimeZone dtz) {
        DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
        if (dtz != null)
            dtf = dtf.withZone(dtz);
        return dtf.print(time);
    }
}
