package com.serotonin.m2m2.view.stats;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;

public class StartsAndRuntimeListTest {
    // TODO

    public static void main(String[] args) {
        {
            MultistateValue startValue = new MultistateValue(3);
            List<PointValueTime> values = new ArrayList<PointValueTime>();
            values.add(new PointValueTime(1, 2000));
            values.add(new PointValueTime(2, 3000));
            values.add(new PointValueTime(2, 5000));
            values.add(new PointValueTime(3, 8000));
            values.add(new PointValueTime(1, 9000));
            values.add(new PointValueTime(3, 10000));
            values.add(new PointValueTime(3, 12000));
            values.add(new PointValueTime(2, 16000));

            System.out.println(new StartsAndRuntimeList(1000, 21000, startValue, values, null));
            System.out.println(new StartsAndRuntimeList(1500, 26000, startValue, values, null));
            System.out.println(new StartsAndRuntimeList(1000, 21000, (DataValue) null, values, null));
            System.out.println(new StartsAndRuntimeList(1500, 26000, (DataValue) null, values, null));

            System.out.println(new StartsAndRuntimeList(0, 30000, (DataValue) null, new ArrayList<PointValueTime>(),
                    null));
            System.out.println(new StartsAndRuntimeList(0, 30000, startValue, new ArrayList<PointValueTime>(), null));
        }

        System.out.println();

        {
            BinaryValue startValue = BinaryValue.ONE;
            List<PointValueTime> values = new ArrayList<PointValueTime>();
            values.add(new PointValueTime(true, 2000));
            values.add(new PointValueTime(false, 3000));
            values.add(new PointValueTime(false, 5000));
            values.add(new PointValueTime(false, 8000));
            values.add(new PointValueTime(true, 9000));
            values.add(new PointValueTime(true, 10000));
            values.add(new PointValueTime(false, 12000));
            values.add(new PointValueTime(true, 16000));

            System.out.println(new StartsAndRuntimeList(1000, 21000, startValue, values, null));
            System.out.println(new StartsAndRuntimeList(1500, 26000, startValue, values, null));
            System.out.println(new StartsAndRuntimeList(1000, 21000, (DataValue) null, values, null));
            System.out.println(new StartsAndRuntimeList(1500, 26000, (DataValue) null, values, null));

            System.out.println(new StartsAndRuntimeList(0, 30000, (DataValue) null, new ArrayList<PointValueTime>(),
                    null));
            System.out.println(new StartsAndRuntimeList(0, 30000, startValue, new ArrayList<PointValueTime>(), null));
        }
    }
}
