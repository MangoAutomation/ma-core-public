/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

package com.serotonin.m2m2.vo;

import java.io.IOException;
import java.io.Serializable;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableJsonException;

/**
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 *
 * @author Jared Wiltshire
 */
public abstract class AbstractVO extends AbstractBasicVO implements Serializable,
JsonSerializable, Cloneable {

    protected static final String DEFAULT_MISSING_IMPORT_KEY = "emport.error.missingValue";
    protected static final String DEFAULT_WRONG_DATA_TYPE_KEY = "emport.error.wrongDataType";

    /*
     * Mango properties
     */
    protected String xid;
    protected String name;

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        // don't use JsonProperty annotation so we can choose whether to read/write in sub type
        xid = jsonObject.getString("xid");
        name = jsonObject.getString("name");
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        // don't use JsonProperty annotation so we can choose whether to read/write in sub type
        writer.writeEntry("xid", xid);
        writer.writeEntry("name", name);
    }

    /**
     * Helper to safely get a boolean during import
     * @param json
     * @param name
     * @return
     * @throws JsonException - if the field DNE or is not of the desired type
     */
    public static boolean getBoolean(JsonObject json, String name) throws JsonException {
        JsonValue o = json.get(name);
        if(o == null)
            throw new TranslatableJsonException(DEFAULT_MISSING_IMPORT_KEY, name);
        try{
            return o.toBoolean();
        }catch(ClassCastException e) {
            throw new TranslatableJsonException(DEFAULT_WRONG_DATA_TYPE_KEY, name, Boolean.class.getSimpleName());
        }
    }

    /**
     * Helper to safely get a String during import
     * @param json
     * @param name
     * @return
     * @throws JsonException - if the field DNE or is not of the desired type
     */
    public static String getString(JsonObject json, String name) throws JsonException {
        JsonValue o = json.get(name);
        if(o == null)
            throw new TranslatableJsonException(DEFAULT_MISSING_IMPORT_KEY, name);
        try {
            return json.getString(name);
        }catch(ClassCastException e) {
            throw new TranslatableJsonException(DEFAULT_WRONG_DATA_TYPE_KEY, name, String.class.getSimpleName());
        }
    }

    /**
     * Helper to safely get a double during import
     * @param json
     * @param name
     * @return
     * @throws JsonException - if the field DNE or is not of the desired type
     */
    @SuppressWarnings("deprecation")
    public static double getDouble(JsonObject json, String name) throws JsonException {
        JsonValue o = json.get(name);
        if(o == null)
            throw new TranslatableJsonException(DEFAULT_MISSING_IMPORT_KEY, name);
        try {
            return json.getDouble(name);
        }catch(ClassCastException e) {
            throw new TranslatableJsonException(DEFAULT_WRONG_DATA_TYPE_KEY, name, Double.class.getSimpleName());
        }

    }

    /**
     * Helper to safely get a int during import
     * @param json
     * @param name
     * @return
     * @throws JsonException - if the field DNE or is not of the desired type
     */
    @SuppressWarnings("deprecation")
    public static int getInt(JsonObject json, String name) throws JsonException {
        JsonValue o = json.get(name);
        if(o == null)
            throw new TranslatableJsonException(DEFAULT_MISSING_IMPORT_KEY, name);
        try {
            return json.getInt(name);
        }catch(ClassCastException e) {
            throw new TranslatableJsonException(DEFAULT_WRONG_DATA_TYPE_KEY, name, Integer.class.getSimpleName());
        }
    }

    /*
     * Serialization
     */
    private static final long serialVersionUID = -1;

    /**
     * Get the Audit Message Key
     * @return
     */
    public abstract String getTypeKey();

    /*
     * Getters and setters
     */
    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Check if a vo is newly created
     *
     * @return true if newly created, false otherwise
     */
    public boolean isNew() {
        return (id == Common.NEW_ID);
    }

    /**
     * Copies a vo
     *
     * @return Copy of this vo
     */
    public AbstractVO copy() {
        try {
            return (AbstractVO) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    /**
     * Useful For Debugging
     */
    @Override
    public String toString() {
        return "id: " + this.id + " name: " + this.name + " xid: " + xid;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((xid == null) ? 0 : xid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractVO other = (AbstractVO) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (xid == null) {
            if (other.xid != null)
                return false;
        } else if (!xid.equals(other.xid))
            return false;
        return true;
    }
}
