/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.audit;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.TemplateDefinition;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;
import com.serotonin.m2m2.vo.template.BaseTemplateVO;

/**
 * Restore a Template from its Audit Trail
 * @author Terry Packer
 *
 */
public class TemplateRestorer extends Restorer<BaseTemplateVO<?>>{

	
	public TemplateRestorer(List<AuditEventInstanceVO> trail, ProcessResult result) {
		super(trail, result);
	}

	@Override
	protected BaseTemplateVO<?> getExisting(int id){
		return TemplateDao.getInstance().get(id, false);
	}
	
	@Override
	protected BaseTemplateVO<?> buildNewVO(JsonObject json){
		BaseTemplateVO<?> vo = null;
		String xid = json.getString("xid");
        String typeStr = json.getString("templateType");
        if (StringUtils.isBlank(typeStr))
            addFailureMessage("emport.template.missingType", xid, ModuleRegistry.getTemplateDefinitionTypes());
        else {
            TemplateDefinition def = ModuleRegistry.getTemplateDefinition(typeStr);
            if (def == null)
                addFailureMessage("emport.template.invalidType", xid, typeStr,
                        ModuleRegistry.getTemplateDefinitionTypes());
            else {
                vo = def.baseCreateTemplateVO();
            }
        }
        return vo;
	}
	
}
