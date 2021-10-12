/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.publish.mock;

import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 *
 * @author Terry Packer
 */
public class MockPublishedPointVO extends PublishedPointVO {

    private String testField;

    public String getTestField() {
        return testField;
    }

    public void setTestField(String testField) {
        this.testField = testField;
    }
}
