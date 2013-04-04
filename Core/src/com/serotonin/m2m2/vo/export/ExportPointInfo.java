/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.export;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.text.TextRenderer;

/**
 * @author Matthew Lohbihler
 */
public class ExportPointInfo {
    private int reportPointId;
    private String deviceName;
    private String pointName;
    private int dataType;
    private DataValue startValue;
    private TextRenderer textRenderer;
    private String colour;
    private float weight;
    private boolean consolidatedChart;
    private boolean individualChart;
    private int plotType;
    private String xid;

    public String getExtendedName() {
        return deviceName + " - " + pointName;
    }

    public int getReportPointId() {
        return reportPointId;
    }

    public void setReportPointId(int reportPointId) {
        this.reportPointId = reportPointId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPointName() {
        return pointName;
    }

    public void setPointName(String pointName) {
        this.pointName = pointName;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public DataValue getStartValue() {
        return startValue;
    }

    public void setStartValue(DataValue startValue) {
        this.startValue = startValue;
    }

    public TextRenderer getTextRenderer() {
        return textRenderer;
    }

    public void setTextRenderer(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public boolean isConsolidatedChart() {
        return consolidatedChart;
    }

    public void setConsolidatedChart(boolean consolidatedChart) {
        this.consolidatedChart = consolidatedChart;
    }

    public boolean isIndividualChart() {
        return individualChart;
    }

    public void setIndividualChart(boolean individualChart) {
        this.individualChart = individualChart;
    }

    public int getPlotType() {
        return plotType;
    }

    public void setPlotType(int plotType) {
        this.plotType = plotType;
    }

	/**
	 * @param xid
	 */
	public void setXid(String xid) {
		this.xid = xid;
		
	}
	
	public String getXid(){
		return this.xid;
	}
}
