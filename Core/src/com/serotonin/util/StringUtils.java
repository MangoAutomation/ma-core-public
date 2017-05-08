package com.serotonin.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;

public class StringUtils {
    private static final int PASSWORD_LENGTH = 7;
    private static final String PASSWORD_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
    public static final Random RANDOM = new Random();

    public static String trimWhitespace(String s) {
        if (s == null)
            return null;
        int start = 0;
        while (start < s.length() && Character.isWhitespace(s.charAt(start)))
            start++;
        int end = s.length();
        while (end > start && Character.isWhitespace(s.charAt(end - 1)))
            end--;
        return s.substring(start, end);
    }

    /**
     * Mask the left side of the given string with the given mask character, leaving the given number of characters on
     * the right side unmasked. If the unmasked length is equal to or greater than the length of the string, the whole
     * string is masked.
     * 
     * @param s
     * @param maskChar
     * @param unmaskedLength
     * @return the masked string
     */
    public static String mask(String s, char maskChar, int unmaskedLength) {
        if (s == null)
            return null;

        if (s.length() > unmaskedLength)
            return org.apache.commons.lang3.StringUtils.leftPad("", s.length() - unmaskedLength, '*')
                    + s.substring(s.length() - unmaskedLength);

        return org.apache.commons.lang3.StringUtils.leftPad("", s.length(), '*');
    }

    /**
     * Generates an ugly random password.
     * 
     * @return the password.
     */
    public static String generatePassword() {
        return generatePassword(PASSWORD_LENGTH);
    }

    public static String generatePassword(int length) {
        return generateRandomString(length, PASSWORD_CHARSET);
    }

    public static String generateRandomString(int length, String charSet) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++)
            sb.append(charSet.charAt(RANDOM.nextInt(charSet.length())));
        return sb.toString();
    }

    public static String escapeLT(String s) {
        if (s == null)
            return null;
        return s.replaceAll("<", "&lt;");
    }

    public static boolean globWhiteListMatchIgnoreCase(String[] values, String value) {
        if (values == null || values.length == 0 || value == null)
            return false;

        int ast = 0;
        for (int i = 0; i < values.length; i++) {
            ast = values[i].indexOf("*");
            if (ast == -1) {
                if (values[i].equalsIgnoreCase(value))
                    return true;
            }
            else {
                if (value.length() >= ast) {
                    if (values[i].substring(0, ast).equalsIgnoreCase(value.substring(0, ast)))
                        return true;
                }
            }
        }

        return false;
    }

    public static String replaceMacros(String s, Properties properties) {
        Pattern p = Pattern.compile("\\$\\{(.*?)\\}");
        Matcher matcher = p.matcher(s);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(result, Matcher.quoteReplacement(ObjectUtils.toString(properties.get(group))));
        }
        matcher.appendTail(result);
        return result.toString();

        //        String result = s;
        //        for (Map.Entry<Object, Object> entry : properties.entrySet())
        //            result = replaceMacro(result, entry.getKey().toString(),
        //                    Matcher.quoteReplacement(entry.getValue().toString()));
        //        return result;
    }

    public static String replaceMacros(String s, Map<String, ?> properties) {
        Pattern p = Pattern.compile("\\$\\{(.*?)\\}");
        Matcher matcher = p.matcher(s);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(result, Matcher.quoteReplacement(ObjectUtils.toString(properties.get(group))));
        }
        matcher.appendTail(result);
        return result.toString();

        //        String result = s;
        //        for (Map.Entry<String, ?> entry : properties.entrySet())
        //            result = replaceMacro(result, entry.getKey().toString(),
        //                    Matcher.quoteReplacement(entry.getValue().toString()));
        //        return result;
    }

    public static String replaceMacro(String s, String name, String replacement) {
        return s.replaceAll(Pattern.quote("${" + name + "}"), replacement);
    }

    public static String replaceMacro(String s, String name, String content, String replacement) {
        return s.replaceAll(Pattern.quote("${" + name + ":" + content + "}"), replacement);
    }

    public static String getMacroContent(String s, String name) {
        Matcher matcher = Pattern.compile("\\$\\{" + Pattern.quote(name) + ":(.*?)\\}").matcher(s);
        if (matcher.find())
            return matcher.group(1);
        return null;
    }

    public static String truncate(String s, int length, String truncateSuffix) {
        if (s == null)
            return s;

        if (s.length() <= length)
            return s;

        s = s.substring(0, length);
        if (truncateSuffix == null)
            return s;
        return s + truncateSuffix;
    }

    public static String findGroup(Pattern pattern, String s) {
        return findGroup(pattern, s, 1);
    }

    public static String findGroup(Pattern pattern, String s, int group) {
        if (s == null)
            return null;

        Matcher matcher = pattern.matcher(s);
        if (matcher.find())
            return matcher.group(group);

        return null;
    }

    public static String[] findAllGroup(Pattern pattern, String s) {
        return findAllGroup(pattern, s, 1);
    }

    public static String[] findAllGroup(Pattern pattern, String s, int group) {
        if (s == null)
            return null;

        Matcher matcher = pattern.matcher(s);
        List<String> result = new ArrayList<String>();
        while (matcher.find())
            result.add(matcher.group(group));

        String[] a = new String[result.size()];
        result.toArray(a);
        return a;
    }

    public static String durationToString(long duration) {
        if (duration < 1000)
            return duration + "ms";

        if (duration < 10000) {
            String s = "" + (duration / 1000) + '.';
            s += (int) (((double) (duration % 1000)) / 10 + 0.5);
            return s + "s";
        }

        if (duration < 60000) {
            String s = "" + (duration / 1000) + '.';
            s += (int) (((double) (duration % 1000)) / 100 + 0.5);
            return s + "s";
        }

        // Convert to seconds
        duration /= 1000;

        if (duration < 600)
            return "" + (duration / 60) + 'm' + (duration % 60) + 's';

        // Convert to minutes
        duration /= 60;

        if (duration < 60)
            return "" + duration + 'm';

        if (duration < 1440)
            return "" + (duration / 60) + 'h' + (duration % 60) + 'm';

        // Convert to hours
        duration /= 60;

        if (duration < 24)
            return "" + duration + 'h';

        if (duration < 168)
            return "" + (duration / 24) + 'd' + (duration % 24) + 'h';

        // Convert to days
        duration /= 24;

        return "" + duration + 'd';
    }

    public static String capitalize(String s) {
        if (s == null)
            return null;
        return s.toUpperCase().replace(' ', '_');
    }

    public static boolean startsWith(String haystack, String needle) {
        if (haystack == null)
            return false;
        return haystack.startsWith(needle);
    }

    public static int compareStrings(String s1, String s2, boolean ignoreCase) {
        if (s1 == null && s2 == null)
            return 0;
        if (s1 == null)
            return -1;
        if (s2 == null)
            return 1;
        if (ignoreCase)
            return s1.compareToIgnoreCase(s2);
        return s1.compareTo(s2);
    }
}
