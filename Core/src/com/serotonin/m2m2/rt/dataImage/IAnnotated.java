/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;

/**
 * Indicate that a value has an annotation
 * @author Terry Packer
 */
public interface IAnnotated {
    
    /**
     * Get the Translatable Message of the Annotation
     * @return
     */
    TranslatableMessage getSourceMessage();
    
    /**
     * Get the translated annotation
     * @param translations
     * @return
     */
    String getAnnotation(Translations translations);

}
