/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.publish.mock;

import org.springframework.stereotype.Component;

import com.serotonin.m2m2.db.dao.PublishedPointDao.PublishedPointSettingsDatabaseModel;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * @author Terry Packer
 *
 */
@Component
public class MockPublishedPointDatabaseV1Model extends PublishedPointSettingsDatabaseModel {

    private String setting;
    
    public MockPublishedPointDatabaseV1Model() {
        
    }
    
    public MockPublishedPointDatabaseV1Model(MockPublishedPointVO vo) {
        this.setting = vo.getMockSetting();
    }

    public String getSetting() {
        return setting;
    }

    public void setSetting(String setting) {
        this.setting = setting;
    }

    @Override
    public PublishedPointVO toVO() {
        MockPublishedPointVO vo = new MockPublishedPointVO();
        vo.setMockSetting(setting);
        return vo;
    }
    
    @Override
    public String getVersion() {
        return this.getClass().getName();
    }
}
