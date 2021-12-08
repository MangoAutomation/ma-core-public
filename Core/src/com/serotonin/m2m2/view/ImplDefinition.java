/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.serotonin.m2m2.DataTypes;

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
    private final Set<DataTypes> supportedDataTypes;

    public ImplDefinition(int id, String exportName, String nameKey, Set<DataTypes> supportedDataTypes) {
        this.id = id;
        this.nameKey = nameKey;
        this.exportName = exportName;
        this.supportedDataTypes = supportedDataTypes;
    }

    public ImplDefinition(String name, String exportName, String nameKey, Set<DataTypes> supportedDataTypes) {
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

    public Set<DataTypes> getSupportedDataTypes() {
        return Collections.unmodifiableSet(supportedDataTypes);
    }

    public boolean supports(DataTypes dataType) {
        return supportedDataTypes.contains(dataType);
    }
}
