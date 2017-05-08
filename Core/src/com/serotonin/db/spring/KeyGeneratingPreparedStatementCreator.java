package com.serotonin.db.spring;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.util.Assert;

public class KeyGeneratingPreparedStatementCreator implements PreparedStatementCreator, SqlProvider {
    private final String sql;

    public KeyGeneratingPreparedStatementCreator(String sql) {
        Assert.notNull(sql, "SQL must not be null");
        this.sql = sql;
    }

    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        return con.prepareStatement(this.sql, Statement.RETURN_GENERATED_KEYS);
    }

    public String getSql() {
        return sql;
    }
}
