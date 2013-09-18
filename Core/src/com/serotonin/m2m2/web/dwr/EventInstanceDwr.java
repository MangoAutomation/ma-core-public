/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr;

import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventInstanceDao;
import com.serotonin.m2m2.db.dao.ResultsWithTotal;
import com.serotonin.m2m2.db.dao.SortOption;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.AuditEventTypeDefinition;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.SystemEventTypeDefinition;
import com.serotonin.m2m2.vo.event.EventInstanceVO;
import com.serotonin.m2m2.web.dwr.beans.EventExportDefinition;
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
		Common.getUser().setAttribute("eventInstanceExportDefinition",reportQuery);
	}
	

	// Utility Methods for help with rendering some strings
	@DwrPermission(user = true)
    public static String getSystemEventTypeLink(String subtype, int ref1, int ref2) {
        SystemEventTypeDefinition def = ModuleRegistry.getSystemEventTypeDefinition(subtype);
        if (def != null)
            return def.getEventListLink(ref1, ref2, Common.getTranslations());
        return null;
    }
	
	@DwrPermission(user = true)
    public static String getAuditEventTypeLink(String subtype, int ref1, int ref2) {
        AuditEventTypeDefinition def = ModuleRegistry.getAuditEventTypeDefinition(subtype);
        if (def != null)
            return def.getEventListLink(ref1, ref2, Common.getTranslations());
        return null;
    }
	
	@DwrPermission(user = true)
    public static String getEventTypeLink(String type, String subtype, int ref1, int ref2) {
        EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(type);
        if (def != null)
            return def.getEventListLink(subtype, ref1, ref2, Common.getTranslations());
        return null;
    }

	
	
}
