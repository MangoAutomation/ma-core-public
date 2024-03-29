/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Class to Log IO with the option to keep historical files
 * 
 * @author Terry Packer
 *
 */
public class RollingIOLog extends BaseIOLog{
	
    private static final Logger LOG = LoggerFactory.getLogger(RollingIOLog.class);
    
    //New Members
    protected int fileSize;
    protected int maxFiles;
    protected int currentFileNumber;
    
    /**
     * 
     * @param baseFilename - The base filename for all logfiles ie. dataLog.log
     * @param fileSize - in bytes of file before rolling over
     * @param maxFiles - max number to keep in addition to the current log file
     */
    public RollingIOLog(final String baseFilename, File logDirectory, int fileSize, int maxFiles) {
    	super(new File(logDirectory, baseFilename));  //Ignoring this
        createOut();
 
        //Detect the current file number
        File[] files = logDirectory.listFiles(new LogFilenameFilter(baseFilename));
        
        //files will contain baseFilename.log, baseFilename.log.1 ... baseFilename.log.n
        // where n is our currentFileNumber
        this.currentFileNumber = files.length -1;
        if(this.currentFileNumber > maxFiles)
        	this.currentFileNumber = maxFiles;

        this.fileSize = fileSize;
        this.maxFiles = maxFiles;
        
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
                LOG.error("An error occurred", e);
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
			return name.contains(this.nameToMatch);
		}
    	
    }
    
}
