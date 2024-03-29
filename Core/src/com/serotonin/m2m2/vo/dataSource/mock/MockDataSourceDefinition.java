/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.dataSource.mock;

import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ConditionalDefinition;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

/**
 * @author Terry Packer
 *
 */
@ConditionalDefinition("testing.enabled")
public class MockDataSourceDefinition extends DataSourceDefinition<MockDataSourceVO> {

    public static final String TYPE_NAME = "MOCK";

    @Override
    public String getDataSourceTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return ""; //
    }

    @Override
    protected MockDataSourceVO createDataSourceVO() {
        return new MockDataSourceVO();
    }

    @Override
    public void validate(ProcessResult response, MockDataSourceVO ds) {

    }

    @Override
    public void validate(ProcessResult response, DataPointVO dpvo, DataSourceVO dsvo) {
        if(!(dsvo instanceof MockDataSourceVO))
            response.addContextualMessage("dataSourceId", "dpEdit.validate.invalidDataSourceType");
    }

}
