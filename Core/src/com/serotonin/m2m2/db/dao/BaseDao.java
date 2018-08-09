/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.infiniteautomation.mango.db.query.SQLConstants;
import com.serotonin.db.DaoUtils;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.TranslatableMessageParseException;
import com.serotonin.m2m2.module.JacksonModuleDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

public class BaseDao extends DaoUtils implements SQLConstants{
    
    //ObjectMapper to Serialize JSON to/from the database
    private final static ObjectMapper mapper;
    static {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setTimeZone(TimeZone.getTimeZone("UTC")); //Set to UTC in case timezone change while data is in database
        
        //Setup Module Defined JSON Modules
        List<JacksonModuleDefinition> defs = ModuleRegistry.getDefinitions(JacksonModuleDefinition.class);
        for(JacksonModuleDefinition def : defs) {
            if(def.getSourceMapperType() == JacksonModuleDefinition.ObjectMapperSource.DATABASE)
                mapper.registerModule(def.getJacksonModule());
        }
    }

    
    /**
     * Public constructor for code that needs to get stuff from the database.
     */
    public BaseDao() {
        super(Common.databaseProxy.getDataSource(), Common.databaseProxy.getTransactionManager());
    }

    /**
     * Get a writer for serializing JSON
     * @return
     */
    public ObjectWriter getObjectWriter(Class<?> type) {
        return mapper.writerFor(type);
    }
    
    /**
     * Get a reader for use de-serializing JSON
     * @return
     */
    public ObjectReader getObjectReader(Class<?> type) {
        return mapper.readerFor(type);
    }
    
    //
    // Convenience methods for storage of booleans.
    //
    public static String boolToChar(boolean b) {
        return b ? Y : N;
    }

    public static boolean charToBool(String s) {
        return Y.equals(s);
    }

    //
    // XID convenience methods
    //
    protected String generateUniqueXid(String prefix, String tableName) {
        return Common.generateXid(prefix);
    }

    protected boolean isXidUnique(String xid, int excludeId, String tableName) {
        return ejt.queryForInt("select count(*) from " + tableName + " where xid=? and id<>?", new Object[] { xid,
                excludeId }, 0) == 0;
    }

    //
    // Convenience methods for translatable messages
    //
    public static String writeTranslatableMessage(TranslatableMessage tm) {
        if (tm == null)
            return null;
        return tm.serialize();
    }

    public static TranslatableMessage readTranslatableMessage(ResultSet rs, int columnIndex) throws SQLException {
        String s = rs.getString(columnIndex);
        if (s == null)
            return null;

        try {
            return TranslatableMessage.deserialize(s);
        }
        catch (TranslatableMessageParseException e) {
            return new TranslatableMessage("common.default", rs.getString(columnIndex));
        }
    }
}
