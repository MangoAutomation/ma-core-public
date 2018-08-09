/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.joda.time.DateTime;
import org.springframework.dao.DataIntegrityViolationException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EnhancedPointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.emport.AbstractSheetEmporter;
import com.serotonin.m2m2.vo.emport.SpreadsheetException;
import com.serotonin.m2m2.vo.export.ExportDataValue;
import com.serotonin.m2m2.vo.export.ExportPointInfo;

/**
 * @author Terry Packer
 *
 */
public class PointValueEmporter extends AbstractSheetEmporter{
	
	private ExportPointInfo pointInfo;
	
    //Map of XIDs to non-running data points
    private Map<String, DataPointVO> voMap = new HashMap<String, DataPointVO>();
    //Map of XIDs to running data points
    private Map<String, DataPointRT> rtMap = new HashMap<String, DataPointRT>();

    private DataPointDao dataPointDao = DataPointDao.getInstance();
    private PointValueDao pointValueDao = Common.databaseProxy.newPointValueDao();
	
    private String name;
    
    public PointValueEmporter(String sheetName){
    	this.name = sheetName;
    }
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.emport.AbstractSheetEmporter#getSheetName()
	 */
	@Override
	protected String getSheetName() {
		return this.name;
	}
	
    private static final String[] headers = {
    	Common.translate("emport.dataPoint.xid"),
    	Common.translate("pointEdit.props.deviceName"),
        Common.translate("common.pointName"),
        Common.translate("common.time"),
        Common.translate("common.value"),
        Common.translate("common.rendered"),
        Common.translate("common.annotation"),
        Common.translate("common.modify")
    };

	/* (non-Javadoc)
	 * @see com.deltamation.mango.downtime.emport.AbstractSheetEmporter#getHeaders()
	 */
	@Override
	protected String[] getHeaders() {
	    return headers;
	}
	
    private static final CellType[] columnTypes = {
    	CellType.STRING,
    	CellType.STRING,
        CellType.STRING,
        CellType.DATE,
        CellType.STRING, //May be able to change to NUMERIC
        CellType.STRING,
        CellType.STRING,
        CellType.STRING
    };
    
    @Override
    protected CellType[] getColumnTypes() {
        return columnTypes;
    }
	
    
    // nb. 256 = one character
    private static final int[] columnWidths = {

        25*256,
        25*256,
        30*256,
        25*256,
        30*256,
        25*256,
        25*256,
        20*256
    };
    
    /*
     * (non-Javadoc)
     * @see com.serotonin.m2m2.vo.emport.AbstractSheetEmporter#getColumnWidths()
     */
     @Override
    protected int[] getColumnWidths() {
        return columnWidths;
    }
  
    

