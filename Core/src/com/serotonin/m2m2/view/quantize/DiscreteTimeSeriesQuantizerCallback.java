package com.serotonin.m2m2.view.quantize;

import java.util.List;

import com.serotonin.m2m2.util.chart.DiscreteTimeSeries;
import com.serotonin.m2m2.view.stats.IValueTime;

/**
 * @deprecated use quantize2 classes instead.
 */
@Deprecated
public class DiscreteTimeSeriesQuantizerCallback implements DataQuantizerCallback {
    private final DiscreteTimeSeries dts;

    public DiscreteTimeSeriesQuantizerCallback(DiscreteTimeSeries dts) {
        this.dts = dts;
    }

    @Override
    public void quantizedData(List<IValueTime> vts) {
        for (IValueTime vt : vts)
            dts.addValueTime(vt);
    }
}
