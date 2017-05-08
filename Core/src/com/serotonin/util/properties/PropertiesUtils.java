package com.serotonin.util.properties;

import java.io.IOException;
import java.io.StringReader;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.ShouldNeverHappenException;

/**
 * @deprecated use ReloadingProperties instead
 */
@Deprecated
public class PropertiesUtils {
    private static final Log LOG = LogFactory.getLog(PropertiesUtils.class);

    private final ResourceBundle props;

    public PropertiesUtils(String bundleName) {
        this(bundleName, false);
    }

    public PropertiesUtils(String bundleName, boolean allowMissing) {
        ResourceBundle rb;
        try {
            rb = ResourceBundle.getBundle(bundleName);
        }
        catch (MissingResourceException e) {
            if (!allowMissing)
                throw e;
            try {
                rb = new PropertyResourceBundle(new StringReader(""));
            }
            catch (IOException e1) {
                throw new ShouldNeverHappenException(e);
            }
        }
        props = rb;
    }

    public String getString(String key) {
        if (props == null)
            throw new MissingResourceException("", "", key);
        return props.getString(key);
    }

    public String getString(String key, String defaultValue) {
        try {
            String value = getString(key);
            if (value.trim().length() == 0)
                return defaultValue;
            return value;
        }
        catch (MissingResourceException e) {
            return defaultValue;
        }
    }

    public int getInt(String key) {
        if (props == null)
            throw new MissingResourceException("", "", key);
        return Integer.parseInt(props.getString(key));
    }

    public int getInt(String key, int defaultValue) {
        if (props == null)
            return defaultValue;

        try {
            String value = props.getString(key);
            if (value.trim().length() == 0)
                return defaultValue;
            return Integer.parseInt(value);
        }
        catch (MissingResourceException e) {
            // no op
        }
        catch (NumberFormatException e) {
            // no op
        }
        return defaultValue;
    }

    public double getDouble(String key) {
        if (props == null)
            throw new MissingResourceException("", "", key);
        return Double.parseDouble(props.getString(key));
    }

    public double getDouble(String key, double defaultValue) {
        if (props == null)
            return defaultValue;

        try {
            String value = props.getString(key);
            if (value.trim().length() == 0)
                return defaultValue;
            return Double.parseDouble(value);
        }
        catch (MissingResourceException e) {
            // no op
        }
        catch (NumberFormatException e) {
            // no op
        }
        return defaultValue;
    }

    public boolean getBoolean(String key) {
        if (props == null)
            throw new MissingResourceException("", "", key);
        return "true".equalsIgnoreCase(props.getString(key));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (props == null)
            return defaultValue;

        try {
            String value = props.getString(key);
            if (value.trim().length() == 0)
                return defaultValue;
            if ("true".equalsIgnoreCase(value))
                return true;
            if ("false".equalsIgnoreCase(value))
                return false;
            LOG.warn("Can't parse boolean from properties key: " + key + ", value=" + value);
        }
        catch (MissingResourceException e) {
            // no op
        }
        return defaultValue;
    }
}
