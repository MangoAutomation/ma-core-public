/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.rules;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;

/**
 * Runs a test as a particular Mango user
 */
public class MangoUserRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        MangoUser annotation = description.getAnnotation(MangoUser.class);
        return annotation == null ? base : new MangoUserRuleStatement(base, annotation);
    }

    public static class MangoUserRuleStatement extends Statement {

        private final Statement base;
        private final MangoUser annotation;

        public MangoUserRuleStatement(Statement base, MangoUser annotation) {
            this.base = base;
            this.annotation = annotation;
        }

        @Override
        public void evaluate() throws Throwable {
            UsersService usersService = Common.getBean(UsersService.class);
            User user;
            if (!annotation.username().isEmpty()) {
                user = usersService.get(annotation.username());
            } else {
                user = newUser(annotation.roles());
                usersService.insert(user);
            }

            SecurityContext original = SecurityContextHolder.getContext();
            SecurityContext newContext = SecurityContextHolder.createEmptyContext();
            newContext.setAuthentication(new PreAuthenticatedAuthenticationToken(user, null));

            try {
                SecurityContextHolder.setContext(newContext);
                base.evaluate();
            } finally {
                SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
                if (emptyContext.equals(original)) {
                    SecurityContextHolder.clearContext();
                } else {
                    SecurityContextHolder.setContext(original);
                }
            }
        }

        private User newUser(String[] roles) {
            RoleService roleService = Common.getBean(RoleService.class);

            String username = UUID.randomUUID().toString();
            User user = new User();
            user.setName(username);
            user.setUsername(username);
            user.setPassword(Common.encrypt(username));
            user.setEmail(username + "@example.com");
            user.setPhone("");

            user.setRoles(Arrays.stream(roles)
                    .map(r -> roleService.getOrInsert(r, r).getRole())
                    .collect(Collectors.toSet()));

            return user;
        }
    }
}
