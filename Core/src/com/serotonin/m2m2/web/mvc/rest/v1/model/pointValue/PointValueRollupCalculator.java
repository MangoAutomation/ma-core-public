/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.pointValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DaoRegistry;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.util.DateUtils;
import com.serotonin.m2m2.view.quantize2.AbstractDataQuantizer;
import com.serotonin.m2m2.view.quantize2.AnalogStatisticsQuantizer;
import com.serotonin.m2m2.view.quantize2.BucketCalculator;
import com.serotonin.m2m2.view.quantize2.BucketsBucketCalculator;
import com.serotonin.m2m2.view.quantize2.StatisticsGeneratorQuantizerCallback;
import com.serotonin.m2m2.view.quantize2.TimePeriodBucketCalculator;
import com.serotonin.m2m2.view.quantize2.ValueChangeCounterQuantizer;
import com.serotonin.m2m2.view.stats.AnalogStatistics;
import com.serotonin.m2m2.view.stats.ValueChangeCounter;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.pair.LongPair;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriod;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriodType;

/**
 * @author Terry Packer
 *
 */
public class PointValueRollupCalculator {

	private static final Log LOG = LogFactory.getLog(PointValueRollupCalculator.class);
	
	private DataPointVO vo;
	private RollupEnum rollup;
	private TimePeriod period;
	private long from;
	private long to;
	
	public PointValueRollupCalculator(DataPointVO vo, RollupEnum rollup, TimePeriod period, long from, long to){
		this.vo = vo;
		this.rollup = rollup;
		this.period = period;
		this.from = from;
		this.to = to;
	}
	
	/**
	 * Calculate statistics, if TimePeriod is null the entire range will be used
	 * @return
	 */
	public List<PointValueTime> calculate(){
		
		final List<PointValueTime> pvts = new ArrayList<PointValueTime>();
        // Determine the start and end times.
        if (from == -1) {
            // Get the start and end from the point values table.
            LongPair lp = DaoRegistry.pointValueDao.getStartAndEndTime(Collections.singletonList(vo.getId()));
            from = lp.getL1();
            to = lp.getL2();
        }

        DateTime startTime = new DateTime(from);
        //Round off the start period if we are using periodic rollup
        if(period != null)
        	startTime = DateUtils.truncateDateTime(startTime, TimePeriodType.convertFrom(this.period.getType()), this.period.getPeriods());
        DateTime endTime = new DateTime(to);

        // Determine the start and end values. This is important for
        // properly calculating average.
        PointValueTime startPvt = DaoRegistry.pointValueDao.getPointValueAt(vo.getId(), from);
        DataValue startValue = PointValueTime.getValue(startPvt);
        PointValueTime endPvt = DaoRegistry.pointValueDao.getPointValueAt(vo.getId(), to);
        DataValue endValue = PointValueTime.getValue(endPvt);
        
        BucketCalculator bc;
        if(this.period == null){
        	bc = new BucketsBucketCalculator(startTime, endTime, 1);
        }else{
        	bc = new TimePeriodBucketCalculator(startTime, endTime, TimePeriodType.convertFrom(this.period.getType()), this.period.getPeriods());
        }
        final AbstractDataQuantizer quantizer;
        if (vo.getPointLocator().getDataTypeId() == DataTypes.NUMERIC) {
            quantizer = new AnalogStatisticsQuantizer(bc, startValue,
                    new StatisticsGeneratorQuantizerCallback<AnalogStatistics>() {
                        @Override
                        public void quantizedStatistics(AnalogStatistics statisticsGenerator, boolean done) {

                            if (statisticsGenerator.getCount() > 0 || !done) {
                                switch(rollup){
	                                case AVGERAGE:
	                                	Double avg = statisticsGenerator.getAverage();
	                                	if(avg == null)
	                                		avg = 0.0D;
	                                	pvts.add(new PointValueTime(
	                                			avg,
	                                			statisticsGenerator.getPeriodEndTime() - 1));
	                                break;
	                                case MINIMUM:
	                                	Double min = statisticsGenerator.getMinimumValue();
	                                	if(min != null)
	                                		pvts.add(new PointValueTime(
	                                			min,
	                                			statisticsGenerator.getMinimumTime()));
	                                break;
	                                case MAXIMUM:
	                                	Double max = statisticsGenerator.getMaximumValue();
	                                	if(max != null)
	                                		pvts.add(new PointValueTime(
	                                			max,
	                                			statisticsGenerator.getMaximumTime()));
	                                break;
	                                case SUM:
	                                	Double sum = statisticsGenerator.getSum();
	                                	if(sum == null)
	                                		sum = 0.0D;
	                                	pvts.add(new PointValueTime(
	                                			sum,
	                                			statisticsGenerator.getPeriodEndTime() - 1));
	                                    
	                                break;
	                                case FIRST:
	                                	Double first = statisticsGenerator.getFirstValue();
	                                	if(first != null)
	                                		pvts.add(new PointValueTime(
	                                			first,
	                                    		statisticsGenerator.getFirstTime()));
	                                break;
	                                case LAST:
	                                	Double last = statisticsGenerator.getLastValue();
	                                	if(last != null)
	                                		pvts.add(new PointValueTime(
	                                			last,
	                                			statisticsGenerator.getLastTime()));
	                                break;
	                                case COUNT:
	                                	pvts.add(new PointValueTime(
	                                			statisticsGenerator.getCount(),
	                                			statisticsGenerator.getPeriodEndTime() - 1));
	                                break;
	                                default:
	                                	throw new ShouldNeverHappenException("Unknown Rollup type" + rollup);
                                }
                            }
                        }
                    });
        }
        else {
            if (!rollup.nonNumericSupport()) {
                LOG.info("Invalid non-numeric rollup type: " + rollup);
                rollup = RollupEnum.FIRST; //Default to first
            }


            quantizer = new ValueChangeCounterQuantizer(bc, startValue,
                    new StatisticsGeneratorQuantizerCallback<ValueChangeCounter>() {
                        @Override
                        public void quantizedStatistics(ValueChangeCounter statisticsGenerator, boolean done) {
                            if (statisticsGenerator.getCount() > 0 || !done) {
                                switch(rollup){
                                case FIRST:
                                	DataValue first = statisticsGenerator.getFirstValue();
                                	if(first != null)
                                		pvts.add(new PointValueTime(
                                			first,
                                    		statisticsGenerator.getFirstTime()));
                                break;
                                case LAST:
                                	DataValue last = statisticsGenerator.getLastValue();
                                	if(last != null)
                                		pvts.add(new PointValueTime(
                                			last,
                                			statisticsGenerator.getLastTime()));
                                break;
                                case COUNT:
                                	pvts.add(new PointValueTime(
                                			statisticsGenerator.getCount(),
                                			statisticsGenerator.getPeriodEndTime() - 1));
                                break;
                                default:
                                	throw new ShouldNeverHappenException("Unsupported Non-numerical Rollup type: " + rollup);
                           
                                }
                            }
                        }
                    });
        }

        //Finally Make the call to get the data and quantize it
        DaoRegistry.pointValueDao.getPointValuesBetween(vo.getId(), from, to,
                new MappedRowCallback<PointValueTime>() {
                    @Override
                    public void row(PointValueTime pvt, int row) {
                        quantizer.data(pvt);
                    }
                });
        quantizer.done(endValue);
        
        
        return pvts;
	}
	
	
}
