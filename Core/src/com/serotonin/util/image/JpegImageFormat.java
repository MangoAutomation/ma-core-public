/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util.image;

/**
 * @author Matthew Lohbihler
 */
public class JpegImageFormat extends BaseImageFormat {
    private final float compressionQuality;
    
    public JpegImageFormat(float compressionQuality) {
        this.compressionQuality = compressionQuality;
    }
    
    @Override
    public String getType() {
        return "jpeg";
    }
    
    @Override
    public boolean supportsCompression() {
        return true;
    }

    @Override
    public float getCompressionQuality() {
        return compressionQuality;
    }
}