    /*
     * (non-Javadoc)
     * @see com.serotonin.m2m2.vo.emport.AbstractSheetEmporter#importRow(org.apache.poi.ss.usermodel.Row)
     */
    @Override
    protected void importRow(Row rowData) throws SpreadsheetException {
    
    	int cellNum = 0;
    	
    	//Data Point XID
    	Cell xidCell = rowData.getCell(cellNum++);
    	if(xidCell == null)
    		throw new SpreadsheetException(rowData.getRowNum(), "emport.error.xidRequired");
    	if((xidCell.getStringCellValue() == null)||(xidCell.getStringCellValue().isEmpty()))
    		throw new SpreadsheetException("emport.error.xidRequired");
    	
    	
    	//First Check to see if we already have a point
    	String xid = xidCell.getStringCellValue();
    	DataPointVO dp = voMap.get(xid);
    	DataPointRT dpRt = rtMap.get(xid);
    	
    	//We will always have the vo in the map but the RT may be null if the point isn't running
    	if(dp == null){
    		dp = dataPointDao.getDataPoint(xid);
            if (dp == null)
            	throw new SpreadsheetException(rowData.getRowNum(),"emport.error.missingPoint", xid);
        	dpRt = Common.runtimeManager.getDataPoint(dp.getId());

    		rtMap.put(xid, dpRt);
    		voMap.put(xid, dp);
    	}

    	PointValueTime pvt;
    	
    	//Cell Device name (Not using Here)
    	cellNum++;

    	//Cell Point name (Not using Here)
    	cellNum++;

    	
    	//Cell Time
    	Date time = rowData.getCell(cellNum++).getDateCellValue();

    	// delete/add column
    	Cell modifyCell = rowData.getCell(7);
    	boolean add = false;
    	boolean delete = false;
    	
    	if(modifyCell != null){
    		String modification = (String) modifyCell.getStringCellValue();
    		if(modification.equalsIgnoreCase("delete")){
    			delete = true;
    		}else if(modification.equalsIgnoreCase("add")){
    			add = true;
    		}else{
    			throw new SpreadsheetException(rowData.getRowNum(), "emport.spreadsheet.modifyCellUnknown");
    		}
    	}//end if modify cell exists

    	//What do we do with the row
    	if(delete){
            if (time == null) {
                throw new SpreadsheetException(rowData.getRowNum(), "emport.error.deleteNew", "no timestamp, unable to delete");
            } 
            else {
                try {
                	this.rowsDeleted += Common.runtimeManager.purgeDataPointValue(dp.getId(), time.getTime());
                }catch (Exception e) {
                    if(e instanceof DataIntegrityViolationException)
                        throw new SpreadsheetException(rowData.getRowNum(), "emport.error.unableToDeleteDueToConstraints");
                    else
                        throw new SpreadsheetException(rowData.getRowNum(), "emport.error.unableToDelete", e.getMessage());
                }
            }
            return; //Done now
    	}else if(add){
    	
	    	//Cell Value
	    	Cell cell;
	    	cell = rowData.getCell(cellNum++);
	    	//Create a data value
	    	DataValue dataValue;
	    	switch(dp.getPointLocator().getDataTypeId()){
			case DataTypes.ALPHANUMERIC:
				dataValue = new AlphanumericValue(cell.getStringCellValue());
				break;
			case DataTypes.BINARY:
				
				switch(cell.getCellType()){
					case Cell.CELL_TYPE_BOOLEAN:
						dataValue = new BinaryValue(new Boolean(cell.getBooleanCellValue()));
					break;
					case Cell.CELL_TYPE_NUMERIC:
						if(cell.getNumericCellValue() == 0)
							dataValue = new BinaryValue(new Boolean(false));
						else
							dataValue = new BinaryValue(new Boolean(true));
					break;	
					case Cell.CELL_TYPE_STRING:
						if(cell.getStringCellValue().equalsIgnoreCase("false"))
							dataValue = new BinaryValue(new Boolean(false));
						else
							dataValue = new BinaryValue(new Boolean(true));
						break;
					default:
						throw new SpreadsheetException(rowData.getRowNum(), "common.default", "Invalid cell type for extracting boolean");
				}
				break;
			case DataTypes.MULTISTATE:
				dataValue = new MultistateValue((int)cell.getNumericCellValue());
				break;
			case DataTypes.NUMERIC:
				dataValue = new NumericValue(cell.getNumericCellValue());
			break;
			default:
				throw new SpreadsheetException(rowData.getRowNum(), "emport.spreadsheet.unsupportedDataType", dp.getPointLocator().getDataTypeId());
	    	}
	    	
	    	
	     	//Cell Rendered Value (Not using yet)
	    	cellNum++;
	    	
	    	//Cell Annotation
	    	Cell annotationRow = rowData.getCell(cellNum++);
	    	if(annotationRow != null){
	    	   	String annotation = annotationRow.getStringCellValue();
	    	    //TODO These methods here do not allow updating the Annotation. We need to be a set point source for that to work
	    		TranslatableMessage sourceMessage = new TranslatableMessage("common.default",annotation);
	    		pvt = new AnnotatedPointValueTime(dataValue, time.getTime(), sourceMessage);
	    	}else{
	    		pvt = new PointValueTime(dataValue,time.getTime());
	    	}
	    	//Save to cache if running
	    	if(dpRt != null)
	    		dpRt.savePointValueDirectToCache(pvt, null, true, true);
	    	else{
	    	    if (pointValueDao instanceof EnhancedPointValueDao) {
	    	        DataSourceVO<?> ds = getDataSource(dp.getDataSourceId());
	    	        ((EnhancedPointValueDao) pointValueDao).savePointValueAsync(dp, ds, pvt,null);
	    	    } else {
	    	        pointValueDao.savePointValueAsync(dp.getId(),pvt,null);
	    	    }
	    	}

	    	//Increment our counter
	    	this.rowsAdded++;
    	}
    }
    
