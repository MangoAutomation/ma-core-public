/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.publish.mock;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ConditionalDefinition;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 *
 * @author Terry Packer
 */
@ConditionalDefinition("testing.enabled")
public class MockPublisherDefinition extends PublisherDefinition<MockPublisherVO> {

    public static final String TYPE_NAME = "MOCK";

    @Override
    public String getPublisherTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return "common.default";
    }

    @Override
    protected MockPublisherVO createPublisherVO() {
        MockPublisherVO pub = new MockPublisherVO();
        pub.setDefinition(this);
        return pub;
    }

    @Override
    public void validate(ProcessResult response, MockPublisherVO pub) {
    }

    @Override
    public void validate(ProcessResult response, PublishedPointVO vo, PermissionHolder user) {

    }

    @Override
    protected @NonNull PublishedPointVO newPublishedPointVO() {
        return new MockPublishedPointVO();
    }

    @Override
    public String createPublishedPointDbData(PublishedPointVO vo) throws JsonProcessingException {
        MockPublishedPointDbData1 data = new MockPublishedPointDbData1((MockPublishedPointVO) vo);
        return getObjectWriter(MockPublishedPointDbData.class).writeValueAsString(data);
    }

    @Override
    public PublishedPointVO mapPublishedPointDbData(PublishedPointVO vo, @NonNull String data) throws JsonProcessingException {
        MockPublishedPointDbData1 dbData = getObjectReader(MockPublishedPointDbData.class).readValue(data);
        MockPublishedPointVO mVO = (MockPublishedPointVO) vo;
        mVO.setTestField(dbData.testField);
        return vo;
    }

    /**
     * Mock published poiunt data models
     *
     * @author Terry Packer
     */
    @JsonTypeInfo(use= JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="version", defaultImpl=MockPublishedPointDbData1.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "1", value = MockPublishedPointDbData1.class)
    })
    private static abstract class MockPublishedPointDbData {

    }

    private static class MockPublishedPointDbData1 extends MockPublishedPointDbData {
        public MockPublishedPointDbData1() { }
        public MockPublishedPointDbData1(MockPublishedPointVO vo) {
            this.testField = vo.getTestField();
        }
        @JsonProperty
        String testField;
    }
}
