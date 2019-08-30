/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import org.springframework.stereotype.Component;

import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointDatabaseV1Model;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;

/**
 * @author Terry Packer
 *
 */
@Component
public class MockPublishedPointDatabaseModelV1Mapping implements DatabaseModelJacksonMapping<MockPublishedPointVO, MockPublishedPointDatabaseV1Model> {

    @Override
    public Class<? extends MockPublishedPointVO> fromClass() {
        return MockPublishedPointVO.class;
    }

    @Override
    public Class<? extends MockPublishedPointDatabaseV1Model> toClass() {
        return MockPublishedPointDatabaseV1Model.class;
    }

    @Override
    public MockPublishedPointDatabaseV1Model map(Object from, DatabaseModelMapper mapper) {
        return new MockPublishedPointDatabaseV1Model((MockPublishedPointVO)from);
    }

    @Override
    public String getVersion() {
        return MockPublishedPointDatabaseV1Model.class.getName();
    }

}
