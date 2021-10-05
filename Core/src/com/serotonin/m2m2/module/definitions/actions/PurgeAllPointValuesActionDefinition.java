/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.SystemActionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.PurgeAllPointValuesActionPermissionDefinition;
import com.serotonin.m2m2.util.timeout.SystemActionTask;
import com.serotonin.timer.OneTimeTrigger;
import com.serotonin.util.ILifecycleState;

/**
 * @author Terry Packer
 */
public class PurgeAllPointValuesActionDefinition extends SystemActionDefinition {

    private final String KEY = "purgeAllPointValues";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public SystemActionTask getTaskImpl(final JsonNode input) {
        return new PurgeAllPointValuesAction();
    }


    @Override
    protected String getPermissionTypeName() {
        return PurgeAllPointValuesActionPermissionDefinition.PERMISSION;
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
    static class PurgeAllPointValuesAction extends SystemActionTask {

        public PurgeAllPointValuesAction() {
            super(new OneTimeTrigger(0L), "Purge All Point Values", "ALL_POINT_VALUE_PURGE", 5);
        }

        @Override
        public void runImpl(long runtime) {
            if (Common.runtimeManager.getLifecycleState() == ILifecycleState.RUNNING) {
                this.results.put("deleted", Common.runtimeManager.purgeDataPointValues().orElse(-1L));
            }
        }
    }
}
