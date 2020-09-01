/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import com.serotonin.m2m2.db.dao.BaseDao;
import net.jazdw.rql.parser.ASTNode;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.SortOrder;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Transforms RQL node into a jOOQ Condition along with sort fields, limit and offset
 *
 * @author Jared Wiltshire
 */
public class RQLToCondition {

    private static final Pattern LIKE_PATTERN_ESCAPE = Pattern.compile("(!|%|_)");

    public static final Function<Object, Object> BOOLEAN_VALUE_CONVERTER = value -> {
        if (value instanceof Boolean) {
            return ((Boolean) value) ? BaseDao.Y : BaseDao.N;
        }
        return value;
    };

    protected final Map<String, RQLSubSelectCondition> subSelectMapping;
    protected final Map<String, Field<?>> fieldMapping;
    protected final Map<String, Function<Object, Object>> valueConverterMap;

    protected List<SortField<Object>> sortFields = null;
    protected Integer limit = null;
    protected Integer offset = null;

    /**
     *
     * @param subSelectMapping - not null
     * @param fieldMapping - not null
     * @param valueConverterMap - not null
     */
    public RQLToCondition(Map<String, RQLSubSelectCondition> subSelectMapping, Map<String, Field<?>> fieldMapping, Map<String, Function<Object, Object>> valueConverterMap) {
        this.subSelectMapping = subSelectMapping;
        this.fieldMapping = fieldMapping;
        this.valueConverterMap = valueConverterMap;
    }

    public ConditionSortLimit visit(ASTNode node) {
        try {
            Condition condition = visitNode(node);
            return new ConditionSortLimit(condition, sortFields, limit, offset);
        } catch (Exception e) {
            throw new RQLVisitException("Exception while visiting RQL node", e);
        }
    }

    protected Condition visitNode(ASTNode node) {
        RQLOperation operation = RQLOperation.convertTo(node.getName().toLowerCase(Locale.ROOT));

        switch (operation) {
            case AND: {
                List<Condition> conditions = visitAndOr(node);
                return conditions.isEmpty() ? null : DSL.and(conditions);
            }
            case OR: {
                List<Condition> conditions = visitAndOr(node);
                return conditions.isEmpty() ? null : DSL.or(conditions);
            }
            case NOT: {
                List<Condition> conditions = visitAndOr(node);
                return conditions.isEmpty() ? null : DSL.not(DSL.and(conditions));
            }
            case SORT:
                sortFields = getSortFields(node);
                return null;
            case LIMIT:
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
        List<Object> arguments = node.getArguments();
        String property = (String) arguments.get(0);
        RQLOperation operation = RQLOperation.convertTo(node.getName().toLowerCase(Locale.ROOT));

        Field<Object> field = getField(property);
        if(field != null) {
            Function<Object, Object> valueConverter = getValueConverter(field);
            Object firstArg = valueConverter.apply(arguments.get(1));

            switch (operation) {
                case EQUAL_TO:
                    if (firstArg == null) {
                        return field.isNull();
                    }
                    return field.eq(firstArg);
                case GREATER_THAN:
                    return field.gt(firstArg);
                case GREATER_THAN_EQUAL_TO:
                    return field.ge(firstArg);
                case LESS_THAN:
                    return field.lt(firstArg);
                case LESS_THAN_EQUAL_TO:
                    return field.le(firstArg);
                case NOT_EQUAL_TO:
                    if (firstArg == null) {
                        return field.isNotNull();
                    }
                    return field.ne(firstArg);
                case MATCH: {
                    boolean caseSensitive = false;
                    if (arguments.size() > 2) {
                        caseSensitive = (boolean) arguments.get(2);
                    }

                    String matchString = ((String) firstArg);

                    // Converts a match string containing * and ? a SQL like pattern
                    String likeText = RQLMatchToken.tokenize(matchString).map(t -> {
                        if (t == RQLMatchToken.SINGLE_CHARACTER_WILDCARD) {
                            return "_";
                        } else if (t == RQLMatchToken.MULTI_CHARACTER_WILDCARD) {
                            return "%";
                        } else {
                            return LIKE_PATTERN_ESCAPE.matcher(t.toString()).replaceAll("!$1");
                        }
                    }).collect(Collectors.joining());

                    if (caseSensitive) {
                        return field.like(likeText).escape('!');
                    } else {
                        return field.likeIgnoreCase(likeText).escape('!');
                    }
                }
                case IN:
                    Stream<?> inArray;
                    if (firstArg instanceof List) {
                        inArray = ((List<?>) firstArg).stream();
                    } else {
                        inArray = arguments.stream().skip(1);
                    }
                    return field.in(inArray.map(valueConverter).collect(Collectors.toList()));
                default:
                    throw new RQLVisitException(String.format("Unknown node type '%s'", node.getName()));
            }
        }else {
            //Sub select conditions
            RQLSubSelectCondition condition = this.subSelectMapping.get(property);
            if(condition == null) {
                List<String> properties = Stream.concat(fieldMapping.keySet().stream(), subSelectMapping.keySet().stream()).collect(Collectors.toList());
                throw new RQLVisitException(String.format("Unknown property '%s', valid properties are %s", property, properties));
            }else {
                return condition.createCondition(operation, node);
            }
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

    @SuppressWarnings("unchecked")
    protected <T> Field<T> getField(String property) {
        if (this.fieldMapping.containsKey(property)) {
            return (Field<T>) this.fieldMapping.get(property);
        }else {
            return null;
        }
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