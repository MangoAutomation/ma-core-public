package com.serotonin.m2m2.web.servlet;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DojoQueryCallback;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.EventInstanceDao;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.emport.SpreadsheetEmporter;
import com.serotonin.m2m2.vo.emport.SpreadsheetEmporter.FileType;
import com.serotonin.m2m2.vo.event.EventInstanceEmporter;
import com.serotonin.m2m2.vo.event.EventInstanceVO;
import com.serotonin.m2m2.vo.export.EventCsvStreamer;
import com.serotonin.m2m2.web.dwr.QueryDefinition;
import com.serotonin.m2m2.web.dwr.beans.EventExportDefinition;

public class EventExportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = Common.getUser(request);
        if (user == null)
            return;

        //Eventually Switch on file type
        String pathInfo = request.getPathInfo();
        if(pathInfo != null){
	        if(pathInfo.endsWith(".xlsx"))
	        	this.exportExcel(response, user);
	        else{
	            EventExportDefinition def = user.getEventExportDefinition();
	            if (def == null)
	                return;
	        	this.exportCsv(response, def, user);
	        }
        }

    }

	/**
	 * @param response
	 * @param def
	 * @param user
	 * @throws IOException 
	 */
	private void exportExcel(HttpServletResponse response, User user) throws IOException {
		
    	// Stream the content.
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        
        final EventInstanceEmporter sheetEmporter = new EventInstanceEmporter();
        final SpreadsheetEmporter emporter = new SpreadsheetEmporter(FileType.XLSX);

        BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
        emporter.prepareExport(bos);
        emporter.prepareSheetExport(sheetEmporter);
        
        QueryDefinition queryData = (QueryDefinition) user.getAttribute("eventInstanceExportDefinition");
        DojoQueryCallback<EventInstanceVO> callback = new DojoQueryCallback<EventInstanceVO>(false) {
        	
        	@Override
            public void row(EventInstanceVO vo, int rowIndex) {
                sheetEmporter.exportRow(vo);
            }
        };
        
        EventInstanceDao.instance.exportQuery(queryData.getQuery(), queryData.getSort(), null, null, queryData.isOr(),callback);

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
