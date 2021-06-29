/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.IAnnotated;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.PointChangeDetectorVO;

/**
 * Detect changes in a data point
 *
 */
public class PointChangeDetectorRT extends PointEventDetectorRT<PointChangeDetectorVO> {

    private DataValue oldValue;
    private DataValue newValue;
    private TranslatableMessage annotation;

    public PointChangeDetectorRT(PointChangeDetectorVO vo) {
        super(vo);
    }

    public DataValue getOldValue() {
        return oldValue;
    }

    public DataValue getNewValue() {
        return newValue;
    }

    public TranslatableMessage getAnnotation() {
        return annotation;
    }

    @Override
    protected TranslatableMessage getMessage() {
        if (annotation != null) {
            // user annotation
            if (annotation.getKey().equals("annotation.user")) {
                return new TranslatableMessage("event.detector.changeCountUser", vo.getDataPoint().getExtendedName(),
                        formatValue(oldValue), formatValue(newValue), annotation.getArgs()[0]);
            }
            return new TranslatableMessage("event.detector.changeCountAnnotation", vo.getDataPoint().getExtendedName(),
                    formatValue(oldValue), formatValue(newValue), annotation);
        }
        return new TranslatableMessage("event.detector.changeCount", vo.getDataPoint().getExtendedName(),
                formatValue(oldValue), formatValue(newValue));
    }

    private String formatValue(DataValue value) {
        return vo.getDataPoint().getTextRenderer().getText(value, TextRenderer.HINT_SPECIFIC);
    }

    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        if (newValue instanceof IAnnotated)
            annotation = ((IAnnotated)newValue).getSourceMessage();

        this.oldValue = PointValueTime.getValue(oldValue);
        this.newValue = newValue.getValue();
        raiseEvent(newValue.getTime(), createEventContext());
    }

    @Override
    public boolean isEventActive() {
        return false;
    }
}
