/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    int tagIndex = 0;
    final Map<String, Name> tagKeyToColumn = new HashMap<>();
    final boolean allPropertiesAreTags;

    public RQLToConditionWithTagKeys() {
        super(Collections.emptyMap());
        this.allPropertiesAreTags = true;
    }
    
    public RQLToConditionWithTagKeys(Map<String, Field<Object>> fieldMapping) {
        super(fieldMapping);
        this.allPropertiesAreTags = false;
    }
    
    @Override
    public ConditionSortLimitWithTagKeys visit(ASTNode node) {
        Condition condition = visitNode(node);
        return new ConditionSortLimitWithTagKeys(condition, sortFields, limit, offset, tagKeyToColumn);
    }
    
    @Override
    protected Field<Object> getField(String property) {
        String tagKey;

        if (allPropertiesAreTags) {
            tagKey = property;
        } else if (property.startsWith("tags.")) {
            tagKey = property.substring("tags.".length());
        } else {
            return super.getField(property);
        }
        
        Name columnName = columnNameForTagKey(tagKey);
        return DSL.field(DataPointTagsDao.DATA_POINT_TAGS_PIVOT_ALIAS.append(columnName));
    }
    
    public Name columnNameForTagKey(String tagKey) {
        return tagKeyToColumn.computeIfAbsent(tagKey, k -> DSL.name("key" + tagIndex++));
    }
}
