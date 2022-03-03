/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.jackson;

import java.util.List;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.serotonin.m2m2.module.JacksonModuleDefinition;
import com.infiniteautomation.mango.spring.annotations.CommonMapper;
import com.infiniteautomation.mango.spring.annotations.DatabaseMapper;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;

@CommonMapper
@DatabaseMapper
public class CoreJacksonModuleDefinition extends JacksonModuleDefinition {

    @Override
    public Iterable<? extends Module> getJacksonModules() {
        SimpleModule module = new SimpleModule(getModule().getName(), createJacksonVersion());

        module.addSerializer(PointValueTime.class, new PointValueTimeSerializer());
        module.addDeserializer(PointValueTime.class, new PointValueTimeDeserializer());

        return List.of(module,
                new JavaTimeModule(),
                new Jdk8Module());
    }

}
