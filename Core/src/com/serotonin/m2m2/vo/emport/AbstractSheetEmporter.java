/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

package com.serotonin.m2m2.vo.emport;

import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.serotonin.m2m2.vo.emport.SpreadsheetEmporter.CellType;

/**
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */
public abstract class AbstractSheetEmporter {
    protected boolean useNames;
	protected int rowNum = 0;
    protected Sheet sheet;
    protected CellStyle dateStyle;
    protected CellStyle percentStyle;
    
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
    protected abstract void importRow(Row rowData) throws SpreadsheetException;
    
    /**
     * Exports all the data into a list of objects
     * @return
     */
    protected abstract List<List<Object>> exportRows();

	/**
	 * @param dateStyle
	 */
	public void setDateStyle(CellStyle dateStyle) {
		this.dateStyle = dateStyle;
		
	}

	/**
	 * @param percentStyle
	 */
	public void setPercentStyle(CellStyle percentStyle) {
		this.percentStyle = percentStyle;
		
	}

	/**
	 * @param currentSheet
	 */
	public void setSheet(Sheet sheet) {
		this.sheet = sheet;
	}
	
	public int getRowNum(){
		return this.rowNum;
	}
	
	/**
	 * Increment and return row number
	 * @return
	 */
	public int incrementRowNum(){
		return this.rowNum++;
	}
}