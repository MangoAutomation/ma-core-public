/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.List;

import org.jooq.Condition;
import org.jooq.SortField;
import org.jooq.impl.DSL;

import com.serotonin.m2m2.db.dao.AbstractBasicDao;

/**
 * @author Jared Wiltshire
 */
public class ConditionSortLimit {
    private Condition condition;
    private final List<SortField<Object>> sort;
    private Integer limit;
    private Integer offset;

    public ConditionSortLimit(Condition condition, List<SortField<Object>> sort, Integer limit, Integer offset) {
        this.condition = condition;
        this.sort = sort;
        this.limit = limit == null ? AbstractBasicDao.DEFAULT_LIMIT : limit;
        this.offset = offset;
    }

    public Condition getCondition() {
        return condition;
    }

    public void addCondition(Condition c) {
        if (condition == null) {
            condition = c;
        } else {
            condition = DSL.and(condition, c);
        }
    }

    public void orCondition(Condition c) {
        if (condition == null) {
            condition = c;
        } else {
            condition = DSL.or(condition, c);
        }
    }

    public List<SortField<Object>> getSort() {
        return sort;
    }

    public Integer getLimit() {
        return limit;
    }

    /**
     * Set limit to null, when going to filter manually
     */
    public void nullLimit() {
        this.limit = null;
    }

    public Integer getOffset() {
        return offset;
    }

    /**
     * Set offset to null, when going to filter manually
     */
    public void nullOffset() {
        this.offset = null;
    }
}