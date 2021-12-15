/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.List;

import org.jooq.Condition;
import org.jooq.SortField;

/**
 * @author Jared Wiltshire
 */
public class ConditionSortLimit {
    protected Condition condition;
    protected final List<SortField<?>> sort;
    protected final Integer limit;
    protected final Integer offset;

    public ConditionSortLimit(Condition condition, List<SortField<?>> sort, Integer limit, Integer offset) {
        this.condition = condition;
        this.sort = sort;
        this.limit = limit;
        this.offset = offset;
    }

    public Condition getCondition() {
        return condition;
    }

    public List<SortField<?>> getSort() {
        return sort;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public ConditionSortLimit withNullLimitOffset() {
        return new ConditionSortLimit(condition, sort, null, null);
    }

    /**
     * Add an AND condition
     */
    public void andCondition(Condition and) {
        if(condition != null) {
            condition = condition.and(and);
        }else {
            condition = and;
        }
    }

    /**
     * Add an OR condition
     */
    public void orCondition(Condition or) {
        if(condition != null) {
            condition = condition.or(or);
        }else {
            condition = or;
        }
    }

}