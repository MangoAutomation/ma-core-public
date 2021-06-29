/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.emport;

import java.util.List;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.module.EmportDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

public class ImportItem {
    private final EmportDefinition emportDefinition;
    private JsonValue root;
    private List<JsonValue> list;
    private int index;

    public ImportItem(EmportDefinition emportDefinition, JsonValue root) {
        if (root != null) {
            if (emportDefinition.importAsList())
                list = root.toJsonArray();
            else
                this.root = root;
        }

        this.emportDefinition = emportDefinition;
    }

    public boolean isComplete() {
        if (root != null)
            return false;
        if (list != null)
            return index >= list.size();
        return true;
    }

    public void importNext(ImportContext importContext, PermissionHolder importer) throws JsonException {
        if (root != null) {
            emportDefinition.doImport(root, importContext, importer);
            root = null;
        }
        else if (list != null)
            emportDefinition.doImport(list.get(index++), importContext, importer);
    }

    /**
     * @return
     */
    public EmportDefinition getEmportDefinition() {
        return emportDefinition;
    }
}
