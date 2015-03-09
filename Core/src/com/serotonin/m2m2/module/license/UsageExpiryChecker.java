package com.serotonin.m2m2.module.license;

import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.util.license.ModuleLicense;

public class UsageExpiryChecker {
    private final ModuleLicense license;
    private final ExpiryAction onExpire;
    private int usagesRemaining;

    public UsageExpiryChecker(String moduleName, int maxUsages, ExpiryAction onExpire) {
        license = ModuleRegistry.getModule(moduleName).license();
        usagesRemaining = maxUsages;
        this.onExpire = onExpire;
    }

    public boolean isExpired(boolean decrementUsagesRemaining) {
        // If the module is licensed, always return false.
        if (license != null)
            return false;

        // If the owner has already expired, always return true.
        if (usagesRemaining <= 0)
            return true;

        if (decrementUsagesRemaining) {
            synchronized (this) {
                usagesRemaining--;

                // Check if the owner has expired.
                if (usagesRemaining == 0) {
                    if (onExpire != null)
                        onExpire.onExpire();
                }
            }
        }

        return false;
    }
}
