/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.settings;

import java.util.Collections;
import java.util.Map;

import com.serotonin.m2m2.db.dao.DataPointTagsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.SystemSettingsDefinition;

/**
 *
 * @author Terry Packer
 */
public class DataPointTagsDisplaySettingDefinition extends SystemSettingsDefinition {

    public static final String DEFAULT_DISPLAY_TAGS = "tags.dataPoint.display";

    @Override
    public String getDescriptionKey() {
        return "systemSettings.tags.defaultDisplayTags";
    }

    @Override
    public Map<String, Object> getDefaultValues() {
        return Collections.emptyMap();
    }

    @Override
    public Integer convertToValueFromCode(String key, String code) {
        return null;
    }

    @Override
    public String convertToCodeFromValue(String key, Integer value) {
        return null;
    }

    @Override
    public void validateSettings(Map<String, Object> settings, ProcessResult response) {
        try{
            String tags = (String)settings.get(DEFAULT_DISPLAY_TAGS);
            if(tags != null) {
                for(String tagKey : tags.split(",")) {
                    if (DataPointTagsDao.NAME_TAG_KEY.equals(tagKey) || DataPointTagsDao.DEVICE_TAG_KEY.equals(tagKey)) {
                        response.addContextualMessage(DEFAULT_DISPLAY_TAGS, "validate.invalidTagKey");
                        break;
                    }
                    //TODO Mango 4.0 Ensure the tag exists?
                }
            }
        }catch(Exception e) {
            response.addContextualMessage(DEFAULT_DISPLAY_TAGS, "literal", e.getMessage());
        }
    }

}
