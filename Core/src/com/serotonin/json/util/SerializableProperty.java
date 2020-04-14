package com.serotonin.json.util;

import java.lang.reflect.Method;

/**
 * Describes how any given attribute of an object can be serialized/deserialized to/from JSON.
 *
 * @author Matthew Lohbihler
 */
public class SerializableProperty {
    private String name;
    private Method readMethod;
    private Method writeMethod;
    private String alias;
    private boolean suppressDefaultValue;
    private String[] includeHints;
    private String[] readAliases;

    public boolean include(String includeHint) {
        // If no hints were specified, always include.
        if (includeHints == null || includeHints.length == 0)
            return true;
        return Utils.contains(includeHints, includeHint);
    }

    public String getNameToUse() {
        if (alias == null)
            return name;
        return alias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Method getReadMethod() {
        return readMethod;
    }

    public void setReadMethod(Method readMethod) {
        this.readMethod = readMethod;
    }

    public Method getWriteMethod() {
        return writeMethod;
    }

    public void setWriteMethod(Method writeMethod) {
        this.writeMethod = writeMethod;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isSuppressDefaultValue() {
        return suppressDefaultValue;
    }

    public void setSuppressDefaultValue(boolean suppressDefaultValue) {
        this.suppressDefaultValue = suppressDefaultValue;
    }

    public String[] getIncludeHints() {
        return includeHints;
    }

    public void setIncludeHints(String[] includeHints) {
        this.includeHints = includeHints;
    }

    public void setReadAliases(String[] readAliases) {
        this.readAliases = readAliases;
    }

    public String[] getReadAliases() {
        return readAliases;
    }

    @Override
    public String toString() {
        return getNameToUse();
    }
}