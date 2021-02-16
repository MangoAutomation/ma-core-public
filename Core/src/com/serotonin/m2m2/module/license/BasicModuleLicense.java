package com.serotonin.m2m2.module.license;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ConditionalDefinition;
import com.serotonin.m2m2.module.LicenseDefinition;

/**
 * Simply checks that a license for the module exists. Useful for modules that only provide a single license type,
 * such as "paid".
 */
@ConditionalDefinition(enabled = false)
public class BasicModuleLicense extends LicenseDefinition {
    private final List<TranslatableMessage> ERRORS = new ArrayList<>();

    @Override
    public void addLicenseErrors(List<TranslatableMessage> errors) {
        errors.addAll(ERRORS);
    }

    @Override
    public void addLicenseWarnings(List<TranslatableMessage> warnings) {
        // no op
    }

    @Override
    public void licenseCheck(boolean initialization) {
        if (initialization) {
            ERRORS.clear();
            if (getModule().license() == null)
                ERRORS.add(new TranslatableMessage("module.notLicensed"));
        }
    }
}
