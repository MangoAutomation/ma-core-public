package com.serotonin.m2m2;

import com.serotonin.provider.Provider;

public interface ICoreLicense extends Provider {
	public int OKAY = 0;
	public int INVALID = 1;
	public int VIOLATED = 2;
	
    void licenseCheck(boolean initialization);
    
    int getLicenseState();

    String getGuid();
}
