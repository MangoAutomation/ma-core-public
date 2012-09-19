/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view;

abstract public class ViewGraphic {
    private final String id;
    private final String name;
    private final int width;
    private final int height;
    private final int textX;
    private final int textY;

    ViewGraphic(String id, String name, int width, int height, int textX, int textY) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.height = height;
        this.textX = textX;
        this.textY = textY;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTextX() {
        return textX;
    }

    public int getTextY() {
        return textY;
    }

    public boolean isImageSet() {
        return false;
    }

    public boolean isDynamicImage() {
        return false;
    }
}
