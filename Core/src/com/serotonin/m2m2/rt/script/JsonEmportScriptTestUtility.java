/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.infiniteautomation.mango.spring.components.RunAs;

public class JsonEmportScriptTestUtility extends JsonEmportScriptUtility {

    @Autowired
    public JsonEmportScriptTestUtility(MangoJavaScriptService service, PermissionService permissionService,
            DataSourceService dataSourceService, DataPointDao dataPointDao, RunAs runAs) {
        super(service, permissionService, dataSourceService, dataPointDao, runAs);
    }

    @Override
    public void doImport(String json) throws Exception {
        //No import in testing! Unless...
        if(importDuringValidation)
            super.doImport(json);
    }

    @Override
    public List<ProcessMessage> doImportGetStatus(String json) throws Exception {
        if(!importDuringValidation) {
            List<ProcessMessage> result = new ArrayList<>(1);
            result.add(new ProcessMessage(new TranslatableMessage("literal", "Cannot run or test imports during validation.")));
            return result;
        } else
            return super.doImportGetStatus(json);
    }

}
