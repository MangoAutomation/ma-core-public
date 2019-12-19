/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

package com.serotonin.m2m2.vo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.util.MapWrap;
import com.serotonin.util.SerializationHelper;
import com.serotonin.validation.StringValidation;

/**
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 *
 * @author Jared Wiltshire
 */
public abstract class AbstractVO<T extends AbstractVO<T>> extends AbstractBasicVO implements Serializable,
JsonSerializable, Cloneable, Validatable {

    private static final Log LOG = LogFactory.getLog(AbstractVO.class);

    protected static final String DEFAULT_MISSING_IMPORT_KEY = "emport.error.missingValue";
    protected static final String DEFAULT_WRONG_DATA_TYPE_KEY = "emport.error.wrongDataType";
    
    /**
     * Allows the conversion of VOs between code versions by providing access to properties that would otherwise have
     * been expunged by the version handling in the readObject method.
     */
    private transient MapWrap deletedProperties;

    /*
     * Mango properties
     */
    protected String xid;
    protected String name;

    /**
     * Get the Dao for this Object
     * @return
     */
    protected abstract AbstractDao<T> getDao();

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        // dont use JsonProperty annotation so we can choose whether to read/write in sub type
        xid = jsonObject.getString("xid");
        name = jsonObject.getString("name");
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        // dont use JsonProperty annotation so we can choose whether to read/write in sub type
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

    /**
     * Helper to read and merge legacy JSON permissions which are 
     *  just a list of role names 
     * @param existing
     * @param jsonObject
     * @return
     */
    protected Set<RoleVO> readLegacyPermissions(String permissionName, Set<RoleVO> existing, JsonObject jsonObject) {
        //Legacy permissions support
        if(jsonObject.containsKey(permissionName)) {
            Set<RoleVO> roles;
            if(existing != null) {
                roles = new HashSet<>(existing);
            }else {
                roles = new HashSet<>();
            }
            JsonArray permissions = jsonObject.getJsonArray(permissionName);
            for(JsonValue jv : permissions) {
                RoleVO role = RoleDao.getInstance().getByXid(jv.toString());
                if(role != null) {
                    roles.add(role);
                }
            }
            return roles;
        }
        return Collections.emptySet();
    }
    
    /*
     * Serialization
     */

    private static final long serialVersionUID = -1;

    /*
     * Deleted properties
     */
    public void saveDeleteProperty(String key, Object value) {
        if (deletedProperties == null)
            deletedProperties = new MapWrap();
        deletedProperties.put(key, value);
    }

    public MapWrap deletedProperties() {
        return deletedProperties;
    }

    protected void writeDeletedProperties(ObjectOutputStream out) throws IOException {
        SerializationHelper.writeObject(out, deletedProperties());
    }

    protected void readDeletedProperties(ObjectInputStream in) throws IOException, ClassNotFoundException {
        MapWrap del = (MapWrap) in.readObject();
        if (del != null) {
            if (deletedProperties != null)
                // Merge
                del.putAll(deletedProperties);
            deletedProperties = del;
        }
    }

    /*
     * ChangeComparable
     */

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

    /*
     * Utility methods
     */

    /**
     * Validates a vo
     *
     * @param response
     */
    @Override
    public void validate(ProcessResult response) {
        if (StringUtils.isBlank(xid))
            response.addContextualMessage("xid", "validate.required");
        else if (StringValidation.isLengthGreaterThan(xid, 100))
            response.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 100));
        else if (!isXidUnique(xid, id))
            response.addContextualMessage("xid", "validate.xidUsed");

        if (StringUtils.isBlank(name))
            response.addContextualMessage("name", "validate.required");
        else if (StringValidation.isLengthGreaterThan(name, 255))
            response.addMessage("name", new TranslatableMessage("validate.notLongerThan", 255));
    }

    protected boolean isXidUnique(String xid, int id){
        AbstractDao<T> dao = getDao();
        if(dao == null){
            LOG.warn("No dao provided to validate XID uniqueness.");
            return true;
        }
        return dao.isXidUnique(xid,id);
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
    @SuppressWarnings("unchecked")
    public T copy() {
        // TODO make sure this works
        try {
            return (T) super.clone();
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
        return "id: " + this.id + " name: " + this.name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((xid == null) ? 0 : xid.hashCode());
        return result;
    }

    @SuppressWarnings("rawtypes")
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
