/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.swagger;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;

/**
 * Register your swagger specs here to be exposed via the root view 
 * 
 * @author Terry Packer
 *
 */
@Component
public class MangoRestSwaggerResourceProvider implements SwaggerResourcesProvider {

    private final List<SwaggerResource> resources = new ArrayList<>();
    
    @Override
    public List<SwaggerResource> get() {
        return resources;
    }

    /**
     * Add a swagger resource definition
     * @param resource
     */
    public void add(SwaggerResource resource) {
        this.resources.add(resource);
    }
    
}
