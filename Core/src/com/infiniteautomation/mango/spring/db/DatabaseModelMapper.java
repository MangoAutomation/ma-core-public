/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.util.exception.TranslatableRuntimeException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Gets a list of DatabaseModelMapping beans from the Spring context and uses them to convert an object to its model.
 * The DatabaseModelMapping beans can be annotated with @Order to specify their priority.
 * 
 * @author Terry Packer
 *
 */
@Component
public class DatabaseModelMapper {
    private final List<DatabaseModelMapping<?,?>> mappings;

    @Autowired
    public DatabaseModelMapper(Optional<List<DatabaseModelMapping<?,?>>> mappings, 
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper objectMapper) {
        this.mappings = mappings.orElseGet(Collections::emptyList);
        
        //Load in the mappings for Jackson
        for(DatabaseModelMapping<?,?> mapping : this.mappings) {
            if(mapping instanceof DatabaseModelJacksonMapping)
                objectMapper.registerSubtypes(new NamedType(mapping.toClass(), ((DatabaseModelJacksonMapping<?,?>)mapping).getVersion()));
        }
    }

    public <T> T map(Object from, Class<T> model) {
        Objects.requireNonNull(from);
        Objects.requireNonNull(model);

        for (DatabaseModelMapping<?,?> mapping : mappings) {
            if (mapping.supports(from, model)) {
                @SuppressWarnings("unchecked")
                T result = (T) mapping.map(from, this);
                if (result != null) {
                    return result;
                }
            }
        }

        throw new TranslatableRuntimeException(new TranslatableMessage("database.missingModelMapping", from.getClass(), model));
    }
    
}
