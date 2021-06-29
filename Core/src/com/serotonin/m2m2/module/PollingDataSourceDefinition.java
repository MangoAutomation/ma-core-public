/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.dataSource.PollingDataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.timer.CronTimerTrigger;

/**
 * Definition for polling data sources
 * 
 * @author Terry Packer
 *
 */
public abstract class PollingDataSourceDefinition<T extends PollingDataSourceVO> extends DataSourceDefinition<T> {

    @Override
    public void validate(ProcessResult response, T vo, PermissionHolder holder) {
        if(vo.isUseCron()) {
            if (StringUtils.isBlank(vo.getCronPattern()))
                response.addContextualMessage("cronPattern", "validate.required");
            else {
                try {
                    new CronTimerTrigger(vo.getCronPattern());
                }
                catch (Exception e) {
                    response.addContextualMessage("cronPattern", "validate.invalidCron", vo.getCronPattern());
                }
            }
            if(vo.isQuantize())
                response.addContextualMessage("quantize", "validate.cronCannotBeQuantized");
        }else {
            if (!Common.TIME_PERIOD_CODES.isValidId(vo.getUpdatePeriodType()))
                response.addContextualMessage("updatePeriodType", "validate.invalidValue");
            if (vo.getUpdatePeriods() <= 0)
                response.addContextualMessage("updatePeriods", "validate.greaterThanZero");
        }
    }

    
}
