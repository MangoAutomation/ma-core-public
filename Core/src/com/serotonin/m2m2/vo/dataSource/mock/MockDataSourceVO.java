package com.serotonin.m2m2.vo.dataSource.mock;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.dataSource.MockDataSourceRT;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.PointLocatorVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractDataSourceModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.MockDataSourceModel;

/**
 * Useful for things like validation and testing
 */
public class MockDataSourceVO extends DataSourceVO<MockDataSourceVO> {
    private static final long serialVersionUID = 1L;
    
    public MockDataSourceVO(){
    	this.setDefinition(new MockDataSourceDefinition());
    }
    

    @Override
    public TranslatableMessage getConnectionDescription() {
        return new TranslatableMessage("common.default", "Mock Data Source");
    }

    @Override
    public PointLocatorVO createPointLocator() {
        return new MockPointLocatorVO();
    }

    @Override
    public DataSourceRT createDataSourceRT() {
        return new MockDataSourceRT(this);
    }

    @Override
    public ExportCodes getEventCodes() {
        return new ExportCodes();
    }

    @Override
    protected void addEventTypes(List<EventTypeVO> eventTypes) {
        // no op
    }
    
    @Override
    public AbstractDataSourceModel<MockDataSourceVO> asModel(){
    	return new MockDataSourceModel(this);
    }
    
}
