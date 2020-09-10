/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.dataPoint;

import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * Definition for hooks into the data point create/update/delete Lifecycle
 *
 * @author Terry Packer
 */
public abstract class DataPointChangeDefinition extends ModuleElementDefinition {

    /**
     * Pre insert of point
     * @param vo
     */
    public void preInsert(DataPointVO vo) { }

    /**
     * Post insert of point
     * @param vo
     */
    public void postInsert(DataPointVO vo) { }

    /**
     * Pre update of point
     * @param vo
     */
    public void preUpdate(DataPointVO vo) { }

    /**
     * Post update of point
     * @param vo
     */
    public void postUpdate(DataPointVO vo) { }

    /**
     * Pre delete of point
     * @param vo
     */
    public void preDelete(DataPointVO vo) { }

    /**
     * Post delete of point
     * @param vo
     */
    public void postDelete(DataPointVO vo) { }

}
