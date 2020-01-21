/*
    Copyright (C) 2013 Deltamation Software All rights reserved.
    @author Terry Packer
 */
package com.serotonin.m2m2.rt;

import com.serotonin.m2m2.vo.AbstractVO;

/**
 * Copyright (C) 2013 Deltamation Software. All Rights Reserved.
 * @author Terry Packer
 *
 */
public abstract class AbstractRT<VO extends AbstractVO> {
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
