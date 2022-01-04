/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.db.query.pojo;

import static org.junit.Assert.assertEquals;

import java.util.stream.Stream;

import org.junit.Test;

import com.infiniteautomation.mango.util.RQLUtils;

import net.jazdw.rql.parser.ASTNode;

/**
 * @author Jared Wiltshire
 */
public class RQLFilterJavaBeanTest {
    @Test
    public void sortStrings() {
        ASTNode node = RQLUtils.parseRQLtoAST("sort(+)");
        RQLFilter<String> filter = new RQLFilterJavaBean<>(node, null);

        String[] filtered = filter.apply(Stream.of("zzz", "aaa")).toArray(String[]::new);
        assertEquals(2, filtered.length);
        assertEquals("aaa", filtered[0]);
        assertEquals("zzz", filtered[1]);
    }

    @Test
    public void sortStringsReverse() {
        ASTNode node = RQLUtils.parseRQLtoAST("sort(-)");
        RQLFilter<String> filter = new RQLFilterJavaBean<>(node, null);

        String[] filtered = filter.apply(Stream.of("aaa", "zzz")).toArray(String[]::new);
        assertEquals(2, filtered.length);
        assertEquals("zzz", filtered[0]);
        assertEquals("aaa", filtered[1]);
    }

    @Test
    public void compareNumbers() {
        ASTNode node = RQLUtils.parseRQLtoAST("gt(1)");
        RQLFilter<Integer> filter = new RQLFilterJavaBean<>(node, null);

        Integer[] filtered = filter.apply(Stream.of(1, 2, 3)).toArray(Integer[]::new);
        assertEquals(2, filtered.length);
        assertEquals(2, (int) filtered[0]);
        assertEquals(3, (int) filtered[1]);
    }
}
