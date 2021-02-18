/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.jackson;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.serotonin.m2m2.MockMangoProperties;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.provider.Providers;
import com.serotonin.util.properties.MangoProperties;

public class PointValueTimeListSerializerTest {

    private ObjectMapper mapper;

    @BeforeClass
    public static void setup() {
        // used by com.serotonin.m2m2.rt.dataImage.PointValueTime.toString
        Providers.add(MangoProperties.class, new MockMangoProperties());
    }

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
        testSerialization(
                new PointValueTime(new BinaryValue(true), System.currentTimeMillis()),
                new PointValueTime(new BinaryValue(false), System.currentTimeMillis())
                );
    }

    @Test
    public void multiState() throws JsonProcessingException {
        testSerialization(
                new PointValueTime(new MultistateValue(1), System.currentTimeMillis()),
                new PointValueTime(new MultistateValue(2), System.currentTimeMillis()),
                new PointValueTime(new MultistateValue(3), System.currentTimeMillis()),
                new PointValueTime(new MultistateValue(4), System.currentTimeMillis())
        );
    }

    @Test
    public void numeric() throws JsonProcessingException {
        testSerialization(
                new PointValueTime(new NumericValue(1.0D), System.currentTimeMillis()),
                new PointValueTime(new NumericValue(2.0D), System.currentTimeMillis()),
                new PointValueTime(new NumericValue(3.0D), System.currentTimeMillis()),
                new PointValueTime(new NumericValue(4.0D), System.currentTimeMillis()),
                new PointValueTime(new NumericValue(5.0D), System.currentTimeMillis())
        );
    }

    @Test
    public void numericInfinity() throws JsonProcessingException {
        testSerialization(
                new PointValueTime(new NumericValue(Double.POSITIVE_INFINITY), System.currentTimeMillis()),
                new PointValueTime(new NumericValue(Double.NEGATIVE_INFINITY), System.currentTimeMillis()),
                new PointValueTime(new NumericValue(Double.NaN), System.currentTimeMillis())
        );
    }

    @Test
    public void alphanumeric() throws JsonProcessingException {
        testSerialization(
                new PointValueTime(new AlphanumericValue("a"), System.currentTimeMillis()),
                new PointValueTime(new AlphanumericValue("ab"), System.currentTimeMillis()),
                new PointValueTime(new AlphanumericValue("abc"), System.currentTimeMillis())
        );
    }

    @Test
    public void annotated() throws JsonProcessingException {
        testSerialization(new AnnotatedPointValueTime(new NumericValue(1.0D), System.currentTimeMillis(), new TranslatableMessage("test1")),
                new AnnotatedPointValueTime(new NumericValue(2.0D), System.currentTimeMillis(), new TranslatableMessage("test2")),
                new AnnotatedPointValueTime(new NumericValue(3.0D), System.currentTimeMillis(), new TranslatableMessage("test3"))
        );
    }

    private void testSerialization(PointValueTime... pvts) throws JsonProcessingException {
        TypeFactory typeFactory = mapper.getTypeFactory();
        CollectionType type = typeFactory.constructCollectionType(List.class, PointValueTime.class);

        String json = mapper.writerFor(type).writeValueAsString(Arrays.stream(pvts).collect(Collectors.toList()));
        List<PointValueTime> deserialized = mapper.readerFor(type).readValue(json);

        Assert.assertEquals(pvts.length, deserialized.size());
        for(int i=0; i<pvts.length; i++) {
            PointValueTime pvt = pvts[i];
            PointValueTime deserializedPvt = deserialized.get(i);
            Assert.assertEquals(pvt, deserializedPvt);
        }
    }

}
