package com.serotonin.m2m2.module.license;

import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.util.license.ModuleLicense;

public class TimedExpiryChecker implements LicenseEnforcement{
    private final ModuleLicense license;
    private final ExpiryAction onExpire;
    private final long expiryTime;
    private boolean expired;

    public TimedExpiryChecker(String moduleName, int licensePeriod, ExpiryAction onExpire) {
        license = ModuleRegistry.getModule(moduleName).license();
        expiryTime = System.currentTimeMillis() + licensePeriod;
        this.onExpire = onExpire;
    }

    public boolean isExpired() {
        // If the module is licensed, always return false.
        if (license != null)
            return false;

        // If the owner has already expired, always return true.
        if (expired)
            return true;

        // Check if owner has expired.
        if (expiryTime < System.currentTimeMillis()) {
            // Synchronize to ensure this works in a multi-threaded environment.
            synchronized (this) {
                // Now that we're sync'ed, check the expired flag again.
                if (!expired) {
                    expired = true;

                    if (onExpire != null)
                        onExpire.onExpire();

                    return true;
                }
            }
        }

        return false;
    }
}
