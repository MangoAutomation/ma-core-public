/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.SimulationTimerProvider;
import com.serotonin.m2m2.db.H2InMemoryDatabaseProxy;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;

public class CleanMangoRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        return new CleanMangoStatement(base);
    }

    private class CleanMangoStatement extends Statement {

        private final Statement base;

        public CleanMangoStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                before();
                base.evaluate();
            } finally {
                after();
            }
        }

        private void before() throws Exception {
            //Lifecycle hook for things that need to run to install into a new database
            for (Module module : ModuleRegistry.getModules()) {
                module.postDatabase();
            }
        }

        private void after() throws Exception {
            Common.eventManager.purgeAllEvents();

            SimulationTimerProvider provider = (SimulationTimerProvider) Providers.get(TimerProvider.class);
            provider.reset();

            for (Module module : ModuleRegistry.getModules()) {
                module.postRuntimeManagerTerminate(false);
            }

            if (Common.databaseProxy instanceof H2InMemoryDatabaseProxy) {
                H2InMemoryDatabaseProxy proxy = (H2InMemoryDatabaseProxy) Common.databaseProxy;
                try {
                    proxy.clean();
                } catch (Exception e) {
                    throw new ShouldNeverHappenException(e);
                }
            }

            for (Module module : ModuleRegistry.getModules()) {
                module.postTerminate(false);
            }
        }

    }
}
