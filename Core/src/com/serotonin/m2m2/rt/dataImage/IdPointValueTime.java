/*
    Mango - Open Source M2M - http://mango.serotoninsoftware.com
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc.
    @author Matthew Lohbihler

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.serotonin.m2m2.rt.dataImage;

import com.infiniteautomation.mango.util.Functions;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

public class IdPointValueTime extends PointValueTime implements SeriesIdTime {
    private static final long serialVersionUID = 1L;

    private final int seriesId;

    public IdPointValueTime(int seriesId, DataValue value, long time) {
        super(value, time);
        this.seriesId = seriesId;
    }

    @Override
    public int getSeriesId() {
        return seriesId;
    }

    @Override
    public String toString() {
        return "IdPointValueTime(" + seriesId + "=" + getValue() + "@" + Functions.getFullMilliSecondTime(getTime()) + ")";
    }
}
