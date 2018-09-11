/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.rest.v1.converters;

import java.util.ArrayList;

import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;

/**
 *
 * @author Terry Packer
 */
public class SqlMessageConverter extends StringHttpMessageConverter {

    public SqlMessageConverter(){
        ArrayList<MediaType> types = new ArrayList<MediaType>();
        types.add(new MediaType("application", "sql"));
        this.setSupportedMediaTypes(types);
    }
}
