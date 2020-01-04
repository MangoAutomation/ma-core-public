package com.serotonin.util.properties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract properties, suitable for use in applications that required testing harnesses. Reloading properties can be
 * used in the functional application, while basic properties can be used for testing.
 *
 * @author Matthew Lohbihler
 */
abstract public class AbstractProperties implements MangoProperties {
    private static final Pattern PATTERN_ENV = Pattern.compile("(\\$\\{env:(.+?)\\})");
    private static final Pattern PATTERN_PROP = Pattern.compile("(\\$\\{prop:(.+?)\\})");
    private static final Pattern PATTERN_BS = Pattern.compile("\\\\");
    private static final Pattern PATTERN_DOL = Pattern.compile("\\$");

    protected Log log = LogFactory.getLog(this.getClass());

    private final String description;

    public AbstractProperties() {
        this("unnamed");
    }

    public AbstractProperties(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getString(String key) {
        String s = getStringImpl(key);
        if (s == null)
            return null;

        //
        // Pattern replacement
        Matcher matcher = PATTERN_ENV.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String pkey = matcher.group(2);
            matcher.appendReplacement(sb, escape(System.getenv(pkey)));
        }
        matcher.appendTail(sb);

        matcher = PATTERN_PROP.matcher(sb.toString());
        sb = new StringBuffer();
        while (matcher.find()) {
            String pkey = matcher.group(2);
            matcher.appendReplacement(sb, escape(System.getProperty(pkey)));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String escape(String s) {
        if (s == null)
            return "";

        s = PATTERN_BS.matcher(s).replaceAll("\\\\\\\\"); // Escape backslashes
        s = PATTERN_DOL.matcher(s).replaceAll("\\\\\\$"); // Escape dollar signs

        return s;
    }

    abstract protected String getStringImpl(String key);

    //    public static String replaceMacros(String s, Properties properties) {
    //        String result = s;
    //        for (Map.Entry<Object, Object> entry : properties.entrySet())
    //            result = replaceMacro(result, entry.getKey().toString(),
    //                    Matcher.quoteReplacement(entry.getValue().toString()));
    //        return result;
    //    }

    @Override
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value))
            return defaultValue;
        return value;
    }

    @Override
    public String[] getStringArray(String key) {
        return this.getStringArray(key, "\\s*,\\s*", new String[] {});
    }

    @Override
    public String[] getStringArray(String key, String[] defaultValue) {
        return this.getStringArray(key, "\\s*,\\s*", defaultValue);
    }

    @Override
    public String[] getStringArray(String key, String delimiter, String[] defaultValue) {
        String value = getString(key);
        if (value == null)
            return defaultValue;
        if (value.isEmpty()) {
            return new String[] {};
        }
        return value.split(delimiter);
    }

    @Override
    public int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value))
            return defaultValue;

        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            if (log.isWarnEnabled()) {
                log.warn("(" + description + ") Can't parse int from properties key: " + key + ", value=" + value);
            }
        }
        return defaultValue;
    }

    @Override
    public long getLong(String key) {
        return Long.parseLong(getString(key));
    }

    @Override
    public long getLong(String key, long defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value))
            return defaultValue;

        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            if (log.isWarnEnabled()) {
                log.warn("(" + description + ") Can't parse long from properties key: " + key + ", value=" + value);
            }
        }
        return defaultValue;
    }

    @Override
    public boolean getBoolean(String key) {
        return "true".equalsIgnoreCase(getString(key));
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value))
            return defaultValue;
        if ("true".equalsIgnoreCase(value))
            return true;
        if ("false".equalsIgnoreCase(value))
            return false;
        if (log.isWarnEnabled()) {
            log.warn("(" + description + ") Can't parse boolean from properties key: " + key + ", value=" + value);
        }
        return defaultValue;
    }

    @Override
    public double getDouble(String key) {
        return Double.parseDouble(getString(key));
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value))
            return defaultValue;

        try {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            if (log.isWarnEnabled()) {
                log.warn("(" + description + ") Can't parse double from properties key: " + key + ", value=" + value);
            }
        }
        return defaultValue;
    }
}
