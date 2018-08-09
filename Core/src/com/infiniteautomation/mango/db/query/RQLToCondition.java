/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.SortOrder;
import org.jooq.impl.DSL;

import net.jazdw.rql.parser.ASTNode;

/**
 * Transforms RQL node into a jOOQ Condition along with sort fields, limit and offset
 *
 * @author Jared Wiltshire
 */
public class RQLToCondition {

    protected final Map<String, Field<Object>> fieldMapping;
    protected final Map<String, Function<Object, Object>> valueConverterMap;

    protected List<SortField<Object>> sortFields = null;
    protected Integer limit = null;
    protected Integer offset = null;

    public RQLToCondition(Map<String, Field<Object>> fieldMapping, Map<String, Function<Object, Object>> valueConverterMap) {
        this.fieldMapping = fieldMapping;
        this.valueConverterMap = valueConverterMap;
    }

    public ConditionSortLimit visit(ASTNode node) {
        Condition condition = visitNode(node);
        return new ConditionSortLimit(condition, sortFields, limit, offset);
    }

    protected Condition visitNode(ASTNode node) {
        switch (node.getName()) {
            case "":
            case "and": {
                List<Condition> conditions = visitAndOr(node);
                return conditions.isEmpty() ? null : DSL.and(conditions);
            }
            case "or": {
                List<Condition> conditions = visitAndOr(node);
                return conditions.isEmpty() ? null : DSL.or(conditions);
            }
            case "sort":
                sortFields = getSortFields(node);
                return null;
            case "limit":
                limit = (Integer) node.getArgument(0);
                if (node.getArgumentsSize() > 1) {
                    offset = (Integer) node.getArgument(1);
                }
                return null;
            default:
                return visitConditionNode(node);
        }
    }

    protected Condition visitConditionNode(ASTNode node) {
        Field<Object> field = getField(node);
        Function<Object, Object> valueConverter = getValueConverter(node);
        Object firstArg = valueConverter.apply(node.getArgument(1));

        switch (node.getName().toLowerCase()) {
            case "eq":
                if (firstArg == null) {
                    return field.isNull();
                }
                return field.eq(firstArg);
            case "gt":
                return field.gt(firstArg);
            case "ge":
                return field.ge(firstArg);
            case "lt":
                return field.lt(firstArg);
            case "le":
                return field.le(firstArg);
            case "ne":
                if (firstArg == null) {
                    return field.isNotNull();
                }
                return field.ne(firstArg);
            case "match":
            case "like": {
                String like = ((String) firstArg).replace('*', '%');
                return field.likeIgnoreCase(like);
            }
            case "nmatch":
            case "nlike": {
                String nlike = ((String) firstArg).replace('*', '%');
                return field.notLikeIgnoreCase(nlike);
            }
            case "in":
                List<?> inArray;
                if (firstArg instanceof List) {
                    inArray = (List<?>) firstArg;
                } else {
                    inArray = node.getArguments().subList(1, node.getArgumentsSize())
                            .stream()
                            .map(valueConverter)
                            .collect(Collectors.toList());
                }
                return field.in(inArray);
            default:
                throw new RQLVisitException(String.format("Unknown node type '%s'", node.getName()));
        }
    }

    protected List<Condition> visitAndOr(ASTNode node) {
        List<Condition> conditions = new ArrayList<>();
        for (Object obj : node) {
            ASTNode arg = (ASTNode) obj;
            Condition result = visitNode(arg);
            if (result != null) {
                conditions.add(result);
            }
        }
        return conditions;
    }

    protected Field<Object> getField(ASTNode node) {
        String property = (String) node.getArgument(0);
        return getField(property);
    }

    protected Field<Object> getField(String property) {
        if (this.fieldMapping.containsKey(property)) {
            return this.fieldMapping.get(property);
        }
        throw new RQLVisitException(String.format("Unknown property '%s', valid properties are %s", property, fieldMapping.keySet().toString()));
    }

    protected Function<Object, Object> getValueConverter(ASTNode node) {
        String property = (String) node.getArgument(0);
        return getValueConverter(property);
    }

    protected Function<Object, Object> getValueConverter(String property) {
        if (this.valueConverterMap.containsKey(property)) {
            return this.valueConverterMap.get(property);
        }
        return Function.identity();
    }

    protected List<SortField<Object>> getSortFields(ASTNode node) {
        List<SortField<Object>> fields = new ArrayList<>(node.getArgumentsSize());

        for (Object obj : node) {
            boolean descending = false;
            String property = (String) obj;
            if (property.startsWith("-")) {
                descending = true;
                property = property.substring(1);
            } else if (property.startsWith("+")) {
                property = property.substring(1);
            }

            SortField<Object> field = getField(property).sort(descending ? SortOrder.DESC : SortOrder.ASC);
            fields.add(field);
        }

        return fields;
    }

    static class RQLVisitException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public RQLVisitException(String message, Throwable cause) {
            super(message, cause);
        }

        public RQLVisitException(String message) {
            super(message);
        }
    }
}