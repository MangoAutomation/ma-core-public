package com.serotonin.m2m2.module;

import com.serotonin.m2m2.vo.DataPointVO;

/**
 * A definition allowing a hook into the data point delete event.
 *
 * TODO Mango 4.0 can we get rid of this?  It is not used by any modules.
 *
 * @author Matthew Lohbihler
 */
abstract public class DataPointChangeDefinition extends ModuleElementDefinition {
    /**
     * Called immediately before the point is to be inserted into the database.
     *
     * @param dpvo
     *            the point to be inserted
     */
    abstract public void beforeInsert(DataPointVO dpvo);

    /**
     * Called immediately after the point is inserted into the database.
     *
     * @param dpvo
     *            the point that was inserted
     */
    abstract public void afterInsert(DataPointVO dpvo);

    /**
     * Called immediately before the point is to be update in the database.
     *
     * @param dpvo
     *            the point to be updated
     */
    abstract public void beforeUpdate(DataPointVO dpvo);

    /**
     * Called immediately after the point is updated in the database.
     *
     * @param dpvo
     *            the point that was updated
     */
    abstract public void afterUpdate(DataPointVO dpvo);

    /**
     * Called immediately before the point is to be deleted from the database.
     *
     * @param dataPointId
     *            the point to be deleted
     */
    abstract public void beforeDelete(int dataPointId);

    /**
     * Called immediately after the point is deleted from the database.
     *
     * @param dataPointId
     *            the point that was deleted
     */
    abstract public void afterDelete(int dataPointId);
}
