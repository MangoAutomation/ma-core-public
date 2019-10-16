/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.dwr.PublisherEditDwr;
import com.serotonin.m2m2.web.mvc.rest.v1.model.publisher.AbstractPublishedPointModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.publisher.AbstractPublisherModel;

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
abstract public class PublisherDefinition extends ModuleElementDefinition implements DwrClassHolder {
    /**
     * Used by MA core code to create a new publisher instance as required. Should not be used by client code.
     */
    public PublisherVO<?> baseCreatePublisherVO() {
        PublisherVO<? extends PublishedPointVO> pub = createPublisherVO();
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
    abstract protected PublisherVO<? extends PublishedPointVO> createPublisherVO();

    /**
     * The path to the publisher editing page relative to the module.
     *
     * @return the relative path to the editing page.
     */
    abstract public String getEditPagePath();

    /**
     * The class of the DWR page with which the publisher editing page communicates. This class will be instantiated
     * upon startup and registered as a DWR proxy.
     *
     * @return the class of the DWR proxy.
     */
    @Override
    abstract public Class<?> getDwrClass();

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
     * Return the model class for the Publisher
     * @return
     */
    public abstract Class<? extends AbstractPublisherModel<?,?>> getPublisherModelClass();

    /**
     * Return the model class for the Published points
     * @return
     */
    public abstract Class<? extends AbstractPublishedPointModel<?>> getPublishedPointModelClass();


}
