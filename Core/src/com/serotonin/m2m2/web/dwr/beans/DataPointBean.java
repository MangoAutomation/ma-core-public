/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.beans;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.DataPointVO;

public class DataPointBean {
    private int id;
    private String name;
    private boolean settable;
    private int dataType;
    private final TranslatableMessage dataTypeMessage;
    private final String chartColour;
    private int plotType;

    public DataPointBean(DataPointVO vo) {
        id = vo.getId();
        name = vo.getExtendedName();
        settable = vo.getPointLocator().isSettable();
        dataType = vo.getPointLocator().getDataTypeId();
        dataTypeMessage = vo.getDataTypeMessage();
        chartColour = vo.getChartColour();
        plotType = vo.getPlotType();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSettable() {
        return settable;
    }

    public void setSettable(boolean settable) {
        this.settable = settable;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public TranslatableMessage getDataTypeMessage() {
        return dataTypeMessage;
    }

    public String getChartColour() {
        return chartColour;
    }
    
    public int getPlotType() {
        return plotType;
    }
}