    Map<Integer, DataSourceVO<?>> cachedDataSources = new HashMap<>();
    
    DataSourceVO<?> getDataSource(int dataSourceId) {
        DataSourceVO<?> ds = cachedDataSources.get(dataSourceId);
        if (ds == null) {
            ds = DataSourceDao.getInstance().get(dataSourceId);
            if (ds != null)
                cachedDataSources.put(dataSourceId, ds);
        }
        return ds;
    }
    
    
    /* (non-Javadoc)
     * @see com.deltamation.mango.downtime.emport.AbstractSheetEmporter#exportRows()
     */
    @Override
    protected List<List<Object>> exportRows() {
    	return null;
    }
    
    public void setPointInfo(ExportPointInfo info){
    	this.pointInfo = info;
    }
    
    public void exportRow(ExportDataValue edv){
    	
    	int cellNum = 0;
        Cell cell;
        Row row;
        row = sheet.createRow(this.rowNum++);
        
        //Set Point XID
        cell = row.createCell(cellNum++);
        cell.setCellValue(this.pointInfo.getXid());
        
        //Set the Device Name
        cell = row.createCell(cellNum++);
        cell.setCellValue(this.pointInfo.getDeviceName());

        //Set the Point Name
        cell = row.createCell(cellNum++);
        cell.setCellValue(this.pointInfo.getPointName());

        
        //Time
        cell = row.createCell(cellNum++);
        cell.setCellValue(new DateTime(edv.getTime()).toDate());
        cell.setCellStyle(dateStyle);
        
        //Value        
        
        if(edv.getValue() != null){
        	cell = row.createCell(cellNum++);
        	DataValue value = edv.getValue();
        	switch(value.getDataType()){
        	case DataTypes.ALPHANUMERIC:
        		cell.setCellValue(value.getStringValue());
        		break;
        	case DataTypes.BINARY:
        		cell.setCellValue(value.getBooleanValue());
        		break;
        	case DataTypes.MULTISTATE:
        		cell.setCellValue(value.getIntegerValue());
        		break;
        	case DataTypes.NUMERIC:
        		cell.setCellValue(value.getDoubleValue());
        		break;
        	default:
        			//Do nothing for now
        	//Set the value here
        	}
        	//Set the text renderer value here
        	cell = row.createCell(cellNum++);
        	cell.setCellValue(this.pointInfo.getTextRenderer().getText(edv.getValue(),TextRenderer.HINT_FULL));
        }else{
        	row.createCell(cellNum++);
        	row.createCell(cellNum++); //Do we need an empty cell?
        }
        
        //Do we have an annotation
        if(edv.getAnnotation() != null){
        	cell = row.createCell(cellNum++);
        	cell.setCellValue(edv.getAnnotation().translate(Common.getTranslations()));
        }else{
        	cell = row.createCell(cellNum++);
        }
        
        this.rowsAdded++;

    }

	/**
	 * @return
	 */
	public ExportPointInfo getPointInfo() {
		return this.pointInfo;
	}
    
}