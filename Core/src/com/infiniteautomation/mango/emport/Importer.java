package com.infiniteautomation.mango.emport;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;

abstract public class Importer {
    protected final JsonObject json;
    protected ImportContext ctx;
    private List<Importer> importers;

    private boolean success;
    private final List<ProcessMessage> failureMessages = new ArrayList<ProcessMessage>();
    private ProcessResult validationMessages;
    private String key;
    private String xid;

    public Importer(JsonObject json) {
        this.json = json;
    }

    public void setImportContext(ImportContext ctx) {
        this.ctx = ctx;
    }

    public void setImporters(List<Importer> importers) {
        this.importers = importers;
    }

    public final void doImport() {
        success = true;
        failureMessages.clear();
        validationMessages = null;
        importImpl();
    }

    abstract protected void importImpl();

    protected void addImporter(Importer importer) {
        importer.setImportContext(ctx);
        importer.setImporters(importers);
        importers.add(importer);
    }

    protected void addFailureMessage(ProcessMessage message){
    	success = false;
        failureMessages.add(message);
    }
    
    protected void addFailureMessage(String key, Object... params) {
        success = false;
        failureMessages.add(new ProcessMessage(key, params));
    }

    protected void setValidationMessages(ProcessResult messages, String key, String xid) {
        success = false;
        this.validationMessages = messages;
        this.key = key;
        this.xid = xid;
    }

    protected void addSuccessMessage(boolean isnew, String key, String desc) {
        ctx.addSuccessMessage(isnew, key, desc);
    }

    public boolean success() {
        return success;
    }

    public void copyMessages() {
        for (ProcessMessage m : failureMessages)
            ctx.getResult().addMessage(m);
        if (validationMessages != null)
            copyValidationMessages(validationMessages, key, xid);
    }

    private void copyValidationMessages(ProcessResult voResponse, String key, String desc) {
        ctx.copyValidationMessages(voResponse, key, desc);
    }

    protected String getJsonExceptionMessage(JsonException e) {
        return ctx.getJsonExceptionMessage(e);
    }
}
