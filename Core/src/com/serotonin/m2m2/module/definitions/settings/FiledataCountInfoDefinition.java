/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.settings;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.SystemInfoDefinition;
import com.serotonin.util.DirectoryInfo;
import com.serotonin.util.DirectoryUtils;

/**
 * Class to define Read only settings/information that can be provided
 *
 * @author Terry Packer
 */
public class FiledataCountInfoDefinition extends SystemInfoDefinition<Integer>{

    public final String KEY = "filedataCount";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public Integer getValue() {
        DirectoryInfo fileDatainfo = DirectoryUtils.getSize(Common.getFiledataPath().toFile());
        return fileDatainfo.getCount();
    }

    @Override
    public String getDescriptionKey() {
        return "systemInfo.filedataCountDesc";
    }

}
