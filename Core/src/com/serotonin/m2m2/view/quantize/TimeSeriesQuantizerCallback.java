package com.serotonin.m2m2.view.quantize;

import java.util.List;

import org.jfree.data.time.TimeSeries;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.util.chart.ImageChartUtils;
import com.serotonin.m2m2.view.stats.IValueTime;

/**
 * @deprecated use quantize2 classes instead.
 */
@Deprecated
public class TimeSeriesQuantizerCallback implements DataQuantizerCallback {
    private final TimeSeries ts;

    public TimeSeriesQuantizerCallback(TimeSeries ts) {
        this.ts = ts;
    }

    @Override
    public void quantizedData(List<IValueTime> vts) {
        for (IValueTime vt : vts)
            ImageChartUtils.addMillisecond(ts, vt.getTime(), DataValue.numberValue(vt.getValue()));
    }
}
