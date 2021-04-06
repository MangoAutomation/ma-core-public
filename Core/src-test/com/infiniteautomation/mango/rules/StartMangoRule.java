/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.rules;

import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MangoTestModule;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockMangoProperties;
import com.serotonin.m2m2.TerminationReason;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.provider.Providers;
import com.serotonin.util.properties.MangoProperties;

public class StartMangoRule implements TestRule {

    private MockMangoLifecycle lifecycle;
    private final List<Module> modules = new ArrayList<>();
    private MockMangoProperties properties;
    private Path dataDirectory;

    @Override
    public Statement apply(Statement base, Description description) {
        return new StartMangoStatement(base);
    }

    private class StartMangoStatement extends Statement {

        private final Statement base;

        private StartMangoStatement(Statement base) {
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
            setSuperadminAuthentication();

            dataDirectory = Files.createTempDirectory("MangoTestBase");

            properties = new MockMangoProperties();
            properties.setProperty("paths.data", dataDirectory.toString());
            Providers.add(MangoProperties.class, properties);

            addModule("BaseTest", new MockDataSourceDefinition());

            lifecycle = getLifecycle();
            try {
                lifecycle.initialize();
            } catch (InterruptedException | ExecutionException e) {
                fail(e.getMessage());
            }
        }

        private void after() throws Exception {
            if (lifecycle != null) {
                lifecycle.terminate(TerminationReason.SHUTDOWN);
            }

            if (dataDirectory != null) {
                FileUtils.deleteDirectory(dataDirectory.toFile());
            }
        }

        private MockMangoLifecycle getLifecycle() {
            return new MockMangoLifecycle(modules);
        }

        private MangoTestModule addModule(String name, ModuleElementDefinition... definitions) {
            MangoTestModule module = new MangoTestModule(name);
            module.loadDefinitions(MangoTestBase.class.getClassLoader());
            Arrays.stream(definitions).forEach(module::addDefinition);
            modules.add(module);
            return module;
        }

        private void setSuperadminAuthentication() {
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(new PreAuthenticatedAuthenticationToken(PermissionHolder.SYSTEM_SUPERADMIN, null));
        }

    }
}
