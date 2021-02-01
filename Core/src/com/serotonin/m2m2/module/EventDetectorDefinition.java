/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module;

import org.jooq.Field;
import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Provides modules with the ability to register additional event detectors
 *
 * @author Terry Packer
 *
 */
public abstract class EventDetectorDefinition<T extends AbstractEventDetectorVO> extends ModuleElementDefinition {

    @Autowired
    protected PermissionService permissionService;

    /**
     * Name of the column in the event detectors into which to store the source id
     * @return
     */
    abstract public Field<Integer> getSourceIdColumnName();

    /**
     * Name of the type of Source [DATA_POINT, DATA_SOURCE, SYSTEM, ...]
     * used to group detectors by source of event
     *
     * @return
     */
    abstract public String getSourceTypeName();

    /**
     * An internal identifier for this type of Event Detector. Must be unique within an MA instance, and is recommended
     * to have the form "&lt;moduleType&gt;.&lt;modelName&gt;" so as to be unique across all modules.
     *
     * @return the model type name.
     */
    abstract public String getEventDetectorTypeName();

    /**
     * A reference to a human readable and translatable brief description of the handler. Key reference values in
     * i18n.properties files. Descriptions are used in drop down select boxes, and so should be as brief as possible.
     *
     * @return the reference key to the handler's short description.
     */
    abstract public String getDescriptionKey();

    /**
     * Create and return an instance of the event detector with its source id for use to look up
     * any additional information about the source
     * @param sourceId
     * @return a new instance of the event detector.
     */
    abstract protected T createEventDetectorVO(int sourceId);

    /**
     * Reload any runtime data for this detector's source as the
     * detector was updated
     * @param vo
     */
    abstract public void restartSource(T vo);

    /**
     * Can this user create this detector?
     *
     * @param user
     * @param vo
     * @return
     */
    public abstract boolean hasCreatePermission(PermissionHolder user, T vo);

    /**
     * Can this user edit this detector?
     *
     * @param user
     * @param vo
     * @return
     */
    public boolean hasEditPermission(PermissionHolder user, T vo) {
        return permissionService.hasPermission(user, vo.getEditPermission());
    }

    /**
     * Can this user view this detector?
     *
     * @param user
     * @param vo
     * @return
     */
    public boolean hasReadPermission(PermissionHolder user, T vo) {
        return permissionService.hasPermission(user, vo.getReadPermission());
    }

    /**
     * Validate a new event detector
     * @param response
     * @param ds
     * @param user
     */
    abstract public void validate(ProcessResult response, T ds, PermissionHolder user);

    /**
     * Validate an event detector that is about to be updated
     *  override as necessary
     * @param response
     * @param existing
     * @param ds
     * @param user
     */
    public void validate(ProcessResult response, T existing, T ds, PermissionHolder user) {
        validate(response, ds, user);
    }

    /**
     * Used by MA core code to create a new event detector instances as required. Should not be used by client code.
     */
    public final T baseCreateEventDetectorVO(int sourceId) {
        T detector = createEventDetectorVO(sourceId);
        detector.setDefinition(this);
        return detector;
    }

    /**
     * Save any relational data prior
     * to the VO being saved
     *
     * NOTE: this logic will be executed in a database transaction.
     *
     * @param existing - null on insert
     * @param vo
     */
    public void savePreRelationalData(T existing, T vo) {

    }

    /**
     * Save any relational data
     *
     * NOTE: this logic will be executed in a database transaction.
     *
     * @param existing - null on insert
     * @param vo
     */
    public void saveRelationalData(T existing, T vo) {

    }

    /**
     * Delete any relational data
     *
     * NOTE: this logic will be executed in a database transaction.
     * @param vo
     */
    public void deleteRelationalData(T vo) {

    }

    /**
     * Delete any relational data
     *
     * NOTE: this logic will be executed in a database transaction.
     * @param vo
     */
    public void deletePostRelationalData(T vo) {

    }

    /**
     * Load in relational data for the data source
     *
     * @param vo
     */
    public void loadRelationalData(T vo) {

    }

}
