/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 * A publisher allows point values to be written (or published) to targets outside of the MA instance. Typically any
 * data points can be provided to a publisher regardless of the data source.
 *
 * When creating a publisher for MA, the following components are required:
 * <dl>
 * <dt>Subclass of {@link PublisherVO}</dt>
 * <dd>A configuration object of a publisher</dd>
 *
 * <dt>Subclass of {@link PublishedPointVO}</dt>
 * <dd>A configuration object of a published point</dd>
 *
 * <dt>Subclass of {@link PublisherRT}</dt>
 * <dd>A runtime implementation of the publisher.</dd>
 *
 * <dt>Editing JSP</dt>
 * <dd>The page on which a user can edit an instance of the publisher</dd>
 *
 * <dt>Subclass of {@link PublisherEditDwr}</dt>
 * <dd>The server-side AJAX controller for the editing JSP</dd>
 *
 * <dt>Optional</dt>
 * <dd>Online documentation files, translation files (strongly recommended), publisher commissioning tools.</dd>
 * </dl>
 *
 * @author Matthew Lohbihler
 */
abstract public class PublisherDefinition<PUB extends PublisherVO<? extends PublishedPointVO>> extends ModuleElementDefinition {
    /**
     * Used by MA core code to create a new publisher instance as required. Should not be used by client code.
     */
    public PUB baseCreatePublisherVO() {
        PUB pub = createPublisherVO();
        pub.setDefinition(this);
        return pub;
    }

    /**
     * An internal identifier for this type of publisher. Must be unique within an MA instance, and is recommended to
     * be unique inasmuch as possible across all modules.
     *
     * @return the publisher type name.
     */
    abstract public String getPublisherTypeName();

    /**
     * A reference to a human readable and translatable brief description of the publisher. Key reference values in
     * i18n.properties files. Descriptions are used in drop down select boxes, and so should be as brief as possible.
     *
     * @return the reference key to the publisher short description.
     */
    abstract public String getDescriptionKey();

    /**
     * Create and return an instance of the publisher.
     *
     * @return a new instance of the publisher.
     */
    abstract protected PUB createPublisherVO();

    /**
     * Validate a new publisher
     * @param response
     * @param pub
     * @param user
     */
    abstract public void validate(ProcessResult response, PUB pub, PermissionHolder user);

    /**
     * Validate a data source about to be updated
     *  override as necessary
     * @param response
     * @param existing
     * @param ds
     * @param user
     */
    public void validate(ProcessResult response, PUB existing, PUB vo, PermissionHolder user) {
        validate(response, vo, user);
    }

    /**
     * If the module is uninstalled, delete any publishers of this type. If this method is overridden, be sure to call
     * super.uninstall so that this code still runs.
     */
    @Override
    public void postRuntimeManagerTerminate(boolean uninstall) {
        if(uninstall)
            PublisherDao.getInstance().deletePublisherType(getPublisherTypeName());
    }

    /**
     * Save any relational data for this publisher i.e. script roles prior
     * to the VO being saved
     *
     * NOTE: this logic will be executed in a database transaction.
     *
     * @param existing - null on insert
     * @param vo
     */
    public void savePreRelationalData(PUB existing, PUB vo) {

    }

    /**
     * Save any relational data for this publisher i.e. script roles
     *
     * NOTE: this logic will be executed in a database transaction.
     *
     * @param existing - null on insert
     * @param vo
     */
    public void saveRelationalData(PUB existing, PUB vo) {

    }

    /**
     * Delete any relational data for the publisher.
     *
     * NOTE: this logic will be executed in a database transaction.
     * @param vo
     */
    public void deleteRelationalData(PUB vo) {

    }

    /**
     * Delete any relational data for the publisher.
     *
     * NOTE: this logic will be executed in a database transaction.
     * @param vo
     */
    public void deletePostRelationalData(PUB vo) {

    }

    /**
     * Load in relational data for the publisher
     *
     * @param vo
     */
    public void loadRelationalData(PUB vo) {

    }

}
