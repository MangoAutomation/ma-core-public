package com.infiniteautomation.mango.emport;

import java.util.Map;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.event.detectors.PointEventDetectorDefinition;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

public class EventDetectorImporter extends Importer {

    private Map<String, DataPointWithEventDetectors> dataPointMap;
    private final DataPointService dataPointService;

    public EventDetectorImporter(JsonObject json, Map<String, DataPointWithEventDetectors> dataPointMap, DataPointService dataPointService) {
        super(json);
        this.dataPointMap = dataPointMap;
        this.dataPointService = dataPointService;
    }

    @Override
    protected void importImpl() {
        String dataPointXid = json.getString("dataPointXid");
        DataPointWithEventDetectors dp;
        //Everyone is in the same thread so no synchronization on dataPointMap required.
        if(dataPointMap.containsKey(dataPointXid))
            dp = dataPointMap.get(dataPointXid);
        else {
            try {
                dp = dataPointService.getWithEventDetectors(dataPointXid);
                dataPointMap.put(dataPointXid, dp);
            }catch(NotFoundException e) {
                addFailureMessage("emport.error.missingPoint", dataPointXid);
                return;
            }
        }

        String typeStr = json.getString("type");
        if(typeStr == null)
            addFailureMessage("emport.error.ped.missingAttr", "type");
        PointEventDetectorDefinition<?> def = ModuleRegistry.getEventDetectorDefinition(typeStr);
        if (def == null) {
            addFailureMessage("emport.error.ped.invalid", "type", typeStr,
                    ModuleRegistry.getEventDetectorDefinitionTypes());
            return;
        }

        String xid = json.getString("xid");
        AbstractEventDetectorVO importing;
        importing = EventDetectorDao.getInstance().getByXid(xid);
        if(importing == null) {
            importing = def.baseCreateEventDetectorVO(dp.getDataPoint());
            importing.setDefinition(def);
            // Create a new one
            importing.setId(Common.NEW_ID);
            importing.setXid(xid);
        }

        JsonArray handlerXids = json.getJsonArray("handlers");
        if(handlerXids != null)
            for(int k = 0; k < handlerXids.size(); k+=1) {
                AbstractEventHandlerVO eh = EventHandlerDao.getInstance().getByXid(handlerXids.getString(k));
                if(eh == null) {
                    addFailureMessage("emport.eventHandler.missing", handlerXids.getString(k));
                    return;
                }else {
                    importing.addAddedEventHandler(eh);
                }
            }
        AbstractPointEventDetectorVO dped = (AbstractPointEventDetectorVO)importing;
        dp.addOrReplaceDetector(dped);

        try {
            ctx.getReader().readInto(importing, json);
        }
        catch (TranslatableJsonException e) {
            addFailureMessage("emport.eventDetector.prefix", xid, e.getMsg());
        }
        catch (JsonException e) {
            addFailureMessage("emport.eventDetector.prefix", xid, getJsonExceptionMessage(e));
        }
    }
}
