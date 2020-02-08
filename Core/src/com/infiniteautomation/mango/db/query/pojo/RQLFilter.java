/**
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.db.query.pojo;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.infiniteautomation.mango.db.query.ComparisonEnum;

import net.jazdw.rql.parser.ASTNode;

public abstract class RQLFilter<T> implements UnaryOperator<Stream<T>> {

    private final Predicate<T> filter;
    private Long limit;
    private Long offset;
    private long total;
    private Comparator<T> comparator;

    public RQLFilter(ASTNode node) {
        this.filter = node == null ? null : this.visit(node);
    }

    @Override
    public Stream<T> apply(Stream<T> stream) {
        this.total = 0;

        if (this.filter != null) {
            stream = stream.filter(filter);
        }

        stream = stream.peek(c -> {
            this.total++;
        });

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

    private Predicate<T> visit(ASTNode node) {
        ComparisonEnum comparison = ComparisonEnum.convertTo(node.getName());

        switch(comparison) {
            case AND: {
                List<Predicate<T>> childPredicates = childPredicates(node);
                return item -> {
                    return childPredicates.stream().allMatch(p -> p.test(item));
                };
            }
            case OR: {
                List<Predicate<T>> childPredicates = childPredicates(node);
                return item -> {
                    return childPredicates.stream().anyMatch(p -> p.test(item));
                };
            }
            case LIMIT: {
                applyLimit(node);
                return null;
            }
            case SORT: {
                applySort(node);
                return null;
            }
            case EQUAL_TO: {
                String property = (String) node.getArgument(0);
                Object target = node.getArgument(1);
                return (item) -> OBJECT_COMPARATOR.compare(getProperty(item, property), target) == 0;
            }
            // TODO implement other cases

            //            case IN: {
            //                // work around for RQL queries with an array in their first argument position
            //                if (node.getArgument(0) instanceof List) {
            //                    node = new ASTNode(node.getName(), (List<?>) node.getArgument(0));
            //                }
            //                String property = (String) node.getArgument(0);
            //                Object target = node.getArgument(1);
            //                return (item) -> OBJECT_COMPARATOR.compare(getProperty(item, property), target) == 0;
            //            }
            default:
                throw new UnsupportedOperationException("Unsupported RQL operation " + comparison);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final Comparator<Object> OBJECT_COMPARATOR = (a, b) -> {
        if (!a.getClass().isAssignableFrom(b.getClass())) {
            return a.getClass().getName().compareTo(b.getClass().getName());
        }

        // TODO make this work better with numbers
        if (a instanceof Comparable) {
            return ((Comparable) a).compareTo(b);
        }

        throw new UnsupportedOperationException("Cant compare " + a.getClass());
    };

    protected Comparator<T> getComparator(String property) {
        return (a, b) -> {
            Object valueA = getProperty(a, property);
            Object valueB = getProperty(b, property);
            return OBJECT_COMPARATOR.compare(valueA, valueB);
        };
    }

    private List<Predicate<T>> childPredicates(ASTNode node) {
        return node.getArguments().stream()
                .map(arg -> this.visit((ASTNode) arg))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void applyLimit(ASTNode node) {
        List<Object> args = node.getArguments();
        if (!args.isEmpty()) {
            this.limit = ((Number) args.get(0)).longValue();
            this.offset = args.size() > 1 ? ((Number) args.get(1)).longValue() : 0;
        }
    }

    private void applySort(ASTNode node) {
        this.comparator = null;
        List<Object> args = node.getArguments();
        for (Object arg : args) {
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

    public long getTotal() {
        return total;
    }
}
