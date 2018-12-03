/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.stereotype.Component;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.vo.User;

/**
 * Gets a list of RestModelMapping beans from the Spring context and uses them to convert an object to its model.
 * The RestModelMapping beans can be annotated with @Order to specify their priority.
 *
 * @author Terry Packer
 */
@Component
public class RestModelMapper {

    private final List<RestModelMapping<?,?>> mappings;

    @Autowired
    public RestModelMapper(Optional<List<RestModelMapping<?,?>>> mappings) {
        this.mappings = mappings.orElseGet(Collections::emptyList);
    }

    public <T> T map(Object from, Class<T> model, User user) {
        Objects.requireNonNull(from);
        Objects.requireNonNull(model);

        for (RestModelMapping<?,?> mapping : mappings) {
            if (mapping.supports(from, model)) {
                @SuppressWarnings("unchecked")
                T result = (T) mapping.map(from, user);
                if (result != null) {
                    return result;
                }
            }
        }

        throw new ShouldNeverHappenException("No model mapping for " + from.getClass() + " to " + model);
    }

    public <T> MappingJacksonValue mapWithView(Object from, Class<T> model, User user) {
        Objects.requireNonNull(from);
        Objects.requireNonNull(model);

        for (RestModelMapping<?,?> mapping : mappings) {
            if (mapping.supports(from, model)) {
                @SuppressWarnings("unchecked")
                T result = (T) mapping.map(from, user);
                if (result != null) {
                    MappingJacksonValue mappingValue = new MappingJacksonValue(result);
                    mappingValue.setSerializationView(mapping.view(from, user));
                    return mappingValue;
                }
            }
        }

        throw new ShouldNeverHappenException("No model mapping for " + from.getClass() + " to " + model);
    }
}
