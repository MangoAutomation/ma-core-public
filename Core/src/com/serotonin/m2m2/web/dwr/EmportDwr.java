/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import com.infiniteautomation.mango.util.ConfigurationExportData;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.dwr.emport.ImportTask;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

/**
 * @author Matthew Lohbihler
 */
public class EmportDwr extends BaseDwr {

    @DwrPermission(admin = true)
    public String createExportData(int prettyIndent, String[] exportElements) {
        Map<String, Object> data = ConfigurationExportData.createExportDataMap(exportElements);
        return export(data, prettyIndent);
    }

    public static String export(Map<String, Object> data) {
        return export(data, 3);
    }

    public static String export(Map<String, Object> data, int prettyIndent) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, stringWriter);
        writer.setPrettyIndent(prettyIndent);
        writer.setPrettyOutput(true);

        try {
            writer.writeObject(data);
            return stringWriter.toString();
        }
        catch (JsonException e) {
            throw new ShouldNeverHappenException(e);
        }
        catch (IOException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    @DwrPermission(admin = true)
    public ProcessResult importData(String data) {
        ProcessResult response = new ProcessResult();
        Translations translations = getTranslations();

        User user = Common.getHttpUser();

        JsonTypeReader reader = new JsonTypeReader(data);
        try {
            JsonValue value = reader.read();
            if (value instanceof JsonObject) {
                JsonObject root = value.toJsonObject();
                ImportTask importTask = new ImportTask(root, translations, user, true);
                user.setImportTask(importTask);
                response.addData("importStarted", true);
            }
            else {
                response.addGenericMessage("emport.invalidImportData");
            }
        }
        catch (ClassCastException e) {
            response.addGenericMessage("emport.parseError", e.getMessage());
        }
        catch (TranslatableJsonException e) {
            response.addMessage(e.getMsg());
        }
        catch (IOException e) {
            response.addGenericMessage("emport.parseError", e.getMessage());
        }
        catch (JsonException e) {
            response.addGenericMessage("emport.parseError", e.getMessage());
        }

        return response;
    }

    @DwrPermission(admin = true)
    public ProcessResult importUpdate() {
        ProcessResult response;
        User user = Common.getUser();
        ImportTask importTask = user.getImportTask();
        if (importTask != null) {
            response = importTask.getResponse();

            if (importTask.isCancelled()) {
                response.addData("cancelled", true);
                user.setImportTask(null);
            }
            else if (importTask.isCompleted()) {
                response.addData("complete", true);
                user.setImportTask(null);
            }
        }
        else {
            response = new ProcessResult();
            response.addData("noImport", true);
        }
        return response;
    }

    @DwrPermission(admin = true)
    public void importCancel() {
        User user = Common.getUser();
        if (user.getImportTask() != null)
            user.getImportTask().cancel();
    }
}
