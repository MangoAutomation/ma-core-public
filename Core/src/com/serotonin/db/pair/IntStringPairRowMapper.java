/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.db.pair;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

/**
 * @author Matthew Lohbihler
 */
public class IntStringPairRowMapper implements RowMapper<IntStringPair> {
    private final int intIndex;
    private final int valueIndex;

    public IntStringPairRowMapper() {
        this(1, 2);
    }

    public IntStringPairRowMapper(int intIndex, int valueIndex) {
        this.intIndex = intIndex;
        this.valueIndex = valueIndex;
    }

    public IntStringPair mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new IntStringPair(rs.getInt(intIndex), rs.getString(valueIndex));
    }
}
