/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr;

import java.util.List;
import java.util.Map;

import com.infiniteautomation.mango.db.query.SortOption;
import com.infiniteautomation.mango.spring.dao.EventInstanceDao;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DojoQueryCallback;
import com.serotonin.m2m2.db.dao.ResultSetCounter;
import com.serotonin.m2m2.db.dao.ResultsWithTotal;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.AuditEventTypeDefinition;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.SystemEventTypeDefinition;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.EventInstanceVO;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

/**
 * @author Terry Packer
 *
 */
public class EventInstanceDwr extends AbstractDwr<EventInstanceVO, EventInstanceDao>{

	/**
	 * @param dao
	 * @param keyName
	 */
	public EventInstanceDwr() {
		super(EventInstanceDao.instance, "eventInstances");
	}
	
    /**
     * Load a list of VOs
     * @return
     */
    @Override
    @DwrPermission(user = true)
    public ProcessResult dojoQuery(Map<String, String> query, List<SortOption> sort, Integer start, Integer count, boolean or) {
        ProcessResult response = new ProcessResult();
        
        //Set the Export Query (HACK, but will work for now for exporting)
        this.setExportQuery(query, sort, or);
        
        //TODO Use the Event Manager to access Current Events since the DO NOT LOG events are only in memory
        query.put("userId", Common.getHttpUser().getId()+"");
        
        ResultsWithTotal results = dao.dojoQuery(query, sort, start, count, or);
        response.addData("list", results.getResults());
        response.addData("total", results.getTotal());
        
        return response;
    }
	
	
	
	
	@DwrPermission(user = true)
	public void setExportQuery(Map<String, String> query, List<SortOption> sort, boolean or){

		//Put the export query info into the user attributes and then
		// on return make a call to the export servlet
		QueryDefinition reportQuery = new QueryDefinition(query,sort,or);
		Common.getHttpUser().setAttribute("eventInstanceExportDefinition",reportQuery);
	}
	

	// Utility Methods for help with rendering some strings
	/**
	 * 
	 * @param divId - Id of link to place div on return
	 * @param subtype
	 * @param ref1
	 * @param ref2
	 * @return
	 */
	@DwrPermission(user = true)
    public static ProcessResult getSystemEventTypeLink(String divId, String subtype, int ref1, int ref2) {
		ProcessResult result = new ProcessResult();
		result.addData("divId", divId);
		SystemEventTypeDefinition def = ModuleRegistry.getSystemEventTypeDefinition(subtype);
        if (def != null)
            result.addData("link", def.getEventListLink(ref1, ref2, Common.getTranslations()));
        return result;
    }
	
	/**
	 * 
	 * @param divId - Id of div to place link on return
	 * @param subtype
	 * @param ref1
	 * @param ref2
	 * @return
	 */
	@DwrPermission(user = true)
    public static ProcessResult getAuditEventTypeLink(String divId, String subtype, int ref1, int ref2) {
		ProcessResult result = new ProcessResult();
		result.addData("divId", divId);
		AuditEventTypeDefinition def = ModuleRegistry.getAuditEventTypeDefinition(subtype);
        if (def != null)
            result.addData("link", def.getEventListLink(ref1, ref2, Common.getTranslations()));
        return result;
    }
	
	/**
	 * 
	 * @param divId - Id of div to place the link
	 * @param type
	 * @param subtype
	 * @param ref1
	 * @param ref2
	 * @return
	 */
	@DwrPermission(user = true)
    public static ProcessResult getEventTypeLink(String divId, String type, String subtype, int ref1, int ref2) {
		ProcessResult result = new ProcessResult();
		result.addData("divId", divId);
		EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(type);
        if (def != null)
            result.addData("link", def.getEventListLink(subtype, ref1, ref2, Common.getTranslations()));
        return result;
    }

    /**
     * Acknowledge all events from the current User Event Query
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult acknowledgeEvents() {
        ProcessResult response = new ProcessResult();
        
        final User user = Common.getHttpUser();
        if (user != null) {
        	final long now = Common.timer.currentTimeMillis();
        	final ResultSetCounter counter = new ResultSetCounter();
        	QueryDefinition queryData = (QueryDefinition) user.getAttribute("eventInstanceExportDefinition");
            DojoQueryCallback<EventInstanceVO> callback = new DojoQueryCallback<EventInstanceVO>(false) {
            	
            	@Override
                public void row(EventInstanceVO vo, int rowIndex) {
            		if(!vo.isAcknowledged()){
            			EventInstance event = Common.eventManager.acknowledgeEventById(vo.getId(), now, user, null);
            			if (event != null && event.isAcknowledged()) {
            			    counter.increment();
            			}
            		}
            		
                }
            };
            
            EventInstanceDao.instance.exportQuery(queryData.getQuery(), queryData.getSort(), null, null, queryData.isOr(),callback);
	
            resetLastAlarmLevelChange();
            response.addGenericMessage("events.acknowledgedEvents", counter.getCount());

	    }else{
	    	response.addGenericMessage("events.acknowledgedEvents", 0);
	    }

        
        return response;
    }
    /**
     * Silence all events from the current User Event Query
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult silenceEvents() {
        ProcessResult response = new ProcessResult();
        
        final User user = Common.getHttpUser();
        if (user != null) {        
        	final ResultSetCounter counter = new ResultSetCounter();
        	QueryDefinition queryData = (QueryDefinition) user.getAttribute("eventInstanceExportDefinition");
            DojoQueryCallback<EventInstanceVO> callback = new DojoQueryCallback<EventInstanceVO>(false) {
            	
            	@Override
                public void row(EventInstanceVO vo, int rowIndex) {
            		if(!vo.isSilenced()){
            			//If not silenced then do it.
	            		Common.eventManager.toggleSilence(vo.getId(), user.getId());
	            		counter.increment();
            		}
            		
                }
            };
            
            EventInstanceDao.instance.exportQuery(queryData.getQuery(), queryData.getSort(), null, null, queryData.isOr(),callback);
	
            resetLastAlarmLevelChange();
            response.addGenericMessage("events.silencedEvents", counter.getCount());

	    }else{
	    	response.addGenericMessage("events.silencedEvents", 0);
	    }

        
        return response;
    }
}
