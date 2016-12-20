/*
    Copyright (C) 2013 Deltamation Software All rights reserved.
    @author Terry Packer
 */
package com.serotonin.m2m2.rt;

/**
 * Copyright (C) 2013 Deltamation Software. All Rights Reserved.
 * @author Terry Packer
 *
 */
public abstract class AbstractRT<VO> {
    protected final VO vo;
    
    public AbstractRT(VO vo) {
        this.vo = vo;
    }
    
    public VO getVo() {
        return vo;
    }
    
    /**
     * Initialize the RT
     */
    public abstract void initialize();
    
    /**
     * Terminate the RT
     */
    public abstract void terminate();
}
