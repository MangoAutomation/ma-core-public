/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util.log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;

/**
 * @author Terry Packer
 *
 */
public class RollingProcessLog extends ProcessLog{

    private static final Log LOG = LogFactory.getLog(RollingProcessLog.class);
    
    //New Members
    protected int fileSize;
    protected int maxFiles;
    protected int currentFileNumber;
    
    public RollingProcessLog(String id, LogLevel logLevel, int fileSize, int maxFiles){
    	super(id, logLevel, null, false);
    	this.fileSize = fileSize;
    	this.maxFiles = maxFiles;
    }
	
    /**
     * List all the files
     * @return
     */
    public File[] getFiles(){
    	File[] files = Common.getLogsDir().listFiles(new LogFilenameFilter(file.getName()));
    	return files;
    }
	
    @Override
    protected void sizeCheck() {
        // Check if the file should be rolled.
        if (file.length() > this.fileSize) {
            out.close();

            try{
	            //Do rollover
 	        	for(int i=this.currentFileNumber; i>0; i--){
	        		Path source = Paths.get( this.file.getAbsolutePath() + "." + i);
	            	Path target = Paths.get(this.file.getAbsolutePath() + "." + (i + 1));
	            	Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
	        	}
	        	
	        	Path source = Paths.get(this.file.toURI());
	        	Path target = Paths.get(this.file.getAbsolutePath() + "." + 1);
	        	Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
	        	
                if(this.currentFileNumber < this.maxFiles - 1){
                	//Use file number
                	this.currentFileNumber++;
                }
	        	
            }catch(IOException e){
            	LOG.error(e);
            }
             
            createOut();
        }
    }
    
    /**
     * Class to filter log filenames from a directory listing
     * @author Terry Packer
     *
     */
    class LogFilenameFilter implements FilenameFilter{
    	
    	private String nameToMatch;
    	
    	public LogFilenameFilter(String nameToMatch){
    		this.nameToMatch = nameToMatch;
    	}

		@Override
		public boolean accept(File dir, String name) {
			return name.startsWith(this.nameToMatch);
		}
    	
    }
}
