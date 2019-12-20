package com.infiniteautomation.mango.emport;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.event.detectors.PointEventDetectorDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

public class EventDetectorImporter extends Importer {

    private Map<String, DataPointVO> dataPointMap;
    
	public EventDetectorImporter(JsonObject json, PermissionHolder user, Map<String, DataPointVO> dataPointMap) {
		super(json, user);
		this.dataPointMap = dataPointMap;
	}

	@Override
	protected void importImpl() {
		String dataPointXid = json.getString("dataPointXid");
		DataPointVO dpvo;
		//Everyone is in the same thread so no synchronization on dataPointMap required.
		if(dataPointMap.containsKey(dataPointXid))
		    dpvo = dataPointMap.get(dataPointXid);
		else if(StringUtils.isEmpty(dataPointXid) || (dpvo = DataPointDao.getInstance().getByXid(dataPointXid)) == null) {
			addFailureMessage("emport.error.missingPoint", dataPointXid);
			return;
		} else {
		    dataPointMap.put(dataPointXid, dpvo);
		    //We're only going to use this to house event detectors imported in the eventDetectors object.
		    dpvo.setEventDetectors(new ArrayList<AbstractPointEventDetectorVO<?>>());
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

        AbstractEventDetectorVO<?> importing = def.baseCreateEventDetectorVO(dpvo);
        importing.setDefinition(def);
        
        JsonArray handlerXids = json.getJsonArray("handlers");
        if(handlerXids != null)
            for(int k = 0; k < handlerXids.size(); k+=1) {
                AbstractEventHandlerVO<?> eh = EventHandlerDao.getInstance().getByXid(handlerXids.getString(k));
                if(eh == null) {
                    addFailureMessage("emport.eventHandler.missing", handlerXids.getString(k));
                    return;
                }else {
                    importing.addAddedEventHandler(eh);
                }
            }
        
        String xid = json.getString("xid");

        // Create a new one
        importing.setId(Common.NEW_ID);
        importing.setXid(xid);
        AbstractPointEventDetectorVO<?> dped = (AbstractPointEventDetectorVO<?>)importing;
        dpvo.getEventDetectors().add(dped);
	
		try {
			ctx.getReader().readInto(importing, json);
			
//			try {
//				if(Common.runtimeManager.getState() == RuntimeManager.RUNNING){
//            		Common.runtimeManager.saveDataPoint(dpvo);
//            		addSuccessMessage(isNew, "emport.eventDetector.prefix", xid);
//            	}else{
//            		addFailureMessage(new ProcessMessage("Runtime Manager not running point with xid: " + xid + " not saved."));
//            	}
//            } catch(LicenseViolatedException e) {
//            	addFailureMessage(new ProcessMessage(e.getErrorMessage()));
//			}
			
		}
		catch (TranslatableJsonException e) {
            addFailureMessage("emport.eventDetector.prefix", xid, e.getMsg());
        }
		catch (JsonException e) {
			addFailureMessage("emport.eventDetector.prefix", xid, getJsonExceptionMessage(e));
		}
	}
}
