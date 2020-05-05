/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Operator;
import org.jooq.SortField;
import org.jooq.SortOrder;
import org.jooq.impl.DSL;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.util.exception.TranslatableIllegalArgumentException;
import com.infiniteautomation.mango.util.exception.TranslatableRuntimeException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

public class QueryBuilder<T> {

    protected static class Group {
        protected final boolean not;
        protected final Operator operator;
        protected final List<Condition> conditions = new ArrayList<>();

        protected Group(Operator operator) {
            this.operator = operator;
            this.not = false;
        }

        protected Group(Operator operator, boolean not) {
            this.operator = operator;
            this.not = not;
        }

        public Condition toCondition() {
            Condition condition = DSL.condition(operator, conditions);
            return not ? DSL.not(condition) : condition;
        }
    }

    protected final Map<String, Field<?>> fields;
    protected final Map<String, Function<Object, Object>> valueConverter;
    protected final Function<ConditionSortLimit, Integer> countFn;
    protected final BiConsumer<ConditionSortLimit, Consumer<T>> queryFn;

    protected Deque<Group> stack = new ArrayDeque<>();
    protected Group group = new Group(Operator.AND);
    protected List<SortField<Object>> sort = new ArrayList<>();

    protected QueryBuilder(Map<String, Field<?>> fields, Map<String, Function<Object, Object>> valueConverter, Function<ConditionSortLimit, Integer> countFn, BiConsumer<ConditionSortLimit, Consumer<T>> queryFn) {
        this.fields = fields;
        this.valueConverter = valueConverter;
        this.countFn = countFn;
        this.queryFn = queryFn;
    }

    public int count() {
        if (!stack.isEmpty()) {
            throw new TranslatableRuntimeException(new TranslatableMessage("dao.query.closeGroups"));
        }
        return countFn.apply(new ConditionSortLimit(group.toCondition(), null, null, null));
    }

    public void query(Consumer<T> consumer) {
        query(consumer, null, null);
    }

    public void query(Consumer<T> consumer, Integer limit, Integer offset) {
        if (!stack.isEmpty()) {
            throw new TranslatableRuntimeException(new TranslatableMessage("dao.query.closeGroups"));
        }
        queryFn.accept(new ConditionSortLimit(group.toCondition(), sort, limit, offset), consumer);
    }

    public List<T> query() {
        return query(null, null);
    }

    public List<T> query(Integer limit, Integer offset) {
        List<T> list = new ArrayList<>();
        query(list::add, limit, offset);
        return list;
    }

    public QueryBuilder<T> and() {
        stack.push(group);
        group = new Group(Operator.AND);
        return this;
    }

    public QueryBuilder<T> or() {
        stack.push(group);
        group = new Group(Operator.OR);
        return this;
    }

    public QueryBuilder<T> not() {
        stack.push(group);
        group = new Group(Operator.AND, true);
        return this;
    }

    public QueryBuilder<T> close() {
        if (stack.isEmpty()) {
            throw new TranslatableRuntimeException(new TranslatableMessage("dao.query.noGroupOpen"));
        }
        Condition condition = group.toCondition();
        group = stack.pop();
        group.conditions.add(condition);
        return this;
    }

    public QueryBuilder<T> sort(String fieldName) {
        return sort(fieldName, true);
    }

    public QueryBuilder<T> sort(String fieldName, boolean ascending) {
        SortField<Object> field = getField(fieldName).sort(ascending ? SortOrder.ASC : SortOrder.DESC);
        sort.add(field);
        return this;
    }

    protected QueryBuilder<T> applyFn(String fieldName, BiFunction<Field<Object>, Function<Object, Object>, Condition> fn) {
        Field<Object> field = getField(fieldName);
        Function<Object, Object> converter = valueConverter.getOrDefault(field.getName(), Function.identity());
        group.conditions.add(fn.apply(field, converter));
        return this;
    }

    public QueryBuilder<T> isNull(String fieldName) {
        return applyFn(fieldName, (f, c) -> f.isNull());
    }

    public QueryBuilder<T> isNotNull(String fieldName) {
        return applyFn(fieldName, (f, c) -> f.isNotNull());
    }

    public QueryBuilder<T> equal(String fieldName, Object value) {
        return applyFn(fieldName, (f, c) -> f.equal(c.apply(value)));
    }

    public QueryBuilder<T> notEqual(String fieldName, Object value) {
        return applyFn(fieldName, (f, c) -> f.notEqual(c.apply(value)));
    }

    public QueryBuilder<T> lessThan(String fieldName, Object value) {
        return applyFn(fieldName, (f, c) -> f.lessThan(c.apply(value)));
    }

    public QueryBuilder<T> lessOrEqual(String fieldName, Object value) {
        return applyFn(fieldName, (f, c) -> f.lessOrEqual(c.apply(value)));
    }

    public QueryBuilder<T> greaterThan(String fieldName, Object value) {
        return applyFn(fieldName, (f, c) -> f.greaterThan(c.apply(value)));
    }

    public QueryBuilder<T> greaterOrEqual(String fieldName, Object value) {
        return applyFn(fieldName, (f, c) -> f.greaterOrEqual(c.apply(value)));
    }

    public QueryBuilder<T> in(String fieldName, Object... values) {
        return applyFn(fieldName, (f, c) -> {
            Object[] v = Arrays.stream(values).map(c).toArray(Object[]::new);
            return f.in(c.apply(v));
        });
    }

    public QueryBuilder<T> like(String fieldName, Object value) {
        return applyFn(fieldName, (f, c) -> f.contains(c.apply(value)));
    }

    protected Field<Object> getField(String fieldName) {
        Field<?> field = fields.get(fieldName);
        if (field == null) {
            throw new TranslatableIllegalArgumentException(new TranslatableMessage("dao.query.unknownField", fieldName, fields.keySet().toString()));
        }
        return field.coerce(Object.class);
    }

    @Override
    public String toString() {
        return group.toCondition().toString();
    }
}