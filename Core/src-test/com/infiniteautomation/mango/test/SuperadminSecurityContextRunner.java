/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.test;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.serotonin.m2m2.MangoTestBase;

/**
 * Test runner to wrap tests in a security context filled with the Superadmin permission holder
 *
 * Useage: @RunWith(SuperadminSecurityContextRunner.class)
 *
 */
public class SuperadminSecurityContextRunner extends BlockJUnit4ClassRunner {

    public SuperadminSecurityContextRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    protected SuperadminSecurityContextRunner(TestClass testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        //Wrap the first statement to put the permission holder into context
        // just before the test runs.  This will ensure the last thing we do
        // before running the test is set the context.
       return new FailOnTimeoutWrapper(super.methodInvoker(method, test));

    }

    class FailOnTimeoutWrapper extends Statement {
        Statement delegate;
        public FailOnTimeoutWrapper(Statement stmt) {
            this.delegate = stmt;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                MangoTestBase.setSuperadminAuthentication();
                this.delegate.evaluate();
            }finally {
                SecurityContext securityContext = SecurityContextHolder.getContext();
                securityContext.setAuthentication(null);
            }
        }
    }
}
