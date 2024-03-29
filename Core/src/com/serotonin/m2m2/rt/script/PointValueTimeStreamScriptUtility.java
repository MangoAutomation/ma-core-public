/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.db.query.QueryCancelledException;
import com.infiniteautomation.mango.db.query.WideCallback;
import com.infiniteautomation.mango.quantize.AbstractPointValueTimeQuantizer;
import com.infiniteautomation.mango.quantize.AnalogStatisticsQuantizer;
import com.infiniteautomation.mango.quantize.BucketCalculator;
import com.infiniteautomation.mango.quantize.StartsAndRuntimeListQuantizer;
import com.infiniteautomation.mango.quantize.StatisticsGeneratorQuantizerCallback;
import com.infiniteautomation.mango.quantize.TemporalAmountBucketCalculator;
import com.infiniteautomation.mango.quantize.ValueChangeCounterQuantizer;
import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.statistics.AnalogStatistics;
import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;
import com.infiniteautomation.mango.statistics.ValueChangeCounter;
import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.Rollups;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.pointvalue.TimeOrder;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.view.stats.StatisticsGenerator;
import com.serotonin.m2m2.vo.DataPointVO;

/*
 * Class to make it easier to stream values in scripts
 * Will query a time range with a rollup period and a rollup, with callbacks always occuring in time order
 *  and bookeneds being included is optional.
 */
public class PointValueTimeStreamScriptUtility extends ScriptUtility {

    private static final Logger LOG = LoggerFactory.getLogger(PointValueTimeStreamScriptUtility.class);
    public static final String CONTEXT_KEY = "PointValueQuery";

    private final DataPointService dataPointService;

    @Autowired
    public PointValueTimeStreamScriptUtility(MangoJavaScriptService service, PermissionService permissionService, DataPointService dataPointService) {
        super(service, permissionService);
        this.dataPointService = dataPointService;
    }

    @Override
    public String getContextKey() {
        return CONTEXT_KEY;
    }

    /**
     * Query and lookup points by id
     */
    public void query(List<Integer> ids, long from, long to, boolean bookend, ScriptPointValueTimeCallback callback) {
        List<DataPointVO> vos = new ArrayList<>(ids.size());
        for(Integer id : ids) {
            if(id == null) {
                continue;
            }else {
                vos.add(dataPointService.get(id));
            }
        }
        queryUsingPoints(vos, from, to, bookend, callback);
    }

    /**
     * Query using points directly
     */
    public void queryUsingPoints(List<DataPointVO> vos, long from, long to, boolean bookend, ScriptPointValueTimeCallback callback) {
        PointValueTimeStream pvts = new PointValueTimeStream(vos, from, to, bookend, callback);
        pvts.execute();
    }

    /**
     * Rollup query using point ids to lookup data points
     */
    public void rollupQuery(List<Integer> ids, long from, long to, ScriptPointValueRollupCallback callback, int rollupType, int rollupPeriods, int rollupPeriodType) throws QueryCancelledException, ScriptPermissionsException {
        List<DataPointVO> vos = new ArrayList<>(ids.size());
        for(Integer id : ids) {
            if(id == null) {
                continue;
            }else {
                vos.add(dataPointService.get(id));
            }
        }
        rollupQueryUsingPoints(vos, from, to, callback, rollupType, rollupPeriods, rollupPeriodType);
    }

    /**
     * Use data points for rollups
     */
    public void rollupQueryUsingPoints(List<DataPointVO> vos, long from, long to,
                                       ScriptPointValueRollupCallback callback, int rollupType,
                                       int rollupPeriods, int rollupPeriodType)
            throws QueryCancelledException, ScriptPermissionsException {

        TemporalAmount rollupPeriod = TimePeriods.toTemporalAmount(rollupPeriodType, rollupPeriods);
        RollupsStream rs = new RollupsStream(vos, from, to, callback, rollupType, rollupPeriod);
        rs.execute();
    }

    class PointValueTimeStream {
        final List<DataPointVO> vos;
        final long from;
        final long to;
        final boolean bookend;
        final ScriptPointValueTimeCallback callback;
        Integer limit = null;

        public PointValueTimeStream(List<DataPointVO> vos, long from, long to, boolean bookend, ScriptPointValueTimeCallback callback) {
            this.vos = vos;
            this.from = from;
            this.to = to;
            this.bookend = bookend;
            this.callback = callback;
        }

        public void execute() {
            if(bookend) {
                PointValueDao pointValueDao = Common.getBean(PointValueDao.class);
                pointValueDao.wideBookendQueryCombined(vos, from, to, limit, callback);
            }
            else
                Common.getBean(PointValueDao.class).getPointValuesCombined(vos, from, to, limit, TimeOrder.ASCENDING, callback);
        }
    }

