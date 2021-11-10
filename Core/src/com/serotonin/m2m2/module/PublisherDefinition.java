/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.infiniteautomation.mango.db.tables.PublishedPoints;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.vo.IDataPoint;
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
 * <dt>Optional</dt>
 * <dd>Online documentation files, translation files (strongly recommended), publisher commissioning tools.</dd>
 * </dl>
 *
 * @author Matthew Lohbihler
 */
abstract public class PublisherDefinition<PUB extends PublisherVO> extends ModuleElementDefinition {

    @Autowired
    @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)
    private ObjectMapper dbMapper;

    protected final PublishedPoints table = PublishedPoints.PUBLISHED_POINTS;

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
     */
    abstract public void validate(ProcessResult response, PUB pub);

    /**
     * Validate a publisher about to be updated
     *  override as necessary
     * @param response
     * @param existing
     * @param vo
     */
    public void validate(ProcessResult response, PUB existing, PUB vo) {
        validate(response, vo);
    }

    /**
     * Validate a new published point
     *
     * @param response
     * @param vo
     * @param publisher
     */
    abstract public void validate(ProcessResult response, PublishedPointVO vo, PublisherVO publisher);

    /**
     * Validate a published point to be updated
     *  override as necessary
     *
     * @param response
     * @param existing
     * @param vo
     * @param publisher
     */
    public void validate(ProcessResult response, PublishedPointVO existing, PublishedPointVO vo, PublisherVO publisher) {
        validate(response, vo, publisher);
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

    /**
     * Create a new published point vo with all base field initialized
     * @return
     */
    @NonNull
    public <T extends PublishedPointVO> T createPublishedPointVO(PUB publisher, IDataPoint dataPoint) {
        T vo = newPublishedPointVO();
        vo.setPublisherId(publisher.getId());
        vo.setPublisherXid(publisher.getXid());
        vo.setDataPointId(dataPoint.getId());
        vo.setDataPointXid(dataPoint.getXid());
        vo.setPublisherTypeName(getPublisherTypeName());
        return vo;
    }

    /**
     * Create a point with the publisher id, type and data point id set
     * @param publisherId
     * @param dataPointId
     * @param <T>
     * @return
     */
    @NonNull
    public <T extends PublishedPointVO> T createPublishedPointVO(int publisherId, int dataPointId) {
        T vo = newPublishedPointVO();
        vo.setPublisherId(publisherId);
        vo.setDataPointId(dataPointId);
        vo.setPublisherTypeName(getPublisherTypeName());
        return vo;
    }

    /**
     * Instantiate a new published point VO for use when creating new points
     * @return
     */
    @NonNull
    abstract protected <T extends PublishedPointVO> T newPublishedPointVO();

    /**
     * Create the module specific settings to store in database
     * @return - JSON for database
     */
    abstract public String createPublishedPointDbData(PublishedPointVO vo) throws JsonProcessingException;

    /**
     * Map settings back into PublishedPoint when extracting from database
     * @param data - JSON from database (won't be null)
     * @return the point
     */
    abstract public PublishedPointVO mapPublishedPointDbData(PublishedPointVO vo, @NonNull String data) throws JsonProcessingException;

    /**
     * Store any module defined point settings into this JsonNode
     * @param point
     * @return
     */
    public String createDbJson(PublishedPointVO point) {
        return null;
    }


    /**
     * Get a writer for serializing JSON
     * @return
     */
    protected ObjectWriter getObjectWriter(Class<?> type) {
        return dbMapper.writerFor(type);
    }

    /**
     * Get a reader for use de-serializing JSON
     * @return
     */
    protected ObjectReader getObjectReader(Class<?> type) {
        return dbMapper.readerFor(type);
    }
}
