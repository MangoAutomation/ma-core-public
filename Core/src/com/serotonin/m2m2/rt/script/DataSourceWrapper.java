/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.List;

import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

/**
 * @author Terry Packer
 *
 */
public class DataSourceWrapper {

    private DataSourceVO vo;
    private List<DataPointWrapper> points;

    public DataSourceWrapper(DataSourceVO vo, List<DataPointWrapper> points) {
        this.vo = vo;
        this.points = points;
    }

    public boolean isEnabled() {
        return vo.isEnabled();
    }

    public String getXid() {
        return vo.getXid();
    }

    public String getName() {
        return vo.getName();
    }

    public String getTypeName(){
        return vo.getDefinition().getDataSourceTypeName();
    }

    public List<DataPointWrapper> getPoints() {
        return points;
    }

    public String getHelp(){
        return toString();
    }

    /**
     * For subclass use
     */
    public void helpImpl(StringBuilder builder){ }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("enabled: ").append(isEnabled()).append(",\n");
        builder.append("xid: ").append(getXid()).append(",\n");
        builder.append("name: ").append(getName()).append(",\n");
        builder.append("typeName: ").append(getTypeName()).append(",\n");
        builder.append("points: [");
        for(DataPointWrapper wrapper : this.points){
            builder.append(wrapper.getHelp());
            builder.append(",\n");
        }
        builder.append("],");
        this.helpImpl(builder);

        builder.append(" }\n");
        return builder.toString();
    }
}
