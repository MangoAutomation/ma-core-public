/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;

/**
 * Indicate that a value has an annotation
 * @author Terry Packer
 */
public interface IAnnotated {
    
    /**
     * Get the Translatable Message of the Annotation
     * @return translatable message
     */
    @NonNull TranslatableMessage getSourceMessage();
    
    /**
     * Get the translated annotation
     */
    String getAnnotation(Translations translations);

}
