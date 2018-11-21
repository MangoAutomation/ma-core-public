/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.vo.User;

/**
 * @author Terry Packer
 *
 */
@Component
public class RestModelMapper {

    private final List<RestModelMapping<?,?>> mappings;
    
    @Autowired
    public RestModelMapper(List<RestModelMapping<?,?>> mappings) {
        this.mappings = mappings;
    }

    @SuppressWarnings("unchecked")
    public <F,T> T map(F o , Class<T> model, User user) {
        for(RestModelMapping<?,?> mapping : mappings)
            if(mapping.supportsFrom(o, model))
                return (T) mapping.map(o);
        throw new ShouldNeverHappenException("No model mapping from " + o.getClass() + " to " + model);
    }

}
