/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.StringStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.dataSource.DataSourceRTM;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.web.comparators.StringStringPairComparator;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

/**
 * 
 * DWR For Data Source Manipulation
 * 
 * @author Terry Packer
 *
 */
public class DataSourceDwr extends AbstractRTDwr<DataSourceVO<?>, DataSourceDao,DataSourceRT,DataSourceRTM>{

	/**
	 * Default Constructor
	 */
	public DataSourceDwr(){
		super(DataSourceDao.instance,
				"dataSources",
				DataSourceRTM.instance,
				"dataSources");
		LOG = LogFactory.getLog(DataSourceListDwr.class);
	}
	

	/**
	 * Init Data Source Types
	 * @return
	 */
	@DwrPermission(user = true)
    public ProcessResult initDataSourceTypes() {
        ProcessResult response = new ProcessResult();

        User user = Common.getUser();

        if (user.isAdmin()) {

            List<StringStringPair> translatedTypes = new ArrayList<StringStringPair>();
            for (String type : ModuleRegistry.getDataSourceDefinitionTypes()){
            	translatedTypes.add(new StringStringPair(type, translate(ModuleRegistry.getDataSourceDefinition(type)
                        .getDescriptionKey())));
            }
            StringStringPairComparator.sort(translatedTypes);
            response.addData("types", translatedTypes);
           
        }
        
        return response;
	}

	

	@DwrPermission(user = true)
    public ProcessResult getNew(String type) {
	 	ProcessResult response = new ProcessResult();
		DataSourceVO<?> vo = null;
		DataSourceDefinition def = ModuleRegistry.getDataSourceDefinition(type);
        if (def == null){
        	//TODO Add message to response about unknown type or invalid type
        }
        try{	
	        vo = def.baseCreateDataSourceVO();
	        vo.setId(Common.NEW_ID);
	        vo.setXid(new DataSourceDao().generateUniqueXid());
	         
	        response.addData("vo", vo);
	        
	        //Setup the page info
	        response.addData("editPagePath",def.getModule().getWebPath() + "/" + def.getEditPagePath());
	        response.addData("statusPagePath",def.getModule().getWebPath() + "/" + def.getStatusPagePath());
        }catch(Exception e){
        	LOG.error(e.getMessage());
        	response.addMessage(new TranslatableMessage("table.error.dwr",e.getMessage()));
        }
        return response;
    }

	@DwrPermission(user = true)
	@Override
    public ProcessResult get(int id) {
		ProcessResult response;
		try{
			if(id > 0)
				response = super.get(id);
			else{
				throw new ShouldNeverHappenException("Unable to get a new DataSource.");
			}
	        //Setup the page info
	        response.addData("editPagePath",((DataSourceVO<?>) response.getData().get("vo")).getDefinition().getModule().getWebPath() + "/" + ((DataSourceVO<?>) response.getData().get("vo")).getDefinition().getEditPagePath());
	        response.addData("statusPagePath",((DataSourceVO<?>) response.getData().get("vo")).getDefinition().getModule().getWebPath() + "/" + ((DataSourceVO<?>) response.getData().get("vo")).getDefinition().getStatusPagePath());
        }catch(Exception e){
        	LOG.error(e.getMessage());
        	response = new ProcessResult();
        	response.addMessage(new TranslatableMessage("table.error.dwr",e.getMessage()));
        }
        return response;
    }
	

	/**
	 * Export Data Source and Points together
	 */
	@DwrPermission(user = true)
    @Override
    public String jsonExport(int id) {
    	
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        List<DataSourceVO<?>> dss = new ArrayList<DataSourceVO<?>>();
        dss.add(new DataSourceDao().getDataSource(id));
        data.put(EmportDwr.DATA_SOURCES, dss);
        data.put(EmportDwr.DATA_POINTS, new DataPointDao().getDataPoints(id, null));
        return EmportDwr.export(data, 3);
    }
	
}
