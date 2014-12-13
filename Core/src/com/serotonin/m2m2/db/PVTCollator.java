package com.serotonin.m2m2.db;

import java.util.List;

import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.MultiValueTime;

public class PVTCollator implements MappedRowCallback<IdPointValueTime> {
    private final List<Integer> pointIds;
    private final MappedRowCallback<MultiValueTime> callback;
    private MultiValueTime mvt = null;
    private int nextIndex = 0;

    public PVTCollator(List<Integer> pointIds, MappedRowCallback<MultiValueTime> callback) {
        this.pointIds = pointIds;
        this.callback = callback;
    }

    @Override
    public void row(IdPointValueTime pvt, int index) {
        if (mvt == null || mvt.getTime() < pvt.getTime()) {
            done();
            mvt = new MultiValueTime(new Object[pointIds.size()], pvt.getTime());
        }
        mvt.getValues()[pointIds.indexOf(pvt.getDataPointId())] = pvt.getValue();
    }

    public void done() {
        if (mvt != null)
            callback.row(mvt, ++nextIndex);
    }
}
