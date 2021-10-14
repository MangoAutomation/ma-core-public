/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.emport;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.PublishedPointService;
import com.infiniteautomation.mango.spring.service.PublisherService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.util.ILifecycleState;

public class PublishedPointImporter extends Importer {

    private final PublishedPointService service;
    private final PublisherService publisherService;
    private final DataPointService dataPointService;

    public PublishedPointImporter(JsonObject json,
                                  PublishedPointService service,
                                  PublisherService publisherService,
                                  DataPointService dataPointService) {
        super(json);
        this.service = service;
        this.dataPointService = dataPointService;
        this.publisherService = publisherService;
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        PublishedPointVO vo = null;
        PublisherVO publisherVO = null;
        DataPointVO dataPointVO = null;

        if (StringUtils.isBlank(xid)) {
            xid = service.generateUniqueXid();
        }else {
            try{
                vo = service.get(xid);
            }catch(NotFoundException e) {

            }
        }
        if (vo == null) {
            String pubXid = json.getString("publisherXid");
            try {
                publisherVO = publisherService.get(pubXid);
            }catch(NotFoundException e) {
                addFailureMessage("emport.publishedPoint.badPublisherReference", xid);
                return;
            }

            String dpXid = json.getString("dataPointXid");
            try {
                dataPointVO = dataPointService.get(dpXid);
            }catch(NotFoundException e) {
                addFailureMessage("emport.publishedPoint.badDataPointReference", xid);
                return;
            }

            vo = publisherVO.getDefinition().createPublishedPointVO(publisherVO, dataPointVO);
            vo.setXid(xid);
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
                    addSuccessMessage(isnew, "emport.publishedPoint.prefix", xid);
                }else{
                    addFailureMessage("emport.publishedPoint.runtimeManagerNotRunning", xid);
                }
            }catch(ValidationException e) {
                setValidationMessages(e.getValidationResult(), "emport.publishedPoint.prefix", xid);
            }catch (TranslatableJsonException e) {
                addFailureMessage("emport.publishedPoint.prefix", xid, e.getMsg());
            }catch (JsonException e) {
                addFailureMessage("emport.publishedPoint.prefix", xid, getJsonExceptionMessage(e));
            }
        }
    }
}
