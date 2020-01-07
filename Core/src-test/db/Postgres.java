package db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowCallbackHandler;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.PostgresProxy;
import com.serotonin.m2m2.db.dao.BaseDao;

public class Postgres {
    public static void main(String[] args) {
        //Common.envProps = new ReloadingProperties("test-env");

        //        DerbyProxy proxy = new DerbyProxy();
        //        MySQLProxy proxy = new MySQLProxy();
        PostgresProxy proxy = new PostgresProxy();
        Common.databaseProxy = proxy;
        proxy.initialize(null);

        new BaseDao() {
            public void test() {
                //System.out.println(ejt.queryForList("SELECT username FROM users", String.class));

                byte[] data = "data".getBytes();
                //                int id = doInsert("insert into dataSources (xid, name, dataSourceType, data) values (?,?,?,?)",
                //                        new Object[] { "xid", "name", "adf", new ByteArrayInputStream(data) }, new int[] {
                //                                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BINARY });
                //                System.out.println(id);
                //
                //                ejt.query("select * from dataSources", new RowCallbackHandler() {
                //                    @Override
                //                    public void processRow(ResultSet rs) throws SQLException {
                //                        System.out.println(rs.getInt(1));
                //                        System.out.println(rs.getString(2));
                //                        System.out.println(rs.getString(3));
                //                        System.out.println(rs.getString(4));
                //                        System.out.println(new String(rs.getBytes(5)));
                //                    }
                //                });

                //                double d = 1E+308;
                //                doInsert("insert into dataPoints (xid, dataSourceId, tolerance, data) values (?,?,?,?)", new Object[] {
                //                        "xid", 1, d, new ByteArrayInputStream(data) }, new int[] { Types.VARCHAR, Types.INTEGER,
                //                        Types.DOUBLE, Types.BINARY });

                ejt.update("update dataPoints set tolerance=?", new Object[] { -Double.MAX_VALUE });

                ejt.query("select id, xid, dataSourceId, tolerance, data from dataPoints", new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        System.out.println(rs.getInt(1));
                        System.out.println(rs.getString(2));
                        System.out.println(rs.getInt(3));
                        System.out.println(rs.getDouble(4));
                        System.out.println(new String(rs.getBytes(5)));
                    }
                });

                //
                //                CREATE TABLE dataPoints (
                //                        id SERIAL,
                //                        xid varchar(50) NOT NULL,
                //                        dataSourceId integer NOT NULL,
                //                        name varchar(255),
                //                        deviceName varchar(255),
                //                        enabled character(1),
                //                        pointFolderId integer,
                //                        loggingType integer,
                //                        intervalLoggingPeriodType integer,
                //                        intervalLoggingPeriod integer,
                //                        intervalLoggingType integer,
                //                        tolerance double precision,
                //                        purgeOverride character(1),
                //                        purgeType integer,
                //                        purgePeriod integer,
                //                        defaultCacheSize integer,
                //                        discardExtremeValues character(1),
                //                        engineeringUnits integer,
                //                        data bytea NOT NULL,
                //                        PRIMARY KEY (id)
                //                      );

            }

            {
                test();
            }
        };
    }
}
