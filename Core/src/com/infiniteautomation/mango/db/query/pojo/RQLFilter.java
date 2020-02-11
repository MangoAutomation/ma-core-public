/**
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.db.query.pojo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.infiniteautomation.mango.db.query.RQLOperation;

import net.jazdw.rql.parser.ASTNode;

public abstract class RQLFilter<T> implements UnaryOperator<Stream<T>> {

    private static final Pattern MATCH_ALLOWED = Pattern.compile("\\*|\\?");

    private final Predicate<T> filter;
    private Long limit;
    private Long offset;
    private Comparator<T> comparator;

    public RQLFilter(ASTNode node) {
        this.filter = node == null ? null : this.visit(node);
    }

    @Override
    public Stream<T> apply(Stream<T> stream) {
        if (this.filter != null) {
            stream = stream.filter(filter);
        }
        if (this.comparator != null) {
            stream = stream.sorted(this.comparator);
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
                String property = (String) arguments.get(0);
                Object target = arguments.get(1);
                return (item) -> ObjectComparator.INSTANCE.compare(getProperty(item, property), target) == 0;
            }
            case NOT_EQUAL_TO: {
                return visit(RQLOperation.EQUAL_TO, arguments).negate();
            }
            case LESS_THAN: {
                String property = (String) arguments.get(0);
                Object target = arguments.get(1);
                return (item) -> ObjectComparator.INSTANCE.compare(getProperty(item, property), target) < 0;
            }
            case LESS_THAN_EQUAL_TO: {
                String property = (String) arguments.get(0);
                Object target = arguments.get(1);
                return (item) -> ObjectComparator.INSTANCE.compare(getProperty(item, property), target) <= 0;
            }
            case GREATER_THAN: {
                return visit(RQLOperation.LESS_THAN_EQUAL_TO, arguments).negate();
            }
            case GREATER_THAN_EQUAL_TO: {
                return visit(RQLOperation.LESS_THAN, arguments).negate();
            }
            case IN: {
                String property = (String) arguments.get(0);
                List<?> args;
                if (arguments.get(1) instanceof List) {
                    args = (List<?>) arguments.get(1);
                } else {
                    args = arguments.subList(1, arguments.size());
                }

                return item -> {
                    return args.stream().anyMatch(arg -> {
                        return ObjectComparator.INSTANCE.compare(getProperty(item, property), arg) == 0;
                    });
                };
            }
            case MATCH: {
                String property = (String) arguments.get(0);

                // we only want to allow the star special character in our like operation
                // convert the expression to a literal pattern then replace all literal star characters with .* regex
                String matchString = (String) arguments.get(1);
                String regex = tokenize(MATCH_ALLOWED, matchString).stream().map(t -> {
                    if ("*".equals(t)) {
                        return ".*";
                    } else if ("?".equals(t)) {
                        return ".";
                    }
                    return Pattern.quote(t);
                }).collect(Collectors.joining());

                boolean caseSensitive = false;
                if (arguments.size() > 2) {
                    caseSensitive = (boolean) arguments.get(2);
                }
                Pattern target = Pattern.compile(regex, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);

                return (item) -> {
                    String value = (String) getProperty(item, property);
                    return target.matcher(value).matches();
                };
            }
            case CONTAINS: {
                String property = (String) arguments.get(0);
                Object target = arguments.get(1);
                return (item) -> {
                    Object value = getProperty(item, property);
                    if (value instanceof String) {
                        return ((String) value).contains((String) target);
                    } else if (value instanceof Collection) {
                        Collection<?> values = (Collection<?>) value;
                        return values.stream().anyMatch(v -> {
                            return ObjectComparator.INSTANCE.compare(v, target) == 0;
                        });
                    }
                    else throw new UnsupportedOperationException("Cant search inside " + value.getClass());
                };
            }
            default:
                throw new UnsupportedOperationException("Unsupported RQL operation " + comparison);
        }
    }

    protected Comparator<T> getComparator(String property) {
        return (a, b) -> {
            Object valueA = getProperty(a, property);
            Object valueB = getProperty(b, property);
            return ObjectComparator.INSTANCE.compare(valueA, valueB);
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
        this.comparator = null;
        for (Object arg : arguments) {
            boolean descending;

            String property = (String) arg;
            if (property.startsWith("-")) {
                descending = true;
                property = property.substring(1);
            } else if (property.startsWith("+")) {
                property = property.substring(1);
                descending = false;
            } else {
                descending = false;
            }

            Comparator<T> comparator = getComparator(property);
            if (descending) {
                comparator = comparator.reversed();
            }
            if (this.comparator == null) {
                this.comparator = comparator;
            } else {
                this.comparator = this.comparator.thenComparing(comparator);
            }
        }
    }

    /**
     * @param item String, Boolean, Integer, Long, BigInteger, Float, Double, BigDecimal or null
     * @param property
     * @return
     */
    protected abstract Object getProperty(T item, String property);

    /**
     * Tokenizes a string, splitting on the pattern but keeping the delimiters.
     *
     * @param pattern
     * @param input
     * @return
     */
    public static List<String> tokenize(Pattern pattern, String input) {
        List<String> tokens = new ArrayList<>();

        Matcher matcher = pattern.matcher(input);
        int position = 0;
        while (matcher.find()) {
            String prevToken = input.substring(position, matcher.start());
            if (!prevToken.isEmpty()) {
                tokens.add(prevToken);
            }
            position = matcher.end();
            tokens.add(matcher.group());
        }

        String lastToken = input.substring(position);
        if (!lastToken.isEmpty()) {
            tokens.add(lastToken);
        }

        return tokens;
    }
}
