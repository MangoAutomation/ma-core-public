/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.util.SerializationHelper;

public class Upgrade19 extends DBUpgrade {

private static final Log LOG = LogFactory.getLog(Upgrade19.class);

@Override
protected void upgrade() throws Exception {
    //Add the data type column into the database.
    Map<String, String[]> scripts = new HashMap<>();
    scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), addColumn);
    scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), addColumn);
    scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), addColumn);
    scripts.put(DatabaseProxy.DatabaseType.H2.name(), addColumn);
    runScript(scripts);
    
    //not using data type id to deserialize, so we don't need a legacy row mapper here
    this.ejt.query(UPGRADE_19_DATA_POINT_SELECT, new Upgrade19ResultSetExtractor());
    
    scripts.clear();
    scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), alterColumn);
    scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), modifyColumn);
    scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), alterColumn);
    scripts.put(DatabaseProxy.DatabaseType.H2.name(), alterColumn);
    runScript(scripts);
    
    if(0 == this.ejt.queryForInt("SELECT id FROM INFORMATION_SCHEMA.CONSTRAINTS WHERE TABLE_NAME = 'USERCOMMENTS' AND CONSTRAINT_TYPE='PRIMARY KEY'", null, 0)){
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), addUserCommentIndexMySQL);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), addUserCommentIndexMySQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), addUserCommentIndexMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), addUserCommentIndexMySQL);
    }
    
}

@Override
protected String getNewSchemaVersion() {
    return "20";
}

private static final String[] addUserCommentIndexMySQL = {
        "ALTER TABLE userComments ADD PRIMARY KEY (id)"
};

private static final String[] addUserCommentIndexMSSQL = {
        "ALTER TABLE userComments ADD CONSTRAINT pk PRIMARY KEY CLUSTERED (id)"
};

private static final String[] addColumn = {
        "ALTER TABLE dataPoints ADD COLUMN dataTypeId INT;",
};
private static final String[] alterColumn = {
        "ALTER TABLE dataPoints ALTER COLUMN dataTypeId INT NOT NULL;"
};
private static final String[] modifyColumn = {
        "ALTER TABLE dataPoints MODIFY COLUMN dataTypeId INT NOT NULL;"
};

private static final String UPGRADE_19_DATA_POINT_SELECT = //
"select dp.data, dp.id, dp.xid, dp.dataSourceId, dp.name, dp.deviceName, dp.enabled, dp.pointFolderId, " //
        + "  dp.loggingType, dp.intervalLoggingPeriodType, dp.intervalLoggingPeriod, dp.intervalLoggingType, " //
        + "  dp.tolerance, dp.purgeOverride, dp.purgeType, dp.purgePeriod, dp.defaultCacheSize, " //
        + "  dp.discardExtremeValues, dp.engineeringUnits, dp.readPermission, dp.setPermission, dp.templateId, dp.rollup, "
        + "  ds.name,  ds.xid, ds.dataSourceType " //
        + "from dataPoints dp join dataSources ds on ds.id = dp.dataSourceId ";

class Upgrade19DataPointRowMapper implements RowMapper<DataPointVO> {
    @Override
    public DataPointVO mapRow(ResultSet rs, int rowNum) throws SQLException {
        int i = 0;

        DataPointVO dp = (DataPointVO) SerializationHelper.readObjectInContext(rs.getBinaryStream(++i));
        dp.setId(rs.getInt(++i));
        dp.setXid(rs.getString(++i));
        dp.setDataSourceId(rs.getInt(++i));
        dp.setName(rs.getString(++i));
        dp.setDeviceName(rs.getString(++i));
        dp.setEnabled(charToBool(rs.getString(++i)));
        dp.setPointFolderId(rs.getInt(++i));
        dp.setLoggingType(rs.getInt(++i));
        dp.setIntervalLoggingPeriodType(rs.getInt(++i));
        dp.setIntervalLoggingPeriod(rs.getInt(++i));
        dp.setIntervalLoggingType(rs.getInt(++i));
        dp.setTolerance(rs.getDouble(++i));
        dp.setPurgeOverride(charToBool(rs.getString(++i)));
        dp.setPurgeType(rs.getInt(++i));
        dp.setPurgePeriod(rs.getInt(++i));
        dp.setDefaultCacheSize(rs.getInt(++i));
        dp.setDiscardExtremeValues(charToBool(rs.getString(++i)));
        dp.setEngineeringUnits(rs.getInt(++i));
        dp.setReadPermission(rs.getString(++i));
        dp.setSetPermission(rs.getString(++i));
        //Because we read 0 for null
        dp.setTemplateId(rs.getInt(++i));
        if(rs.wasNull())
            dp.setTemplateId(null);
        dp.setRollup(rs.getInt(++i));

        // Data source information.
        dp.setDataSourceName(rs.getString(++i));
        dp.setDataSourceXid(rs.getString(++i));
        dp.setDataSourceTypeName(rs.getString(++i));

        dp.ensureUnitsCorrect();

        return dp;
    }
}

    class Upgrade19ResultSetExtractor implements ResultSetExtractor<Void> {
    
        @Override
        public Void extractData(ResultSet rs) throws SQLException, DataAccessException {
            Upgrade19DataPointRowMapper dprw = new Upgrade19DataPointRowMapper();
            while(rs.next()) {
                DataPointVO dpvo = dprw.mapRow(rs, rs.getRow());
                if(LOG.isDebugEnabled())
                    LOG.debug("Updating dpid: " + dpvo.getId() + " setting data type id to: " + dpvo.getPointLocator().getDataTypeId());
                
                ejt.update("update dataPoints set dataTypeId=? where id=?",
                        new Object[]{dpvo.getPointLocator().getDataTypeId(), dpvo.getId()}, 
                        new int[] {Types.INTEGER, Types.INTEGER});
            }
            return null;
        }
        
    }
}