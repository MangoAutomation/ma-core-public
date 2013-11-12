/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.dataPoint;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;

import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.emport.AbstractSheetEmporter;
import com.serotonin.m2m2.emport.CellTypeValue;
import com.serotonin.m2m2.emport.JsonCapturer;
import com.serotonin.m2m2.emport.SpreadsheetEmporter.CellType;
import com.serotonin.m2m2.emport.SpreadsheetException;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.PointLocatorVO;

/**
 * @author Terry Packer
 *
 */
public class DataPointEmporter extends AbstractSheetEmporter{

	private Boolean useNames;
	private Integer dataSourceId;
	private DataSourceVO<?> dataSource;
	
	public DataPointEmporter(Integer dataSourceId, Boolean useNames){
		this.useNames = useNames;
		this.dataSourceId = dataSourceId;
		this.dataSource = DataSourceDao.instance.get(dataSourceId);
		
	}
	
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.emport.AbstractSheetEmporter#getSheetName()
	 */
	@Override
	protected String getSheetName() {
		return Common.translate("emport.dataPoints");
	}

	private String[] headers;
	private CellType[] columnTypes;
	private int[] columnWidths;
	
//	 private static final String[] headers = {
//         "XID",
//         "Name",
//         "Data Source",
//         "Device Name",
//         "Logging Type",
//         "Delete"
//	 };
//    private static final CellType[] columnTypes = {
//        CellType.STRING,
//        CellType.STRING,
//        CellType.STRING,
//        CellType.STRING,
//        CellType.STRING,
//        CellType.STRING,
//        
//    };
//    // nb. 256 = one character in the Excel Column
//    private static final int[] columnWidths = {
//        25*256,
//        10*256,
//        25*256,
//        30*256,
//        25*256,
//        30*256,
//    };

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.emport.AbstractSheetEmporter#getHeaders()
	 */
	@Override
	protected String[] getHeaders() {
				
		return headers;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.emport.AbstractSheetEmporter#getColumnTypes()
	 */
	@Override
	protected CellType[] getColumnTypes() {
		return columnTypes;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.emport.AbstractSheetEmporter#getColumnWidths()
	 */
	@Override
	protected int[] getColumnWidths() {
		return columnWidths;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.emport.AbstractSheetEmporter#importRow(java.util.List)
	 */
	@Override
	protected void importRow(List<Object> rowData) throws SpreadsheetException {
		
		DataPointVO dp = new DataPointVO();
	    PointLocatorVO pointLocator = this.dataSource.createPointLocator();
		dp.setPointLocator(pointLocator);

		//List of CellTypeValues
		for(Object cell : rowData){
			CellTypeValue typeValue = (CellTypeValue)cell;
			try {
				PropertyUtils.setSimpleProperty(dp, typeValue.getMemberName(), typeValue.getValue());
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.emport.AbstractSheetEmporter#exportRows()
	 */
	@Override
	protected List<List<Object>> exportRows() {
		
		List<DataPointVO> points = DataPointDao.instance.getDataPoints(dataSourceId, null);
        		
		List<List<Object>> rows = new ArrayList<List<Object>>();
		for(DataPointVO vo : points){
			rows.add(exportRow(vo));
		}
		return rows;
	}
	

	/**
	 * Map the data to output
	 * 
	 * 
	 * 
	 * 
	 * @param vo
	 * @return
	 */
	private List<Object> exportRow(DataPointVO vo){
		
		List<Object> row = new ArrayList<Object>();
	
		JsonContext ctx = Common.JSON_CONTEXT;
		JsonCapturer capturer = new JsonCapturer(ctx,vo);
		int i=0;
		try {
			capturer.capture();
			this.columnTypes = new CellType[capturer.getValues().keySet().size()+1];
			this.columnWidths = new int[capturer.getValues().keySet().size()+1];
			this.headers = new String[capturer.getValues().keySet().size()+1];

			for(String column : capturer.getValues().keySet()){
				row.add(capturer.getValues().get(column));
				this.columnTypes[i] = CellType.STRING;
				this.columnWidths[i] = 256*25;
				this.headers[i] = column;
				i++;
			}
			
			
		} catch (JsonException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

			
			
			
		this.columnTypes[i] = CellType.STRING;
		this.columnWidths[i] = 256*25;
		this.headers[i] = "Delete";

		
		row.add("no"); //For delete
		return row;
		
	}
	
}
