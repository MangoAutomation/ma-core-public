/**
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.db.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public enum ComparisonEnum {
    /**
     * Check for equality using a {@link java.util.Comparator Comparator}
     */
    EQUAL_TO("eq"),

    /**
     * Inverse of {@link #EQUAL_TO}
     */
    NOT_EQUAL_TO("ne"),

    /**
     * Check for less than using a {@link java.util.Comparator Comparator}
     */
    LESS_THAN("lt"),

    /**
     * Check for less than or equals using a {@link java.util.Comparator Comparator}
     */
    LESS_THAN_EQUAL_TO("le"),

    /**
     * Check for greater than using a {@link java.util.Comparator Comparator}, inverse of {@link #LESS_THAN_EQUAL_TO}
     */
    GREATER_THAN("gt"),

    /**
     * Check for greater than or equals using a {@link java.util.Comparator Comparator}, inverse of {@link #LESS_THAN}
     */
    GREATER_THAN_EQUAL_TO("ge"),

    /**
     * Check if the value is contained in the supplied array.
     */
    IN("in"),

    /**
     * Check if the value matches the supplied match pattern, only the star operator (*) is supported and is equivalent to the regex .*
     */
    MATCH("match", "like"),

    /**
     * Check if the value contains the supplied argument, the value can be String or a Collection
     */
    CONTAINS("contains"),

    /**
     * Returns true if all of it's child predicates return true
     */
    AND("and"),

    /**
     * Returns true if any of it's child predicates return true
     */
    OR("or"),

    /**
     * Inverts the result of it's child predicate, if multiple children are supplied they are treated as being ANDed
     */
    NOT("not"),

    /**
     * Sorts the results by the list of properties
     */
    SORT("sort"),

    /**
     * Limits the results and may optionally skip (offset) the the first x results.
     */
    LIMIT("limit");

    public static ComparisonEnum convertTo(String comparisonString) {
        ComparisonEnum comparison = REVERSE_MAP.get(comparisonString);
        if (comparison == null) {
            throw new UnsupportedOperationException("Comparison: " + comparisonString + " not supported.");
        }
        return comparison;
    }

    private static final Map<String, ComparisonEnum> REVERSE_MAP;
    static {
        ComparisonEnum[] constants = ComparisonEnum.class.getEnumConstants();
        Map<String, ComparisonEnum> map = new HashMap<>(constants.length, 1);

        for (ComparisonEnum comparison : constants) {
            for (String op : comparison.getOpCodes()) {
                map.put(op, comparison);
            }
        }

        REVERSE_MAP = Collections.unmodifiableMap(map);
    }

    private final String[] opCodes;

    private ComparisonEnum(String... opCodes) {
        this.opCodes = opCodes;
    }

    public String[] getOpCodes() {
        return opCodes;
    }
}
