/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Select;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import org.jooq.TableOnConditionStep;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import com.infiniteautomation.mango.db.query.ConditionSortLimitWithTagKeys;
import com.infiniteautomation.mango.db.query.RQLToConditionWithTagKeys;
import com.serotonin.m2m2.vo.User;

import net.jazdw.rql.parser.ASTNode;

/**
 * @author Jared Wiltshire
 */
public class DataPointTagsDao extends BaseDao {
    static final Log LOG = LogFactory.getLog(DataPointTagsDao.class);
    public static final DataPointTagsDao instance = new DataPointTagsDao();

    public static final Name DATA_POINT_TAGS_ALIAS = DSL.name("tags");
    public static final Table<Record> DATA_POINT_TAGS = DSL.table(DSL.name("dataPointTags")).as(DATA_POINT_TAGS_ALIAS);
    
    public static final Field<Integer> DATA_POINT_ID = DSL.field(DATA_POINT_TAGS_ALIAS.append("dataPointId"), SQLDataType.INTEGER.nullable(false));
    public static final Field<String> TAG_KEY = DSL.field(DATA_POINT_TAGS_ALIAS.append("tagKey"), SQLDataType.VARCHAR(255).nullable(false));
    public static final Field<String> TAG_VALUE = DSL.field(DATA_POINT_TAGS_ALIAS.append("tagValue"), SQLDataType.VARCHAR(255).nullable(false));

    public static final Name DATA_POINT_TAGS_PIVOT_ALIAS = DSL.name("tagsPivot");
    public static final Field<Integer> PIVOT_ALIAS_DATA_POINT_ID = DSL.field(DATA_POINT_TAGS_PIVOT_ALIAS.append(DATA_POINT_ID.getUnqualifiedName()), DATA_POINT_ID.getDataType());

    public Map<String, String> getTagsForDataPointId(int dataPointId) {
        Select<Record2<String, String>> query = this.create.select(TAG_KEY, TAG_VALUE)
            .from(DATA_POINT_TAGS)
            .where(DATA_POINT_ID.eq(dataPointId));

        try (Stream<Record2<String, String>> stream = query.stream()) {
            return stream.collect(Collectors.toMap(r -> r.value1(), r -> r.value2()));
        }
    }
    
    public int deleteTagsForDataPointId(int dataPointId) {
        return this.create.deleteFrom(DATA_POINT_TAGS)
                .where(DATA_POINT_ID.eq(dataPointId))
                .execute();
    }
    
    public void addTagsForDataPointId(int dataPointId, Map<String, String> tags) {
        BatchBindStep b = this.create.batch(this.create.insertInto(DATA_POINT_TAGS).columns(DATA_POINT_ID, TAG_KEY, TAG_VALUE).values((Integer) null, null, null));
        tags.entrySet().forEach(e -> b.bind(dataPointId, e.getKey(), e.getValue()));
        b.execute();
    }

    TableOnConditionStep<Record> joinPointPermissions(Table<Record> table, Field<Integer> dataPointIdField, User user) {
        Condition userHasPermission = DataPointDao.instance.userHasPermission(user);
        
        return table
                .join(DataPointDao.DATA_POINTS).on(dataPointIdField.eq(DataPointDao.ID))
                .join(DataSourceDao.DATA_SOURCES).on(DSL.and(DataPointDao.DATA_SOURCE_ID.eq(DataSourceDao.ID), userHasPermission));
    }
    
    public Set<String> getTagKeys(User user) {
        Table<Record> fromTable = DATA_POINT_TAGS;
        if (!user.isAdmin()) {
            fromTable = joinPointPermissions(fromTable, DATA_POINT_ID, user);
        }
        
        Select<Record1<String>> query = this.create.selectDistinct(TAG_KEY)
                .from(fromTable);
        
        try (Stream<Record1<String>> stream = query.stream()) {
            return stream.map(r -> r.value1()).collect(Collectors.toSet());
        }
    }
    
    public Set<String> getTagValuesForKey(String tagKey, User user) {
        Table<Record> fromTable = DATA_POINT_TAGS;
        if (!user.isAdmin()) {
            fromTable = joinPointPermissions(fromTable, DATA_POINT_ID, user);
        }
        
        Select<Record1<String>> query = this.create.selectDistinct(TAG_VALUE)
                .from(fromTable)
                .where(TAG_KEY.eq(tagKey));

        try (Stream<Record1<String>> stream = query.stream()) {
            return stream.map(r -> r.value1()).collect(Collectors.toSet());
        }
    }

