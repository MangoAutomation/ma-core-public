package com.infiniteautomation.mango.emport;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.PublishedPointService;
import com.infiniteautomation.mango.spring.service.PublisherService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.util.ILifecycleState;

public class PublisherImporter extends Importer {

    private final PublisherService service;
    private final PublishedPointService publishedPointService;
    private final DataPointService dataPointService;

    public PublisherImporter(JsonObject json, PublisherService service,
                             PublishedPointService publishedPointService,
                             DataPointService dataPointService) {
        super(json);
        this.service = service;
        this.publishedPointService = publishedPointService;
        this.dataPointService = dataPointService;
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        PublisherVO vo = null;
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

                    // Handle embedded points (pre Mango 4.3 this was the case)
                    JsonArray arr = json.getJsonArray("points");
                    if (arr != null) {
                        List<PublishedPointVO> points = new ArrayList<>();
                        for (JsonValue jv : arr) {
                            String dataPointXid = jv.getJsonValue("dataPointId").toString();
                            try {
                                DataPointVO dataPointVO = dataPointService.get(dataPointXid);
                                PublishedPointVO point = vo.getDefinition().createPublishedPointVO(vo, dataPointVO);
                                ctx.getReader().readInto(point, jv.toJsonObject());
                                points.add(point);
                            }catch(NotFoundException e) {
                                addFailureMessage("emport.publisher.prefix",
                                        xid,
                                        new TranslatableMessage("emport.error.missingPoint", dataPointXid));
                            }
                        }
                        savePublishedPoints(xid, points);
                    }
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

    /**
     * Save published points, these must always be new points
     *  since importing this way they could not already exist
     *  TODO Published Points - Is this the best way to handle this?
     * @param publisherXid
     * @param points
     */
    private void savePublishedPoints(String publisherXid, List<PublishedPointVO> points) {
        for(PublishedPointVO vo : points) {
            try {
                publishedPointService.insert(vo);
            }catch(ValidationException e) {
                setValidationMessages(e.getValidationResult(), "emport.publisher.prefix", publisherXid);
            }
        }
    }
}
