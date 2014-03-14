/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view;

import java.util.ArrayList;
import java.util.List;

public class ImageSet extends ViewGraphic {
    private final List<String> imageFilenames = new ArrayList<String>();

    ImageSet(String id, String name, String[] imageFilenames, int width, int height, int textX, int textY) {
        super(id, name, width, height, textX, textY);
        for (String filename : imageFilenames)
            this.imageFilenames.add(filename);
    }

    @Override
    public boolean isImageSet() {
        return true;
    }

    public int getImageCount() {
        return imageFilenames.size();
    }

    public String getImageFilename(int index) {
        return imageFilenames.get(index);
    }

    public List<String> getImageFilenames() {
        return imageFilenames;
    }
}
