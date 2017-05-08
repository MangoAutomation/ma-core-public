/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util.image;

/**
 * @author Matthew Lohbihler
 */
abstract public class BaseImageFormat {
    abstract public String getType();
    abstract public boolean supportsCompression();
    abstract public float getCompressionQuality();
}
