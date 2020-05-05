/**
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.infiniteautomation.mango.emport;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

public class DataSourceImporter extends Importer {

    private final DataSourceService service;
    public DataSourceImporter(JsonObject json, DataSourceService dataSourceService) {
        super(json);
        this.service = dataSourceService;
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        DataSourceVO vo = null;
        if (StringUtils.isBlank(xid)) {
            xid = service.generateUniqueXid();
        }else {
            try {
                vo = service.get(xid);
            }catch(NotFoundException e) {

            }
        }
        if (vo == null) {
            String typeStr = json.getString("type");
            if (StringUtils.isBlank(typeStr))
                addFailureMessage("emport.dataSource.missingType", xid, ModuleRegistry.getDataSourceDefinitionTypes());
            else {
                DataSourceDefinition<?> def = ModuleRegistry.getDataSourceDefinition(typeStr);
                if (def == null)
                    addFailureMessage("emport.dataSource.invalidType", xid, typeStr,
                            ModuleRegistry.getDataSourceDefinitionTypes());
                else {
                    vo = def.baseCreateDataSourceVO();
                    vo.setXid(xid);
                }
            }
        }

        if (vo != null) {
            try {
                // The VO was found or successfully created. Finish reading it in.
                ctx.getReader().readInto(vo, json);
                boolean isnew = vo.isNew();
                if(Common.runtimeManager.getState() == RuntimeManager.RUNNING) {
                    if(isnew) {
                        service.insert(vo);
                    }else {
                        service.update(vo.getId(), vo);
                    }
                    addSuccessMessage(isnew, "emport.dataSource.prefix", xid);
                }else{
                    addFailureMessage("emport.dataSource.runtimeManagerNotRunning", xid);
                }
            }catch(ValidationException e) {
                setValidationMessages(e.getValidationResult(), "emport.dataSource.prefix", xid);
            }catch (TranslatableJsonException e) {
                addFailureMessage("emport.dataSource.prefix", xid, e.getMsg());
            }catch (JsonException e) {
                addFailureMessage("emport.dataSource.prefix", xid, getJsonExceptionMessage(e));
            }
        }
    }
}
