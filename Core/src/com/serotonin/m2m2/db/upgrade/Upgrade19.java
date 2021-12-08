/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.util.SerializationHelper;

public class Upgrade19 extends DBUpgrade {

    private final Logger LOG = LoggerFactory.getLogger(Upgrade19.class);

    @Override
    protected void upgrade() throws Exception {
        //Add the data type column into the database.
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseType.POSTGRES.name(), addColumn);
        scripts.put(DatabaseType.MYSQL.name(), addColumn);
        scripts.put(DatabaseType.MSSQL.name(), addColumn);
        scripts.put(DatabaseType.H2.name(), addColumn);
        runScript(scripts);

        //not using data type id to deserialize, so we don't need a legacy row mapper here
        this.ejt.query(UPGRADE_19_DATA_POINT_SELECT, new Upgrade19ResultSetExtractor());

        scripts.clear();
        scripts.put(DatabaseType.POSTGRES.name(), alterColumn);
        scripts.put(DatabaseType.MYSQL.name(), modifyColumn);
        scripts.put(DatabaseType.MSSQL.name(), alterColumn);
        scripts.put(DatabaseType.H2.name(), alterColumn);
        runScript(scripts);

        scripts.clear();
        String[] empty = new String[0];
        scripts.put(DatabaseType.POSTGRES.name(), empty);
        scripts.put(DatabaseType.MYSQL.name(), empty);
        scripts.put(DatabaseType.MSSQL.name(), empty);
        //Upgrade 14 omitted setting this primary key for the user comments, but it was in the create tables
        scripts.put(DatabaseType.H2.name(), new String[] {"ALTER TABLE userComments ADD PRIMARY KEY (id);"});
        try {
            runScript(scripts);
        } catch(Exception e) {
            //It may already have existed.
        }

    }

    @Override
    protected String getNewSchemaVersion() {
        return "20";
    }

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
            "select dp.id, ds.dataSourceType, dp.data " //
            + "from dataPoints dp join dataSources ds on ds.id = dp.dataSourceId ";

    class RawDataPoint {
        int id;
        Integer dataTypeId;
        String dataSourceTypeName;
    }

    class Upgrade19DataPointRowMapper implements RowMapper<RawDataPoint> {
        @Override
        public RawDataPoint mapRow(ResultSet rs, int rowNum) throws SQLException {

            RawDataPoint dp = new RawDataPoint();
            try{
                dp.id = rs.getInt(1);
                dp.dataSourceTypeName = rs.getString(2);
                DataPointVO dpVo = (DataPointVO) SerializationHelper.readObjectInContext(rs.getBinaryStream(3));
                DataType dataType = dpVo.getPointLocator().getDataType();
                dp.dataTypeId = dataType == null ? 0 : dataType.getId();
            }catch(Exception e) {
                //Munchy munch, we will handle this later when we see the dataTypeId is null
            }

            return dp;
        }
    }

    class Upgrade19ResultSetExtractor implements ResultSetExtractor<Void> {

        @Override
        public Void extractData(ResultSet rs) throws SQLException, DataAccessException {
            Upgrade19DataPointRowMapper dprw = new Upgrade19DataPointRowMapper();

            try(PrintWriter pw = new PrintWriter(createUpdateLogOutputStream())) {
                while(rs.next()) {
                    RawDataPoint rdp = dprw.mapRow(rs, rs.getRow());
                    if(rdp.dataTypeId != null) {
                        String message = "Updating dpid: " + rdp.id + " setting data type id to: " + rdp.dataTypeId;
                        if(LOG.isDebugEnabled())
                            LOG.debug(message);
                        pw.write(message + "\n");
                        ejt.update("update dataPoints set dataTypeId=? where id=?",
                                new Object[]{rdp.dataTypeId, rdp.id},
                                new int[] {Types.INTEGER, Types.INTEGER});
                    }else {
                        String message = "Data source module " + rdp.dataSourceTypeName + " is missing.  Data point with id " + rdp.id + " not upgraded yet.";
                        LOG.info(message);
                        pw.write(message + "\n");
                        ejt.update("update dataPoints set dataTypeId=? where id=?",
                                new Object[]{-1, rdp.id},
                                new int[] {Types.INTEGER, Types.INTEGER});
                    }
                }
            }
            return null;
        }

    }
}