    class RollupsStream implements WideCallback<IdPointValueTime> {
        final List<DataPointVO> vos;
        Integer limit = null;
        final ScriptPointValueRollupCallback callback;
        final ZonedDateTime from;
        final ZonedDateTime to;
        final int rollup;
        final TemporalAmount rollupPeriod;
        final Map<Integer, DataPointStatisticsQuantizer<? extends StatisticsGenerator>> quantizerMap;
        boolean warned = false;

        public RollupsStream(List<DataPointVO> vos, long from, long to, ScriptPointValueRollupCallback callback, int rollup, TemporalAmount rollupPeriod) {
            this.vos = vos;
            Instant instantFrom = Instant.ofEpochMilli(from);
            Instant instantTo = Instant.ofEpochMilli(to);
            ZoneId zid = ZoneId.of(TimeZone.getDefault().getID());
            this.from = ZonedDateTime.ofInstant(instantFrom, zid);
            this.to = ZonedDateTime.ofInstant(instantTo, zid);
            this.callback = callback;
            this.rollup = rollup;
            this.rollupPeriod = rollupPeriod;
            quantizerMap = new HashMap<Integer, DataPointStatisticsQuantizer<? extends StatisticsGenerator>>();
        }

        public void execute() throws QueryCancelledException, ScriptPermissionsException {
            createQuantizerMap();
            PointValueDao pointValueDao = Common.getBean(PointValueDao.class);
            pointValueDao.wideBookendQueryCombined(vos, from.toInstant().toEpochMilli(), to.toInstant().toEpochMilli(), null, this);
            //Fast forward to end to fill any gaps at the end
            for(DataPointStatisticsQuantizer<?> quant : this.quantizerMap.values())
                if(!quant.isDone())
                    quant.done();
        }

        @Override
        public void firstValue(IdPointValueTime value, boolean bookend) {
            DataPointStatisticsQuantizer<?> quantizer = this.quantizerMap.get(value.getSeriesId());
            quantizer.firstValue(value, bookend);
        }

        @Override
        public void accept(IdPointValueTime value) {
            DataPointStatisticsQuantizer<?> quantizer = this.quantizerMap.get(value.getSeriesId());
            quantizer.accept(value);

        }

        @Override
        public void lastValue(IdPointValueTime value, boolean bookend) {
            DataPointStatisticsQuantizer<?> quantizer = this.quantizerMap.get(value.getSeriesId());
            quantizer.lastValue(value, bookend);
        }

        public void quantizedStatistics(AnalogStatistics statisticsGenerator) {
            switch(rollup) {
                case Rollups.ALL :
                    callback.item(statisticsGenerator);
                    break;
                case Rollups.AVERAGE :
                    callback.item(statisticsGenerator.getAverage());
                    break;
                case Rollups.FIRST :
                    callback.item(statisticsGenerator.getFirstValue());
                    break;
                case Rollups.LAST :
                    callback.item(statisticsGenerator.getLastValue());
                    break;
                case Rollups.START :
                    callback.item(statisticsGenerator.getStartValue());
                    break;
                case Rollups.MINIMUM :
                    callback.item(statisticsGenerator.getMinimumValue());
                    break;
                case Rollups.MAXIMUM :
                    callback.item(statisticsGenerator.getMaximumValue());
                    break;
                case Rollups.DELTA :
                    callback.item(statisticsGenerator.getDelta());
                    break;
                case Rollups.COUNT :
                    callback.item(statisticsGenerator.getCount());
                    break;
                case Rollups.INTEGRAL :
                    callback.item(statisticsGenerator.getIntegral());
                    break;
                case Rollups.ACCUMULATOR :
                case Rollups.NONE :
                default :
                    if(!warned) {
                        LOG.warn("Unsupported rollup in script runtime");
                        warned = true;
                    }
                    break;
            }
        }

        public void quantizedStatistics(StartsAndRuntimeList statisticsGenerator) {
            switch(rollup) {
                case Rollups.ALL :
                    callback.item(statisticsGenerator);
                    break;
                case Rollups.FIRST :
                    callback.item(statisticsGenerator.getFirstValue());
                    break;
                case Rollups.LAST :
                    callback.item(statisticsGenerator.getLastValue());
                    break;
                case Rollups.START :
                    callback.item(statisticsGenerator.getStartValue());
                    break;
                case Rollups.COUNT :
                    callback.item(statisticsGenerator.getCount());
                    break;
                case Rollups.MINIMUM :
                case Rollups.MAXIMUM :
                case Rollups.DELTA :
                case Rollups.INTEGRAL :
                case Rollups.AVERAGE :
                case Rollups.ACCUMULATOR :
                case Rollups.NONE :
                default :
                    if(!warned) {
                        LOG.warn("Unsupported rollup in script runtime");
                        warned = true;
                    }
                    break;
            }
        }

