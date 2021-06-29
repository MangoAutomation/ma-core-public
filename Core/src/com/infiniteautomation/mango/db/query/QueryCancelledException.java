/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Marker exception for when a streaming query was aborted.  Likely
 * due to the stream for writing the results to was closed or broken.
 *
 * @author Terry Packer
 */
public class QueryCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public QueryCancelledException(Throwable t) {
        super(t);
    }

    public QueryCancelledException(TranslatableMessage m) {
        super(m.translate(Common.getTranslations()));
    }

}
