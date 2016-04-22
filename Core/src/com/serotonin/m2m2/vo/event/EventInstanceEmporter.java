/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.emport.AbstractSheetEmporter;

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
			CellType.DATE,
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



	/**
	 * @param vo
	 */
	public void exportRow(EventInstanceVO vo) {
    	int cellNum = 0;
        Cell cell;
        Row row;
        row = sheet.createRow(this.rowNum++);
        
        //Set Event Id
        cell = row.createCell(cellNum++);
        cell.setCellValue(vo.getId());
        
        //Alarm Level
        cell = row.createCell(cellNum++);
        cell.setCellValue(AlarmLevels.getAlarmLevelMessage(vo.getAlarmLevel()).translate(Common.getTranslations()));
        
        //Active Time
        cell = row.createCell(cellNum++);
        cell.setCellValue(new Date(vo.getActiveTimestamp()));
        cell.setCellStyle(dateStyle);

        //Message (Remove any HTML)
        cell = row.createCell(cellNum++);
        String messageStringHTML = vo.getMessageString();
        String messageString = StringEscapeUtils.unescapeHtml4(messageStringHTML);
        //Since we have <br/> in the code and that isn't proper HTML we need to remove it by hand
        messageString = messageString.replace("<br/>", "\n");
        cell.setCellValue(messageString);
        
        //Status
        cell = row.createCell(cellNum++);
        if (vo.isActive())
        	cell.setCellValue(Common.translate("common.active"));
        else if (!vo.isRtnApplicable())
            cell.setCellValue("");
        else
            cell.setCellValue(vo.getRtnTimestampString() + " - " + vo.getRtnMessageString());

        //Ack Time     
        //Ack User      
        cell = row.createCell(cellNum++);
        Cell ackMsgCell = row.createCell(cellNum++);
        cell.setCellStyle(dateStyle);
        if (vo.isAcknowledged()) {
            cell.setCellValue(new Date(vo.getAcknowledgedTimestamp()));
            TranslatableMessage ackMessage;
            if(vo.getAcknowledgedByUserId() != 0){
            	ackMessage = new TranslatableMessage("events.export.ackedByUser", vo.getAcknowledgedByUsername());
            }else{
            	ackMessage = vo.getAlternateAckSource();
            }
            if(ackMessage != null)
            	ackMsgCell.setCellValue(ackMessage.translate(Common.getTranslations()));
        }else{
        	//Do we need to set the cell to null explicitly
        }
        

		
	}

}
