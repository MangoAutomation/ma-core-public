package com.infiniteautomation.mango.emport;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.PublisherService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.util.ILifecycleState;

public class PublisherImporter extends Importer {

    private final PublisherService service;

    public PublisherImporter(JsonObject json, PublisherService service) {
        super(json);
        this.service = service;
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        PublisherVO<? extends PublishedPointVO> vo = null;
        if (StringUtils.isBlank(xid)) {
            xid = service.generateUniqueXid();
        }else {
            try{
                vo = service.get(xid);
            }catch(NotFoundException e) {

            }
        }
        if (vo == null) {
            String typeStr = json.getString("type");
            if (StringUtils.isBlank(typeStr))
                addFailureMessage("emport.publisher.missingType", xid, ModuleRegistry.getPublisherDefinitionTypes());
            else {
                PublisherDefinition<?> def = ModuleRegistry.getPublisherDefinition(typeStr);
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

                boolean isnew = vo.isNew();
                if(Common.runtimeManager.getLifecycleState() == ILifecycleState.RUNNING){
                    if (isnew) {
                        service.insert(vo);
                    } else {
                        service.update(vo.getId(), vo);
                    }
                    addSuccessMessage(isnew, "emport.publisher.prefix", xid);
                }else{
                    addFailureMessage("emport.publisher.runtimeManagerNotRunning", xid);
                }
            }catch(ValidationException e) {
                setValidationMessages(e.getValidationResult(), "emport.publisher.prefix", xid);
            }catch (TranslatableJsonException e) {
                addFailureMessage("emport.publisher.prefix", xid, e.getMsg());
            }catch (JsonException e) {
                addFailureMessage("emport.publisher.prefix", xid, getJsonExceptionMessage(e));
            }
        }
    }
}
