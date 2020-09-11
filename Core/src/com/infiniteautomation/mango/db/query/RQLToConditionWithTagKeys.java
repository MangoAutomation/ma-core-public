/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;

import com.serotonin.m2m2.db.dao.DataPointTagsDao;

import net.jazdw.rql.parser.ASTNode;

/**
 * Transforms RQL node into a jOOQ Condition along with sort fields, limit and offset.
 * Stores a map of tag keys used in the RQL query and maps them to the aliased column names.
 *
 * @author Jared Wiltshire
 */
public class RQLToConditionWithTagKeys extends RQLToCondition {

    public static final String TAGS_PREFIX = "tags.";
    public static final int TAGS_PREFIX_LENGTH = TAGS_PREFIX.length();

    int tagIndex = 0;
    final Map<String, Name> tagKeyToColumn = new HashMap<>();
    final boolean allPropertiesAreTags;

    /**
     * This constructor is only used when querying the data point tags table
     */
    public RQLToConditionWithTagKeys() {
        super(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        this.allPropertiesAreTags = true;
    }

    /**
     * This constructor is used when joining tags onto another table e.g. the data points table
     *
     * @param fieldMapping
     * @param valueConverterMap
     */
    public RQLToConditionWithTagKeys(Map<String, Field<?>> fieldMapping, Map<String, Function<Object, Object>> valueConverterMap) {
        super(Collections.emptyMap(), fieldMapping, valueConverterMap);
        this.allPropertiesAreTags = false;
    }

    @Override
    public ConditionSortLimitWithTagKeys visit(ASTNode node) {
        try {
            Condition condition = visitNode(node);
            return new ConditionSortLimitWithTagKeys(condition, sortFields, limit, offset, tagKeyToColumn);
        } catch(Exception e) {
            throw new RQLVisitException("Exception while visiting RQL node", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> Field<T> getField(String property) {
        String tagKey;

        if (allPropertiesAreTags) {
            tagKey = property;
        } else if (property.startsWith(TAGS_PREFIX)) {
            tagKey = property.substring(TAGS_PREFIX_LENGTH);
        } else {
            return super.getField(property);
        }

        Name columnName = columnNameForTagKey(tagKey);
        return (Field<T>) DSL.field(DataPointTagsDao.DATA_POINT_TAGS_PIVOT_ALIAS.append(columnName));
    }

    public Name columnNameForTagKey(String tagKey) {
        return tagKeyToColumn.computeIfAbsent(tagKey, k -> DSL.name("key" + tagIndex++));
    }
}
