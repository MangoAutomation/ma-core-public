/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;

public class PointChangeDetectorRT extends PointEventDetectorRT {
    private DataValue oldValue;
    private DataValue newValue;
    private TranslatableMessage annotation;

    public PointChangeDetectorRT(PointEventDetectorVO vo) {
        this.vo = vo;
    }

    @Override
    protected TranslatableMessage getMessage() {
        if (annotation != null) {
            // user annotation
            if (annotation.getKey().equals("annotation.user")) {
                return new TranslatableMessage("event.detector.changeCountUser", vo.njbGetDataPoint().getName(),
                        formatValue(oldValue), formatValue(newValue), annotation.getArgs()[0]);
            }
            return new TranslatableMessage("event.detector.changeCountAnnotation", vo.njbGetDataPoint().getName(),
                    formatValue(oldValue), formatValue(newValue), annotation);
        }
        return new TranslatableMessage("event.detector.changeCount", vo.njbGetDataPoint().getName(),
                formatValue(oldValue), formatValue(newValue));
    }

    private String formatValue(DataValue value) {
        return vo.njbGetDataPoint().getTextRenderer().getText(value, TextRenderer.HINT_SPECIFIC);
    }

    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        if (newValue.isAnnotated()) {
            annotation = ((AnnotatedPointValueTime) newValue).getSourceMessage();
        }
        this.oldValue = PointValueTime.getValue(oldValue);
        this.newValue = newValue.getValue();
        raiseEvent(newValue.getTime(), createEventContext());
    }

    @Override
    protected boolean isEventActive() {
        return false;
    }
}
