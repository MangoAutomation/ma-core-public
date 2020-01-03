/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.settings;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;
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
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.ReadOnlySettingDefinition#getName()
     */
    @Override
    public String getKey() {
        return KEY;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.ReadOnlySettingDefinition#getValue()
     */
    @Override
    public Long getValue() {
        DirectoryInfo fileDatainfo = DirectoryUtils.getSize(Common.getFiledataPath().toFile());
        return fileDatainfo.getSize();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.ModuleElementDefinition#getModule()
     */
    @Override
    public Module getModule() {
        return ModuleRegistry.getCoreModule();
    }

    @Override
    public String getDescriptionKey() {
        return "systemInfo.filedataSizeDesc";
    }

}
