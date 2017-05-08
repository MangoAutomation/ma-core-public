package com.serotonin.db.pair;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class StringStringPairRowMapper implements RowMapper<StringStringPair> {
    private final int keyIndex;
    private final int valueIndex;

    public StringStringPairRowMapper() {
        this(1, 2);
    }

    public StringStringPairRowMapper(int keyIndex, int valueIndex) {
        this.keyIndex = keyIndex;
        this.valueIndex = valueIndex;
    }

    public StringStringPair mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new StringStringPair(rs.getString(keyIndex), rs.getString(valueIndex));
    }
}
