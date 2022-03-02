/*
 * Copyright (C) 2022 RadixIot LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.event.detectors;

import static com.serotonin.m2m2.Common.TimePeriods.MINUTES;
import static com.serotonin.m2m2.Common.TimePeriods.SECONDS;
import static com.serotonin.m2m2.vo.DataPointVO.IntervalLoggingTypes.INSTANT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.spring.service.EventDetectorsService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.event.detectors.MultistateStateEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.PointEventDetectorDefinition;
import com.serotonin.m2m2.rt.EventManager;
import com.serotonin.m2m2.rt.EventManagerImpl;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.UserEventListener;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.maint.BackgroundProcessing;
import com.serotonin.m2m2.rt.maint.BackgroundProcessingImpl;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.event.detector.MultistateStateDetectorVO;

public class EventDetectorAlarmTest extends MangoTestBase {


    public static final String TEST_DETECTOR_1 = "Test_detector_1";
    private EventDetectorAlarmTest.EventDetectorEventListener listener;
    private MockDataSourceVO dsVo;

    private DataSourceService dataSourceService;
    private DataPointService dataPointService;
    private EventDetectorsService eventDetectorsService;

    private Map<Integer, Long> riseTimesMap = new ConcurrentHashMap<>();
    private Map<Integer, Long> rtnTimesMap = new ConcurrentHashMap<>();


    private RunAs runAs;

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    static {
        Configurator.initialize(new DefaultConfiguration());
        //Configurator.setLevel("com.serotonin.m2m2.rt.event.detectors.EventDetectorAlarmTest", Level.TRACE);
        //Configurator.setLevel("com.serotonin.m2m2.rt.event.detectors", Level.TRACE);
    }


    @Before
    public void configure() {
        runAs = Common.getBean(RunAs.class);
        //retryRule = new RetryRule(5, false, false, RetryRule.FailBehaviour.ANY);

        this.listener = new EventDetectorEventListener();
        Common.eventManager.addUserEventListener(this.listener);

        dataSourceService = Common.getBean(DataSourceService.class);
        dataPointService = Common.getBean(DataPointService.class);
        eventDetectorsService = Common.getBean(EventDetectorsService.class);

        dsVo = createMockDataSource(true);
        dsVo.setTypeDescriptionString("META");



    }

    @After
    public void tearDown() {
        // removes the data source from the RTM
        Common.eventManager.removeUserEventListener(this.listener);
        dataSourceService.delete(dsVo.getId());
    }

    @Test
    public void testEventDetectorAfterXS() {
        testEventDetectorAssertAfter(10  , 10, SECONDS);
    }

    @Test
    public void testEventDetectorAfterS() {
        testEventDetectorAssertAfter(100  , 10, SECONDS);
    }

    @Test
    public void testEventDetectorAfterM() {
        testEventDetectorAssertAfter(1000, 10, SECONDS);
    }

    @Test
    public void testEventDetectorAfterL() {
        testEventDetectorAssertAfter(10000, 10, SECONDS);
    }


    //@Test
    public void testStateDetectorRT(){


        //Create data point
        DataPointVO dp = createDataPoint(true);

        PointEventDetectorDefinition<MultistateStateDetectorVO> multistateDef = ModuleRegistry.getEventDetectorDefinition(MultistateStateEventDetectorDefinition.TYPE_NAME);
        MultistateStateDetectorVO detector = multistateDef.baseCreateEventDetectorVO(dp);
        detector.setXid(TEST_DETECTOR_1);
        detector.setName("Detector_for_"+dp.getName());
        detector.setState(1);
        detector.setAlarmLevel(AlarmLevels.CRITICAL);
        detector.setDuration(10);
        detector.setDurationType(SECONDS);
        detector.setInverted(false);
        detector.setStates(null);
        detector.setData(null);

        //create the RT
        MultistateStateDetectorRT rt = (MultistateStateDetectorRT) detector.createRuntime();

        //initialize the RT
        rt.initialize();


        //timer.fastForwardTo( timer.currentTimeMillis() + Common.getMillis(SECONDS, 10));
        AtomicBoolean testing = new AtomicBoolean(true);
        AtomicBoolean pauseTimer = new AtomicBoolean(false);


        new Thread(() ->{
            while(testing.get()){
                while (pauseTimer.get()) {}
                timer.fastForwardTo( timer.currentTimeMillis() + Common.getMillis(SECONDS, 10));;
                pauseTimer.set(true);

                if(log.isTraceEnabled())
                    log.trace("Timer: " + timer.currentTimeMillis());

            }

        }).start();
        AtomicInteger raiseEventCounter = new AtomicInteger();
        new Thread(() ->{
            runAs.runAs(runAs.systemSuperadmin(), () -> {
                PointValueTime newPvt;
                PointValueTime oldPvt =  new PointValueTime(0,timer.currentTimeMillis());
                int value = 0;
                for (int eventId = 0; eventId <= 202 ; eventId++) {
                    while (!pauseTimer.get()) {}
                    if(eventId < 200){
                        value = eventId % 2;
                        if(value == 1)
                            raiseEventCounter.getAndIncrement();

                    }
                    else {
                        //set the value to 0 to RTN last couple of loops
                        value =0;
                    }
                    newPvt = new PointValueTime(value ,timer.currentTimeMillis() - 5);
                    rt.pointChanged(oldPvt, newPvt);
                    oldPvt = newPvt;
                    pauseTimer.set(false);
                }

                testing.set(false);
                pauseTimer.set(false);

            });
        }).start();

        while(testing.get()){
            //loop until the other threads finish working
        }
        rt.terminate();

        assertEquals("Failed to rise the expected events.",raiseEventCounter.get() - listener.notRtnApplicableEvents, listener.events.size());

        if(log.isDebugEnabled()) {
            for (Map.Entry<Integer, EventTracker> entry : listener.events.entrySet()) {
                if (!entry.getValue().isActive()) {
                    log.debug("Event = " + entry.getKey() +
                            " " + entry.getValue().getRoseEvt().getAlarmLevel() +
                            ", isActive = " + entry.getValue().getRtnEvt().isActive() +
                            ", roseTime = " + entry.getValue().getRoseEvt().getActiveTimestamp() +
                            ", return to normal time = " + entry.getValue().getRtnEvt().getRtnTimestamp() +
                            ", up time = " + (entry.getValue().getRtnEvt().getRtnTimestamp() - entry.getValue().getRoseEvt().getActiveTimestamp()));
                } else {
                    log.debug("Event = " + entry.getKey() +
                            " " + entry.getValue().getRoseEvt().getAlarmLevel() +
                            ", isActive = " + entry.getValue().isActive() +
                            ", roseTime = " + entry.getValue().getRoseEvt().getActiveTimestamp() +
                            ", return to normal time = nope, up time = " + (timer.currentTimeMillis() - entry.getValue().getRoseEvt().getActiveTimestamp()));
                }
            }
        }

        for (Map.Entry<Integer, EventTracker> entry: listener.events.entrySet()) {
            assertNotNull(entry.getValue());
            assertFalse("At least one event never returned to normal", entry.getValue().isActive());
        }


    }

    private void testEventDetectorAssertAfter(int events  , int eventDetectorDuration, int eventDetectorDurationType) {
        //Create data point
        DataPointVO dp = createDataPoint(true);

        DataPointRT rt = getRuntimeManager().getDataPoint(dp.getId());

        //Set the value to initialize the detector
        rt.updatePointValue(new PointValueTime(0, timer.currentTimeMillis()));

        //Add a detector
        MultistateStateDetectorVO ed = createEventDetector(dp, eventDetectorDuration, eventDetectorDurationType);

        //Get it again with the new detector added
        rt = getRuntimeManager().getDataPoint(dp.getId());

        long duration = Common.getMillis(eventDetectorDurationType, eventDetectorDuration);
        for (int eventId = 1; eventId <= events ; eventId++) {
            timer.fastForwardTo(timer.currentTimeMillis() + duration);

            //change the value to rise the alarm.
            ensureSetPointValue(rt, new PointValueTime(1, timer.currentTimeMillis()));

            timer.fastForwardTo( timer.currentTimeMillis() + duration);

            //Set value back to normal
            ensureSetPointValue(rt, new PointValueTime(0, timer.currentTimeMillis()));
        }

        assertFalse("There are " + listener.events.size() + " events in the map", listener.events.isEmpty());

        for (Map.Entry<Integer, EventTracker> entry: listener.events.entrySet()) {
            if(!entry.getValue().isActive()) {
                log.info("Event = {} {}, isActive={}, raiseTime={}, rtnTime={}, upTime={}ms",
                        entry.getKey(),
                        entry.getValue().getRoseEvt().getAlarmLevel(),
                        entry.getValue().getRtnEvt().isActive(),
                        new Date(entry.getValue().getRoseEvt().getActiveTimestamp()),
                        new Date(entry.getValue().getRtnEvt().getRtnTimestamp()),
                        (entry.getValue().getRtnEvt().getRtnTimestamp() - entry.getValue().getRoseEvt().getActiveTimestamp()));
            }else{
                log.info("Event = {} {}, isActive={}, raiseTime={}, rtnTime=N/A, upTime={}ms",
                        entry.getKey(),
                        entry.getValue().getRoseEvt().getAlarmLevel(),
                        entry.getValue().isActive(),
                        new Date(entry.getValue().getRoseEvt().getActiveTimestamp()),
                        (timer.currentTimeMillis() - entry.getValue().getRoseEvt().getActiveTimestamp() ));
            }
        }
        for (Map.Entry<Integer, EventTracker> entry: listener.events.entrySet()) {
            assertNotNull(entry.getValue());
            assertFalse("At least one event never returned to normal", entry.getValue().isActive());
        }

        assertEquals("Failed to raise the expected events.",events, listener.events.size());
    }


    protected DataPointVO createDataPoint(boolean enabled) {
        //Create point locator
        MockPointLocatorVO plVo = new MockPointLocatorVO(DataType.MULTISTATE, true);

        //Setup Data Point VO
        DataPointVO dpVo = new DataPointVO();
        dpVo.setName("Test");
        dpVo.setXid("DP_1");
        dpVo.setEnabled(enabled);
        dpVo.setLoggingType(DataPointVO.LoggingTypes.ON_CHANGE);
        dpVo.setIntervalLoggingPeriodType(MINUTES);
        dpVo.setIntervalLoggingType(INSTANT);

        dpVo.setPointLocator(plVo);
        dpVo.setDataSourceId(dsVo.getId());

        return dataPointService.insert(dpVo);
    }

    /**
     * Create a detector for state 1
     */
    protected MultistateStateDetectorVO createEventDetector(DataPointVO dataPointVO, int duration, int durationType) {
        PointEventDetectorDefinition<MultistateStateDetectorVO> multistateDef = ModuleRegistry.getEventDetectorDefinition(MultistateStateEventDetectorDefinition.TYPE_NAME);
        MultistateStateDetectorVO detector = multistateDef.baseCreateEventDetectorVO(dataPointVO);
        detector.setXid(TEST_DETECTOR_1);
        detector.setName("Detector_for_"+dataPointVO.getName());
        detector.setState(1);
        detector.setAlarmLevel(AlarmLevels.CRITICAL);
        detector.setDuration(duration);
        detector.setDurationType(durationType);
        detector.setInverted(false);
        detector.setStates(null);
        detector.setData(null);
        eventDetectorsService.insertAndReload(detector, true);
        return detector;
    }

    /**
     * Save a value and ensure the event detector has received the event
     */
    protected void ensureSetPointValue(DataPointRT rt, PointValueTime value) {
        ConfirmedDataPointListener l = new ConfirmedDataPointListener();
        Common.runtimeManager.addDataPointListener(rt.getId(), l);
        rt.setPointValue(value, null);
        int retries = 2000;//2 seconds is enough
        while(retries > 0) {
            if(l.updated.get()){
                /*if (log.isDebugEnabled())
                    log.debug("Data point updated, time is: " + timer.currentTimeMillis() + " newValue" + l.value);*/
                break;
            }
            else{
                try{Thread.sleep(1);}catch(Exception e) {}
                //timer.fastForwardTo(timer.currentTimeMillis() + 1);
            }
            retries--;
        }
        Common.runtimeManager.removeDataPointListener(rt.getId(), l);
        if(retries == 0)
            fail("did not receive updated event");
    }


    protected RuntimeManager getRuntimeManager() {
        return Common.getBean(RuntimeManager.class);
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        return new MockMangoLifecycle(modules) {
            @Override
            protected RuntimeManager getRuntimeManager() {
                return EventDetectorAlarmTest.this.getRuntimeManager();
            }

            @Override
            protected EventManager getEventManager() {
                return new EventManagerImpl();
            }

            @Override
            protected BackgroundProcessing getBackgroundProcessing() {
                return new BackgroundProcessingImpl();
            }
        };
    }

    class EventTracker {
        private long eventRoseTime = 0;
        private long eventRtnTime = 0;
        private EventInstance roseEvt = null;
        private EventInstance rtnEvt = null;

        public EventTracker(long eventRoseTime, EventInstance roseEvt) {
            this.eventRoseTime = eventRoseTime;
            this.roseEvt = roseEvt;
        }

        public boolean isActive(){
            if (eventRtnTime != 0){
                return false;
            }else
                return true;
        }

        public long getTimeOnActive(){
            if (isActive()){
                return timer.currentTimeMillis() - eventRoseTime;
            }
            return eventRtnTime - eventRoseTime;
        }

        public long getEventRoseTime() {
            return eventRoseTime;
        }

        public long getEventRtnTime() {
            return eventRtnTime;
        }

        public void setEventRtnTime(long eventRtnTime) {
            this.eventRtnTime = eventRtnTime;
        }

        public EventInstance getRoseEvt() {
            return roseEvt;
        }

        public EventInstance getRtnEvt() {
            return rtnEvt;
        }

        public void setRtnEvt(EventInstance rtnEvt) {
            this.rtnEvt = rtnEvt;
        }

    }

    class EventDetectorEventListener implements UserEventListener {

        ConcurrentHashMap<Integer, EventTracker> events = new ConcurrentHashMap<Integer, EventTracker>();
        int notRtnApplicableEvents = 0;
        private int rtnCounter = 0;
        private int deactivatedCounter = 0;


        @Override
        public int getUserId() {
            return 1;
        }

        @Override
        public void raised(EventInstance evt) {
            if(evt.getEventType() instanceof DataPointEventType) {
                if (log.isTraceEnabled()) {
                    log.trace("Listener: Event {} is a {} time is {}", evt.getId(), evt.getAlarmLevel(), new Date(evt.getActiveTimestamp()));
                }
                if (evt.isRtnApplicable()) {
                    assertNull("The event " + evt.getId() + " has already risen",
                            events.put(evt.getId(), new EventTracker(timer.currentTimeMillis(), evt)));
                }
            }

        }

        @Override
        public void returnToNormal(EventInstance evt) {
            if(evt.getEventType() instanceof DataPointEventType) {
                if (log.isTraceEnabled())
                    log.trace("Listener: Return to normal {} time is {}", evt.getId(), new Date(evt.getRtnTimestamp()));

                EventTracker tempTracker = events.remove(evt.getId());
                if (tempTracker != null) {
                    tempTracker.setEventRtnTime(timer.currentTimeMillis());
                    tempTracker.setRtnEvt(evt);
                    assertNull(events.put(evt.getId(), tempTracker));
                    rtnCounter++;
                }
            }
        }

        @Override
        public void deactivated(EventInstance evt) {
            //Will happen after every test
            log.warn("Listener: event "+ evt.getId() +
                    ", Level: "+ evt.getAlarmLevel() +
                    ", Message: "+ evt.getMessageString() );
            deactivatedCounter++;
        }

        @Override
        public void acknowledged(EventInstance evt) {
            fail("Should not happen");
        }

        public void assertRaised(int count) {
            assertEquals(count, events.size());
        }
        public void assertRtn(int count) {
            assertEquals(count, rtnCounter);
        }
        public void assertDeactivated(int count) {
            assertEquals(count, deactivatedCounter);
        }
        public void assertNoEvents() {
            assertRaised(0);
            assertRtn(0);
            assertDeactivated(0);
        }
    }

    class ConfirmedDataPointListener implements DataPointListener {

        AtomicBoolean changed = new AtomicBoolean();
        AtomicBoolean updated = new AtomicBoolean();
        AtomicBoolean logged = new AtomicBoolean();
        PointValueTime value = null;

        @Override
        public String getListenerName() {
            return "TEST";
        }

        @Override
        public void pointInitialized() {

        }

        @Override
        public void pointUpdated(PointValueTime newValue) {
            value = newValue;
            updated.set(true);
        }

        @Override
        public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
            this.changed.set(true);
        }

        @Override
        public void pointSet(PointValueTime oldValue, PointValueTime newValue) {

        }

        @Override
        public void pointBackdated(PointValueTime value) {

        }

        @Override
        public void pointTerminated(DataPointVO vo) {

        }

        @Override
        public void pointLogged(PointValueTime value) {
            logged.set(true);
        }

    }

}
