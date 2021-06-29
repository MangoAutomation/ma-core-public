/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

public class ImplDefinition {
    public static ImplDefinition findByName(List<ImplDefinition> list, String name) {
        for (ImplDefinition def : list) {
            if (def.getName().equals(name))
                return def;
        }
        return null;
    }

    public static ImplDefinition findByExportName(List<ImplDefinition> list, String exportName) {
        for (ImplDefinition def : list) {
            if (def.getExportName().equalsIgnoreCase(exportName))
                return def;
        }
        return null;
    }

    private int id;
    private String name;
    private String exportName;
    private final String nameKey;
    private final int[] supportedDataTypes;

    public ImplDefinition(int id, String exportName, String nameKey, int[] supportedDataTypes) {
        this.id = id;
        this.nameKey = nameKey;
        this.exportName = exportName;
        this.supportedDataTypes = supportedDataTypes;
    }

    public ImplDefinition(String name, String exportName, String nameKey, int[] supportedDataTypes) {
        this.name = name;
        this.nameKey = nameKey;
        this.exportName = exportName;
        this.supportedDataTypes = supportedDataTypes;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNameKey() {
        return nameKey;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    public int[] getSupportedDataTypes() {
        return supportedDataTypes;
    }

    public boolean supports(int dataType) {
        return ArrayUtils.contains(supportedDataTypes, dataType);
    }
}
