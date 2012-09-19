package com.serotonin.m2m2.vo;

public class DataPointSummary implements IDataPoint {
    private int id;
    private String xid;
    private String name;
    private int dataSourceId;
    private String deviceName;
    private int pointFolderId;

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
}
