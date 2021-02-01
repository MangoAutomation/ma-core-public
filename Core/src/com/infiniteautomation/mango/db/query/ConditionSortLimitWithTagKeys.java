/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.List;
import java.util.Map;

import org.jooq.Condition;
import org.jooq.Name;
import org.jooq.SortField;

/**
 * Stores a map of tag keys used in the RQL query and maps them to the aliased column names.
 *
 * @author Jared Wiltshire
 */
public class ConditionSortLimitWithTagKeys extends ConditionSortLimit {
    private final Map<String, Name> tagKeyToColumn;

    public ConditionSortLimitWithTagKeys(Condition condition, List<SortField<?>> sort, Integer limit, Integer offset, Map<String, Name> tagKeyToColumn) {
        super(condition, sort, limit, offset);
        this.tagKeyToColumn = tagKeyToColumn;
    }

    public Map<String, Name> getTagKeyToColumn() {
        return tagKeyToColumn;
    }

    @Override
    public ConditionSortLimitWithTagKeys withNullLimitOffset() {
        return new ConditionSortLimitWithTagKeys(condition, sort, null, null, tagKeyToColumn);
    }
}