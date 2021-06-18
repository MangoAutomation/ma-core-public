/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.Condition;
import org.jooq.Field;

import com.infiniteautomation.mango.db.tables.DataPointTags;

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
    final Map<String, Field<String>> tagFields = new HashMap<>();
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
     * @param fieldMapping map of RQL property name to SQL field
     * @param valueConverterMap map of field name to a converter function, converter function converts RQL arguments to a value able to be compared to the SQL field
     */
    public RQLToConditionWithTagKeys(@NonNull Map<String, Field<?>> fieldMapping,
                                     @NonNull Map<String, Function<Object, Object>> valueConverterMap) {
        super(Collections.emptyMap(), fieldMapping, valueConverterMap);
        this.allPropertiesAreTags = false;
    }

    @Override
    public ConditionSortLimitWithTagKeys visit(ASTNode node) {
        try {
            Condition condition = visitNode(node);
            return new ConditionSortLimitWithTagKeys(condition, sortFields, limit, offset, tagFields);
        } catch (Exception e) {
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

        return (Field<T>) getTagField(tagKey);
    }

    public Field<String> getTagField(String tagKey) {
        return tagFields.computeIfAbsent(tagKey, k -> DataPointTags.DATA_POINT_TAGS.as("key" + tagIndex++).tagValue);
    }
}
