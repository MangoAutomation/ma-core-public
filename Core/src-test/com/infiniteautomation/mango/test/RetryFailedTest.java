/*
 * Copyright (C) 2022 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.infiniteautomation.mango.rules.RetryRule;
import com.serotonin.m2m2.MangoTestBase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RetryFailedTest extends MangoTestBase {
    private static int counter = 0;

    // For desired test behaviour override retryRule property in constructor
    public RetryFailedTest() {
        retryRule = new RetryRule(3, true, false, RetryRule.FailBehaviour.ALL);
    }

    @Before
    public void increment() {
        counter++;
    }

    @Test
    public void test1ShouldPassOnFirstRun() {
        assertEquals(retryRule.currentRun, counter);
        assertEquals(1, counter);
    }

    @Test
    public void test2ShouldPassOnSecondRun() {
        assertEquals(retryRule.currentRun, counter - 1);
        assertEquals(3, counter);
    }

    @Test
    public void test3ShouldPassOnThirdRun() {
        assertEquals(retryRule.currentRun, counter - 3);
        assertEquals(6, counter);
    }
}
