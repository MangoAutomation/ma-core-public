/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo;

import java.util.Map;

import com.infiniteautomation.mango.permission.MangoPermission;

/**
 * Summary of a data point
 *
 * @author Terry Packer
 */
public class DataPointSummary implements IDataPoint {

    private int id;
    private String xid;
    private String name;
    private int dataSourceId;
    private String deviceName;
    private MangoPermission readPermission;
    private MangoPermission setPermission;
    private Map<String, String> tags;

    public DataPointSummary() {
        // no op
    }

    public DataPointSummary(DataPointVO vo) {
        id = vo.getId();
        xid = vo.getXid();
        name = vo.getName();
        dataSourceId = vo.getDataSourceId();
        deviceName = vo.getDeviceName();
        readPermission = vo.getReadPermission();
        setPermission = vo.getSetPermission();
        tags = vo.getTags();
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(int dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    @Override
    public MangoPermission getReadPermission() {
        return readPermission;
    }

    public void setReadPermission(MangoPermission readPermission) {
        this.readPermission = readPermission;
    }

    @Override
    public MangoPermission getSetPermission() {
        return setPermission;
    }

    public void setSetPermission(MangoPermission setPermission) {
        this.setPermission = setPermission;
    }

    @Override
    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return "XID: " + this.xid;
    }
}
