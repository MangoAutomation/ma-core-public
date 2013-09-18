package com.serotonin.m2m2.web.servlet;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.EventInstanceDao;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueIdTime;
import com.serotonin.m2m2.rt.dataImage.PointValueIdTime;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.emport.SpreadsheetEmporter;
import com.serotonin.m2m2.vo.emport.SpreadsheetEmporter.FileType;
import com.serotonin.m2m2.vo.event.EventInstanceEmporter;
import com.serotonin.m2m2.vo.export.EventCsvStreamer;
import com.serotonin.m2m2.vo.export.ExportDataValue;
import com.serotonin.m2m2.vo.export.ExportPointInfo;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.dwr.beans.EventExportDefinition;

public class EventExportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = Common.getUser(request);
        if (user == null)
            return;

        EventExportDefinition def = user.getEventExportDefinition();
        if (def == null)
            return;

        //Eventually Switch on file type
        String pathInfo = request.getPathInfo();
        if(pathInfo != null){
	        if(request.getPathInfo().endsWith(".csv"))
	        	this.exportCsv(response, def, user);
	        else if(pathInfo.endsWith(".xlsx"))
	        	this.exportExcel(response, def, user);
	        else
	        	this.exportCsv(response, def, user); //For general error catching
        }

    }

	/**
	 * @param response
	 * @param def
	 * @param user
	 * @throws IOException 
	 */
	private void exportExcel(HttpServletResponse response,
			EventExportDefinition def, User user) throws IOException {
		
    	// Stream the content.
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        
        final EventInstanceEmporter sheetEmporter = new EventInstanceEmporter();
        final SpreadsheetEmporter emporter = new SpreadsheetEmporter(FileType.XLSX);

        BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
        emporter.prepareExport(bos);
        emporter.prepareSheetExport(sheetEmporter);

        
//        EventInstanceDao.instance.exportQuery(query, sort, null, null, true,callback);
//        
//        final ExportDataValue edv = new ExportDataValue();
//        MappedRowCallback<PointValueIdTime> callback = new MappedRowCallback<PointValueIdTime>() {
//            @Override
//            public void row(PointValueIdTime pvt, int rowIndex) {
//            	edv.setPointValueId(pvt.getPointValueId());
//                edv.setValue(pvt.getValue());
//                edv.setTime(pvt.getTime());
//                if (pvt instanceof AnnotatedPointValueIdTime)
//                    edv.setAnnotation(((AnnotatedPointValueIdTime) pvt).getSourceMessage());
//                else
//                    edv.setAnnotation(null);
//                sheetEmporter.exportRow(edv);
//            }
//        };
//
//        for (int pointId : def.getPointIds()) {
//            DataPointVO dp = dataPointDao.getDataPoint(pointId);
//            if (Permissions.hasDataPointReadPermission(user, dp)) {
//                ExportPointInfo pointInfo = new ExportPointInfo();
//                pointInfo.setXid(dp.getXid());
//                pointInfo.setPointName(dp.getName());
//                pointInfo.setDeviceName(dp.getDeviceName());
//                pointInfo.setTextRenderer(dp.getTextRenderer());
//                sheetEmporter.setPointInfo(pointInfo);
//
//                pointValueDao.getPointValuesWithIdsBetween(pointId, from, to, callback);
//            }
//        }
       emporter.finishExport();
		
		
		
		
	}

	/**
	 * @param response
	 * @param def
	 * @param user
	 * @throws IOException 
	 */
	private void exportCsv(HttpServletResponse response,
			EventExportDefinition def, User user) throws IOException {
        
        final Translations translations = Common.getTranslations();
        List<EventInstance> events = new EventDao().search(def.getEventId(), def.getEventType(), def.getStatus(),
                def.getAlarmLevel(), def.getKeywords(), def.getDateFrom(), def.getDateTo(), user.getId(), translations,
                0, Integer.MAX_VALUE, null);
        // Stream the content.
        response.setContentType("text/csv");
        new EventCsvStreamer(response.getWriter(), events, translations);

		
	}
    
    
    
}
