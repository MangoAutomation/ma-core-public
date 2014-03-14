/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;

/**
 * @author Matthew Lohbihler
 */
public class AnonymousUser implements SetPointSource {
    public int getSetPointSourceId() {
        return 0;
    }

    public String getSetPointSourceType() {
        return "ANONYMOUS";
    }

    public TranslatableMessage getSetPointSourceMessage() {
        return new TranslatableMessage("annotation.anonymous");
    }

    public void raiseRecursionFailureEvent() {
        throw new ShouldNeverHappenException("");
    }
}
