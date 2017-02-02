/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataSource;

import com.serotonin.m2m2.vo.dataSource.PointLocatorVO;

/**
 * This type provides the data source with the information that it needs to locate the point data.
 * 
 * @author mlohbihler
 */
abstract public class PointLocatorRT<VO extends PointLocatorVO<?>> {
    protected final VO vo;
    
    public PointLocatorRT(VO vo) {
        this.vo = vo;
    }
    
    public boolean isSettable() {
        return vo.isSettable();
    }

    public boolean isRelinquishable() {
        return vo.isRelinquishable();
    }
    
    public VO getVo() {
        return vo;
    }
}