    public Set<String> getTagValuesForKey(String tagKey, Map<String, String> restrictions, User user) {
        if (restrictions.isEmpty()) {
            return getTagValuesForKey(tagKey, user);
        }
        
        Set<String> keys = new HashSet<>();
        keys.addAll(restrictions.keySet());
        keys.add(tagKey);
        Map<String, Name> tagKeyToColumn = tagKeyToColumn(keys);
        
        Name tagKeyColumn = tagKeyToColumn.get(tagKey);

        List<Condition> conditions = restrictions.entrySet().stream().map(e -> {
            return DSL.field(DATA_POINT_TAGS_PIVOT_ALIAS.append(tagKeyToColumn.get(e.getKey()))).eq(e.getValue());
        }).collect(Collectors.toCollection(ArrayList::new));

        Table<Record> from = createTagPivotSql(tagKeyToColumn).asTable().as(DATA_POINT_TAGS_PIVOT_ALIAS);
        if (!user.isAdmin()) {
            from = joinPointPermissions(from, PIVOT_ALIAS_DATA_POINT_ID, user);
        }

        Select<Record1<String>> result = this.create
                .selectDistinct(DSL.field(DATA_POINT_TAGS_PIVOT_ALIAS.append(tagKeyColumn), String.class))
                .from(from)
                .where(DSL.and(conditions));

        try (Stream<Record1<String>> stream = result.stream()) {
            return stream.map(r -> r.value1()).collect(Collectors.toSet());
        }
    }

    public Set<String> getTagValuesForKey(String tagKey, ASTNode restrictions, User user) {
        RQLToConditionWithTagKeys visitor = new RQLToConditionWithTagKeys();
        Name tagKeyColumn = visitor.columnNameForTagKey(tagKey);

        List<Condition> conditionList = new ArrayList<>();
        ConditionSortLimitWithTagKeys conditions = visitor.visit(restrictions);
        if (conditions.getCondition() != null) {
            conditionList.add(conditions.getCondition());
        }
        
        Map<String, Name> tagKeyToColumn = conditions.getTagKeyToColumn();
        
        Table<Record> from = createTagPivotSql(tagKeyToColumn).asTable().as(DATA_POINT_TAGS_PIVOT_ALIAS);
        if (!user.isAdmin()) {
            from = joinPointPermissions(from, PIVOT_ALIAS_DATA_POINT_ID, user);
        }

        SelectJoinStep<Record1<String>> query = this.create
            .selectDistinct(DSL.field(DATA_POINT_TAGS_PIVOT_ALIAS.append(tagKeyColumn), String.class))
            .from(from);

        Select<Record1<String>> result = query;

        if (!conditionList.isEmpty()) {
            result = query.where(DSL.and(conditionList));
        }

        try (Stream<Record1<String>> stream = result.stream()) {
            return stream.map(r -> r.value1()).collect(Collectors.toSet());
        }
    }

    /**
     * This method does not filter the tags based on the data point permissions. It should only be used
     * when joining onto the data points table (the filtering happens there post-join).
     * 
     * @param tagKeyToColumn
     * @return
     */
    Select<Record> createTagPivotSql(Map<String, Name> tagKeyToColumn) {
        List<Field<?>> fields = new ArrayList<>(tagKeyToColumn.size() + 1);
        fields.add(DATA_POINT_ID);

        for (Entry<String, Name> entry : tagKeyToColumn.entrySet()) {
            fields.add(DSL.max(DSL.when(TAG_KEY.eq(entry.getKey()), TAG_VALUE)).as(entry.getValue()));
        }
        
        return DSL.select(fields).from(DATA_POINT_TAGS).groupBy(DATA_POINT_ID);
    }

    /**
     * Maps tag keys to generic keyX to prevent SQL injection
     * @return
     */
    Map<String, Name> tagKeyToColumn(Set<String> tagKeys) {
        int i = 1;
        Map<String, Name> tagNameToColumn = new HashMap<>(tagKeys.size());
        for (String key : tagKeys) {
            String fieldName = String.format("key%d", i++);
            tagNameToColumn.put(key, DSL.name(fieldName));
        }
        return tagNameToColumn;
    }
}
