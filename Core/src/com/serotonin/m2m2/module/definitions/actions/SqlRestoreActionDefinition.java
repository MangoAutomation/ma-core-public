/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.SystemActionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.SqlRestoreActionPermissionDefinition;
import com.serotonin.m2m2.rt.maint.work.DatabaseBackupWorkItem;
import com.serotonin.m2m2.util.timeout.SystemActionTask;
import com.serotonin.timer.OneTimeTrigger;

/**
 *
 * @author Terry Packer
 */
public class SqlRestoreActionDefinition extends SystemActionDefinition{

    private final String KEY = "sqlRestore";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public SystemActionTask getTaskImpl(final JsonNode input) {
        JsonNode filename = input.get("filename");
        return new Action(filename.asText());
    }

    @Override
    protected String getPermissionTypeName() {
        return SqlRestoreActionPermissionDefinition.PERMISSION;
    }

    @Override
    protected void validate(JsonNode input) throws ValidationException {
        ProcessResult result = new ProcessResult();

        JsonNode node = input.get("filename");
        if(node == null)
            result.addContextualMessage("filename", "validate.required");

        result.ensureValid();
    }

    /**
     * Class to allow purging data in ordered tasks with a queue
     * of up to 5 waiting purges
     *
     * @author Terry Packer
     */
    class Action extends SystemActionTask{

        private String filename;

        public Action(String filename){
            super(new OneTimeTrigger(0l), "SQL Database Restore", "DATABASE_RESTORE", 5);
            this.filename = filename;
        }

        @Override
        public void runImpl(long runtime) {
            ProcessResult result = DatabaseBackupWorkItem.restore(filename);
            if(result.getHasMessages()){
                this.results.put("messages", result.getMessages());
                this.results.put("failed", true);
            }
        }
    }
}
