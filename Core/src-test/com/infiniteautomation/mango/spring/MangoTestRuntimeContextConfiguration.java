/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.infiniteautomation.mango.spring.components.RegisterModuleElementDefinitions;
import com.infiniteautomation.mango.spring.eventMulticaster.EventMulticasterRegistry;
import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEventMulticaster;
import com.infiniteautomation.mango.test.CurrentThreadExecutorService;
import com.serotonin.m2m2.MockPointValueDao;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.H2InMemoryDatabaseProxy;
import com.serotonin.m2m2.db.dao.PointValueDao;

/**
 * @author Terry Packer
 *
 */
@Configuration
@Import({MangoCommonConfiguration.class, RegisterModuleElementDefinitions.class})
@ComponentScan(basePackages = {
        "com.infiniteautomation.mango.spring",  //General Runtime Spring Components
        "com.serotonin.m2m2.db.dao" //DAOs
})
public class MangoTestRuntimeContextConfiguration extends MangoRuntimeContextConfiguration {

    //Defined here to take precedence in testing (also define in common configuration)
    @Bean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ApplicationEventMulticaster eventMulticaster(ApplicationContext context, EventMulticasterRegistry eventMulticasterRegistry) {
        return new PropagatingEventMulticaster(context, eventMulticasterRegistry, new CurrentThreadExecutorService());
    }

    @Bean
    @Primary
    public PointValueDao pointValueDao() {
        return new MockPointValueDao();
    }

    @Bean
    @Primary
    public DatabaseProxy databaseProxy(DatabaseProxyConfiguration configuration) {
        return new H2InMemoryDatabaseProxy(configuration);
    }

}
