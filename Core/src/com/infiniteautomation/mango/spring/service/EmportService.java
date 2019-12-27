/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.ConfigurationExportData;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Service to facilitate JSON import/export
 * @author Terry Packer
 *
 */
@Service
public class EmportService {

    private final PermissionService permissionService;
    
    @Autowired
    public EmportService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }
    
    public String createExportData(int prettyIndent, String[] exportElements, PermissionHolder user) throws PermissionException {
        Map<String, Object> data = ConfigurationExportData.createExportDataMap(exportElements);
        return export(data, prettyIndent, user);
    }

    public String export(Map<String, Object> data, int prettyIndent, PermissionHolder user) throws PermissionException {
        permissionService.ensureAdminRole(user);
        JsonTypeWriter typeWriter = new JsonTypeWriter(Common.JSON_CONTEXT);
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, stringWriter);
        writer.setPrettyIndent(prettyIndent);
        writer.setPrettyOutput(true);

        try {
            JsonValue export = typeWriter.writeObject(data);
            writer.writeObject(export);
            return stringWriter.toString();
        }
        catch (JsonException e) {
            throw new ShouldNeverHappenException(e);
        }
        catch (IOException e) {
            throw new ShouldNeverHappenException(e);
        }
    }
}
