/**
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.db.query.pojo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.infiniteautomation.mango.db.query.RQLMatchToken;
import com.infiniteautomation.mango.db.query.RQLOperation;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;

import net.jazdw.rql.parser.ASTNode;

/**
 *  TODO This class could use a tune up as the visit method is
 *  called in the constructor so you can't override methods that use it
 *  which depend on fields in the superclass
 *
 */
public abstract class RQLFilter<T> implements UnaryOperator<Stream<T>> {

    private final Predicate<T> filter;
    private final ObjectComparator defaultComparator;
    private final Translations translations;
    private Long limit;
    private Long offset;
    private Comparator<T> sort;

    public RQLFilter(ASTNode node, Translations translations) {
        this.translations = translations;
        this.defaultComparator = new ObjectComparator(translations);
        this.filter = node == null ? null : this.visit(node);
    }

    @Override
    public Stream<T> apply(Stream<T> stream) {
        if (this.filter != null) {
            stream = stream.filter(filter);
        }
        if (this.sort != null) {
            stream = stream.sorted(this.sort);
        }
        if (this.offset != null) {
            stream = stream.skip(this.offset);
        }
        if (this.limit != null) {
            stream = stream.limit(this.limit);
        }
        return stream;
    }

    public long count(Stream<T> stream) {
        if (this.filter != null) {
            stream = stream.filter(filter);
        }
        return stream.count();
    }

    private Predicate<T> visit(ASTNode node) {
        RQLOperation comparison = RQLOperation.convertTo(node.getName().toLowerCase());
        return visit(comparison, node.getArguments());
    }

    private Predicate<T> visit(RQLOperation comparison, List<Object> arguments) {
        switch(comparison) {
            case AND: {
                List<Predicate<T>> childPredicates = childPredicates(arguments);
                return item -> {
                    return childPredicates.stream().allMatch(p -> p.test(item));
                };
            }
            case OR: {
                List<Predicate<T>> childPredicates = childPredicates(arguments);
                return item -> {
                    return childPredicates.stream().anyMatch(p -> p.test(item));
                };
            }
            case NOT: {
                return visit(RQLOperation.AND, arguments).negate();
            }
            case LIMIT: {
                applyLimit(arguments);
                return null;
            }
            case SORT: {
                applySort(arguments);
                return null;
            }
            case EQUAL_TO: {
                String property = mapPropertyName((String) arguments.get(0));
                Object target = convertRQLArgument(property, arguments.get(1));
                Comparator<Object> comparator = getComparator(property);
                return (item) -> comparator.compare(getProperty(item, property), target) == 0;
            }
            case NOT_EQUAL_TO: {
                return visit(RQLOperation.EQUAL_TO, arguments).negate();
            }
            case LESS_THAN: {
                String property = mapPropertyName((String) arguments.get(0));
                Object target = convertRQLArgument(property, arguments.get(1));
                Comparator<Object> comparator = getComparator(property);
                return (item) -> comparator.compare(getProperty(item, property), target) < 0;
            }
            case LESS_THAN_EQUAL_TO: {
                String property = mapPropertyName((String) arguments.get(0));
                Object target = convertRQLArgument(property, arguments.get(1));
                Comparator<Object> comparator = getComparator(property);
                return (item) -> comparator.compare(getProperty(item, property), target) <= 0;
            }
            case GREATER_THAN: {
                return visit(RQLOperation.LESS_THAN_EQUAL_TO, arguments).negate();
            }
            case GREATER_THAN_EQUAL_TO: {
                return visit(RQLOperation.LESS_THAN, arguments).negate();
            }
            case IN: {
                String property = mapPropertyName((String) arguments.get(0));

                List<?> args;
                if (arguments.get(1) instanceof List) {
                    args = (List<?>) arguments.get(1);
                } else {
                    args = arguments.subList(1, arguments.size());
                }
                List<?> convertedArgs = args.stream().map(v -> convertRQLArgument(property, v)).collect(Collectors.toList());
                Comparator<Object> comparator = getComparator(property);

                return item -> {
                    Object propertyValue = getProperty(item, property);
                    return convertedArgs.stream().anyMatch(arg -> {
                        return comparator.compare(propertyValue, arg) == 0;
                    });
                };
            }
            case MATCH: {
                String property = mapPropertyName((String) arguments.get(0));
                String matchString = convertRQLArgument(property, arguments.get(1)).toString();

                // Converts a match string containing * and ? into a regex pattern.
                String regex = RQLMatchToken.tokenize(matchString).map(t -> {
                    if (t == RQLMatchToken.SINGLE_CHARACTER_WILDCARD) {
                        return ".";
                    } else if (t == RQLMatchToken.MULTI_CHARACTER_WILDCARD) {
                        return ".*";
                    } else {
                        return Pattern.quote(t.toString());
                    }
                }).collect(Collectors.joining());

                boolean caseSensitive = false;
                if (arguments.size() > 2) {
                    caseSensitive = (boolean) arguments.get(2);
                }
                Pattern target = Pattern.compile(regex, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);

                return (item) -> {
                    Object value = getProperty(item, property);
                    if (value == null) {
                        return false;
                    }

                    String stringValue;
                    if (value instanceof TranslatableMessage) {
                        stringValue = ((TranslatableMessage) value).translate(this.translations);
                    } else {
                        stringValue = value.toString();
                    }

                    return target.matcher(stringValue).matches();
                };
            }
            case CONTAINS: {
                String property = mapPropertyName((String) arguments.get(0));
                Object target = convertRQLArgument(property, arguments.get(1));
                Comparator<Object> comparator = getComparator(property);

                return (item) -> {
                    Object value = getProperty(item, property);
                    if (value instanceof String) {
                        return ((String) value).contains((String) target);
                    } else if (value instanceof Collection) {
                        Collection<?> values = (Collection<?>) value;
                        return values.stream().anyMatch(v -> {
                            return comparator.compare(v, target) == 0;
                        });
                    }
                    else throw new UnsupportedOperationException("Cant search inside " + value.getClass());
                };
            }
            default:
                throw new UnsupportedOperationException("Unsupported RQL operation " + comparison);
        }
    }

