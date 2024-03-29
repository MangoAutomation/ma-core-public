/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.timer;

/**
 *
 * @author Terry Packer
 */
public class RejectedTaskException extends Exception {

    private static final long serialVersionUID = 1L;
    private RejectedTaskReason reason;

    public RejectedTaskException(RejectedTaskReason reason) {
        this.reason = reason;
    }

    @Override
    public String getMessage() {
        return this.reason.getDescription();
    }
}
