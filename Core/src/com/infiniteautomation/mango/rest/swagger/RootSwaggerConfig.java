/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author Terry Packer
 *
 */
@Configuration
@EnableSwagger2
public class RootSwaggerConfig {

    @Primary
    @Bean
    public MangoRestSwaggerResourceProvider getMangoRestSwaggerResourceProvider() {
        return new MangoRestSwaggerResourceProvider();
    }
}
