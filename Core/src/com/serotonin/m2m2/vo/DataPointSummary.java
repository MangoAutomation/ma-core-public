package com.serotonin.m2m2.vo;

import java.util.Set;

import com.serotonin.m2m2.vo.role.Role;

public class DataPointSummary implements IDataPoint {
    private int id;
    private String xid;
    private String name;
    private int dataSourceId;
    private String deviceName;
    private Set<Role> readRoles;
    private Set<Role> setRoles;

    public DataPointSummary() {
        // no op
    }

    public DataPointSummary(DataPointVO vo) {
        id = vo.getId();
        xid = vo.getXid();
        name = vo.getName();
        dataSourceId = vo.getDataSourceId();
        deviceName = vo.getDeviceName();
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
    public String getExtendedName() {
        return DataPointVO.getExtendedName(this);
    }

    @Override
    public Set<Role> getReadRoles() {
        return readRoles;
    }

    public void setReadRoles(Set<Role>  readRoles) {
        this.readRoles = readRoles;
    }

    @Override
    public Set<Role>  getSetRoles() {
        return setRoles;
    }

    public void setSetPermission(Set<Role> setRoles) {
        this.setRoles = setRoles;
    }

	@Override
    public String toString() {
        return "XID: " + this.xid;
    }
}
