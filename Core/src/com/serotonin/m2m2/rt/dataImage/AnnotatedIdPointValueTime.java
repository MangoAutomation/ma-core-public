/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.Objects;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;

/**
 *
 * @author Terry Packer
 */
public class AnnotatedIdPointValueTime extends IdPointValueTime implements IAnnotated {

    private static final long serialVersionUID = 1L;
    private final TranslatableMessage sourceMessage;

    public AnnotatedIdPointValueTime(IdPointValueTime value, TranslatableMessage sourceMessage) {
        this(value.getSeriesId(), value.getValue(), value.getTime(), sourceMessage);
    }

    public AnnotatedIdPointValueTime(int seriesId, DataValue value, long time, TranslatableMessage sourceMessage) {
        super(seriesId, value, time);
        this.sourceMessage = Objects.requireNonNull(sourceMessage);
    }

    @Override
    public TranslatableMessage getSourceMessage() {
        return sourceMessage;
    }
    @Override
    public String getAnnotation(Translations translations) {
        return sourceMessage.translate(translations);
    }

    @Override
    public AnnotatedIdPointValueTime withNewTime(long newTime) {
        if (newTime == getTime()) {
            return this;
        }
        return new AnnotatedMetaIdPointValueTime(this, true, isFromCache());
    }

    @Override
    public IdPointValueTime withFromCache() {
        if (isFromCache()) {
            return this;
        }
        return new AnnotatedMetaIdPointValueTime(this, isBookend(), true);
    }

    public static class AnnotatedMetaIdPointValueTime extends AnnotatedIdPointValueTime {

        final boolean bookend;
        final boolean fromCache;

        public <T extends SeriesIdTime & IValueTime & IAnnotated> AnnotatedMetaIdPointValueTime(T source, boolean bookend, boolean fromCache) {
            this(source, source.getSourceMessage(), bookend, fromCache);
        }

        public <T extends SeriesIdTime & IValueTime> AnnotatedMetaIdPointValueTime(T source, TranslatableMessage annotation, boolean bookend, boolean fromCache) {
            super(source.getSeriesId(), source.getValue(), source.getTime(), annotation);
            this.bookend = bookend;
            this.fromCache = fromCache;
        }

        @Override
        public boolean isBookend() {
            return bookend;
        }

        @Override
        public boolean isFromCache() {
            return fromCache;
        }

        @Override
        public PointValueTime withAnnotation(TranslatableMessage message) {
            return new AnnotatedMetaIdPointValueTime(this, message, bookend, fromCache);
        }
    }
}
