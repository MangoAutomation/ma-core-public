/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.dataImage;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 *
 * @author Terry Packer
 */
public class AnnotatedIdPointValueTime extends IdPointValueTime implements IAnnotated {

    private static final long serialVersionUID = 1L;
    private final TranslatableMessage sourceMessage;

    /**
     * @param seriesId
     * @param value
     * @param time
     * @param sourceMessage
     */
    public AnnotatedIdPointValueTime(int seriesId, DataValue value, long time,
            TranslatableMessage sourceMessage) {
        super(seriesId, value, time);
        this.sourceMessage = sourceMessage;
    }

    @Override
    public TranslatableMessage getSourceMessage() {
        return sourceMessage;
    }
    @Override
    public String getAnnotation(Translations translations) {
        return sourceMessage.translate(translations);
    }

}
