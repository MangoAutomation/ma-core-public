/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.emport;

import java.util.concurrent.CopyOnWriteArrayList;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessMessage.Level;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.Translations;

public class ImportContext {

    private final JsonReader reader;
    private final ProcessResult result;
    private final Translations translations;

    public ImportContext(JsonReader reader, ProcessResult result, Translations translations) {
        this.reader = reader;
        this.result = result;
        this.translations = translations;

        result.setMessages(new CopyOnWriteArrayList<ProcessMessage>());
    }
    
    public JsonReader getReader() {
        return reader;
    }

    public ProcessResult getResult() {
        return result;
    }

    public Translations getTranslations() {
        return translations;
    }

    public void copyValidationMessages(ProcessResult voResponse, String key, String desc) {
        for (ProcessMessage msg : voResponse.getMessages())
            result.addGenericMessage(key, desc, msg.toString(translations));
    }

    public void addSuccessMessage(boolean isnew, String key, String desc) {
        if (isnew)
            result.addGenericMessage(Level.info, key, desc, translations.translate("emport.added"));
        else
            result.addGenericMessage(Level.info, key, desc, translations.translate("emport.saved"));
    }

    public String getJsonExceptionMessage(JsonException e) {
        String msg = "'" + e.getMessage() + "'";
        Throwable t = e;
        while ((t = t.getCause()) != null) {
            if (t instanceof TranslatableJsonException)
                msg += ", " + translations.translate("emport.causedBy") + " '"
                        + ((TranslatableJsonException) t).getMsg().translate(translations) + "'";
            else
                msg += ", " + translations.translate("emport.causedBy") + " '" + t.getMessage() + "'";
        }
        return msg;
    }
}
