package com.serotonin.m2m2.web.dwr.emport.importers;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.LicenseViolatedException;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.RuntimeManagerImpl;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.web.dwr.emport.Importer;

public class EventDetectorImporter extends Importer {

	public EventDetectorImporter(JsonObject json) {
		super(json);
	}

	@Override
	protected void importImpl() {
		String dataPointXid = json.getString("dataPointXid");
		DataPointVO dpvo;
		if(StringUtils.isEmpty(dataPointXid) || (dpvo = DataPointDao.instance.getByXid(dataPointXid)) == null) {
			addFailureMessage("emport.error.missingPoint");
			return;
		}
		String xid = json.getString("xid");
		boolean isNew = true;
		DataPointDao.instance.setEventDetectors(dpvo);
		AbstractEventDetectorVO<?> importing = null;
		for(AbstractEventDetectorVO<?> ed : dpvo.getEventDetectors())
			if(ed.getXid().equals(xid)) {
				importing = ed;
				isNew = false;
				break;
			}
		
		if (importing == null) {
        	String typeStr = json.getString("type");
        	if(typeStr == null)
        		addFailureMessage("emport.error.ped.missingAttr", "type");
            EventDetectorDefinition<?> def = ModuleRegistry.getEventDetectorDefinition(typeStr);
            if (def == null)
                addFailureMessage("emport.error.ped.invalid", "type", typeStr,
                        ModuleRegistry.getEventDetectorDefinitionTypes());
            else {
                importing = def.baseCreateEventDetectorVO();
                importing.setDefinition(def);
            }

            // Create a new one
            importing.setId(Common.NEW_ID);
            importing.setXid(xid);
            AbstractPointEventDetectorVO<?> dped = (AbstractPointEventDetectorVO<?>)importing;
            dped.njbSetDataPoint(dpvo);
            dpvo.getEventDetectors().add(dped);
        }
		
		try {
			ctx.getReader().readInto(importing, json);
			
			try {
				if(Common.runtimeManager.getState() == RuntimeManagerImpl.RUNNING){
            		Common.runtimeManager.saveDataPoint(dpvo);
            		addSuccessMessage(isNew, "emport.eventDetector.prefix", xid);
            	}else{
            		addFailureMessage(new ProcessMessage("Runtime Manager not running point with xid: " + xid + " not saved."));
            	}
            } catch(LicenseViolatedException e) {
            	addFailureMessage(new ProcessMessage(e.getErrorMessage()));
			}
			
		}
		catch (TranslatableJsonException e) {
            addFailureMessage("emport.eventDetector.prefix", xid, e.getMsg());
        }
		catch (JsonException e) {
			addFailureMessage("emport.eventDetector.prefix", xid, getJsonExceptionMessage(e));
		}
	}
}
