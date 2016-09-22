package com.serotonin.m2m2.vo;

import java.util.Set;

import com.serotonin.m2m2.vo.permission.Permissions;

public class DataPointSummary implements IDataPoint {
    private int id;
    private String xid;
    private String name;
    private int dataSourceId;
    private String deviceName;
    private int pointFolderId;
    private String readPermission;
    private Set<String> readPermissionsSet;
    private String setPermission;
    private Set<String> setPermissionsSet;

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
        readPermission = vo.getReadPermission();
        readPermissionsSet = Permissions.explodePermissionGroups(readPermission);
        setPermission = vo.getSetPermission();
        setPermissionsSet = Permissions.explodePermissionGroups(setPermission);
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
    public String getReadPermission() {
        return readPermission;
    }

    public void setReadPermission(String readPermission) {
        this.readPermission = readPermission;
    }

    @Override
    public String getSetPermission() {
        return setPermission;
    }

    public void setSetPermission(String setPermission) {
        this.setPermission = setPermission;
    }

    public Set<String> getReadPermissionsSet() {
		return readPermissionsSet;
	}

	public void setReadPermissionsSet(Set<String> readPermissionsSet) {
		this.readPermissionsSet = readPermissionsSet;
	}

	public Set<String> getSetPermissionsSet() {
		return setPermissionsSet;
	}

	public void setSetPermissionsSet(Set<String> setPermissionsSet) {
		this.setPermissionsSet = setPermissionsSet;
	}

	@Override
    public String toString() {
        return "XID: " + this.xid;
    }
}
