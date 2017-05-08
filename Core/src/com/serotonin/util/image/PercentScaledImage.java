/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util.image;

/**
 * @author Matthew Lohbihler
 */
public class PercentScaledImage extends BaseScaledImage {
    private final float percent;
    
    public PercentScaledImage(float percent) {
        this(percent, true);
    }
    
    public PercentScaledImage(float percent, boolean scaleUp) {
        super(scaleUp);
        this.percent = percent;
    }
        
    @Override
    protected void setScalingParameters(int width, int height) {
        scaleRatio = 1 / percent;
        scaledWidth = (int)(width * percent);
        scaledHeight = (int)(height * percent);
    }
}