        public void quantizedStatistics(ValueChangeCounter statisticsGenerator) {
            switch(rollup) {
                case Rollups.ALL :
                    callback.item(statisticsGenerator);
                    break;
                case Rollups.FIRST :
                    callback.item(statisticsGenerator.getFirstValue());
                    break;
                case Rollups.LAST :
                    callback.item(statisticsGenerator.getLastValue());
                    break;
                case Rollups.START :
                    callback.item(statisticsGenerator.getStartValue());
                    break;
                case Rollups.COUNT :
                    callback.item(statisticsGenerator.getCount());
                    break;
                case Rollups.MINIMUM :
                case Rollups.MAXIMUM :
                case Rollups.DELTA :
                case Rollups.INTEGRAL :
                case Rollups.AVERAGE :
                case Rollups.ACCUMULATOR :
                case Rollups.NONE :
                default :
                    if(!warned) {
                        LOG.warn("Unsupported rollup in script runtime");
                        warned = true;
                    }
                    break;
            }
        }

        private void createQuantizerMap() throws ScriptPermissionsException {
            for(DataPointVO vo : vos) {
                if(vo == null)
                    continue;

                DataPointStatisticsQuantizer<?> quantizer;
                switch(vo.getPointLocator().getDataType()) {
                    case ALPHANUMERIC:
                        quantizer = new ValueChangeCounterDataPointQuantizer(vo, getBucketCalculator(), this);
                        break;
                    case BINARY:
                    case MULTISTATE:
                        quantizer = new StartsAndRuntimeListDataPointQuantizer(vo, getBucketCalculator(), this);
                        break;
                    case NUMERIC:
                        quantizer = new AnalogStatisticsDataPointQuantizer(vo, getBucketCalculator(), this);
                        break;
                    default:
                        throw new RuntimeException("Unknown Data Type: " + vo.getPointLocator().getDataType());
                }

                this.quantizerMap.put(vo.getId(), quantizer);
            }
        }

        BucketCalculator getBucketCalculator(){
            return new TemporalAmountBucketCalculator(from, to, rollupPeriod);
        }
    }

    abstract class DataPointStatisticsQuantizer<T extends StatisticsGenerator> implements StatisticsGeneratorQuantizerCallback<T>, WideCallback<IdPointValueTime> {

        protected final RollupsStream callback;
        protected AbstractPointValueTimeQuantizer<T> quantizer;
        protected final DataPointVO vo;
        protected boolean open;
        protected boolean done;

        public DataPointStatisticsQuantizer(DataPointVO vo, RollupsStream callback) {
            this.vo = vo;
            this.callback = callback;
            this.open = false;
            this.done = false;
        }

        @Override
        public void firstValue(IdPointValueTime value, boolean bookend) {
            quantizer.firstValue(value, bookend);
            open = true;
        }

        @Override
        public void accept(IdPointValueTime value) {
            quantizer.accept(value);
        }

        @Override
        public void lastValue(IdPointValueTime value, boolean bookend) {
            quantizer.lastValue(value, bookend);
            quantizer.done();
            this.done = true;
        }

        public boolean isDone() {
            return done;
        }

        public void done() throws QueryCancelledException {
            quantizer.done();
            done = true;
        }
    }

    class ValueChangeCounterDataPointQuantizer extends DataPointStatisticsQuantizer<ValueChangeCounter> {
        public ValueChangeCounterDataPointQuantizer(DataPointVO vo, BucketCalculator calc, RollupsStream callback) {
            super(vo, callback);
            quantizer = new ValueChangeCounterQuantizer(calc, this);
        }

        @Override
        public void quantizedStatistics(ValueChangeCounter statisticsGenerator) throws QueryCancelledException {
            this.callback.quantizedStatistics(statisticsGenerator);
        }
    }

    class StartsAndRuntimeListDataPointQuantizer extends DataPointStatisticsQuantizer<StartsAndRuntimeList> {
        public StartsAndRuntimeListDataPointQuantizer(DataPointVO vo, BucketCalculator calc, RollupsStream callback) {
            super(vo, callback);
            quantizer = new StartsAndRuntimeListQuantizer(calc, this);
        }

        @Override
        public void quantizedStatistics(StartsAndRuntimeList statisticsGenerator) throws QueryCancelledException {
            this.callback.quantizedStatistics(statisticsGenerator);
        }
    }

    class AnalogStatisticsDataPointQuantizer extends DataPointStatisticsQuantizer<AnalogStatistics> {
        public AnalogStatisticsDataPointQuantizer(DataPointVO vo, BucketCalculator calc, RollupsStream callback) {
            super(vo, callback);
            quantizer = new AnalogStatisticsQuantizer(calc, this);
        }

        @Override
        public void quantizedStatistics(AnalogStatistics statisticsGenerator) throws QueryCancelledException {
            this.callback.quantizedStatistics(statisticsGenerator);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{ \n");
        builder.append("query([dataPointIds], long from, long to, boolean bookend, ScriptPointValueTimeCallback callback): void, \n");
        builder.append("rollupQuery([dataPointIds], long from, long to, ScriptPointValueRollupCallback callback, int rollupType, int rollupPeriods, int rollupPeriodType): void \n");
        builder.append("}\n");
        return builder.toString();
    }
}
