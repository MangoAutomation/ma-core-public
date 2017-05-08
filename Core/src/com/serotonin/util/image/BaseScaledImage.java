/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util.image;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * @author Matthew Lohbihler
 */
abstract public class BaseScaledImage {
    protected static final double MAX_SCALE_RATIO = 2;
    
    protected boolean scaleUp;
    protected double scaleRatio;
    protected int scaledWidth;
    protected int scaledHeight;
    protected BufferedImage scaledImage;
    
    protected BaseScaledImage(boolean scaleUp) {
        this.scaleUp = scaleUp;
    }
    
    abstract protected void setScalingParameters(int width, int height);
    
    public void scaleImage(Image originalImage) {
        int width = originalImage.getWidth(null);
        int height = originalImage.getHeight(null);
        
        setScalingParameters(width, height);
        
        if (scaledWidth < 1)
            scaledWidth = 1;
        if (scaledHeight < 1)
            scaledHeight = 1;
        
        // Scale the image in multiple passes as necessary.
        double remainingRatio = scaleRatio;
        
        if (scaleRatio == 1 || scaleRatio < 1 && !scaleUp)
            scaleImage(originalImage, width, height);
        else {
            Image preScale = originalImage;
            int passWidth;
            int passHeight;
            while (true) {
                double passRatio = remainingRatio;
                if (passRatio > MAX_SCALE_RATIO) {
                    passRatio = MAX_SCALE_RATIO;
                    passWidth = (int)(width / passRatio);
                    passHeight = (int)(height / passRatio);
                }
                else {
                    passWidth = scaledWidth;
                    passHeight = scaledHeight;
                }
                
                scaleImage(preScale, passWidth, passHeight);
                
                if (passRatio == remainingRatio)
                    break;
                
                preScale = scaledImage;
                width = passWidth;
                height = passHeight;
                remainingRatio /= passRatio;
            }
        }
    }
    
    protected void scaleImage(Image preScale, int passWidth, int passHeight) {
        scaledImage = new BufferedImage(passWidth, passHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = scaledImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(preScale, 0, 0, passWidth, passHeight, null);
    }
    
    public BufferedImage getScaledImage() {
        return scaledImage;
    }
    public int getScaledHeight() {
        return scaledHeight;
    }
    public int getScaledWidth() {
        return scaledWidth;
    }
    public void flush() {
        scaledImage.flush();
    }
}
