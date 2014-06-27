/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

package com.serotonin.m2m2.emport;

import java.util.List;

import com.serotonin.m2m2.emport.SpreadsheetEmporter.CellType;



/**
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */
public abstract class AbstractSheetEmporter {
	
	boolean useNames;
    
    /**
     * Returns the name of the sheet
     * @return 
     */
    protected abstract String getSheetName();
    
    /**
     * Get the column Headers to use
     * @return
     */
    protected abstract String[] getHeaders();
    
    /**
     * Gets the type of each column for checking when importing
     * @return
     */
    protected abstract CellType[] getColumnTypes();
    
    /**
     * Get the Column Widths to use
     * nb. 256 = one character
     * @return
     */
    protected abstract int[] getColumnWidths();

    /**
     * Import the row data
     * @param rowNum
     * @param rowData
     * @throws SpreadsheetException
     */
    protected abstract void importRow(List<Object> rowData) throws SpreadsheetException;
    
    /**
     * Exports all the data into a list of objects
     * @return
     */
    protected abstract List<List<Object>> exportRows();
    

}