package com.serotonin.util.image;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 * Scales down an image so that the shortest side is the given length provided while preserving the original 
 * aspect ratio. Does this in multiple passes if necessary.

 * @author lohbihma
 */
public class ShortSideScaledImage extends BaseScaledImage {
    private final int length;
    
    public ShortSideScaledImage(int length) {
        this(length, true);
    }
    
    public ShortSideScaledImage(int length, boolean scaleUp) {
        super(scaleUp);
        this.length = length;
    }
    
    @Override
    protected void setScalingParameters(int width, int height) {
        // Determine the target size of the image
        if (width == height) {
            scaleRatio = width / (double)length;
            scaledWidth = length;
            scaledHeight = length;
        }
        else if (width < height) {
            scaleRatio = width / (double)length;
            scaledWidth = length;
            scaledHeight = (int)(height / scaleRatio);
        }
        else {
            scaleRatio = height / (double)length;
            scaledWidth = (int)(width / scaleRatio);
            scaledHeight = length;
        }
    }

    @Override
    public void scaleImage(Image originalImage) {
        super.scaleImage(originalImage);
        
        // Crop the image by creating a square of length*length and centering the scaled image in it.
        BufferedImage croppedImage = new BufferedImage(length, length, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = croppedImage.createGraphics();
        
        int x = (length - scaledWidth) / 2;
        int y = (length - scaledHeight) / 2;
        
        graphics2D.drawImage(scaledImage, x, y, null);
        
        scaledImage = croppedImage;
        scaledWidth = length;
        scaledHeight = length;
    }
}
