/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.license;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.util.license.LicenseFeature;
import com.serotonin.m2m2.util.license.ModuleLicense;

/**
 * License feature that limits a data source type to a maximum number of data points.
 * 
 */
public class DataSourceTypePointsLimit implements LicenseEnforcement {
    public static void checkLimit(String dataSourceType, ProcessResult response) {
        for (DataSourceTypePointsLimit limit : ModuleRegistry.getLicenseEnforcements(DataSourceTypePointsLimit.class)) {
            if (StringUtils.equals(dataSourceType, limit.getDataSourceType())) {
                // Found a limit object. 
                ModuleLicense license = ModuleRegistry.getModule(limit.getModuleName()).license();
                int pointLimit = -1;
                if (license == null)
                    // No license.
                    pointLimit = limit.getNoLicenselimit();
                else if (limit.getFeatureName() != null) {
                    try {
                        LicenseFeature feature = license.getFeature(limit.getFeatureName());
                        if (feature != null)
                            pointLimit = Integer.parseInt(feature.getName());
                    }
                    catch (NumberFormatException e) {
                        // If the feature is non-numeric, we assume that it is unlimited.
                    }
                }

                if (pointLimit != -1) {
                    // Find out how many points there are for this type of data source.
                    int count = DataPointDao.instance.countPointsForDataSourceType(dataSourceType);

                    // Apply count restriction.
                    if (count >= pointLimit)
                        response.addGenericMessage("license.dataSourcePointLimit", pointLimit);
                }
            }
        }
    }

    private final String moduleName;
    private final String dataSourceType;
    private final int noLicenselimit;
    private final String featureName;

    public DataSourceTypePointsLimit(String moduleName, String dataSourceType, int noLicenselimit, String featureName) {
        this.moduleName = moduleName;
        this.dataSourceType = dataSourceType;
        this.noLicenselimit = noLicenselimit;
        this.featureName = featureName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getDataSourceType() {
        return dataSourceType;
    }

    public int getNoLicenselimit() {
        return noLicenselimit;
    }

    public String getFeatureName() {
        return featureName;
    }
}

