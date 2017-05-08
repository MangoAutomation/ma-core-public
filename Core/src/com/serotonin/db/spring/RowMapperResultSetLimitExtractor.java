package com.serotonin.db.spring;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

/**
 * Copied from spring to apply a limit to a list query. This extractor will break out of the extraction loop when the
 * limit has been reached. This allows applications to apply limits where the database does not support the keyword.
 * 
 * @author Matthew Lohbihler
 */
public class RowMapperResultSetLimitExtractor<T> implements ResultSetExtractor<List<T>> {
    private final RowMapper<T> rowMapper;
    private final int limit;

    /**
     * Constructor
     * 
     * @param rowMapper
     *            the row mapper object
     * @param limit
     *            the limit to apply to the results. 0 implies no limit.
     */
    public RowMapperResultSetLimitExtractor(RowMapper<T> rowMapper, int limit) {
        Assert.notNull(rowMapper, "RowMapper is required");
        this.rowMapper = rowMapper;
        this.limit = limit;
    }

    public List<T> extractData(ResultSet rs) throws SQLException {
        List<T> results = limit == 0 ? new LinkedList<T>() : new ArrayList<T>(limit);
        int rowNum = 0;
        while (rs.next()) {
            results.add(this.rowMapper.mapRow(rs, rowNum++));
            if (limit > 0 && rowNum >= limit)
                break;
        }
        return results;
    }
}
