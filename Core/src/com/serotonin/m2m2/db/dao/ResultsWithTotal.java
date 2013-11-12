/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

package com.serotonin.m2m2.db.dao;

import java.util.List;

/**
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */
public class ResultsWithTotal {
    final int total;
    final List<?> results;
    
    public ResultsWithTotal(List<?> results, int total) {
        this.results = results;
        this.total = total;
    }
    
    public int getTotal() {
        return total;
    }

    public List<?> getResults() {
        return results;
    }
}
