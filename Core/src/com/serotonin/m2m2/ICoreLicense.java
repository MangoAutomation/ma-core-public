package com.serotonin.m2m2;

import com.serotonin.provider.Provider;

public interface ICoreLicense extends Provider {
    void licenseCheck(boolean initialization);

    String getGuid();
}
