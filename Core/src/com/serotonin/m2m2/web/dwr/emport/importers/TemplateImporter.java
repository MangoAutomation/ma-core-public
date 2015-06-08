/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr.emport.importers;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.TemplateDefinition;
import com.serotonin.m2m2.vo.template.BaseTemplateVO;
import com.serotonin.m2m2.web.dwr.emport.Importer;

/**
 * Template Importer
 * 
 * @author Terry Packer
 *
 */
public class TemplateImporter extends Importer {
    public TemplateImporter(JsonObject json) {
        super(json);
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");

        if (StringUtils.isBlank(xid))
            xid = TemplateDao.instance.generateUniqueXid();

        BaseTemplateVO<?> vo = TemplateDao.instance.getByXid(xid);
        if (vo == null) {
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
                    vo.setXid(xid);
                }
            }
        }

        if (vo != null) {
            try {
                // The VO was found or successfully created. Finish reading it in.
                ctx.getReader().readInto(vo, json);

                // Now validate it. Use a new response object so we can distinguish errors in this vo from
                // other errors.
                ProcessResult voResponse = new ProcessResult();
                vo.validate(voResponse);
                if (voResponse.getHasMessages())
                    setValidationMessages(voResponse, "emport.template.prefix", xid);
                else {
                    // Sweet. Save it.
                    boolean isnew = vo.isNew();
                    TemplateDao.instance.save(vo);
                    addSuccessMessage(isnew, "emport.template.prefix", xid);
                }
            }
            catch (TranslatableJsonException e) {
                addFailureMessage("emport.template.prefix", xid, e.getMsg());
            }
            catch (JsonException e) {
                addFailureMessage("emport.template.prefix", xid, getJsonExceptionMessage(e));
            }
        }
    }

}
