/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.audit;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;

/**
 * Restore a Data Source from its Audit Trail
 * @author Terry Packer
 *
 */
public class DataSourceRestorer extends Restorer<DataSourceVO> {

    public DataSourceRestorer(List<AuditEventInstanceVO> trail, ProcessResult result) {
        super(trail, result);
    }

    @Override
    protected DataSourceVO getExisting(int id){
        return DataSourceDao.getInstance().get(id);
    }

    @Override
    protected DataSourceVO buildNewVO(JsonObject json){
        DataSourceVO vo = null;
        String xid = json.getString("xid");
        String typeStr = json.getString("type");
        if (StringUtils.isBlank(typeStr))
            addFailureMessage("emport.dataSource.missingType", xid, ModuleRegistry.getDataSourceDefinitionTypes());
        else {
            DataSourceDefinition<?> def = ModuleRegistry.getDataSourceDefinition(typeStr);
            if (def == null)
                addFailureMessage("emport.template.invalidType", xid, typeStr,
                        ModuleRegistry.getDataSourceDefinitionTypes());
            else {
                vo = def.baseCreateDataSourceVO();
            }
        }
        return vo;
    }

}
