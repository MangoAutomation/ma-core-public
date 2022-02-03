/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.Objects;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * This class provides a way of arbitrarily annotating a PointValue. Point value annotations should not be confused with
 * Java annotations. A point value annotation will typically explain the source of the value when it did not simply come
 * from data source.
 * 
 * @see SetPointSource
 * @author Matthew Lohbihler
 */
public class AnnotatedPointValueTime extends PointValueTime implements IAnnotated {
    private static final long serialVersionUID = -1;
    private final TranslatableMessage sourceMessage;

    public AnnotatedPointValueTime(PointValueTime value, TranslatableMessage sourceMessage) {
        this(value.getValue(), value.getTime(), sourceMessage);
    }

    public AnnotatedPointValueTime(DataValue value, long time, TranslatableMessage sourceMessage) {
        super(value, time);
        this.sourceMessage = Objects.requireNonNull(sourceMessage);
    }
    
    @Override
    public TranslatableMessage getSourceMessage() {
        return sourceMessage;
    }

    @Override
    public String getAnnotation(Translations translations) {
        if(sourceMessage != null)
         return sourceMessage.translate(translations);
        else
            return null;
    }

    @Override
    public IdPointValueTime withSeriesId(int seriesId) {
        return new AnnotatedIdPointValueTime(seriesId, getValue(), getTime(), sourceMessage);
    }
}
