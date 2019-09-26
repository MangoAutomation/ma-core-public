package com.serotonin.m2m2.web.dwr.emport.importers;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.dwr.emport.Importer;

public class PublisherImporter extends Importer {
    public PublisherImporter(JsonObject json, PermissionHolder user) {
        super(json, user);
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");

        if (StringUtils.isBlank(xid))
            xid = ctx.getPublisherDao().generateUniqueXid();

        PublisherVO<?> vo = ctx.getPublisherDao().getPublisher(xid);
        if (vo == null) {
            String typeStr = json.getString("type");
            if (StringUtils.isBlank(typeStr))
                addFailureMessage("emport.publisher.missingType", xid, ModuleRegistry.getPublisherDefinitionTypes());
            else {
                PublisherDefinition def = ModuleRegistry.getPublisherDefinition(typeStr);
                if (def == null)
                    addFailureMessage("emport.publisher.invalidType", xid, typeStr,
                            ModuleRegistry.getPublisherDefinitionTypes());
                else {
                    vo = def.baseCreatePublisherVO();
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
                    setValidationMessages(voResponse, "emport.publisher.prefix", xid);
                else {
                    // Sweet. Save it.
                    boolean isnew = vo.isNew();
                    if(Common.runtimeManager.getState() == RuntimeManager.RUNNING){
	                    Common.runtimeManager.savePublisher(vo);
	                    addSuccessMessage(isnew, "emport.publisher.prefix", xid);
                    }else{
                    	addFailureMessage(new ProcessMessage("Runtime manager not running publisher with xid : " + vo.getXid() + " not saved."));
                    }
                }
            }
            catch (TranslatableJsonException e) {
                addFailureMessage("emport.publisher.prefix", xid, e.getMsg());
            }
            catch (JsonException e) {
                addFailureMessage("emport.publisher.prefix", xid, getJsonExceptionMessage(e));
            }
        }
    }
}
