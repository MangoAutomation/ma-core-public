/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    
    protected List<SortField<Object>> sortFields = null;
    protected Integer limit = null;
    protected Integer offset = null;

    public RQLToCondition(Map<String, Field<Object>> fieldMapping) {
        this.fieldMapping = fieldMapping;
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
        switch (node.getName()) {
        case "eq":
            if (node.getArgument(1) == null) {
                return getField(node).isNull();
            }
            return getField(node).eq(node.getArgument(1));
        case "gt":
            return getField(node).gt(node.getArgument(1));
        case "ge":
            return getField(node).ge(node.getArgument(1));
        case "lt":
            return getField(node).lt(node.getArgument(1));
        case "le":
            return getField(node).le(node.getArgument(1));
        case "ne":
            if (node.getArgument(1) == null) {
                return getField(node).isNotNull();
            }
            return getField(node).ne(node.getArgument(1));
        case "match":
        case "like":
            return getField(node).like((String) node.getArgument(1));
        case "in":
            List<?> inArray;
            if (node.getArgument(1) instanceof List ) {
                inArray = (List<?>) node.getArgument(1);
            } else {
                inArray = node.getArguments().subList(1, node.getArgumentsSize());
            }
            return getField(node).in(inArray);
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