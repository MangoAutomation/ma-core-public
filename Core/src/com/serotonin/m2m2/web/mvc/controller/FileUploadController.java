/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.View;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.dataImage.PointValueEmporter;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.emport.SpreadsheetEmporter;
import com.serotonin.m2m2.vo.emport.SpreadsheetEmporter.FileType;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.mvc.UrlHandler;

/**
 * @author Terry Packer
 *
 */
public class FileUploadController implements UrlHandler {
	
	
    public View handleRequest(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model)
            throws Exception {
        ensurePermission(Common.getHttpUser());
        return prepareResponse(request,response,model);
    }
    
    /**
     * Ensure that the User has permissions to upload the file for this controller.
     * 
     * NOTE: This is used by the DataImportController in the Data Import Module
     * @param user
     */
    protected void ensurePermission(User user) throws PermissionException{
    	Permissions.ensureHasAdminPermission(Common.getHttpUser());
    }

	/**
     * @param request
     * @param response
     * @param model
     * @throws IOException 
     * @throws JsonException 
     * @throws FileUploadException 
     */
    private FileUploadView prepareResponse(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model)
            throws IOException, JsonException, FileUploadException {
       
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest)request;
        String dataType = multipartRequest.getParameter("dataType");
        String uploadType = multipartRequest.getParameter("uploadType");
        
        List<Object> fileInfo = new ArrayList<Object>();
        
        Translations translations = ControllerUtils.getTranslations(request);
        Iterator<String> itr =  multipartRequest.getFileNames();
        while(itr.hasNext()){
			
            MultipartFile file = multipartRequest.getFile(itr.next());
    		if (!file.isEmpty()) {
    			Map<String, Object> info = new HashMap<String, Object>();
    			info.put("filename", file.getOriginalFilename());
    			info.put("dataType", dataType);
    			parseFile(file.getInputStream(), info, translations, request);
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

        if (iframe || html5) {
            return new FileUploadView(iframe);
        }
        else if (flash) {
            // TODO handle Flash
            throw new ShouldNeverHappenException("Flash upload not supported.");
        }
        else {
            throw new ShouldNeverHappenException("Invalid file upload type.");
        }
    }
    
    /**
     * Parse the Import Files
     * @param input
     * @param model
     * @param translations
     */
    protected void parseFile(InputStream input, Map<String, Object> model, Translations translations,HttpServletRequest request) {
    	
    	//Get the filename
    	String filename = (String)model.get("filename");
        SpreadsheetEmporter emporter;
    	if(filename == null)
    		return;
    	else{
    		if(filename.toLowerCase().endsWith(".xls"))
    			emporter = new SpreadsheetEmporter(FileType.XLS);
    		else if(filename.toLowerCase().endsWith(".xlsx"))
    			emporter = new SpreadsheetEmporter(FileType.XLSX);
    		else
    			return;
    	}
         
        
        //Switch on the type
        String dataType = (String) model.get("dataType");

        if(dataType != null){
	        if(dataType.equals("pointValue")){
	        	//List the sheets and create sheet emporters for each
	        	for(Sheet sheet: emporter.listSheets(input))
	        		emporter.doImport(input, new PointValueEmporter(sheet.getSheetName()));
	        }else
	        	throw new ShouldNeverHappenException("Unsupported data.");
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

        model.put("rowsImported", emporter.getRowsAdded());
        model.put("rowsDeleted", emporter.getRowsDeleted());
        model.put("rowsWithErrors", emporter.getRowErrors());
    }
	
	class FileUploadView implements View{

		private boolean iframe;
		
		public FileUploadView(boolean iframe){
			this.iframe = iframe;
		}
		
		/* (non-Javadoc)
		 * @see org.springframework.web.servlet.View#getContentType()
		 */
		@Override
		public String getContentType() {
			return "application/json";
		}

		/* (non-Javadoc)
		 * @see org.springframework.web.servlet.View#render(java.util.Map, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
		 */
		@Override
		public void render(Map<String, ?> model, HttpServletRequest request,
				HttpServletResponse response) throws Exception {
			
			OutputStreamWriter osWriter = new OutputStreamWriter(response.getOutputStream());
	        
	        if (iframe) {
	            osWriter.write("<textarea>");
	        }
	        
	        JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, osWriter);
	        writer.writeObject(model);
	        
	        if (iframe) {
	            osWriter.write("</textarea>");
	        }
	        
	        osWriter.flush();
			
		}
		
	}
	
}
