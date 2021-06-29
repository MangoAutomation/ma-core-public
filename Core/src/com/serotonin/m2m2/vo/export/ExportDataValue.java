/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.export;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;

/**
 * @author Matthew Lohbihler
 */
public class ExportDataValue implements IValueTime {
    private int reportPointId;
    private DataValue value;
    private long time;
    private TranslatableMessage annotation;

    public ExportDataValue() {
        // no op
    }

    public ExportDataValue(DataValue value, long time) {
        this.value = value;
        this.time = time;
    }

    public int getReportPointId() {
        return reportPointId;
    }

    public void setReportPointId(int reportPointId) {
        this.reportPointId = reportPointId;
    }

    public DataValue getValue() {
        return value;
    }

    public void setValue(DataValue value) {
        this.value = value;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public TranslatableMessage getAnnotation() {
        return annotation;
    }

    public void setAnnotation(TranslatableMessage annotation) {
        this.annotation = annotation;
    }


	@Override
    public String toString() {
        return value + "@" + time;
    }
}
