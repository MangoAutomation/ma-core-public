/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.i18n.ProcessMessage;

public class ExpectValidationErrorRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        return new ExpectValidationErrorRuleStatement(base, description);
    }

    public static class ExpectValidationErrorRuleStatement extends Statement {
        private final Statement base;
        private final boolean validationExceptionExpected;
        private final Set<String> expectedKeys;

        public ExpectValidationErrorRuleStatement(Statement base, Description description) {
            this.base = base;

            ExpectValidationException annotation = description.getAnnotation(ExpectValidationException.class);
            this.validationExceptionExpected = annotation != null;
            if (annotation != null) {
                this.expectedKeys = Arrays.stream(annotation.value()).collect(Collectors.toSet());
            } else {
                this.expectedKeys = null;
            }
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                base.evaluate();
            } catch (ValidationException e) {
                if (!validationExceptionExpected) {
                    throw e;
                }

                if (!expectedKeys.isEmpty()) {
                    Set<String> failedKeys = e.getValidationResult().getMessages().stream()
                            .map(ProcessMessage::getContextKey)
                            .collect(Collectors.toSet());
                    if (!expectedKeys.equals(failedKeys)) {
                        fail(String.format("Expected ValidationException to be thrown with keys %s, but got %s", expectedKeys, failedKeys));
                    }
                }
                return;
            }

            if (validationExceptionExpected) {
                if (expectedKeys.isEmpty()) {
                    fail("Expected ValidationException to be thrown");
                } else {
                    fail(String.format("Expected ValidationException to be thrown with keys %s", expectedKeys));
                }
            }
        }
    }
}
