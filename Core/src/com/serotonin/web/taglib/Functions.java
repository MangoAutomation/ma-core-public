/*
 * Created on 26-Jul-2006
 */
package com.serotonin.web.taglib;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import com.serotonin.ShouldNeverHappenException;

public class Functions {
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+?");

    public static int size(Object o) throws JspException {
        if (o instanceof Collection<?>)
            return ((Collection<?>) o).size();

        if (o.getClass().isArray())
            return ((Object[]) o).length;

        throw new JspException("Object of type " + o.getClass().getName() + " not implemented");
    }

    public static Object get(Object o, int index) throws JspException {
        if (index < 0)
            return null;

        if (o instanceof List<?>) {
            List<?> list = (List<?>) o;
            if (index >= list.size())
                return null;
            return list.get(index);
        }

        if (o.getClass().isArray()) {
            Object[] arr = (Object[]) o;
            if (index >= arr.length)
                return null;
            return arr[index];
        }

        throw new JspException("Object of type " + o.getClass().getName() + " not implemented");
    }

    public static boolean contains(Collection<?> c, Object o) {
        return c.contains(o);
    }

    public static boolean contains(int value, int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == value)
                return true;
        }
        return false;
    }

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

    public static String truncate(String s, int maxLength) {
        if (s == null || s.length() <= maxLength)
            return s;
        return s.substring(0, maxLength) + "...";
    }

    public static String lower(String s) {
        if (s == null)
            return null;
        return s.toLowerCase();
    }

    public static void printAttribute(JspWriter out, String attributeName, String attributeValue) throws IOException {
        if (attributeValue != null) {
            out.print(" ");
            out.print(attributeName);
            out.print("=\"");
            out.print(attributeValue);
            out.print("\"");
        }
    }

    public static Object replaceRequestAttribute(ServletRequest request, String name, Object newAttribute) {
        Object old = request.getAttribute(name);
        if (newAttribute == null)
            request.removeAttribute(name);
        else
            request.setAttribute(name, newAttribute);
        return old;
    }

    public static void restoreRequestAttribute(ServletRequest request, String name, Object oldAttribute) {
        if (oldAttribute == null)
            request.removeAttribute(name);
        else
            request.setAttribute(name, oldAttribute);
    }

    public static Object getConstant(String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getField(fieldName);
            return field.get(clazz);
        }
        catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    public static int toInt(Number n) {
        if (n == null)
            return 0;
        return n.intValue();
    }

    public static String urlEncode(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    public static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    public static String cleanParameter(String s) {
        if (s == null)
            return null;
        s = s.replaceAll("<", "&lt;");
        s = s.replaceAll(">", "&gt;");
        s = s.replaceAll("script", "");
        s = s.replaceAll("iframe", "");
        return s;
    }

    public static String formatTime(long time, String format) {
        return new SimpleDateFormat(format).format(new Date(time));
    }

    public static void main(String[] args) {
        System.out.println(quotEncode("That's \"it\"!"));
        System.out.println(dquotEncode("That's \"it\"!"));
        System.out.println(escapeQuotes("That's \"it\"!"));
    }
}
