package com.infiniteautomation.mango.emport;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.LicenseViolatedException;
import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.template.DataPointPropertiesTemplateVO;

public class DataPointImporter<DS extends DataSourceVO<DS>> extends Importer {
    
    final List<DataPointSummaryPathPair> hierarchyList;
    final String PATH = "path";
    
    private final DataPointService dataPointService;
    private final DataSourceService<DS> dataSourceService;
    
    public DataPointImporter(JsonObject json, 
            DataPointService dataPointService,
            DataSourceService<DS> dataSourceService,
            PermissionHolder user, 
            List<DataPointSummaryPathPair> hierarchyList) {
        super(json, user);
        this.dataPointService = dataPointService;
        this.dataSourceService = dataSourceService;
        this.hierarchyList = hierarchyList;
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        DataPointVO vo = null;
        DataSourceVO<?> dsvo = null;

        if (StringUtils.isBlank(xid)) {
            xid = dataPointService.getDao().generateUniqueXid();
        }else {
            try {
                vo = dataPointService.getFull(xid, user);
            }catch(NotFoundException e) {
                
            }
        }
        
        if (vo == null) {
            // Locate the data source for the point.
            String dsxid = json.getString("dataSourceXid");
            try {
                dsvo = dataSourceService.get(dsxid, user);
            }catch(NotFoundException e) {
                addFailureMessage("emport.dataPoint.badReference", xid);
                return;
            }
            vo = new DataPointVO();
            vo.setXid(xid);
            vo.setDataSourceId(dsvo.getId());
            vo.setDataSourceXid(dsxid);
            vo.setPointLocator(dsvo.createPointLocator());
            vo.setEventDetectors(new ArrayList<AbstractPointEventDetectorVO<?>>(0));
            //Not needed as it will be set via the template or JSON or it exists in the DB already: vo.setTextRenderer(new PlainRenderer());
        }
        
        if (vo != null) {
        	try {
            	DataPointPropertiesTemplateVO template = null;            	
            	if(json.containsKey("templateXid")){
                	String templateXid = json.getString("templateXid");
                	if(!StringUtils.isEmpty(templateXid))
                		template = (DataPointPropertiesTemplateVO) TemplateDao.getInstance().getByXid(templateXid);
                	
                }
            	//Read into the VO to get all properties
            	ctx.getReader().readInto(vo, json);
            	
            	//Override the settings if we need to
                if(template != null){
                	template.updateDataPointVO(vo);
                }

                // If the name is not provided, default to the XID
                if (StringUtils.isBlank(vo.getName()))
                    vo.setName(xid);

                // If the chart colour is null provide default of '' to handle legacy code that sets colour to null
                if(vo.getChartColour() == null)
                	vo.setChartColour("");
                
                // Now validate it. Use a new response object so we can distinguish errors in this vo from
                // other errors.
                ProcessResult voResponse = new ProcessResult();
                vo.validate(voResponse);
                if (voResponse.getHasMessages())
                    setValidationMessages(voResponse, "emport.dataPoint.prefix", xid);
                else {

                	//We will always override the DS Info with the one from the XID Lookup
                    try{
                        dsvo = dataSourceService.getFull(vo.getDataSourceXid(), user);
                        //Compare this point to the existing point in DB to ensure
                        // that we aren't moving a point to a different type of Data Source
                        // note that we don't need the join data
                        DataPointVO oldPoint = dataPointService.get(vo.getId(), user);
                        
                        //Does the old point have a different data source?
                        if(oldPoint != null&&(oldPoint.getDataSourceId() != dsvo.getId())){
                            vo.setDataSourceId(dsvo.getId());
                            vo.setDataSourceName(dsvo.getName());
                        }
                    }catch(NotFoundException e) {
                        addFailureMessage("emport.dataPoint.badReference", xid);
                        return;
                    }

                    boolean isNew = vo.isNew();
                    try {
                    	if(Common.runtimeManager.getState() == RuntimeManager.RUNNING) {
                    	    if(isNew) {
                    	        dataPointService.insertFull(vo, user);
                    	    }else {
                    	        dataPointService.updateFull(vo.getId(), vo, user);
                    	    }
                    		if(hierarchyList != null && json.containsKey(PATH)) {
                    		    String path = json.getString(PATH);
                    		    if(StringUtils.isNotEmpty(path))
                    		        hierarchyList.add(new DataPointSummaryPathPair(new DataPointSummary(vo), path));
                    		}
                    		addSuccessMessage(isNew, "emport.dataPoint.prefix", xid);
                    	}else{
                    		addFailureMessage("emport.dataPoint.runtimeManagerNotRunning", xid);
                    	}
                    } catch(LicenseViolatedException e) {
                    	addFailureMessage(new ProcessMessage(e.getErrorMessage()));
                    }
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