    protected Object convertRQLArgument(String propertyName, Object argument) {
        return argument;
    }

    protected Comparator<Object> getComparator(String propertyName) {
        return this.defaultComparator;
    }

    protected Comparator<T> getSortComparator(String property) {
        Comparator<Object> objComparator = getComparator(property);
        return (a, b) -> {
            Object valueA = getProperty(a, property);
            Object valueB = getProperty(b, property);
            return objComparator.compare(valueA, valueB);
        };
    }

    private List<Predicate<T>> childPredicates(List<Object> arguments) {
        return arguments.stream()
                .map(arg -> this.visit((ASTNode) arg))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void applyLimit(List<Object> arguments) {
        if (!arguments.isEmpty()) {
            this.limit = ((Number) arguments.get(0)).longValue();
            this.offset = arguments.size() > 1 ? ((Number) arguments.get(1)).longValue() : 0;
        }
    }

    private void applySort(List<Object> arguments) {
        this.sort = null;
        for (Object arg : arguments) {
            boolean descending = false;
            String property = null;

            if (arg instanceof List) {
                List<Object> sortArgs = (List) arg;
                property = (String) sortArgs.get(0);
                if (sortArgs.size() > 1) {
                    descending = (boolean) sortArgs.get(1);
                }
            } else if (arg != null) {
                String argStr = (String) arg;
                if (argStr.startsWith("-")) {
                    descending = true;
                    property = argStr.substring(1);
                } else if (argStr.startsWith("+")) {
                    property = argStr.substring(1);
                } else {
                    property = argStr;
                }
            }

            Comparator<T> comparator = getSortComparator(mapPropertyName(property));
            if (descending) {
                comparator = comparator.reversed();
            }
            if (this.sort == null) {
                this.sort = comparator;
            } else {
                this.sort = this.sort.thenComparing(comparator);
            }
        }
    }

    /**
     * @param item
     * @param propertyName
     * @return String, Boolean, Integer, Long, BigInteger, Float, Double, BigDecimal or null. Return MissingProperty if property does not exist
     */
    protected abstract Object getProperty(T item, String propertyName);

    public static final class MissingProperty {
        public static final MissingProperty INSTANCE = new MissingProperty();
        private MissingProperty() {}
    }

    protected String mapPropertyName(String propertyName) {
        return propertyName;
    }

    public Predicate<T> getFilter() {
        return filter;
    }

    public Long getLimit() {
        return limit;
    }

    public Long getOffset() {
        return offset;
    }

    public Comparator<T> getSort() {
        return sort;
    }
}
