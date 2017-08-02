/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.chart;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.rt.dataImage.PointValueFacade;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.ImplDefinition;
import com.serotonin.m2m2.view.stats.AnalogStatistics;
import com.serotonin.m2m2.view.stats.StartsAndRuntimeList;
import com.serotonin.m2m2.view.stats.ValueChangeCounter;
import com.serotonin.m2m2.vo.DataPointVO;

public class StatisticsChartRenderer extends TimePeriodChartRenderer {
    private static ImplDefinition definition = new ImplDefinition("chartRendererStats", "STATS",
            "chartRenderer.statistics", new int[] { DataTypes.ALPHANUMERIC, DataTypes.BINARY, DataTypes.MULTISTATE,
                    DataTypes.NUMERIC });

    public static ImplDefinition getDefinition() {
        return definition;
    }

    @Override
    public String getTypeName() {
        return definition.getName();
    }

    @Override
    public ImplDefinition getDef() {
        return definition;
    }
    
    @JsonProperty
    private boolean includeSum;
    
    public StatisticsChartRenderer() {
        // no op
    }

    public StatisticsChartRenderer(int timePeriod, int numberOfPeriods, boolean includeSum) {
        super(timePeriod, numberOfPeriods);
        this.includeSum = includeSum;
    }

    public boolean isIncludeSum() {
        return includeSum;
    }

    public void setIncludeSum(boolean includeSum) {
        this.includeSum = includeSum;
    }

    @Override
    public void addDataToModel(Map<String, Object> model, DataPointVO point) {
        long startTime = getStartTime();
        long endTime = startTime + getDuration();

        PointValueFacade pointValueFacade = new PointValueFacade(point.getId());
        List<PointValueTime> values = pointValueFacade.getPointValuesBetween(startTime, endTime);

        PointValueTime startVT = null;
        if (!values.isEmpty()) {
            startVT = pointValueFacade.getPointValueBefore(startTime);
        }

        // Generate statistics on the values.
        int dataTypeId = point.getPointLocator().getDataTypeId();

        if (values.size() > 0) {
            if (dataTypeId == DataTypes.BINARY || dataTypeId == DataTypes.MULTISTATE) {
                // Runtime stats
                StartsAndRuntimeList stats = new StartsAndRuntimeList(startTime, endTime, startVT, values);
                model.put("start", startVT != null ? startTime : stats.getFirstTime());
                model.put("end", endTime);
                model.put("startsAndRuntimes", stats.getData());
            }
            else if (dataTypeId == DataTypes.NUMERIC) {
                AnalogStatistics stats = new AnalogStatistics(startTime, endTime, startVT, values);
                model.put("start", startVT != null ? startTime : stats.getFirstTime());
                model.put("end", endTime);
                model.put("minimum", stats.getMinimumValue());
                model.put("minTime", stats.getMinimumTime());
                model.put("maximum", stats.getMaximumValue());
                model.put("maxTime", stats.getMaximumTime());
                model.put("average", stats.getAverage());
                if (includeSum)
                    model.put("sum", stats.getSum());
                model.put("count", stats.getCount());
                model.put("noData", stats.getAverage() == null);
                model.put("integral", stats.getIntegral());
            }
            else if (dataTypeId == DataTypes.ALPHANUMERIC) {
                ValueChangeCounter stats = new ValueChangeCounter(startTime, endTime, startVT, values);
                model.put("changeCount", stats.getChanges());
            }
        }

        model.put("logEntries", values.size());
    }

    @Override
    public String getChartSnippetFilename() {
        return "statsChart.jsp";
    }
    
    //
    // /
    // / Serialization
    // /
    //
    private static final long serialVersionUID = -1;
    private static final int version = 2;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeBoolean(includeSum);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1)
            includeSum = true;
        else if (ver == 2)
            includeSum = in.readBoolean();
    }
}
