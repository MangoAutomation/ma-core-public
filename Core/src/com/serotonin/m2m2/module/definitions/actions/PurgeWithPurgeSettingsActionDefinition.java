/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.SystemActionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.PurgeWithPurgeSettingsActionPermissionDefinition;
import com.serotonin.m2m2.rt.maint.DataPurge;
import com.serotonin.m2m2.util.timeout.SystemActionTask;
import com.serotonin.timer.OneTimeTrigger;

/**
 *
 * @author Terry Packer
 */
public class PurgeWithPurgeSettingsActionDefinition extends SystemActionDefinition{

    private final String KEY = "purgeUsingSettings";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public SystemActionTask getTaskImpl(final JsonNode input) {
        return new Action();
    }

    @Override
    protected String getPermissionTypeName() {
        return PurgeWithPurgeSettingsActionPermissionDefinition.PERMISSION;
    }

    @Override
    protected void validate(JsonNode input) throws ValidationException {

    }

    /**
     * Class to allow purging data in ordered tasks with a queue
     * of up to 5 waiting purges
     *
     * @author Terry Packer
     */
    class Action extends SystemActionTask {

        public Action() {
            super(new OneTimeTrigger(0l), "Purge Using Settings", "PURGE_USING_SETTINGS", 5);
        }

        @Override
        public void runImpl(long runtime) {
            DataPurge dataPurge = new DataPurge();
            dataPurge.execute(Common.timer.currentTimeMillis());
            this.results.put("countPointValues", dataPurge.isCountPointValues());
            this.results.put("deletedPointValues", dataPurge.getDeletedSamples());
            this.results.put("deletedFiles", dataPurge.getDeletedFiles());
            this.results.put("deletedEvents", dataPurge.getDeletedEvents());
            this.results.put("anyDeletedSamples", dataPurge.isAnyDeletedSamples());
        }
    }
}
