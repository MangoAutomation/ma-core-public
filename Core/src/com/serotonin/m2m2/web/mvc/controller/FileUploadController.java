/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.servlet.View;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.emport.SpreadsheetEmporter;
import com.serotonin.m2m2.vo.emport.SpreadsheetEmporter.FileType;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.dataImage.PointValueEmporter;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataPoint.DataPointEmporter;
import com.serotonin.m2m2.web.mvc.UrlHandler;

/**
 * @author Terry Packer
 *
 */
public class FileUploadController implements UrlHandler {
    public View handleRequest(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model)
            throws Exception {
        @SuppressWarnings("unused")
        User user = Common.getUser(request);

        prepareResponse(request,response,model);

        return null;
    }

    /**
     * @param request
     * @param response
     * @param model
     * @throws IOException 
     * @throws JsonException 
     * @throws FileUploadException 
     */
    private void prepareResponse(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model)
            throws IOException, JsonException, FileUploadException {
        String uploadType = "";
       
        @SuppressWarnings("unchecked")
        List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
        List<Object> fileInfo = new ArrayList<Object>();
        Map<String, Object> info = new HashMap<String, Object>();
        for (FileItem item : items) {
            if (item.isFormField()) {
                // Process regular form field (input type="text|radio|checkbox|etc", select, etc).
                String fieldname = item.getFieldName();
                String fieldvalue = item.getString();

                if (fieldname.equals("uploadType")) {
                    model.put("uploadType", fieldvalue);
                    uploadType = fieldvalue;
                }else{
                	info.put(fieldname, fieldvalue);
                }
            }
        }
        for (FileItem item : items) {    
                
        	if (!item.isFormField()) {
                // Process form file field (input type="file").
              
                
                String filename = FilenameUtils.getName(item.getName());
                info.put("filename", filename);
                
                Translations translations = ControllerUtils.getTranslations(request);
                InputStream filecontent = item.getInputStream();
                
                parseFile(filecontent, info, translations,request);
                
                fileInfo.add(info);
            }
            
        }
        model.put("fileInfo", fileInfo);
        
        boolean iframe = false;
        boolean html5 = false;
        boolean flash = false;
        
        if (uploadType.equals("iframe")) {
            iframe = true;
            response.setContentType("text/html");
        }
        else if (uploadType.equals("html5")) {
            html5 = true;
            response.setContentType("application/json");
        }
        else if (uploadType.equals("flash")) {
            flash = true;
            response.setContentType("text/plain");
        }
        
        OutputStream output = response.getOutputStream();
        if (iframe || html5) {
            writeJson(output, model, iframe);
        }
        else if (flash) {
            // TODO handle Flash
            throw new ShouldNeverHappenException("Flash upload not supported.");
        }
        else {
            throw new ShouldNeverHappenException("Invalid file upload type.");
        }
        
        output.close();
    }
    
    private void writeJson(OutputStream output, Map<String, Object> json, boolean iframe) throws IOException, JsonException {
        OutputStreamWriter osWriter = new OutputStreamWriter(output);
        
        if (iframe) {
            osWriter.write("<textarea>");
        }
        
        JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, osWriter);
        writer.writeObject(json);
        
        if (iframe) {
            osWriter.write("</textarea>");
        }
        
        osWriter.flush();
    }
    
    /**
     * Parse the Import Files
     * @param input
     * @param model
     * @param translations
     */
    private void parseFile(InputStream input, Map<String, Object> model, Translations translations,HttpServletRequest request) {
    	
    	//Get the filename
    	String filename = (String)model.get("filename");
        SpreadsheetEmporter emporter;
    	if(filename == null)
    		return;
    	else{
    		if(filename.endsWith(".xls"))
    			emporter = new SpreadsheetEmporter(FileType.XLS);
    		else if(filename.endsWith(".xlsx"))
    			emporter = new SpreadsheetEmporter(FileType.XLSX);
    		else
    			return;
    	}
         
        
        //Switch on the type
        String dataType = (String) model.get("dataType");

        if(dataType != null){
	        if(dataType.equals("dataPoint")){
//	            Integer dataSourceId;
//	            try{
//	            	dataSourceId = Integer.parseInt((String)model.get("dsId"));
//	            }
//	            catch (NumberFormatException e) {
//	            	dataSourceId = null;
//	            }
//	        	emporter.doImport(input, new DataPointEmporter(dataSourceId,true));
	        }else if(dataType.equals("pointValue")){

	        	//Get the file
	        	emporter.doImport(input, new PointValueEmporter());
	        	
	        	
	        }
    	}

        model.put("hasImportErrors", emporter.hasErrors());
        //Get the messages
        if (emporter.hasErrors()) {
            List<String> errorMessages = new ArrayList<String>();
            
            for(TranslatableMessage msg : emporter.getErrorMessages()) {
                errorMessages.add(msg.translate(translations));
            }
            
            model.put("errorMessages", errorMessages);
        }

        model.put("rowsImported", emporter.getRowsProcessed());
        model.put("rowsWithErrors", emporter.getRowErrors());
    }
	
	
	
}
