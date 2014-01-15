package com.serotonin.m2m2.web.servlet;

import java.io.BufferedOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueIdTime;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueEmporter;
import com.serotonin.m2m2.rt.dataImage.PointValueIdTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.emport.SpreadsheetEmporter;
import com.serotonin.m2m2.vo.emport.SpreadsheetEmporter.FileType;
import com.serotonin.m2m2.vo.export.ExportCsvStreamer;
import com.serotonin.m2m2.vo.export.ExportDataValue;
import com.serotonin.m2m2.vo.export.ExportPointInfo;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.dwr.beans.DataExportDefinition;

public class ChartExportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = Common.getUser(request);
        if (user == null)
            return;

        DataExportDefinition def = user.getDataExportDefinition();
        if (def == null)
            return;
        
        long from = def.getFrom() == null ? -1 : def.getFrom().getMillis();
        long to = def.getTo() == null ? System.currentTimeMillis() : def.getTo().getMillis();

        //Eventually Switch on file type
        String pathInfo = request.getPathInfo();
        if(pathInfo != null){
	        if(request.getPathInfo().endsWith(".csv"))
	        	this.exportCsv(response, from, to, def, user);
	        else if(pathInfo.endsWith(".xlsx"))
	        	this.exportExcel(response, from, to, def, user);
	        else
	        	this.exportCsv(response, from, to, def, user); //For general error catching
        }
    }

    /**
     * Do the export as a CSV File
     * @param response
     * @param from
     * @param to
     * @param def
     * @param user
     * @throws IOException
     */
    private void exportCsv(HttpServletResponse response,long from, long to, DataExportDefinition def, User user) throws IOException{
        
        DataPointDao dataPointDao = new DataPointDao();
        PointValueDao pointValueDao = Common.databaseProxy.newPointValueDao();

    	// Stream the content.
        response.setContentType("text/csv");

        final Translations translations = Common.getTranslations();
        final ExportCsvStreamer exportCreator = new ExportCsvStreamer(response.getWriter(), translations);

        final ExportDataValue edv = new ExportDataValue();
        MappedRowCallback<PointValueTime> callback = new MappedRowCallback<PointValueTime>() {
            @Override
            public void row(PointValueTime pvt, int rowIndex) {
                edv.setValue(pvt.getValue());
                edv.setTime(pvt.getTime());
                if (pvt instanceof AnnotatedPointValueTime)
                    edv.setAnnotation(((AnnotatedPointValueTime) pvt).getSourceMessage());
                else
                    edv.setAnnotation(null);
                exportCreator.pointData(edv);
            }
        };

        for (int pointId : def.getPointIds()) {
            DataPointVO dp = dataPointDao.getDataPoint(pointId);
            if (Permissions.hasDataPointReadPermission(user, dp)) {
                ExportPointInfo pointInfo = new ExportPointInfo();
                pointInfo.setPointName(dp.getName());
                pointInfo.setDeviceName(dp.getDeviceName());
                pointInfo.setTextRenderer(dp.getTextRenderer());
                exportCreator.startPoint(pointInfo);

                pointValueDao.getPointValuesBetween(pointId, from, to, callback);
            }
        }

        exportCreator.done();
    }
    
    /**
     * Do the export as Excel XLSX File
     * @param response
     * @param from
     * @param to
     * @param def
     * @param user
     * @throws IOException
     */
    private void exportExcel(HttpServletResponse response,long from, long to, DataExportDefinition def, User user) throws IOException{
        
        DataPointDao dataPointDao = new DataPointDao();
        PointValueDao pointValueDao = Common.databaseProxy.newPointValueDao();

    	// Stream the content.
        //response.setContentType("text/csv");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        
//        final Translations translations = Common.getTranslations();
//        final ExportCsvStreamer exportCreator = new ExportCsvStreamer(response.getWriter(), translations);

        final PointValueEmporter sheetEmporter = new PointValueEmporter();
        final SpreadsheetEmporter emporter = new SpreadsheetEmporter(FileType.XLSX);

        BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
        emporter.prepareExport(bos);
        emporter.prepareSheetExport(sheetEmporter);

        
        
        final ExportDataValue edv = new ExportDataValue();
        MappedRowCallback<PointValueIdTime> callback = new MappedRowCallback<PointValueIdTime>() {
            @Override
            public void row(PointValueIdTime pvt, int rowIndex) {
            	edv.setPointValueId(pvt.getPointValueId());
                edv.setValue(pvt.getValue());
                edv.setTime(pvt.getTime());
                if (pvt instanceof AnnotatedPointValueIdTime)
                    edv.setAnnotation(((AnnotatedPointValueIdTime) pvt).getSourceMessage());
                else
                    edv.setAnnotation(null);
                sheetEmporter.exportRow(edv);
            }
        };

        for (int pointId : def.getPointIds()) {
            DataPointVO dp = dataPointDao.getDataPoint(pointId);
            if (Permissions.hasDataPointReadPermission(user, dp)) {
                ExportPointInfo pointInfo = new ExportPointInfo();
                pointInfo.setXid(dp.getXid());
                pointInfo.setPointName(dp.getName());
                pointInfo.setDeviceName(dp.getDeviceName());
                pointInfo.setTextRenderer(dp.getTextRenderer());
                sheetEmporter.setPointInfo(pointInfo);

                pointValueDao.getPointValuesWithIdsBetween(pointId, from, to, callback);
            }
        }
       emporter.finishExport();
    }

}
    
    
    
    
    
    
