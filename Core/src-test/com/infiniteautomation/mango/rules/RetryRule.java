/*
 * Copyright (C) 2022 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.rules;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryRule implements TestRule {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    public static enum FailBehaviour {
        ANY, ALL
    }

    public final int retryCount;
    public final boolean stopOnSuccess;
    public final boolean stopOnFailure;
    public final FailBehaviour failBehaviour;
    public int currentRun;

    private Throwable lastError;

    public RetryRule(int retryCount, boolean stopOnSuccess, boolean stopOnFailure, FailBehaviour failBehaviour) {
        this.retryCount = retryCount;
        this.stopOnSuccess = stopOnSuccess;
        this.stopOnFailure = stopOnFailure;
        this.failBehaviour = failBehaviour;
        this.currentRun = 0;
        this.lastError = null;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            @SuppressWarnings("synthetic-access")
            public void evaluate() throws Throwable {
                int failuresCount = 0;

                for (int i = 0; i < retryCount; i++) {
                    currentRun = i + 1;
                    if (failBehaviour == FailBehaviour.ALL) lastError = null;
                    try {
                        base.evaluate();
                        if (stopOnSuccess) break;
                    }
                    catch (Throwable t) {
                        lastError = t;
                        if (lastError instanceof AssumptionViolatedException) throw t;
                        String errorMessage = description.getDisplayName() + ": Run " + currentRun + " failed:";
                        log.error(errorMessage, t);
                        if (stopOnFailure) break;
                        ++failuresCount;
                    }
                }
                if (lastError == null) return;
                String errorMessage = description.getDisplayName() + ": failures " + failuresCount + " out of " + retryCount + " tries. See last throwable as the cause.";
                throw new AssertionError(errorMessage, lastError);
            }
        };
    }
}
