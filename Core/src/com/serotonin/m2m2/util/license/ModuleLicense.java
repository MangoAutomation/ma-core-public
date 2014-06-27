package com.serotonin.m2m2.util.license;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import com.serotonin.util.XmlUtilsTS;

public class ModuleLicense {
    private final String name;
    private final int version;
    private final String licenseType;
    private final List<LicenseFeature> features;

    ModuleLicense(Element element) {
        name = element.getTagName();
        version = XmlUtilsTS.getChildElementTextAsInt(element, "version", -1);
        licenseType = XmlUtilsTS.getChildElementText(element, "licenseType");

        Element featuresElement = XmlUtilsTS.getChildElement(element, "features");
        List<LicenseFeature> f = new ArrayList<LicenseFeature>();
        for (Element feature : XmlUtilsTS.getChildElements(featuresElement))
            f.add(new LicenseFeature(feature));
        features = Collections.unmodifiableList(f);
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public List<LicenseFeature> getFeatures() {
        return features;
    }

    public LicenseFeature getFeature(String name) {
        for (LicenseFeature feature : features) {
            if (feature.getName().equals(name))
                return feature;
        }
        return null;
    }
}
