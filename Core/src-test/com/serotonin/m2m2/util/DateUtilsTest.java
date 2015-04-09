/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.joda.time.DateTime;
import org.junit.Test;

import com.serotonin.m2m2.Common.TimePeriods;

/**
 * @author Terry Packer
 *
 */
public class DateUtilsTest {

	@Test
	public void testPreviousMonth(){
		
		DateTime baseTime = new DateTime().minusYears(5);
		long now = new Date().getTime();
		while(baseTime.isBefore(now)){
		
			int periodType = TimePeriods.MONTHS;
			int count = 1;
			
			long to = DateUtils.truncate(baseTime.getMillis(), periodType);
			long from = DateUtils.minus(to, periodType, count);
			
			DateTime startOfMonth = baseTime.minusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay();
			assertEquals(startOfMonth.getMillis(), from);
	
			DateTime endOfMonth = startOfMonth.plusMonths(1);
			assertEquals(endOfMonth.getMillis(), to);
			
			//Print results neatly for fun
			Double days = (double)(endOfMonth.getMillis() - startOfMonth.getMillis())/(1000D*60D*60D*24D);
			System.out.println(startOfMonth.toString("MMM-yyyy") + " Duration in Days: " + days);
			
			//Step along
			baseTime = baseTime.plusMonths(1);
		}

	}
	
}
