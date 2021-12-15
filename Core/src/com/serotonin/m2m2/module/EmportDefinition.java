/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import com.infiniteautomation.mango.emport.ImportContext;
import com.serotonin.json.JsonException;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Import/Export functionality (aka 'emport') is a powerful feature that allows configuration to be imported or exported
 * via JSON files. An emport definition provides the information necessary to add new elements to the available emport
 * list.
 * 
 * @author Matthew Lohbihler
 */
abstract public class EmportDefinition extends ModuleElementDefinition {
    /**
     * @return the emport element id. Required to be unique within the MA instance.
     */
    abstract public String getElementId();

    /**
     * @return a reference key to the brief description to be used for this emport element.
     */
    abstract public String getDescriptionKey();

    /**
     * @return the data to be converted to JSON. Can be a Map<String, Object>, a {@link JsonSerializable}, or a
     *         {@link JsonRemoteEntity}
     */
    abstract public Object getExportData();

    public boolean importAsList() {
        return true;
    }

    /**
     * Import the given JSON data.  The importing User will be in the Background context.
     * 
     * @param json
     *            the JSON data to import
     * @param importContext
     *            the import context
     * @param holder 
     *            the permission holder doing the import
     */
    abstract public void doImport(JsonValue json, ImportContext importContext, PermissionHolder holder) throws JsonException;
    
    /**
     * Does this emporter need to show up in the UI and be included in configuration backups
     */
    public boolean getInView(){
    	return true; 
    }
    
}
