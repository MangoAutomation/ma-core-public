/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.dataImage;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Terry Packer
 *
 */
public class AnnotatedPointValueIdTime extends PointValueIdTime{
    private static final long serialVersionUID = -1;
    
    private final TranslatableMessage sourceMessage;

    public AnnotatedPointValueIdTime(int id, DataValue value, long time, TranslatableMessage sourceMessage) {
        super(id,value, time);
        this.sourceMessage = sourceMessage;
    }

    @Override
    public boolean isAnnotated() {
        return true;
    }

    public TranslatableMessage getSourceMessage() {
        return sourceMessage;
    }

    public String getAnnotation(Translations translations) {
        return sourceMessage.translate(translations);
    }

}
