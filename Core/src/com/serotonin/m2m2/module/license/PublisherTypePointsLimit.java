package com.serotonin.m2m2.module.license;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.util.license.LicenseFeature;
import com.serotonin.m2m2.util.license.ModuleLicense;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;

public class PublisherTypePointsLimit implements LicenseEnforcement {
    public static void checkLimit(PublisherVO<? extends PublishedPointVO> publisher, ProcessResult response) {
        String publisherType = publisher.getDefinition().getPublisherTypeName();

        for (PublisherTypePointsLimit limit : ModuleRegistry.getLicenseEnforcements(PublisherTypePointsLimit.class)) {
            if (StringUtils.equals(publisherType, limit.getPublisherType())) {
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
                    int count = new PublisherDao().countPointsForPublisherType(publisherType, publisher.getId());
                    count += publisher.getPoints().size();

                    // Apply count restriction.
                    if (count > pointLimit)
                        response.addGenericMessage("license.publisherPointLimit", pointLimit);
                }
            }
        }
    }

    private final String moduleName;
    private final String publisherType;
    private final int noLicenselimit;
    private final String featureName;

    public PublisherTypePointsLimit(String moduleName, String publisherType, int noLicenselimit, String featureName) {
        this.moduleName = moduleName;
        this.publisherType = publisherType;
        this.noLicenselimit = noLicenselimit;
        this.featureName = featureName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getPublisherType() {
        return publisherType;
    }

    public int getNoLicenselimit() {
        return noLicenselimit;
    }

    public String getFeatureName() {
        return featureName;
    }
}
