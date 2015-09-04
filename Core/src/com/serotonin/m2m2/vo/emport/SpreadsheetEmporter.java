/*
    Copyright (C) 2013 Deltamation Software All rights reserved.
    @author Terry Packer
 */
package com.serotonin.m2m2.vo.emport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.serotonin.m2m2.DeltamationCommon;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.emport.AbstractSheetEmporter.CellType;

/**
 * Copyright (C) 2013 Deltamation Software. All Rights Reserved.
 * @author Terry Packer
 *
 */
public class SpreadsheetEmporter {
    protected File file = null;
    protected InputStream inStream = null;
    protected OutputStream outStream = null;
    
    protected FileType fileType;
    protected Workbook wb;
    
    protected List<TranslatableMessage> errorMessages;
    
    private int rowsProcessed;
    private int rowErrors;
    private int rowNum; // current row
    
    /* Date Format for date cells can be set before export (This is the Excel Format not the Java Simple Date Format) */
    private String dateFormat = "mm/dd/yy hh:mm:ss.000"; //Show Milliseconds for default
    
    public enum FileType {
        AUTO, UNKNOWN, XLS, XLSX
    }

    /**
     * Create a spreadsheet import/exporter
     * @param useNames if true then use the monitor and machine columns refer to names instead of XIDs
     */
    public SpreadsheetEmporter(FileType fileType) {
        this.fileType = fileType;
    }

    /**
     * Create a spreadsheet import/exporter, auto detect the file type
     * @param filename
     * @param useNames if true then use the monitor and machine columns refer to names instead of XIDs
     */
    public SpreadsheetEmporter(String filename) {
        this(filename, FileType.AUTO);
    }
    
    /**
     * Create a spreadsheet import/exporter
     * @param filename
     * @param fileType
     * @param useNames if true then use the monitor and machine columns refer to names instead of XIDs
     */
    public SpreadsheetEmporter(String filename, FileType fileType) {
        file = new File(filename);
        
        if (fileType == FileType.AUTO) {
            if (filename.endsWith(".xls")) {
                this.fileType = FileType.XLS;
            }
            else if (filename.endsWith(".xlsx")) {
                this.fileType = FileType.XLSX;
            }
            else {
                this.fileType = FileType.UNKNOWN;
            }
        }
        else {
            this.fileType = fileType;
        }
    }
    
    /**
     * Import from the specified input stream
     * @param inStream
     * @param sheetEmporters list of sheet emporters
     */
    public void doImport(InputStream inStream, AbstractSheetEmporter... sheetEmporters) {
        this.inStream = inStream;
        doImport(sheetEmporters);
    }
    
    /**
     * Import from the file specified in constructor
     * @param sheetEmporters list of sheet emporters
     */
    public void doImport(AbstractSheetEmporter... sheetEmporters) {
        openInputWorkbook();
        if (wb == null) {
            return;
        }
        rowsProcessed = 0;
        rowErrors = 0;
        errorMessages = new ArrayList<TranslatableMessage>();
        
        for (AbstractSheetEmporter sheetEmporter : sheetEmporters) {
            importSheet(sheetEmporter);
        }
    }
    
    
    
    
    
    
    
    /**
     * Imports one sheet
     */
    private void importSheet(AbstractSheetEmporter sheetEmporter) {
        
        Sheet sheet = wb.getSheet(sheetEmporter.getSheetName());
        if (sheet == null) {
            errorMessages.add(new TranslatableMessage("emport.spreadsheet.sheetNotFound", sheetEmporter.getSheetName()));
            return;
        }
        
        int cellNum = 0;
        Cell cell;
        Row row;
        
        int numRows = sheet.getLastRowNum() + 1;
        if (numRows == 0) {
            errorMessages.add(new TranslatableMessage("emport.spreadsheet.nowRows", sheetEmporter.getSheetName()));
            return;
        }
        
        // check the headers are correct, just in case the user has reordered the columns
        rowNum = 0;
        row = sheet.getRow(rowNum);
        
        if(sheetEmporter.hasHeaders()) {
	        String[] headers = sheetEmporter.getHeaders();
	        if (row.getLastCellNum() < headers.length) {
	            errorMessages.add(new TranslatableMessage("emport.spreadsheet.headerLengthIncorrect", sheetEmporter.getSheetName(), headers.length, row.getLastCellNum()));
	            return;
	        }
	        
	        for (cellNum = 0; cellNum < headers.length; cellNum++) {
	            cell = row.getCell(cellNum);
	            
	            String cellValue;
	            try {
	                cellValue = readStringCell(rowNum, cell);
	            } catch (SpreadsheetException e) {
	                cellValue = "";
	            }
	            
	            if (cellValue == null || !cellValue.equalsIgnoreCase(headers[cellNum])) {
	                errorMessages.add(new TranslatableMessage("emport.spreadsheet.headersMismatch", sheetEmporter.getSheetName(), cellNum, headers[cellNum], cellValue));
	                return;
	            }
	        }
	        rowNum += 1;
        }
 
        // import the actual data rows
        for (; rowNum < numRows; rowNum++) {
            try {
            	//Import this row
            	row  = sheet.getRow(rowNum);
            	if(!isRowEmpty(row)){
            		sheetEmporter.importRow(row);
                    rowsProcessed++;
            	}
            }
            catch (Exception e) {
            	if(e instanceof SpreadsheetException){
            		((SpreadsheetException)e).setRowNum(rowNum);
            		errorMessages.addAll(((SpreadsheetException)e).getMessages());
            	}else{
            		errorMessages.add(new TranslatableMessage("common.default", e.getMessage() + " - row " + rowNum));
            	}
                rowErrors++;
            }

        }
    }
    
