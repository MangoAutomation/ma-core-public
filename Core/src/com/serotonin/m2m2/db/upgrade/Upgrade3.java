package com.serotonin.m2m2.db.upgrade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.db.dao.BaseDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.util.SerializationHelper;

public class Upgrade3 extends DBUpgrade {
    @Override
    public void upgrade() throws Exception {
        // Run the script.
        Map<String, String[]> scripts = new HashMap<String, String[]>();
        scripts.put(DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseType.MSSQL.name(), mssqlScript);
        scripts.put(DatabaseType.H2.name(), new String[0]);
        runScript(scripts);

        updatePoints();
    }

    @Override
    protected String getNewSchemaVersion() {
        return "4";
    }

    private final String[] derbyScript = { //
    "alter table dataPoints add column name varchar(255);", //
            "alter table dataPoints add column deviceName varchar(255);", //
            "alter table dataPoints add column enabled char(1);", //
            "alter table dataPoints add column loggingType int;", //
            "alter table dataPoints add column intervalLoggingPeriodType int;", //
            "alter table dataPoints add column intervalLoggingPeriod int;", //
            "alter table dataPoints add column intervalLoggingType int;", //
            "alter table dataPoints add column tolerance double;", //
            "alter table dataPoints add column purgeType int;", //
            "alter table dataPoints add column purgePeriod int;", //
            "alter table dataPoints add column defaultCacheSize int;", //
            "alter table dataPoints add column discardExtremeValues char(1);", //
            "alter table dataPoints add column engineeringUnits int;", //
    };

    private final String[] mssqlScript = { //
    "alter table dataPoints add column name nvarchar(255);", //
            "alter table dataPoints add column deviceName nvarchar(255);", //
            "alter table dataPoints add column enabled char(1);", //
            "alter table dataPoints add column loggingType int;", //
            "alter table dataPoints add column intervalLoggingPeriodType int;", //
            "alter table dataPoints add column intervalLoggingPeriod int;", //
            "alter table dataPoints add column intervalLoggingType int;", //
            "alter table dataPoints add column tolerance float;", //
            "alter table dataPoints add column purgeType int;", //
            "alter table dataPoints add column purgePeriod int;", //
            "alter table dataPoints add column defaultCacheSize int;", //
            "alter table dataPoints add column discardExtremeValues char(1);", //
            "alter table dataPoints add column engineeringUnits int;", //
    };

    private final String[] mysqlScript = { //
    "alter table dataPoints add column name varchar(255);", //
            "alter table dataPoints add column deviceName varchar(255);", //
            "alter table dataPoints add column enabled char(1);", //
            "alter table dataPoints add column loggingType int;", //
            "alter table dataPoints add column intervalLoggingPeriodType int;", //
            "alter table dataPoints add column intervalLoggingPeriod int;", //
            "alter table dataPoints add column intervalLoggingType int;", //
            "alter table dataPoints add column tolerance double;", //
            "alter table dataPoints add column purgeType int;", //
            "alter table dataPoints add column purgePeriod int;", //
            "alter table dataPoints add column defaultCacheSize int;", //
            "alter table dataPoints add column discardExtremeValues char(1);", //
            "alter table dataPoints add column engineeringUnits int;", //
    };

    private void updatePoints() {
        // Get the points
        List<DataPointVO> dps = ejt.query("select dp.id, dp.xid, dp.dataSourceId, dp.data, ds.name, " //
                + "ds.xid, ds.dataSourceType " //
                + "from dataPoints dp join dataSources ds on ds.id = dp.dataSourceId ", new DataPointRowMapper());

        // Resave
        for (DataPointVO dp : dps)
            ejt.update(
                    "update dataPoints set xid=?, name=?, deviceName=?, enabled=?, loggingType=?, " //
                            + "intervalLoggingPeriodType=?, intervalLoggingPeriod=?, intervalLoggingType=?, " //
                            + "tolerance=?, purgeType=?, purgePeriod=?, defaultCacheSize=?, discardExtremeValues=?, " //
                            + "engineeringUnits=?, data=? where id=?", //
                    new Object[] { dp.getXid(), dp.getName(), dp.getDeviceName(), BaseDao.boolToChar(dp.isEnabled()),
                            dp.getLoggingType(), dp.getIntervalLoggingPeriodType(), dp.getIntervalLoggingPeriod(),
                            dp.getIntervalLoggingType(), dp.getTolerance(), dp.getPurgeType(), dp.getPurgePeriod(),
                            dp.getDefaultCacheSize(), BaseDao.boolToChar(dp.isDiscardExtremeValues()),
                            dp.getEngineeringUnits(), SerializationHelper.writeObject(dp), dp.getId() }, //
                    new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.CHAR, Types.INTEGER, Types.INTEGER,
                            Types.INTEGER, Types.INTEGER, Types.DOUBLE, Types.INTEGER, Types.INTEGER, Types.INTEGER,
                            Types.CHAR, Types.INTEGER, Types.BLOB, Types.INTEGER });
    }

    class DataPointRowMapper implements RowMapper<DataPointVO> {
        @Override
        public DataPointVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            DataPointVO dp = (DataPointVO) SerializationHelper.readObjectInContext(rs.getBlob(4).getBinaryStream());
            dp.setId(rs.getInt(1));
            dp.setXid(rs.getString(2));
            dp.setDataSourceId(rs.getInt(3));

            // Data source information.
            dp.setDataSourceName(rs.getString(5));
            dp.setDataSourceXid(rs.getString(6));
            dp.setDataSourceTypeName(rs.getString(7));

            return dp;
        }
    }
}
