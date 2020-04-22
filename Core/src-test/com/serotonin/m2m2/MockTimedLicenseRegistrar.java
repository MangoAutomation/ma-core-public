/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2;

import com.serotonin.m2m2.module.license.ITimedLicenseDefinition;
import com.serotonin.m2m2.module.license.ITimedLicenseRegistrar;

/**
 *
 * @author Terry Packer
 */
public class MockTimedLicenseRegistrar implements ITimedLicenseRegistrar {

    @Override
    public void registerTimedLicense(ITimedLicenseDefinition license) {

    }

    @Override
    public void checkLicenses(boolean initialization) {

    }

    @Override
    public void shutdown() {

    }

}
