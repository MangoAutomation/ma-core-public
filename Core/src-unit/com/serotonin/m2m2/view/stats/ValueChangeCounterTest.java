package com.serotonin.m2m2.view.stats;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * No, this is not intended to be a JUnit test, but it shouldn't be too hard to make it into one.
 * 
 * @author Matthew
 */
public class ValueChangeCounterTest {
    public static void main(String[] args) {
        AlphanumericValue startValue = new AlphanumericValue("asdf");
        List<PointValueTime> values = new ArrayList<PointValueTime>();
        values.add(new PointValueTime("asdf", 2000));
        values.add(new PointValueTime("zxcv", 3000));
        values.add(new PointValueTime("qwer", 4000));
        values.add(new PointValueTime("wert", 5000));
        values.add(new PointValueTime("wert", 6000));
        values.add(new PointValueTime("erty", 8000));

        validate(new ValueChangeCounter(0, 10000, startValue, values), 6, 4);
        validate(new ValueChangeCounter(0, 10000, startValue, values), 6, 4);
        validate(new ValueChangeCounter(0, 10000, (DataValue) null, values), 6, 5);
        validate(new ValueChangeCounter(0, 10000, (DataValue) null, values), 6, 5);
        validate(new ValueChangeCounter(0, 10000, (DataValue) null, new ArrayList<PointValueTime>()), 0, 0);
        validate(new ValueChangeCounter(0, 10000, startValue, new ArrayList<PointValueTime>()), 0, 0);
    }

    static void validate(ValueChangeCounter s, int count, int changes) {
        if (s.getCount() != count)
            System.out.println("Count mismatch: expected: " + count + ", got: " + s.getCount());
        if (s.getChanges() != changes)
            System.out.println("Changes mismatch: expected: " + changes + ", got: " + s.getChanges());
    }
}
