/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.infiniteautomation.mango.rules.CleanMangoRule;
import com.infiniteautomation.mango.rules.ExpectValidationException;
import com.infiniteautomation.mango.rules.ExpectValidationExceptionRule;
import com.infiniteautomation.mango.rules.MangoUser;
import com.infiniteautomation.mango.rules.MangoUserRule;
import com.infiniteautomation.mango.rules.StartMangoRule;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;

public class UsersServiceTestExperimental {

    @ClassRule
    public static final StartMangoRule startMango = new StartMangoRule();

    @Rule(order = 1)
    public final CleanMangoRule cleanMangoRule = new CleanMangoRule();

    @Rule(order = 2)
    public final ExpectValidationExceptionRule validation = new ExpectValidationExceptionRule();

    @Rule(order = 3)
    public final MangoUserRule mangoUser = new MangoUserRule();

    @Test
    @MangoUser
    @ExpectValidationException({"sessionExpirationOverride", "sessionExpirationPeriods", "sessionExpirationPeriodType"})
    public void cantChangeSessionExpirationOfSelf() {
        UsersService service = Common.getBean(UsersService.class);
        User self = service.get(Common.getUser().getUser().getId());
        self.setSessionExpirationOverride(true);
        self.setSessionExpirationPeriods(5);
        self.setSessionExpirationPeriodType("MINUTES");
        service.update(self.getId(), self);
    }

}
