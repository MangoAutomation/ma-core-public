/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2;

import java.util.List;
import java.util.UUID;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.LicenseDefinition;

/**
 * @author Terry Packer
 *
 */
public class TestLicenseDefinition extends LicenseDefinition implements ICoreLicense {

    final String guid = "2-" + UUID.randomUUID().toString();

    @Override
    public void licenseCheck(boolean initialization) {

    }

    @Override
    public int getLicenseState() {
        return 0;
    }

    @Override
    public String getGuid() {
        return guid;
    }

    @Override
    public void addLicenseErrors(List<TranslatableMessage> errors) {

    }

    @Override
    public void addLicenseWarnings(List<TranslatableMessage> warnings) {

    }

}
