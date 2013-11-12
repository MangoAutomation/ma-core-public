/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.View;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.emport.AbstractSheetEmporter;
import com.serotonin.m2m2.emport.SpreadsheetEmporter;
import com.serotonin.m2m2.emport.SpreadsheetEmporter.FileType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataPoint.DataPointEmporter;
import com.serotonin.m2m2.web.mvc.UrlHandler;

/**
 * @author Terry Packer
 *
 */
public class FileDownloadController implements UrlHandler{

	private final Log LOG = LogFactory
            .getLog(FileDownloadController.class);
	
	
	public View handleRequest(HttpServletRequest request,
            HttpServletResponse response, Map<String, Object> model)
            throws Exception {
        User user = Common.getUser(request);
        prepareModel(request, response, model, user);
        return null;
    }

    protected void prepareModel(HttpServletRequest request,
            HttpServletResponse response, Map<String, Object> model,
            User user) {

        // Now check for download
        String downloadFileBoolString = request
                .getParameter("downloadFile");
        if (downloadFileBoolString != null) {
            boolean download = Boolean
                    .parseBoolean(downloadFileBoolString);
            if (download) {
                BufferedOutputStream bos = null;
                try {
                    response.setHeader("Content-Type",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    // response.setHeader("Content-Length",file.length());
                    // Jared note - Do we need the content-length?, the download will finish when the TCP connection closes
                    // if the content length is not set, if the connection is broken before the download
                    // finishes then the user will get a corrupt file
                    
                    // TODO add the file type on the URL to determine
                    // how to encode the file, also change Content-Type header appropriately
                    // TODO add option for exporting monitor/machine names or XIDs
                    SpreadsheetEmporter emporter = new SpreadsheetEmporter(FileType.XLSX);
                    AbstractSheetEmporter[] emporters;
                   
                    //Collect the data type to export
                    String dataType = request.getParameter("dataType");
                    
                    //Switch on the type and prepare for export
                    if(dataType.equals("dataPoint")){
                    	emporters = this.prepareDataPoint(request, response, model, user);
                    }else{
                    	//Not available for download
                    	LOG.error("Usupported type for downloading");
                    	return;
                    }
                    
                    bos = new BufferedOutputStream(response.getOutputStream());
                    emporter.doExport(bos,emporters);
                } catch (IOException e) {
                    // TODO Return errors
                    LOG.error(e);
                } finally {
                    if (bos != null)
                        try {
                            bos.close();
                        } catch (IOException ignore) {
                        }
                }
            }
        }
        // Download via DWR.download method

    }
    
    
    /**
     * Prepare to output Data P
     * @param request
     * @param response
     * @param model
     * @param user
     * @return
     */
    private AbstractSheetEmporter[] prepareDataPoint(HttpServletRequest request,
            HttpServletResponse response, Map<String, Object> model,
            User user){
        response.setHeader("Content-Disposition",
                "inline; filename=\"dataPoints.xlsx\"");
        
        //Get the data point parameters
        Integer dataSourceId;
        try{
        	dataSourceId = Integer.parseInt(request.getParameter("dsId"));
        }
        catch (NumberFormatException e) {
        	dataSourceId = null;
        }
        
        AbstractSheetEmporter[] emporters = new AbstractSheetEmporter[1];
    	emporters[0] = new DataPointEmporter(dataSourceId,true);
    	
    	return emporters;
    	
    }
    
    
    
	
}
