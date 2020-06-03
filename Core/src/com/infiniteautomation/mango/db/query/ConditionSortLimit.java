/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.List;

import org.jooq.Condition;
import org.jooq.SortField;

/**
 * @author Jared Wiltshire
 */
public class ConditionSortLimit {
    private final Condition condition;
    private final List<SortField<Object>> sort;
    private final Integer limit;
    private final Integer offset;

    public ConditionSortLimit(Condition condition, List<SortField<Object>> sort, Integer limit, Integer offset) {
        this.condition = condition;
        this.sort = sort;
        this.limit = limit;
        this.offset = offset;
    }

    public Condition getCondition() {
        return condition;
    }

    public List<SortField<Object>> getSort() {
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
}