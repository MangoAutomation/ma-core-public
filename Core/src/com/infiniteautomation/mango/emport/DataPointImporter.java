/*
 *   Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 *   @author Matthew Lohbihler,Terry Packer
 */
package com.infiniteautomation.mango.emport;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.LicenseViolatedException;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.event.detectors.PointEventDetectorDefinition;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

/**
 *
 *
 * @author Terry Packer
 */
public class DataPointImporter extends Importer {

    private Map<String, DataPointWithEventDetectors> dataPointMap;
    private final DataPointService dataPointService;
    private final DataSourceService dataSourceService;

    public DataPointImporter(JsonObject json,
            Map<String, DataPointWithEventDetectors> dataPointMap,
            DataPointService dataPointService,
            DataSourceService dataSourceService) {
        super(json);
        this.dataPointMap = dataPointMap;
        this.dataPointService = dataPointService;
        this.dataSourceService = dataSourceService;
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        DataSourceVO dsvo = null;
        DataPointWithEventDetectors dp = null;

        if (StringUtils.isBlank(xid)) {
            xid = dataPointService.getDao().generateUniqueXid();
        }else {
            try {
                dp = dataPointService.getWithEventDetectors(xid);
            }catch(NotFoundException e) {

            }
        }
        if (dp == null) {
            // Locate the data source for the point.
            String dsxid = json.getString("dataSourceXid");
            try {
                dsvo = dataSourceService.get(dsxid);
            }catch(NotFoundException e) {
                addFailureMessage("emport.dataPoint.badReference", xid);
                return;
            }
            DataPointVO vo = new DataPointVO();
            vo.setXid(xid);
            vo.setDataSourceId(dsvo.getId());
            vo.setDataSourceXid(dsxid);
            vo.setPointLocator(dsvo.createPointLocator());

            dp = new DataPointWithEventDetectors(vo, new ArrayList<>());
        }

        //If there is already an entry, merge the event detectors
        DataPointWithEventDetectors existingMapping = dataPointMap.get(xid);
        if(existingMapping != null) {
            dp.getEventDetectors().addAll(existingMapping.getEventDetectors());
        }
        dataPointMap.put(xid, dp);

        if (dp != null) {

            try {
                //Read into the VO to get all properties
                ctx.getReader().readInto(dp.getDataPoint(), json);

                // If the name is not provided, default to the XID
                if (StringUtils.isBlank(dp.getDataPoint().getName()))
                    dp.getDataPoint().setName(xid);

                // If the chart colour is null provide default of '' to handle legacy code that sets colour to null
                if(dp.getDataPoint().getChartColour() == null)
                    dp.getDataPoint().setChartColour("");

                //Handle embedded event detectors
                JsonArray pedArray = json.getJsonArray("eventDetectors");
                if (pedArray != null) {

                    for (JsonValue jv : pedArray) {
                        JsonObject pedObject = jv.toJsonObject();

                        String pedXid = pedObject.getString("xid");
                        AbstractPointEventDetectorVO ped = null;

                        //TODO Mango 4.0 adding the option to create new ones during import, this used to be thrown if the xid was missing
                        //throw new TranslatableJsonException("emport.error.ped.missingAttr", "xid");

                        if (!StringUtils.isBlank(pedXid)) {
                            // Use the ped xid to lookup an existing ped.
                            for (AbstractPointEventDetectorVO existing : dp.getEventDetectors()) {
                                if (StringUtils.equals(pedXid, existing.getXid())) {
                                    ped = existing;
                                    break;
                                }
                            }
                        }

                        if (ped == null) {
                            String typeStr = pedObject.getString("type");
                            if(typeStr == null)
                                throw new TranslatableJsonException("emport.error.ped.missingAttr", "type");
                            //This assumes that all definitions used for data points are Data Point Event Detectors
                            PointEventDetectorDefinition<?> def = ModuleRegistry.getEventDetectorDefinition(typeStr);
                            if (def == null)
                                throw new TranslatableJsonException("emport.error.ped.invalid", "type", typeStr,
                                        ModuleRegistry.getEventDetectorDefinitionTypes());
                            else {
                                ped = def.baseCreateEventDetectorVO(dp.getDataPoint());
                                ped.setDefinition(def);
                            }

                            // Create a new one
                            ped.setId(Common.NEW_ID);
                            ped.setXid(pedXid);
                            dp.addOrReplaceDetector(ped);
                        }

                        JsonArray handlerXids = pedObject.getJsonArray("handlers");
                        if(handlerXids != null)
                            for(int k = 0; k < handlerXids.size(); k+=1) {
                                AbstractEventHandlerVO eh = EventHandlerDao.getInstance().getByXid(handlerXids.getString(k));
                                if(eh == null) {
                                    throw new TranslatableJsonException("emport.eventHandler.missing", handlerXids.getString(k));
                                }else {
                                    ped.addAddedEventHandler(eh);
                                }
                            }

                        ctx.getReader().readInto(ped, pedObject);
                    }
                }


                boolean isNew = dp.getDataPoint().isNew();
                try {
                    if(Common.runtimeManager.getState() == RuntimeManager.RUNNING) {
                        if(isNew) {
                            dataPointService.insert(dp.getDataPoint());
                            //Update all our event detector source Ids
                            for(AbstractPointEventDetectorVO ed : dp.getEventDetectors()) {
                                ed.setSourceId(dp.getDataPoint().getId());
                            }
                        }else {
                            dataPointService.update(dp.getDataPoint().getId(), dp.getDataPoint());
                        }
                        addSuccessMessage(isNew, "emport.dataPoint.prefix", xid);
                    }else{
                        addFailureMessage("emport.dataPoint.runtimeManagerNotRunning", xid);
                    }
                } catch(LicenseViolatedException e) {
                    addFailureMessage(new ProcessMessage(e.getErrorMessage()));
                }
            }catch(ValidationException e) {
                setValidationMessages(e.getValidationResult(), "emport.dataPoint.prefix", xid);
            }catch (TranslatableJsonException e) {
                addFailureMessage("emport.dataPoint.prefix", xid, e.getMsg());
            }catch (JsonException e) {
                addFailureMessage("emport.dataPoint.prefix", xid, getJsonExceptionMessage(e));
            }
        }
    }
}
