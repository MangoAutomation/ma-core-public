/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.regex;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;

/**
 * Callback for matching a point value time from Regex
 * @author Terry Packer
 *
 */
public interface MatchCallback {

	public void onMatch(String pointIdentifier, PointValueTime value);
	public void pointPatternMismatch(String message, String pointValueRegex);
	public void messagePatternMismatch(String message, String messageRegex);
	public void pointNotIdentified(String message, String messageRegex, int pointIdentifierIndex);
	public void matchGeneralFailure(Exception e);
	
}
