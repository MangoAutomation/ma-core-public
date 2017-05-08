/*
 * Created on 3-Aug-2006
 */
package com.serotonin.web.util;

import java.util.List;

/**
 * @deprecated use something else
 * @author x
 */
@Deprecated
public class PaginatedData<T> {
    private final List<T> data;
    private final int rowCount;

    public PaginatedData(List<T> data, int rowCount) {
        this.data = data;
        this.rowCount = rowCount;
    }

    public List<T> getData() {
        return data;
    }

    public int getRowCount() {
        return rowCount;
    }
}
