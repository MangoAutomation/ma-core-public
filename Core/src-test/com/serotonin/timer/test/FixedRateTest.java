package com.serotonin.timer.test;

import java.util.Date;

import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.RealTimeTimer;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.TimeSource;
import com.serotonin.timer.TimerTask;

public class FixedRateTest {
    public static void main(String[] args) {
        RealTimeTimer timer = new RealTimeTimer();
        timer.setTimeSource(new TimeSource() {
            int count = 10;

            @Override
            public long currentTimeMillis() {
                long time;
                if (count-- > 0)
                    time = System.currentTimeMillis();
                else
                    time = System.currentTimeMillis() - 1000001000;
                System.out.println("Returning " + new Date(time));
                return time;
            }
        });

        timer.init();

        long period = 5000;
        long delay = period - (System.currentTimeMillis() % period);
        FixedRateTrigger trigger = new FixedRateTrigger(delay, period);
        TimerTask task = new TimerTask(trigger, "Test Timer Task") {
            @Override
            public void run(long runtime) {
                System.out.println("executed at " + new Date(runtime));
            }

			@Override
			public void rejected(RejectedTaskReason reason) {
				System.out.println("task rejected: " + reason.getDescription());
			}
        };

        timer.schedule(task);

        try {
            synchronized (timer) {
                timer.wait(30000);
            }
        }
        catch (InterruptedException e) {
            // no op
        }

        timer.cancel();
    }
}
