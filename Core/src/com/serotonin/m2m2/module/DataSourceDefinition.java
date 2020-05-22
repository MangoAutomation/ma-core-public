/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.dataSource.EventDataSource;
import com.serotonin.m2m2.rt.dataSource.PointLocatorRT;
import com.serotonin.m2m2.rt.dataSource.PollingDataSource;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.PointLocatorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * A data source is the means by which MA gets values into a data point, and writes set point values back to source
 * equipment (if possible).
 *
 * MA's primary object is a data point. There are many common attributes of points, such as data type, engineering
 * units, logging, etc. But points differ in how they get their values, and these differences are encapsulated in a
 * "point locator". For example, a Modbus point locator consists of a slave id, register range, offset, etc. An HTTP
 * retriever point consists of a regular expression that extracts data from a web page. The data source is (in this
 * sense, but not literally) the container for the point locators, and has appropriate attributes.
 *
 * Data points, locators, and data sources (and other concepts as well) are split into VO (value object) and RT
 * (runtime) objects. The VO represents the configuration of the object, i.e. what is saved to the database. The RT is
 * the code (usually a thread or scheduled process) that does the actual work of connecting or listening or whatever is
 * appropriate for the given protocol. When you are editing a data source, you are changing the attributes of the VO.
 * When you start the data source, you are providing a VO to an RT, and then running the RT.
 * *
 * When creating a data source for MA, the following components are required:
 * <dl>
 * <dt>Subclass of {@link DataSourceVO}</dt>
 * <dd>A configuration object of a data source</dd>
 *
 * <dt>Subclass of {@link PointLocatorVO}</dt>
 * <dd>A configuration object of a point locator</dd>
 *
 * <dt>Subclass of {@link DataSourceRT}</dt>
 * <dd>A runtime implementation of the data source. Convenience base classes are available including
 * {@link PollingDataSource} and {@link EventDataSource}</dd>
 *
 * <dt>Subclass of {@link PointLocatorRT}</dt>
 * <dd>A runtime implementation of the point locator</dd>
 *
 * <dt>Optional</dt>
 * <dd>Online documentation files, translation files (strongly recommended), data source commissioning tools.</dd>
 * </dl>
 *
 * @author Matthew Lohbihler
 */
abstract public class DataSourceDefinition<T extends DataSourceVO> extends ModuleElementDefinition {
    /**
     * Used by MA core code to create a new data source instance as required. Should not be used by client code.
     * TODO Mango 4.0 Review setting the definition in the constructor
     * TODO Mango 4.0 Add abstract method getDataSourceClass() in DataSourceDefinition that returns the concrete Class of the DataSourceVO, add default method (which can be overridden) that creates a DataSourceVO by calling getDataSourceClass().newInstance()
     */
    public final T baseCreateDataSourceVO() {
        T ds = createDataSourceVO();
        ds.setDefinition(this);
        return ds;
    }

    /**
     * The available start priorities for data sources.
     */
    public enum StartPriority {
        FIRST, NORMAL, LAST;
    }

    /**
     * An internal identifier for this type of data source. Must be unique within an MA instance, and is recommended
     * to be unique inasmuch as possible across all modules.
     *
     * @return the data source type name.
     */
    abstract public String getDataSourceTypeName();

    /**
     * A reference to a human readable and translatable brief description of the data source. Key reference values in
     * i18n.properties files. Descriptions are used in drop down select boxes, and so should be as brief as possible.
     *
     * @return the reference key to the data source short description.
     */
    abstract public String getDescriptionKey();

    /**
     * Create and return an instance of the data source.
     *
     * @return a new instance of the data source.
     */
    abstract protected T createDataSourceVO();

    /**
     * Validate a new data source
     * @param response
     * @param ds
     * @param user
     */
    abstract public void validate(ProcessResult response, T ds, PermissionHolder user);

    /**
     * Validate a data source about to be updated
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
     * Override this method as required. The start priority determines the order in which data sources are started by
     * MA. By default this method returns NORMAL, and should only be overridden when absolutely necessary. For
     * example, the Meta data source has a start priority of LAST because it depends up on the points from other data
     * sources.
     *
     * @return the data source's {@link StartPriority} value.
     */
    public StartPriority getStartPriority() {
        return StartPriority.NORMAL;
    }

    /**
     * If the module is uninstalled, delete any data sources of this type. If this method is overridden, be sure to call
     * super.uninstall so that this code still runs.
     */
    @Override
    public void postRuntimeManagerTerminate(boolean uninstall) {
        if(uninstall)
            DataSourceDao.getInstance().deleteDataSourceType(getDataSourceTypeName());
    }

    /**
     * Save any relational data for this data source i.e. script roles prior
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
     * Save any relational data for this data source i.e. script roles
     *
     * NOTE: this logic will be executed in a database transaction.
     *
     * @param existing - null on insert
     * @param vo
     */
    public void saveRelationalData(T existing, T vo) {

    }

    /**
     * Delete any relational data for the data source.
     *
     * NOTE: this logic will be executed in a database transaction.
     * @param vo
     */
    public void deleteRelationalData(T vo) {

    }

    /**
     * Delete any relational data for the data source.
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

    /**
     * Validate a point locator, with access to the parent data point and data source both guaranteed to be non-null
     *
     * @param response
     * @param dpvo
     * @param dsvo
     * @param user - permission holder saving the point
     */
    abstract public void validate(ProcessResult response, DataPointVO dpvo, DataSourceVO dsvo, PermissionHolder user);

    /**
     * Validate a point locator with access to a pre-existing point, when updating.
     * @param repsponse
     * @param existing
     * @param dpvo
     * @param dsvo
     * @param user
     */
    public void validate(ProcessResult response, DataPointVO existing, DataPointVO dpvo, DataSourceVO dsvo, PermissionHolder user) {
        validate(response, dpvo, dsvo, user);
    }

    /**
     * Save any relational data for this point locator i.e. script roles
     *
     * @param existing - null on inserts
     * @param vo
     */
    public void saveRelationalData(DataPointVO existing, DataPointVO vo) {

    }

    /**
     * Delete any relational data for this point locator
     * @param vo
     */
    public void deleteRelationalData(DataPointVO vo) {

    }

    /**
     * Load in relational data fort this point locator
     * @param vo
     */
    public void loadRelationalData(DataPointVO vo) {

    }

}
