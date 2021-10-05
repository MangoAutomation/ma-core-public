/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.dataPoint;

import java.util.Collection;
import java.util.Collections;

import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

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
     * Post insert of point, after inserting into the database but before the data point is started
     * by the runtime manager.
     *
     * Any event detectors which are added by this hook MUST be returned by this method.
     *
     * @param vo data point
     * @return collection of event detectors which were added by this method.
     * The event detectors must be for the inserted point, and saved (inserted into the database).
     */
    public Collection<AbstractPointEventDetectorVO> postInsert(DataPointVO vo) {
        return Collections.emptyList();
    }

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
