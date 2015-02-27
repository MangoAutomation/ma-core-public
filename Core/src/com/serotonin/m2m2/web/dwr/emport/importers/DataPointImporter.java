package com.serotonin.m2m2.web.dwr.emport.importers;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.view.text.PlainRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;
import com.serotonin.m2m2.vo.template.DataPointPropertiesTemplateVO;
import com.serotonin.m2m2.web.dwr.emport.Importer;

public class DataPointImporter extends Importer {
    public DataPointImporter(JsonObject json) {
        super(json);
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");

        if (StringUtils.isBlank(xid))
            xid = ctx.getDataPointDao().generateUniqueXid();

        DataSourceVO<?> dsvo;
        DataPointVO vo = ctx.getDataPointDao().getDataPoint(xid);
        if (vo == null) {
            // Locate the data source for the point.
            String dsxid = json.getString("dataSourceXid");
            dsvo = ctx.getDataSourceDao().getDataSource(dsxid);
            if (dsvo == null)
                addFailureMessage("emport.dataPoint.badReference", xid);
            else {
                vo = new DataPointVO();
                vo.setXid(xid);
                vo.setDataSourceId(dsvo.getId());
                vo.setDataSourceXid(dsxid);
                vo.setPointLocator(dsvo.createPointLocator());
                vo.setEventDetectors(new ArrayList<PointEventDetectorVO>(0));
                if(json.containsKey("templateXid")){
                	String templateXid = json.getString("templateXid");
                	DataPointPropertiesTemplateVO template = (DataPointPropertiesTemplateVO) TemplateDao.instance.getByXid(templateXid);
                	if(template != null){
                		template.updateDataPointVO(vo);
                	}else{
                		addFailureMessage("emport.dataPoint.badReference", templateXid);
                	}
                }else{
                    vo.setTextRenderer(new PlainRenderer());
                }
            }
        }
        
        if (vo != null) {
            try {
            	if(json.containsKey("templateXid")){
                	String templateXid = json.getString("templateXid");
                	DataPointPropertiesTemplateVO template = (DataPointPropertiesTemplateVO) TemplateDao.instance.getByXid(templateXid);
                	if(template != null){
                		template.updateDataPointVO(vo);
                	}else{
                		addFailureMessage("emport.dataPoint.badReference", templateXid);
                	}
                }else{
                    ctx.getReader().readInto(vo, json);
                }
                
                // If the name is not provided, default to the XID
                if (StringUtils.isBlank(vo.getName()))
                    vo.setName(xid);

                // Now validate it. Use a new response object so we can distinguish errors in this vo from
                // other errors.
                ProcessResult voResponse = new ProcessResult();
                vo.validate(voResponse);
                if (voResponse.getHasMessages())
                    setValidationMessages(voResponse, "emport.dataPoint.prefix", xid);
                else {

                	//We will always override the DS Info with the one from the XID Lookup
                    dsvo = ctx.getDataSourceDao().getDataSource(vo.getDataSourceXid());
                    if (dsvo == null)
	                      	addFailureMessage("emport.dataPoint.badReference", xid);
	                else {
	                    //TODO Compare this point to the existing point in DB to ensure
	                    // that we aren't moving a point to a different type of Data Source
	                    DataPointDao dpDao = new DataPointDao();
	                    DataPointVO oldPoint = dpDao.getDataPoint(vo.getId());
	                    
	                    //Does the old point have a different data source?
	                    if(oldPoint != null&&(oldPoint.getDataSourceId() != dsvo.getId())){
	                        vo.setDataSourceId(dsvo.getId());
	                        vo.setDataSourceName(dsvo.getName());
	                    }
	
	                    // Sweet. Save it.
	                	//Why would you save it 2x? DataPointDao.instance.saveFull(vo);
                    }

                    boolean isnew = vo.isNew();

                    //                        // Check if this data source is enabled. Because data sources do automatic stuff upon the
                    //                        // starting of a point, we need to shut it down. We restart again once all data points are
                    //                        // imported.
                    //                        if (dsvo.isEnabled() && !disabledDataSources.contains(dsvo.getId())) {
                    //                            disabledDataSources.add(dsvo.getId());
                    //                            dsvo.setEnabled(false);
                    //                            Common.runtimeManager.saveDataSource(dsvo);
                    //                        }
             
                    Common.runtimeManager.saveDataPoint(vo);
                    addSuccessMessage(isnew, "emport.dataPoint.prefix", xid);
                }
            }
            catch (TranslatableJsonException e) {
                addFailureMessage("emport.dataPoint.prefix", xid, e.getMsg());
            }
            catch (JsonException e) {
                addFailureMessage("emport.dataPoint.prefix", xid, getJsonExceptionMessage(e));
            }
        }
    }
}
