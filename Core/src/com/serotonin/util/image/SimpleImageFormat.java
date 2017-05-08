/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util.image;

/**
 * @author Matthew Lohbihler
 */
public class SimpleImageFormat extends BaseImageFormat {
    private final String type;
    
    public SimpleImageFormat(String type) {
        this.type = type;
    }

    @Override
    public float getCompressionQuality() {
        return 0;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public boolean supportsCompression() {
        return false;
    }
}
