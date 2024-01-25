/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module;

import java.util.Comparator;

/**
 * @author Jared Wiltshire
 */
public class ModuleComparator implements Comparator<Module> {

    public static final Comparator<Module> INSTANCE = new ModuleComparator();

    private ModuleComparator() {
    }

    @Override
    public int compare(Module moduleA, Module moduleB) {
        if (moduleA == moduleB) return 0;
        return Integer.compare(moduleA.getLoadOrder(), moduleB.getLoadOrder());
    }
}
