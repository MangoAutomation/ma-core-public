/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.util.SerializationHelper;

/**
 * Add settable column to data points table
 * 
 * @author Terry Packer
 *
 */
public class Upgrade27 extends DBUpgrade {
    
    private static final Logger LOG = LoggerFactory.getLogger(Upgrade27.class);
    
    @Override
    protected void upgrade() throws Exception {
        
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseType.MYSQL.name(), sql);
        scripts.put(DatabaseType.H2.name(), sql);
        scripts.put(DatabaseType.MSSQL.name(), sql);
        scripts.put(DatabaseType.POSTGRES.name(), sql);
        runScript(scripts);
        
        //Now update all the data point's to have a value
        ejt.query("SELECT data,id FROM dataPoints", new RowCallbackHandler() {

            @Override
            public void processRow(ResultSet rs) throws SQLException {
                try {
                    int id = rs.getInt(2);
                    DataPointVO dp = (DataPointVO) SerializationHelper.readObjectInContext(rs.getBinaryStream(1));
                    ejt.update("UPDATE dataPoints SET settable=? WHERE id=?", new Object[] {boolToChar(dp.getPointLocator().isSettable()), id});
                }catch(Exception e) {
                    LOG.error("Failed to set settable column on data point with id " + rs.getInt(2), e);
                }
            }
            
        });
    }
    
    private String[] sql = new String[]{
            "ALTER TABLE dataPoints ADD COLUMN settable char(1);",
    };

    @Override
    protected String getNewSchemaVersion() {
        return "28";
    }
}
