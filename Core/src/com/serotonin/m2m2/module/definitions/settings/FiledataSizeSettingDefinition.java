/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.settings;

import java.io.File;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.ReadOnlySettingDefinition;
import com.serotonin.util.DirectoryInfo;
import com.serotonin.util.DirectoryUtils;

/**
 * Class to define Read only settings/information that can be provided
 * 
 * @author Terry Packer
 */
public class FiledataSizeSettingDefinition extends ReadOnlySettingDefinition<Integer>{

	public final String KEY = "filedataCount";
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
	public Integer getValue() {
        DirectoryInfo fileDatainfo = DirectoryUtils.getSize(new File(Common.getFiledataPath()));
        return fileDatainfo.getCount();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ReadOnlySettingDefinition#getDescription()
	 */
	@Override
	public TranslatableMessage getDescription() {
		return new TranslatableMessage("systemSettings.filedataCount");
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ModuleElementDefinition#getModule()
	 */
	@Override
	public Module getModule() {
		return ModuleRegistry.getCoreModule();
	}

}
