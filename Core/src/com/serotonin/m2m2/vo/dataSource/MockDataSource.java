package com.serotonin.m2m2.vo.dataSource;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.event.EventTypeVO;

/**
 * Useful for things like validation.
 */
public class MockDataSource extends DataSourceVO<MockDataSource> {
    private static final long serialVersionUID = 1L;

    @Override
    public TranslatableMessage getConnectionDescription() {
        return null;
    }

    @Override
    public PointLocatorVO createPointLocator() {
        return null;
    }

    @Override
    public DataSourceRT createDataSourceRT() {
        return null;
    }

    @Override
    public ExportCodes getEventCodes() {
        return null;
    }

    @Override
    protected void addEventTypes(List<EventTypeVO> eventTypes) {
        // no op
    }

    @Override
    protected void addPropertiesImpl(List<TranslatableMessage> list) {
        // no op
    }

    @Override
    protected void addPropertyChangesImpl(List<TranslatableMessage> list, MockDataSource from) {
        // no op
    }
}
