/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.IAnnotated;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.UpdateDetectorVO;

/**
 * Detect point value updates
 *
 * @author Jared Wiltshire
 */
public class UpdateDetectorRT extends PointEventDetectorRT<UpdateDetectorVO> {

    private DataValue newValue;
    private TranslatableMessage annotation;

    public UpdateDetectorRT(UpdateDetectorVO vo) {
        super(vo);
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
                return new TranslatableMessage("event.detector.updateUser", vo.getDataPoint().getExtendedName(),
                        formatValue(newValue), annotation.getArgs()[0]);
            }
            return new TranslatableMessage("event.detector.updateAnnotation", vo.getDataPoint().getExtendedName(),
                    formatValue(newValue), annotation);
        }
        return new TranslatableMessage("event.detector.update", vo.getDataPoint().getExtendedName(),
                formatValue(newValue));
    }

    private String formatValue(DataValue value) {
        return vo.getDataPoint().getTextRenderer().getText(value, TextRenderer.HINT_SPECIFIC);
    }

    @Override
    public void pointUpdated(PointValueTime newValue) {
        if (newValue instanceof IAnnotated)
            annotation = ((IAnnotated) newValue).getSourceMessage();

        this.newValue = newValue.getValue();
        raiseEvent(newValue.getTime(), createEventContext());
    }

    @Override
    public boolean isEventActive() {
        return false;
    }
}
