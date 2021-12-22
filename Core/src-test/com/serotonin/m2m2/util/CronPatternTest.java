package com.serotonin.m2m2.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.joda.time.DateTime;
import org.junit.Test;

import com.serotonin.timer.CronExpression;

public class CronPatternTest {

	/**
	 * Test to show that CronExpression is invalid for some dates,
	 * specifically time 0  See RAD-1453.
	 *
	 * Since this test fails it is disabled until we fix it.
	 * @throws ParseException
	 */
	//@Test
	public void testTimeZero() throws ParseException {
		CronExpression ce = new CronExpression("0/5 * * * * ?");
		Date timeStart = new Date(0);
		long time = ce.getNextValidTimeAfter(timeStart).getTime();

		//Time should be 5000
		assertEquals(5000, time);
	}

	/**
	 * Test to show that the Spring CronExpression is valid at time 0 See RAD-1453.
	 *
	 * @throws ParseException
	 */
	@Test
	public void testTimeZeroSpring() throws ParseException {
		org.springframework.scheduling.support.CronExpression ce = org.springframework.scheduling.support.CronExpression.parse("0/5 * * * * ?");

		LocalDateTime ldt = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
		LocalDateTime next = ce.next(ldt);

		assertTrue(ldt.isBefore(next));

		LocalDateTime expected = ldt.plusSeconds(5);
		assertEquals(expected, next);
	}

	@Test
	public void testCronPattern() {
		DateTime baseTime = DateTime.parse("2017-03-02T00:00:00.000");
		Date timeStart = new Date(baseTime.getMillis());
		long timeEnd = timeStart.getTime() + 86400000;
		long timeStep = 1000;
		boolean fail = false;
		try {
			CronExpression ce = new CronExpression("0/5 * * * * ?");
			while(timeStart.getTime() < timeEnd) {
				try {
					long time = ce.getNextValidTimeAfter(timeStart).getTime();
					if(!(timeStart.getTime() < time)) {
						System.out.println("For time " + timeStart.getTime() + " time " + time + " is not after it");
						fail = true;
					}
					long difference = time - timeStart.getTime();
					long expectedDifference = 5000 - (timeStart.getTime() % 5000);
					if(expectedDifference == 0)
						expectedDifference = 5000;
					if(difference != expectedDifference)
						fail("For time " + timeStart.getTime() + " next time " + time + " does not seem right");
					
				} catch(NullPointerException e) {
					System.out.println("For time " + timeStart.getTime() + " next time was null");
					fail = true;
				}
				
				timeStart.setTime(timeStart.getTime() + timeStep);
			}
		} catch(ParseException e) {
			fail("Bad test: " + e.getMessage());
		}
		if(fail)
			fail();
	}
}
