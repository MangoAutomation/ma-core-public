package com.serotonin.m2m2.web.servlet;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueEmporter;
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
        long to = def.getTo() == null ? Common.timer.currentTimeMillis() : def.getTo().getMillis();

        //Add in the necessary headers
        long currentTime = Common.timer.currentTimeMillis();
        response.setDateHeader("Expires", currentTime);
        response.setDateHeader("Last-Modified", currentTime);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Cache-Control", "post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        
        //Detect file type
        //Eventually Switch on file type
        String pathInfo = request.getPathInfo();
        if(pathInfo != null){
        	 String [] requestFilenameParts =  request.getPathInfo().split("\\.");
	        String suffix = requestFilenameParts[1];
	        
	        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
	        String currentTimeString = sdf.format(new Date(currentTime));
	        
	        //Create and set the filename
	        String filename = requestFilenameParts[0].replace("/", "");
	        filename = filename + "-" + currentTimeString + "." + suffix;
	        response.setHeader("Content-Disposition","attachment;filename=\"" + filename + "\"");
        

	        if(request.getPathInfo().endsWith(".csv"))
	        	this.exportCsv(request, response, from, to, def, user);
	        else if(pathInfo.endsWith(".xlsx"))
	        	this.exportExcel(response, from, to, def, user);
	        else
	        	this.exportCsv(request, response, from, to, def, user); //For general error catching
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
    private void exportCsv(HttpServletRequest request, HttpServletResponse response,long from, long to, DataExportDefinition def, User user) throws IOException{
        
        DataPointDao dataPointDao = DataPointDao.instance;
        PointValueDao pointValueDao = Common.databaseProxy.newPointValueDao();

    	// Stream the content.
        response.setContentType("text/csv");

        final Translations translations = Common.getTranslations();
        final ExportCsvStreamer exportCreator = new ExportCsvStreamer(request.getServerName(), request.getLocalPort(), response.getWriter(), translations);

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
            DataPointVO dp = dataPointDao.getDataPoint(pointId, false);
            if (Permissions.hasDataPointReadPermission(user, dp)) {
                ExportPointInfo pointInfo = new ExportPointInfo();
                pointInfo.setXid(dp.getXid());
                pointInfo.setPointName(dp.getName());
                pointInfo.setDeviceName(dp.getDeviceName());
                pointInfo.setTextRenderer(dp.getTextRenderer());
                pointInfo.setDataPointId(pointId);
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
        
        DataPointDao dataPointDao = DataPointDao.instance;
        PointValueDao pointValueDao = Common.databaseProxy.newPointValueDao();

    	// Stream the content.
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        
        final List<PointValueEmporter> sheetEmporters = new ArrayList<PointValueEmporter>();
        final AtomicInteger sheetIndex = new AtomicInteger();
        sheetEmporters.add(new PointValueEmporter(Common.translate("emport.pointValues") + " " + sheetIndex.get()));
        final SpreadsheetEmporter emporter = new SpreadsheetEmporter(FileType.XLSX);

        BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
        emporter.prepareExport(bos);
        emporter.prepareSheetExport(sheetEmporters.get(0));
        
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
                sheetEmporters.get(sheetIndex.get()).exportRow(edv);
                
                if(sheetEmporters.get(sheetIndex.get()).getRowsAdded() >= emporter.getMaxRowsPerSheet()){
                	
                	ExportPointInfo info = sheetEmporters.get(sheetIndex.get()).getPointInfo();
                	sheetIndex.incrementAndGet();
                	PointValueEmporter sheetEmporter = new PointValueEmporter(Common.translate("emport.pointValues") + " " + sheetIndex.get());
                	sheetEmporter.setPointInfo(info);
                	sheetEmporters.add(sheetEmporter);
                	emporter.prepareSheetExport(sheetEmporters.get(sheetIndex.get()));
                }
                
            }
        };

        for (int pointId : def.getPointIds()) {
            DataPointVO dp = dataPointDao.getDataPoint(pointId, false);
            if (Permissions.hasDataPointReadPermission(user, dp)) {
                ExportPointInfo pointInfo = new ExportPointInfo();
                pointInfo.setXid(dp.getXid());
                pointInfo.setPointName(dp.getName());
                pointInfo.setDeviceName(dp.getDeviceName());
                pointInfo.setTextRenderer(dp.getTextRenderer());
                sheetEmporters.get(sheetIndex.get()).setPointInfo(pointInfo);

                pointValueDao.getPointValuesBetween(pointId, from, to, callback);
            }
        }
       emporter.finishExport();
    }

}
    
    
    
    
    
    
