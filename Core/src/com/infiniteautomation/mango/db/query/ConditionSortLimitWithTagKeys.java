/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.List;
import java.util.Map;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SortField;

/**
 * Stores a map of tag keys used in the RQL query and maps them to the aliased column names.
 *
 * @author Jared Wiltshire
 */
public class ConditionSortLimitWithTagKeys extends ConditionSortLimit {
    private final Map<String, Field<String>> tagFields;

    public ConditionSortLimitWithTagKeys(Condition condition, List<SortField<?>> sort, Integer limit, Integer offset, Map<String, Field<String>> tagFields) {
        super(condition, sort, limit, offset);
        this.tagFields = tagFields;
    }

    public Map<String, Field<String>> getTagFields() {
        return tagFields;
    }

    @Override
    public ConditionSortLimitWithTagKeys withNullLimitOffset() {
        return new ConditionSortLimitWithTagKeys(condition, sort, null, null, tagFields);
    }
}