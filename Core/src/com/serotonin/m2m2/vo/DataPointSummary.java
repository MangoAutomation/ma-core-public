package com.serotonin.m2m2.vo;

import java.util.Set;

public class DataPointSummary implements IDataPoint {
    private int id;
    private String xid;
    private String name;
    private int dataSourceId;
    private String deviceName;
    private int pointFolderId;
    private Set<RoleVO> readRoles;
    private Set<RoleVO> setRoles;

    public DataPointSummary() {
        // no op
    }

    public DataPointSummary(DataPointVO vo) {
        id = vo.getId();
        xid = vo.getXid();
        name = vo.getName();
        dataSourceId = vo.getDataSourceId();
        deviceName = vo.getDeviceName();
        pointFolderId = vo.getPointFolderId();
        readRoles = vo.getReadRoles();
        setRoles = vo.getSetRoles();
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
    public int getPointFolderId() {
        return pointFolderId;
    }

    public void setPointFolderId(int pointFolderId) {
        this.pointFolderId = pointFolderId;
    }

    @Override
    public String getExtendedName() {
        return DataPointVO.getExtendedName(this);
    }

    @Override
    public Set<RoleVO> getReadRoles() {
        return readRoles;
    }

    public void setReadRoles(Set<RoleVO>  readRoles) {
        this.readRoles = readRoles;
    }

    @Override
    public Set<RoleVO>  getSetRoles() {
        return setRoles;
    }

    public void setSetPermission(Set<RoleVO> setRoles) {
        this.setRoles = setRoles;
    }

	@Override
    public String toString() {
        return "XID: " + this.xid;
    }
}
