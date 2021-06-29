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
public class FiledataSizeInfoDefinition extends SystemInfoDefinition<Long>{

    public final String KEY = "filedataSize";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public Long getValue() {
        DirectoryInfo fileDatainfo = DirectoryUtils.getSize(Common.getFiledataPath().toFile());
        return fileDatainfo.getSize();
    }

    @Override
    public String getDescriptionKey() {
        return "systemInfo.filedataSizeDesc";
    }

}
