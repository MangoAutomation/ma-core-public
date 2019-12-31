package com.serotonin.m2m2.vo.dataSource.mock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataSource.MockDataSourceRT;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.PollingDataSourceVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;

/**
 * Useful for things like validation and testing
 */
public class MockDataSourceVO extends PollingDataSourceVO<MockDataSourceVO> {
    
    public MockDataSourceVO(){
        this.setDefinition(new MockDataSourceDefinition());
    }
    
    public MockDataSourceVO(String xid, String name) {
        this.xid = xid;
        this.name = name;
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
        super.addEventTypes(eventTypes);
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
