package com.serotonin.m2m2.util;

import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.Date;

import org.joda.time.DateTime;
import org.junit.Test;

import com.serotonin.timer.CronExpression;

public class CronPatternTest {

	@Test
	public void testCronPattern() {
//		Date timeStart = new Date(2016, 3, 3);
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
