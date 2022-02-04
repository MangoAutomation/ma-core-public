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

    public static final Function<Object, Object> BOOLEAN_VALUE_CONVERTER = value -> {
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "Y" : "N";
        }
        return value;
    };

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
        try {
            sortFields = null;
            limit = null;
            offset = null;
            Condition condition = visitNode(node);
            return new ConditionSortLimit(condition, sortFields, limit, offset);
        } catch (Exception e) {
            throw new RQLVisitException("Exception while visiting RQL node", e);
        }
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
        String property = (String) node.getArgument(0);

        Field<Object> field = getField(property);
        Function<Object, Object> valueConverter = getValueConverter(field);
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
            case "permissionscontainsany":
            case "csvcontainsany":
                List<?> csvContainsAnyPermissionsArray;
                if (firstArg instanceof List) {
                    csvContainsAnyPermissionsArray = (List<?>) firstArg;
                } else {
                    csvContainsAnyPermissionsArray = node.getArguments().subList(1, node.getArgumentsSize())
                            .stream()
                            .map(valueConverter)
                            .collect(Collectors.toList());
                }
                return fieldMatchesAnyPermission(field, csvContainsAnyPermissionsArray);
            case "permissionscontainsall":
            case "csvcontainsall":
                List<?> csvContainsAllPermissionsArray;
                if (firstArg instanceof List) {
                    csvContainsAllPermissionsArray = (List<?>) firstArg;
                } else {
                    csvContainsAllPermissionsArray = node.getArguments().subList(1, node.getArgumentsSize())
                            .stream()
                            .map(valueConverter)
                            .collect(Collectors.toList());
                }
                return fieldMatchesAllPermissions(field, csvContainsAllPermissionsArray);

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

    protected Field<Object> getField(String property) {
        if (this.fieldMapping.containsKey(property)) {
            return this.fieldMapping.get(property);
        }
        throw new RQLVisitException(String.format("Unknown property '%s', valid properties are %s", property, fieldMapping.keySet().toString()));
    }

    protected Function<Object, Object> getValueConverter(Field<Object> field) {
        String name = field.getName();
        Function<Object, Object> converter = this.valueConverterMap.get(name);
        return converter == null ? Function.identity() : converter;
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

    public static final String PERMISSION_START_REGEX = "(^|[,])\\s*";
    public static final String PERMISSION_END_REGEX = "\\s*($|[,])";

    /**
     * Match any permission
     * @param field
     * @param userPermission
     * @return
     */
    protected Condition fieldMatchesAnyPermission(Field<Object> field, List<?> permissions) {
        if(permissions.size() == 0) {
            return DSL.trueCondition();
        }else {
            Condition or = null;
            for(Object permission : permissions) {
                if(or == null)
                    or = fieldMatchesPermission(field, permission);
                else
                    or = DSL.or(or, fieldMatchesPermission(field, permission));
            }
            return or;
        }
    }

    /**
     * Must match all permissions
     * @param field
     * @param userPermission
     * @return
     */
    protected Condition fieldMatchesAllPermissions(Field<Object> field, List<?> permissions) {
        if(permissions.size() == 0) {
            return DSL.trueCondition();
        }else {
            Condition and = null;
            for(Object permission : permissions) {
                if(and == null)
                    and = fieldMatchesPermission(field, permission);
                else
                    and = DSL.and(and, fieldMatchesPermission(field, permission));
            }
            return and;
        }
    }

    private Condition fieldMatchesPermission(Field<Object> field, Object permission) {
        //For now only support strings
        if(!(permission instanceof String))
            return DSL.falseCondition();
        return DSL.or(
                field.eq(permission),
                DSL.and(
                        field.isNotNull(),
                        field.notEqual(""),
                        field.likeRegex(PERMISSION_START_REGEX + permission + PERMISSION_END_REGEX)
                        )
                );
    }

    public static class RQLVisitException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private final ASTNode node;

        public RQLVisitException(String message, Throwable cause, ASTNode node) {
            super(message, cause);
            this.node = node;
        }

        public RQLVisitException(String message, Throwable cause) {
            super(message, cause);
            this.node = null;
        }

        public RQLVisitException(String message) {
            super(message);
            this.node = null;
        }

        public ASTNode getNode() {
            return node;
        }
    }
}