    /**
     * Export to the specified output stream
     * @param outStream
     * @param sheetEmporters list of sheet emporters
     */
    public void doExport(OutputStream outStream, AbstractSheetEmporter... sheetEmporters) {
        this.outStream = outStream;
        doExport(sheetEmporters);
    }
    
    /**
     * @param sheetEmporters
     */
    public void doExport(AbstractSheetEmporter... sheetEmporters) {
        openOutputWorkbook();
        if (wb == null) {
            return;
        }
        //Setup Stats For Entire Workbook
        rowsProcessed = 0;
        rowErrors = 0;
        errorMessages = new ArrayList<TranslatableMessage>();
        for (AbstractSheetEmporter sheetEmporter : sheetEmporters) {
            doExport(sheetEmporter);
        }
        
        try {
            wb.write(outStream);
            outStream.close();
        } catch (IOException e) {
            errorMessages.add(new TranslatableMessage("emport.spreadsheet.ioError", e.getMessage()));
        }
    }
    
    /**
     * Export machine states to the spreadsheet
     * Check for error messages afterwards by calling getErrorMessages()
     * @param monitorId set to null if dont care
     * @param machineId set to null if dont care
     */
    private void doExport(AbstractSheetEmporter sheetEmporter) {

        
        Sheet sheet = wb.createSheet(sheetEmporter.getSheetName());
        
        rowNum = 0;
        int cellNum = 0;
        Cell cell;
        Row row;
        
        CreationHelper createHelper = wb.getCreationHelper();
        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yy hh:mm:ss"));
        
        CellStyle percentStyle = wb.createCellStyle();
        percentStyle.setDataFormat(createHelper.createDataFormat().getFormat(DeltamationCommon.decimalFormat));
        
        // headers
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerStyle.setFont(headerFont);
        
        row = sheet.createRow(rowNum++);
        int[] columnWidths = sheetEmporter.getColumnWidths();
        for (String text : sheetEmporter.getHeaders()) {
            cell = row.createCell(cellNum);
            cell.setCellValue(text);
            sheet.setColumnWidth(cellNum, columnWidths[cellNum]);
            cell.setCellStyle(headerStyle);
            cellNum++;
        }
        
        List<List<Object>> rows = sheetEmporter.exportRows();
        CellType[] columnTypes = sheetEmporter.getColumnTypes();
        
        for(List<Object> rowData : rows) {
            row = sheet.createRow(rowNum++);
            cellNum = 0;
            for (Object cellData : rowData) {

                cell = row.createCell(cellNum);
                if (cellData == null) {
                    // empty cell
                    cellNum++;
                    continue;
                }
                
                //Switch On Type
                switch(columnTypes[cellNum]){
                    case STRING:
                        cell.setCellValue((String) cellData);
                        break;
                    case NUMERIC:
                        if (cellData instanceof Integer) {
                            cell.setCellValue((Integer) cellData);
                        }
                        else if (cellData instanceof Long) {
                            cell.setCellValue((Long) cellData);
                        }
                        break;
                    case DATE:
                        cell.setCellValue((Date) cellData);
                        cell.setCellStyle(dateStyle);
                        break;
                    case PERCENT:
                        cell.setCellValue((Double) cellData);
                        cell.setCellStyle(percentStyle);
                        break;
                    default:
                            throw new RuntimeException("Unknown cell data type");
                    }
                cellNum++; //Increment It

            }
            
            rowsProcessed++;
        }
    }
    
    
    public void prepareExport(OutputStream outStream){
    	this.outStream = outStream;
        openOutputWorkbook();
    }
    
    
    public void prepareSheetExport(AbstractSheetEmporter sheetEmporter) {
        if (wb == null) {
            return;
        }
        //Setup Stats For Entire Workbook
        rowsProcessed = 0;
        rowErrors = 0;
        errorMessages = new ArrayList<TranslatableMessage>();
        
        Sheet currentSheet = wb.createSheet(sheetEmporter.getSheetName());
        sheetEmporter.setSheet(currentSheet);
        
        CreationHelper createHelper = wb.getCreationHelper();
        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat(this.getDateFormat()));
        sheetEmporter.setDateStyle(dateStyle);
        
        CellStyle percentStyle = wb.createCellStyle();
        percentStyle.setDataFormat(createHelper.createDataFormat().getFormat(DeltamationCommon.decimalFormat));
        sheetEmporter.setPercentStyle(percentStyle);
        
        rowNum = 0;
        int cellNum = 0;
        Cell cell;
        Row row;
        
        
        // headers
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerStyle.setFont(headerFont);
        
