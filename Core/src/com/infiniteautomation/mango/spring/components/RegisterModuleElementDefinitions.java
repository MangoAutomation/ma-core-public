/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components;

import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.SystemSettingsDefinition;

/**
 * Registers ModuleElementDefinitions as Spring beans.
 *
 * @author Jared Wiltshire
 */
@Configuration
public class RegisterModuleElementDefinitions implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    /**
     * Adds a dependency on SystemSettingsDao for all SystemSettingsDefinition to protect against uses of {@link com.serotonin.m2m2.db.dao.SystemSettingsDao#instance}
     * which may use methods which depend on the DAO ObjectMapper
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        List<ModuleElementDefinition> list = ModuleRegistry.getDefinitions(ModuleElementDefinition.class);
        for (ModuleElementDefinition def : list) {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(def.getClass());
            beanDefinition.setInstanceSupplier(() -> def);
            beanDefinition.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);

            // Add dependency on SystemSettingsDao which also depends on the DAO ObjectMapper
            if (def instanceof SystemSettingsDefinition) {
                beanDefinition.setDependsOn(MangoRuntimeContextConfiguration.SYSTEM_SETTING_DAO_NAME);
            }

            String name = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, registry);
            registry.registerBeanDefinition(name, beanDefinition);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // within PriorityOrdered
    }
}
