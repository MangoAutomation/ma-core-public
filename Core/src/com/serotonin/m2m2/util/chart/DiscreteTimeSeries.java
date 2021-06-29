/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util.chart;

import java.awt.Paint;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;

/**
 * @author Matthew Lohbihler
 */
public class DiscreteTimeSeries {
    private final String name;
    private final TextRenderer textRenderer;
    private final Paint paint;
    private final Stroke stroke;
    private List<IValueTime> valueTimes = new ArrayList<IValueTime>();
    private List<ValueDescription> valueDescriptions = new ArrayList<ValueDescription>();

    public DiscreteTimeSeries(String name, TextRenderer textRenderer) {
        this(name, textRenderer, null, null);
    }

    public DiscreteTimeSeries(String name, TextRenderer textRenderer, Paint paint, Stroke stroke) {
        this.name = name;
        this.textRenderer = textRenderer;
        this.paint = paint;
        this.stroke = stroke;
    }

    public DiscreteTimeSeries plainCopy() {
        DiscreteTimeSeries copy = new DiscreteTimeSeries(name, textRenderer);
        copy.valueTimes = valueTimes;
        copy.valueDescriptions = valueDescriptions;
        return copy;
    }

    @SuppressWarnings("unchecked")
    public void addValueTime(IValueTime pvt) {
        DataValue value = pvt.getValue();
        if (value == null)
            return;

        valueTimes.add(pvt);

        if (getValueIndex(value) == -1) {
            String text;
            if (textRenderer == null)
                text = value.toString();
            else
                text = textRenderer.getText(value, TextRenderer.HINT_FULL);

            ValueDescription vd = new ValueDescription((Comparable<Object>) value, text);

            int index = Collections.binarySearch(valueDescriptions, vd);
            valueDescriptions.add(-index - 1, vd);
        }
    }

    public String getName() {
        return name;
    }

    public Paint getPaint() {
        return paint;
    }

    public Stroke getStroke() {
        return stroke;
    }

    public List<IValueTime> getValueTimes() {
        return valueTimes;
    }

    public int getDiscreteValueCount() {
        return valueDescriptions.size();
    }

    public int getValueIndex(DataValue value) {
        for (int i = 0; i < valueDescriptions.size(); i++) {
            if (valueDescriptions.get(i).getValue().equals(value))
                return i;
        }
        return -1;
    }

    public String getValueText(int index) {
        return valueDescriptions.get(index).getDescription();
    }

    class ValueDescription implements Comparable<ValueDescription> {
        private final Comparable<Object> value;
        private final String description;

        public ValueDescription(Comparable<Object> value, String description) {
            this.value = value;
            this.description = description;
        }

        @Override
        public int compareTo(ValueDescription that) {
            return value.compareTo(that.getValue());
        }

        public Comparable<Object> getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }
}
