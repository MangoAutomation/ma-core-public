/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

/**
 * MangoMethodSecurityExpressionHandler is provided as a bean in MangoSecurityConfiguration and is autowired into this configuration's
 * super class.
 *
 * @author Jared Wiltshire
 */
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
public class MangoMethodSecurityConfiguration extends GlobalMethodSecurityConfiguration {

}
