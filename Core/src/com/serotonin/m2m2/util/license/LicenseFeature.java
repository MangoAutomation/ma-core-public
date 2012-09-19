package com.serotonin.m2m2.util.license;

import org.w3c.dom.Element;

import com.serotonin.util.XmlUtilsTS;

public class LicenseFeature {
    private final String name;
    private final String value;

    LicenseFeature(Element element) {
        name = element.getTagName();
        value = XmlUtilsTS.getElementText(element, null);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
