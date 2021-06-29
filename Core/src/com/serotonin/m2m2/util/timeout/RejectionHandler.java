/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util.timeout;

import com.serotonin.timer.RejectedTaskReason;

/**
 * @author Terry Packer
 *
 */
public interface RejectionHandler {

	/**
	 * Task was rejected
	 * @param reason
	 */
	public void rejected(RejectedTaskReason reason);
}
