package com.serotonin.m2m2.web.mvc.rest.v1.csv;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import com.serotonin.m2m2.web.mvc.rest.v1.exception.NoSupportingModelException;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * A CSV file writer that will create a CSV file with data supplied by annotated Ps
 *
 * Based on work done by Staffan Friberg
 * @author Terry Packer
 *
 * @param <T> Class with CSV annotations
 */
public class CSVPojoWriter<T> implements Closeable {

   private final CSVWriter writer;
   private final CSVPojoHandler pojoHandler;
   private String[] headers;
   private boolean hasPrintedHeaders = false;

   /**
    * Constructs CSVPojoWriter using a comma for the separator.
    *
    * @param clazz The pojo class to write
    * @param writer The CSVWriter to use
    */
   public CSVPojoWriter(CSVWriter writer) {
      this.writer = writer;
      this.pojoHandler = new CSVPojoHandler();      
   }

   /**
    * Flush underlying CSVWriter.
    *
    * @throws IOException if bad things happen
    */
   public void flush() throws IOException {
      writer.flush();
   }

   /**
    * Close the underlying CSVWriter and flushing any buffered content.
    *
    * @throws IOException if bad things happen
    *
    */
   public void close() throws IOException {
      flush();
      writer.close();
   }

   /**
    * Writes the next pojo to the file.
    * The writer will make sure that the CSV file starts with a header
    *
    * @param pojo The pojo to write
 * @throws NoSupportingModelException 
    */
   public void writeNext(Object pojo) throws NoSupportingModelException, CSVException {
	  if(!pojoHandler.isInitialized() && pojo != null){
		  pojoHandler.initialize(pojo);
		  this.headers = pojoHandler.getHeaders().toArray(new String[0]);
	  }
      if (!this.hasPrintedHeaders) {
         this.hasPrintedHeaders = true;
         writer.writeNext(headers);
      }
      
      /**
       * Ability to write Empty List with only headers on top
       */
      if(pojo != null){
	      String[] line = new String[headers.length];
	
	      for (int i = 0; i < headers.length; i++) {
	         line[i] = pojoHandler.getField(pojo, headers[i]);
	      }
	
	      writer.writeNext(line);
      }
   }

   /**
    * Writes the entire list of pojos to a CSV file.
    * The writer will make sure the file starts with a header.
    *
    * @param list The list of pojos to write
    * @throws NoSupportingModelException 
    */
   public void writeAll(List<T> list) throws NoSupportingModelException, CSVException {
	   if(list.size() == 0)
		   writeNext(null);
	   else{
	      for (T pojo : list) {
	         writeNext(pojo);
	      }
	   }
   }
   
   public CSVWriter getWriter(){
	   return this.writer;
   }
}