        row = currentSheet.createRow(sheetEmporter.incrementRowNum());
        int[] columnWidths = sheetEmporter.getColumnWidths();
        for (String text : sheetEmporter.getHeaders()) {
            cell = row.createCell(cellNum);
            cell.setCellValue(text);
            currentSheet.setColumnWidth(cellNum, columnWidths[cellNum]);
            cell.setCellStyle(headerStyle);
            cellNum++;
        }
        
    }
    

    
    /**
     * Get the Date Format for date cells
	 * @return
	 */
	private String getDateFormat() {
		return this.dateFormat;
	}

	/**
	 * Set the date format for date cells.
	 * @param format
	 */
	public void setDateFormat(String format){
		this.dateFormat = format;
	}
	public void finishExport(){
        try {
            wb.write(outStream);
            outStream.close();
        } catch (IOException e) {
            errorMessages.add(new TranslatableMessage("emport.spreadsheet.ioError", e.getMessage()));
        }

    }
    
    
    
    /**
     * Read a cell containing a String value
     * @param cell
     * @return
     * @throws SpreadsheetException
     */
    private String readStringCell(Integer rowNum, Cell cell) throws SpreadsheetException {
        if (cell == null || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
            return null;
        }
        
        String ret = cell.getStringCellValue();
        if (ret == null) {
            throw new SpreadsheetException(rowNum,"emport.spreadsheet.notString", new CellReference(cell).formatAsString());
        }
        
        return ret;
    }
    
    /**
     * Read a Numeric cell as a double
     * @param cell
     * @return
     * @throws SpreadsheetException
     */
    public double readNumericCell(Integer rowNum, Cell cell) throws SpreadsheetException {
        if (cell == null) {
            throw new SpreadsheetException(rowNum,"emport.spreadsheet.notNumberGeneric");
        }
        
        if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
            throw new SpreadsheetException(rowNum,"emport.spreadsheet.notNumber", new CellReference(cell).formatAsString());
        }
        
        double ret;
        try {
            ret = cell.getNumericCellValue();
        }
        catch (IllegalStateException e) {
            throw new SpreadsheetException(rowNum,"emport.spreadsheet.notNumber", new CellReference(cell).formatAsString());
        }
        
        return ret;
    }
    
    
    /**
     * Reads a Numeric cell as a Date
     * @param rowNum
     * @param cell
     * @return Date object
     * @throws SpreadsheetException
     */
    public Date readDateCell(Integer rowNum, Cell cell) throws SpreadsheetException {
        if (cell == null) {
            throw new SpreadsheetException(rowNum,"emport.spreadsheet.notNumberGeneric");
        }
        
        if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
            throw new SpreadsheetException(rowNum,"emport.spreadsheet.notDate", new CellReference(cell).formatAsString());
        }
        
        Date ret;
        try {
            ret = cell.getDateCellValue();
        }
        catch (IllegalStateException e) {
            throw new SpreadsheetException(rowNum,"emport.spreadsheet.notDate", new CellReference(cell).formatAsString());
        }
        
        return ret;
    }
    
    private void openInputWorkbook() {
        try {
            if (file != null) {
                inStream = new FileInputStream(file);
            }
            
            if (inStream == null) {
                errorMessages.add(new TranslatableMessage("emport.spreadsheet.noInStream"));
                return;
            }
            
            if (fileType == FileType.XLSX) {
                wb = new XSSFWorkbook(inStream);
            }
            else if (fileType == FileType.XLS) {
                wb = new HSSFWorkbook(inStream);
            }
            else {
                wb = null;
                errorMessages.add(new TranslatableMessage("emport.spreadsheet.unknownType", fileType));
            }
            inStream.close();
        }
        catch (IOException e) {
            errorMessages.add(new TranslatableMessage("emport.spreadsheet.ioError", e.getMessage()));
            return;
        }
    }
    
    private void openOutputWorkbook() {
        try {
            if (file != null) {
                outStream = new FileOutputStream(file);
            }
            
            if (outStream == null) {
                errorMessages.add(new TranslatableMessage("emport.spreadsheet.noOutStream"));
                return;
            }
            
            if (fileType == FileType.XLSX) {
                wb = new SXSSFWorkbook(1000); //Streaming Output to temp disk a
            }
            else if (fileType == FileType.XLS) {
                wb = new HSSFWorkbook();
            }
            else {
                wb = null;
                errorMessages.add(new TranslatableMessage("emport.spreadsheet.unknownType"));
            }
        }
        catch (IOException e) {
            errorMessages.add(new TranslatableMessage("emport.spreadsheet.ioError", e.getMessage()));
            return;
        }
    }
    
    /**
     * Helper function to check for blank Rows
     * @param row
     * @return
     */
    public boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                return false;
        }
        return true;
    }
    
    public boolean hasErrors() {
        return !errorMessages.isEmpty();
    }

    public List<TranslatableMessage> getErrorMessages() {
        return errorMessages;
    }

    public int getRowsProcessed() {
        return rowsProcessed;
    }

    public int getRowErrors() {
        return rowErrors;
    }

    public FileType getFileType() {
        return fileType;
    }
    
    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }
}
