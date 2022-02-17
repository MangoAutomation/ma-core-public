/*
    Mango - Open Source M2M - http://mango.serotoninsoftware.com
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc.
    @author Matthew Lohbihler

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.Objects;

import com.infiniteautomation.mango.util.Functions;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedIdPointValueTime.AnnotatedMetaIdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;

public class IdPointValueTime extends PointValueTime implements SeriesIdTime {

    private final int seriesId;

    public IdPointValueTime(int seriesId, DataValue value, long time) {
        super(value, time);
        this.seriesId = seriesId;
    }

    @Override
    public int getSeriesId() {
        return seriesId;
    }

    @Override
    public String toString() {
        return "IdPointValueTime(" + seriesId + "=" + getValue() + "@" + Functions.getFullMilliSecondTime(getTime()) + ")";
    }

    public IdPointValueTime withNewTime(long newTime) {
        if (newTime == getTime()) {
            return this;
        }
        return new MetaIdPointValueTime(seriesId, getValue(), newTime, true, isFromCache());
    }

    public IdPointValueTime withFromCache() {
        if (isFromCache()) {
            return this;
        }
        return new MetaIdPointValueTime(this, isBookend(), true);
    }

    @Override
    public PointValueTime withAnnotation(TranslatableMessage message) {
        return new AnnotatedIdPointValueTime(this, message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdPointValueTime)) return false;
        if (!super.equals(o)) return false;
        IdPointValueTime that = (IdPointValueTime) o;
        return seriesId == that.seriesId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), seriesId);
    }

    public static class MetaIdPointValueTime extends IdPointValueTime {

        final boolean bookend;
        final boolean fromCache;

        public <T extends SeriesIdTime & IValueTime<DataValue>> MetaIdPointValueTime(T source, boolean bookend, boolean fromCache) {
            this(source.getSeriesId(), source.getValue(), source.getTime(), bookend, fromCache);
        }

        public MetaIdPointValueTime(int seriesId, DataValue value, long time, boolean bookend, boolean fromCache) {
            super(seriesId, value, time);
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
