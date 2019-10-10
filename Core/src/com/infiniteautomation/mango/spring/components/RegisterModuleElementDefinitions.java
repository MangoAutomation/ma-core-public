/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components;

import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

/**
 * Registers ModuleElementDefinitions as Spring beans
 * @author Jared Wiltshire
 */
@Component
public class RegisterModuleElementDefinitions implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        List<ModuleElementDefinition> list = ModuleRegistry.getDefinitions(ModuleElementDefinition.class);
        for (ModuleElementDefinition def : list) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ModuleElementDefinition.class, () -> def)
                    .setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);

            AbstractBeanDefinition definition = builder.getBeanDefinition();
            String name = BeanDefinitionReaderUtils.generateBeanName(definition, registry);

            registry.registerBeanDefinition(name, definition);
        }
    }
}
