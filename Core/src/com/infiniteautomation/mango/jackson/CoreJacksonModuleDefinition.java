/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.jackson;

import java.util.Collections;
import java.util.EnumSet;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.serotonin.m2m2.module.JacksonModuleDefinition;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;

public class CoreJacksonModuleDefinition extends JacksonModuleDefinition {

    @Override
    public Iterable<? extends Module> getJacksonModules() {
        SimpleModule module = new SimpleModule(getModule().getName(), createJacksonVersion());

        module.addSerializer(PointValueTime.class, new PointValueTimeSerializer());
        module.addDeserializer(PointValueTime.class, new PointValueTimeDeserializer());

        return Collections.singleton(module);
    }

    @Override
    public EnumSet<ObjectMapperSource> getSourceMapperTypes() {
        return EnumSet.of(ObjectMapperSource.DATABASE, ObjectMapperSource.COMMON);
    }
}
