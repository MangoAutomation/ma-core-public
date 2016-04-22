/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Terry Packer
 *
 */
public class LoggingPropertiesModel{

	@JsonProperty("loggingType")
	private String loggingType;
	@JsonProperty("intervalLoggingType")
	private String intervalLoggingType;
	@JsonProperty("intervalLoggingPeriod")
	private TimePeriodModel intervalLoggingPeriod;
	@JsonProperty
	private double tolerance;
	@JsonProperty
	private boolean discardExtremeValues;
	@JsonProperty
	private double discardLowLimit;
	@JsonProperty
	private double discardHighLimit;	
	@JsonProperty("overrideIntervalLoggingSamples")
	private boolean overrideIntervalLoggingSamples;
	@JsonProperty("intervalLoggingSampleWindowSize")
	private int intervalLoggingSampleWindowSize;
	@JsonProperty("cacheSize")
	private int defaultCacheSize;

	public LoggingPropertiesModel(){ }
	
	/**
	 * @param loggingType
	 * @param intervalLoggingType
	 * @param intervalLoggingPeriod
	 * @param tolerance
	 * @param discardExtremeValues
	 * @param discardLowLimit
	 * @param discardHighLimit
	 * @param overrideIntervalLoggingSamples
	 * @param sampleWindowSize
	 * @param defaultCacheSize
	 */
	public LoggingPropertiesModel(String loggingType,
			String intervalLoggingType, TimePeriodModel intervalLoggingPeriod,
			double tolerance, boolean discardExtremeValues,
			double discardLowLimit, double discardHighLimit,
			boolean overrideIntervalLoggingSamples, int sampleWindowSize,
			int defaultCacheSize) {
		super();
		this.loggingType = loggingType;
		this.intervalLoggingType = intervalLoggingType;
		this.intervalLoggingPeriod = intervalLoggingPeriod;
		this.tolerance = tolerance;
		this.discardExtremeValues = discardExtremeValues;
		this.discardLowLimit = discardLowLimit;
		this.discardHighLimit = discardHighLimit;
		this.overrideIntervalLoggingSamples = overrideIntervalLoggingSamples;
		this.intervalLoggingSampleWindowSize = sampleWindowSize;
		this.defaultCacheSize = defaultCacheSize;
	}

	public String getLoggingType() {
		return loggingType;
	}

	public void setLoggingType(String loggingType) {
		this.loggingType = loggingType;
	}

	public String getIntervalLoggingType() {
		return intervalLoggingType;
	}

	public void setIntervalLoggingType(String intervalLoggingType) {
		this.intervalLoggingType = intervalLoggingType;
	}

	public TimePeriodModel getIntervalLoggingPeriod() {
		return intervalLoggingPeriod;
	}

	public void setIntervalLoggingPeriod(TimePeriodModel intervalLoggingPeriod) {
		this.intervalLoggingPeriod = intervalLoggingPeriod;
	}

	public double getTolerance() {
		return tolerance;
	}

	public void setTolerance(double tolerance) {
		this.tolerance = tolerance;
	}

	public boolean isDiscardExtremeValues() {
		return discardExtremeValues;
	}

	public void setDiscardExtremeValues(boolean discardExtremeValues) {
		this.discardExtremeValues = discardExtremeValues;
	}

	public double getDiscardLowLimit() {
		return discardLowLimit;
	}

	public void setDiscardLowLimit(double discardLowLimit) {
		this.discardLowLimit = discardLowLimit;
	}

	public double getDiscardHighLimit() {
		return discardHighLimit;
	}

	public void setDiscardHighLimit(double discardHighLimit) {
		this.discardHighLimit = discardHighLimit;
	}

	public boolean isOverrideIntervalLoggingSamples() {
		return overrideIntervalLoggingSamples;
	}

	public void setOverrideIntervalLoggingSamples(
			boolean overrideIntervalLoggingSamples) {
		this.overrideIntervalLoggingSamples = overrideIntervalLoggingSamples;
	}

	public int getIntervalLoggingSampleWindowSize() {
		return intervalLoggingSampleWindowSize;
	}

	public void setIntervalLoggingSampleWindowSize(int sampleWindowSize) {
		this.intervalLoggingSampleWindowSize = sampleWindowSize;
	}

	public int getDefaultCacheSize() {
		return defaultCacheSize;
	}

	public void setDefaultCacheSize(int defaultCacheSize) {
		this.defaultCacheSize = defaultCacheSize;
	}
}
