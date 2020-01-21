package com.infiniteautomation.mango.emport;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.LicenseViolatedException;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

public class DataPointImporter extends Importer {

    final String PATH = "path";

    private final DataPointService dataPointService;
    private final DataSourceService dataSourceService;

    public DataPointImporter(JsonObject json,
            DataPointService dataPointService,
            DataSourceService dataSourceService) {
        super(json);
        this.dataPointService = dataPointService;
        this.dataSourceService = dataSourceService;
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        DataPointVO vo = null;
        DataSourceVO dsvo = null;

        if (StringUtils.isBlank(xid)) {
            xid = dataPointService.getDao().generateUniqueXid();
        }else {
            try {
                vo = dataPointService.get(xid);
            }catch(NotFoundException e) {

            }
        }

        if (vo == null) {
            // Locate the data source for the point.
            String dsxid = json.getString("dataSourceXid");
            try {
                dsvo = dataSourceService.get(dsxid);
            }catch(NotFoundException e) {
                addFailureMessage("emport.dataPoint.badReference", xid);
                return;
            }
            vo = new DataPointVO();
            vo.setXid(xid);
            vo.setDataSourceId(dsvo.getId());
            vo.setDataSourceXid(dsxid);
            vo.setPointLocator(dsvo.createPointLocator());
            vo.setEventDetectors(new ArrayList<AbstractPointEventDetectorVO>(0));
            //Not needed as it will be set via the template or JSON or it exists in the DB already: vo.setTextRenderer(new PlainRenderer());
        }

        if (vo != null) {
            try {
                //Read into the VO to get all properties
                ctx.getReader().readInto(vo, json);

                // If the name is not provided, default to the XID
                if (StringUtils.isBlank(vo.getName()))
                    vo.setName(xid);

                // If the chart colour is null provide default of '' to handle legacy code that sets colour to null
                if(vo.getChartColour() == null)
                    vo.setChartColour("");

                boolean isNew = vo.isNew();
                try {
                    if(Common.runtimeManager.getState() == RuntimeManager.RUNNING) {
                        if(isNew) {
                            dataPointService.insert(vo);
                        }else {
                            dataPointService.update(vo.getId(), vo);
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
