/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.jackson;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;

public class PointValueTimeSerializerTest {

    private ObjectMapper mapper;

    @Before
    public void init() {
        this.mapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addSerializer(PointValueTime.class, new PointValueTimeSerializer());
        module.addDeserializer(PointValueTime.class, new PointValueTimeDeserializer());
        mapper.registerModule(module);
    }

    @Test
    public void binary() throws JsonProcessingException {
        testSerialization(new PointValueTime(new BinaryValue(true), System.currentTimeMillis()));
    }

    @Test
    public void multiState() throws JsonProcessingException {
        testSerialization(new PointValueTime(new MultistateValue(1), System.currentTimeMillis()));
    }

    @Test
    public void numeric() throws JsonProcessingException {
        testSerialization(new PointValueTime(new NumericValue(1.0D), System.currentTimeMillis()));
    }

    @Test
    public void numericInfinity() throws JsonProcessingException {
        testSerialization(new PointValueTime(new NumericValue(Double.POSITIVE_INFINITY), System.currentTimeMillis()));
    }

    @Test
    public void alphanumeric() throws JsonProcessingException {
        testSerialization(new PointValueTime(new AlphanumericValue("abc"), System.currentTimeMillis()));
    }

    @Test
    public void annotated() throws JsonProcessingException {
        testSerialization(new AnnotatedPointValueTime(new NumericValue(1.0D), System.currentTimeMillis(), new TranslatableMessage("test")));
    }

    private void testSerialization(PointValueTime pvt) throws JsonProcessingException {
        String json = mapper.writeValueAsString(pvt);
        PointValueTime deserialized = mapper.readValue(json, PointValueTime.class);
        Assert.assertEquals(pvt, deserialized);
    }

}
