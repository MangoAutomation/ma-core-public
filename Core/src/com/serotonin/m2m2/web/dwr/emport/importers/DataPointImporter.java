package com.serotonin.m2m2.web.dwr.emport.importers;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.license.DataSourceTypePointsLimit;
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
        DataPointVO vo = null;
        DataSourceVO<?> dsvo = null;

        if (StringUtils.isBlank(xid))
            xid = ctx.getDataPointDao().generateUniqueXid();
        else
        	vo = ctx.getDataPointDao().getDataPoint(xid);
        
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
                //Not needed as it will be set via the template or JSON or it exists in the DB already: vo.setTextRenderer(new PlainRenderer());
                ProcessResult response = new ProcessResult();
                DataSourceTypePointsLimit.checkLimit(vo.getDataSourceTypeName(), response);
                if(response.getHasMessages()){
                	addFailureMessage(response.getMessages().get(0));
                	return; //Don't allow adding more than license points
                }
            }
        }
        
        if (vo != null) {
        	try {
            	DataPointPropertiesTemplateVO template = null;            	
            	if(json.containsKey("templateXid")){
                	String templateXid = json.getString("templateXid");
                	if(!StringUtils.isEmpty(templateXid))
                		template = (DataPointPropertiesTemplateVO) TemplateDao.instance.getByXid(templateXid);
                	
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
	                    //Compare this point to the existing point in DB to ensure
	                    // that we aren't moving a point to a different type of Data Source
	                
	                    DataPointVO oldPoint = ctx.getDataPointDao().getDataPoint(vo.getId());
	                    
	                    //Does the old point have a different data source?
	                    if(oldPoint != null&&(oldPoint.getDataSourceId() != dsvo.getId())){
	                        vo.setDataSourceId(dsvo.getId());
	                        vo.setDataSourceName(dsvo.getName());
	                    }
                    }

                    boolean isnew = vo.isNew();
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
