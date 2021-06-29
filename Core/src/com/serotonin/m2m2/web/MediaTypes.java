/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.http.MediaType;

public final class MediaTypes {
    private MediaTypes() {}

    public static final class VersionedMediaType extends MediaType {
        private static final long serialVersionUID = 1L;
        public static final String VERSION_PARAMETER = "version";
        private final Set<String> compatibleVersions = new HashSet<>();

        public VersionedMediaType(String type, String subtype, String version, String... compatibleVersions) {
            super(type, subtype, Collections.singletonMap(VERSION_PARAMETER, version));

            this.compatibleVersions.add(version);
            for (String v : compatibleVersions) {
                this.compatibleVersions.add(v);
            }
        }

        @Override
        public boolean isCompatibleWith(MediaType other) {
            boolean compatible = super.isCompatibleWith(other);

            if (!compatible) {
                return false;
            }

            String otherVersion = other.getParameter(VERSION_PARAMETER);
            return this.compatibleVersions.contains(otherVersion);
        }
    }

    // use these for produces/consumes annotations
    public static final String CSV_VALUE = "text/csv";
    public static final String SEROTONIN_JSON_VALUE = "application/vnd.infinite-automation-systems.mango.serotonin-json";

    // TODO Mango 4.0 remove null from compatible versions, only allow explicit version=1
    public static final MediaType CSV_V1 = new VersionedMediaType("text", "csv", "1", (String) null);
    public static final MediaType CSV_V2 = new VersionedMediaType("text", "csv", "2", (String) null);
    public static final MediaType SEROTONIN_JSON = new MediaType("application", "vnd.infinite-automation-systems.mango.serotonin-json");
}
