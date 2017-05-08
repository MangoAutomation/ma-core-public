package com.serotonin.util.image;

/**
 * Scales down an image to fit entirely inside the "box", or dimension that are provided while preserving the original 
 * aspect ratio. Does this in multiple passes if necessary.
 * 
 * @author lohbihma
 */
public class BoxScaledImage extends BaseScaledImage {
    private final int maxWidth;
    private final int maxHeight;
    
    public BoxScaledImage(int maxWidth, int maxHeight) {
        this(maxWidth, maxHeight, true);
    }
    
    public BoxScaledImage(int maxWidth, int maxHeight, boolean scaleUp) {
        super(scaleUp);
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }
        
    @Override
    protected void setScalingParameters(int width, int height) {
        double widthRatio = width / (double)maxWidth;
        double heightRatio = height / (double)maxHeight;
        
        if (widthRatio >= heightRatio) {
            scaleRatio = widthRatio;
            scaledWidth = maxWidth;
            scaledHeight = (int)(height / scaleRatio);
        }
        else {
            scaleRatio = heightRatio;
            scaledWidth = (int)(width / scaleRatio);
            scaledHeight = maxHeight;
        }
    }
}
