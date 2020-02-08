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

    EQUAL_TO("eq"),
    NOT_EQUAL_TO("ne"),
    LESS_THAN("lt"),
    LESS_THAN_EQUAL_TO("le"),
    GREATER_THAN("gt"),
    GREATER_THAN_EQUAL_TO("ge"),
    IN("in"),
    LIKE("like", "match"),
    NOT_LIKE("not like", "not match", "nlike", "nmatch"),
    CONTAINS(),
    IS("is"),
    IS_NOT("is not"),
    AND("and"),
    OR("or"),
    LIMIT("limit"),
    SORT("sort");

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
