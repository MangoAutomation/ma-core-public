package com.serotonin.m2m2.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.postgresql.util.PSQLException;
import org.springframework.jdbc.BadSqlGrammarException;

import com.infiniteautomation.mango.spring.DatabaseProxyConfiguration;

public class PostgresProxy extends BasePooledProxy {
    public PostgresProxy(DatabaseProxyFactory factory, DatabaseProxyConfiguration configuration) {
        super(factory, configuration);
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.POSTGRES;
    }

    @Override
    protected String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    protected String getUrl(String propertyPrefix) {
        String url = super.getUrl(propertyPrefix);
        if (url.indexOf('?') > 0)
            url += "&";
        else
            url += "?";
        url += "autosave=always";
        return url;
    }

    @Override
    public boolean tableExists(String tableName) {
        try {
            getJdbcTemplate().execute("SELECT COUNT(*) FROM " + tableName);
        }
        catch (BadSqlGrammarException e) {
            if (e.getCause() instanceof PSQLException) {
                SQLException se = (SQLException) e.getCause();
                // This state means a missing table.
                return !"42P01".equals(se.getSQLState());
            }
            throw e;
        }
        return true;
    }

    @Override
    public String getTableListQuery() {
        return "SELECT table_name FROM information_schema.tables "
                + "WHERE table_catalog=current_database() AND table_schema=current_schema()";
    }

    @Override
    public void clean() {
        String testUrl = env.getProperty("db.test.url");
        String dbName = env.getProperty("db.test.name");
        String username = env.getProperty("db.username");
        String password = env.getProperty("db.password");

        try {
            int majorVersion;
            int minorVersion;
            try (var connection = getDataSource().getConnection()) {
                majorVersion = connection.getMetaData().getDatabaseMajorVersion();
                minorVersion = connection.getMetaData().getDatabaseMinorVersion();
            }

            String pidKey = "pid";
            // https://www.postgresql.org/docs/release/9.2.0/
            if (majorVersion < 9 || majorVersion == 9 && minorVersion < 2) {
                pidKey = "procpid";
            }
            try (Connection conn = DriverManager.getConnection(testUrl, username, password)) {
                Statement stmt = conn.createStatement();
                stmt.execute(String.format("SELECT pg_terminate_backend(%s) FROM pg_stat_activity WHERE %s <> pg_backend_pid() AND datname = '%s'", pidKey, pidKey, dbName));
                stmt.executeUpdate(String.format("DROP DATABASE %s WITH (FORCE)", dbName));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
