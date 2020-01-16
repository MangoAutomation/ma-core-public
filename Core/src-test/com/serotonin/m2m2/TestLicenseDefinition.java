/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.LicenseDefinition;

/**
 * @author Terry Packer
 *
 */
public class TestLicenseDefinition extends LicenseDefinition implements ICoreLicense {

    @Override
    public void licenseCheck(boolean initialization) {

    }

    @Override
    public int getLicenseState() {
        return 0;
    }

    @Override
    public String getGuid() {
        return null;
    }

    @Override
    public void addLicenseErrors(List<TranslatableMessage> errors) {

    }

    @Override
    public void addLicenseWarnings(List<TranslatableMessage> warnings) {

    }

}
