/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.dataSource.EventDataSource;
import com.serotonin.m2m2.rt.dataSource.PointLocatorRT;
import com.serotonin.m2m2.rt.dataSource.PollingDataSource;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.PointLocatorVO;

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
 *
 * DWR is how MA realizes AJAX. More information is available here: http://directwebremoting.org/dwr/index.html.
 *
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
 * <dt>Editing JSP</dt>
 * <dd>The page on which a user can edit an instance of the data source</dd>
 *
 * <dt>Subclass of {@link DataSourceEditDwr}</dt>
 * <dd>The server-side AJAX controller for the editing JSP</dd>
 *
 * <dt>Optional</dt>
 * <dd>Online documentation files, translation files (strongly recommended), data source commissioning tools.</dd>
 * </dl>
 *
 * @author Matthew Lohbihler
 */
abstract public class DataSourceDefinition extends ModuleElementDefinition implements DwrClassHolder {
    /**
     * Used by MA core code to create a new data source instance as required. Should not be used by client code.
     */
    public final DataSourceVO<?> baseCreateDataSourceVO() {
        DataSourceVO<?> ds = createDataSourceVO();
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
    abstract protected DataSourceVO<?> createDataSourceVO();

    /**
     * The path to the data source editing page relative to the module.
     *
     * @return the relative path to the editing page.
     */
    abstract public String getEditPagePath();

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

    public String getStatusPagePath() {
        return null;
    }
}
