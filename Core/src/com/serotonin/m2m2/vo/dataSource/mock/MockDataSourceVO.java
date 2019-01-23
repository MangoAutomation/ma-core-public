package com.serotonin.m2m2.vo.dataSource.mock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataSource.MockDataSourceRT;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.PollingDataSourceVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.MockDataSourceModel;

/**
 * Useful for things like validation and testing
 */
public class MockDataSourceVO extends PollingDataSourceVO<MockDataSourceVO> {
    
    public MockDataSourceVO(){
    	    this.setDefinition(new MockDataSourceDefinition());
    }
    

    @Override
    public TranslatableMessage getConnectionDescription() {
        return new TranslatableMessage("common.default", "Mock Data Source");
    }

    @Override
    public MockPointLocatorVO createPointLocator() {
        return new MockPointLocatorVO();
    }

    @Override
    public MockDataSourceRT createDataSourceRT() {
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
    public MockDataSourceModel asModel(){
    	return new MockDataSourceModel(this);
    }


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.AbstractVO#getDao()
	 */
	@Override
	protected AbstractDao<MockDataSourceVO> getDao() {
		return null;
	}
    
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
    }
    private void readObject(ObjectInputStream in) throws IOException {
        in.readInt();
    }


    @Override
    public int getPollAbortedExceptionEventId() {
        return -1;
    }
}
