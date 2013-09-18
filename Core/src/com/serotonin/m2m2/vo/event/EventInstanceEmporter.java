/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event;

import java.util.List;

import org.apache.poi.ss.usermodel.Row;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.emport.AbstractSheetEmporter;
import com.serotonin.m2m2.vo.emport.SpreadsheetEmporter.CellType;
import com.serotonin.m2m2.emport.SpreadsheetException;

/**
 * 
 * 
 * 
 * @author Terry Packer
 *
 */
public class EventInstanceEmporter extends AbstractSheetEmporter{

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.emport.AbstractSheetEmporter#getSheetName()
	 */
	@Override
	protected String getSheetName() {
		return Common.translate("events.alarms");
	}

	private final String[] headers = {
			Common.translate("events.id"),
			Common.translate("common.alarmLevel"),
			Common.translate("common.activeTime"),
			Common.translate("events.msg"),
			Common.translate("events.status"),
			Common.translate("events.ackTime"),
			Common.translate("events.ackUser"),
			
	};
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.emport.AbstractSheetEmporter#getHeaders()
	 */
	@Override
	protected String[] getHeaders() {
		return headers;
	}

	private final CellType[] columnTypes = {
			CellType.NUMERIC,
			CellType.STRING,
			CellType.STRING,
			CellType.STRING,
			CellType.DATE,
			CellType.STRING
	};
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.emport.AbstractSheetEmporter#getColumnTypes()
	 */
	@Override
	protected CellType[] getColumnTypes() {
		return columnTypes;
	}

	private final int[] columnWidths ={
			// nb. 256 = one character in the Excel Column
			256*10,
			256*15,
			256*30,
			256*100,
			256*30,
			256*30,
			256*20
	};
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.emport.AbstractSheetEmporter#getColumnWidths()
	 */
	@Override
	protected int[] getColumnWidths() {
		return this.columnWidths;
	}



	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.emport.AbstractSheetEmporter#exportRows()
	 */
	@Override
	protected List<List<Object>> exportRows() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.emport.AbstractSheetEmporter#importRow(org.apache.poi.ss.usermodel.Row)
	 */
	@Override
	protected void importRow(Row rowData)
			throws com.serotonin.m2m2.vo.emport.SpreadsheetException {
		// TODO Auto-generated method stub
		
	}

	
	
	
	
}
