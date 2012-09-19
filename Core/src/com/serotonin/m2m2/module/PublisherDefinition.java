/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.dwr.PublisherEditDwr;

/**
 * A publisher allows point values to be written (or published) to targets outside of the m2m2 instance. Typically any
 * data points can be provided to a publisher regardless of the data source.
 * 
 * When creating a publisher for m2m2, the following components are required:
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
abstract public class PublisherDefinition extends ModuleElementDefinition {
    /**
     * Used by m2m2 core code to create a new publisher instance as required. Should not be used by client code.
     */
    public PublisherVO<?> baseCreatePublisherVO() {
        PublisherVO<? extends PublishedPointVO> pub = createPublisherVO();
        pub.setDefinition(this);
        return pub;
    }

    /**
     * An internal identifier for this type of publisher. Must be unique within an m2m2 instance, and is recommended to
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
    abstract public Class<?> getDwrClass();
}
