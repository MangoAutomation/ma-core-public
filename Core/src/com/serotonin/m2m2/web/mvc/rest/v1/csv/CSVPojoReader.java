package com.serotonin.m2m2.web.mvc.rest.v1.csv;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

import com.serotonin.m2m2.module.ModelDefinition;

/**
 * A CSV reader that will read the contents of a CSV file in to an annotated class
 * 
 * Based on work done by Staffan Friberg
 * @author Terry Packer
 *
 * @param <T> Class with CSV annotations
 */
public class CSVPojoReader<T> implements Closeable {

   private CSVReader reader;
   private String[] headers;
   private CSVPojoHandler<T> pojoHandler;
   private Class<T> clazz;

   /**
    * Constructs CSVPojoReader using a comma for the separator.
    *
    * @param clazz The pojo class to populate
    * @param reader
    *            the reader to an underlying CSV source.
    */
   public CSVPojoReader(Class<T> clazz, CSVReader reader) {
      this.reader = reader;
      pojoHandler = new CSVPojoHandler<T>();
      this.clazz = clazz;
   }

   /**
    * Closes the underlying CSVReader.
    *
    * @throws IOException if the close fails
    */
   public void close() throws IOException {
      reader.close();
   }

   /**
    * Read the next pojo from the csv file
    * 
    * @return The next pojo from the csv file or null if end of file
    * @throws IOException
    */
   public T readNext() throws IOException {
      if (headers == null) {
         headers = reader.readNext();
         // Make sure we don't have any accidental whitespaces
         for (int i = 0; i < headers.length; i++) {
            headers[i] = headers[i].trim();
         }
      }
      String[] line = reader.readNext();
      
      //Check the first Header for Type and read it
      
      T pojo = null;
      if (line != null) {
    	 //TODO Use the headers to determine what new type to create
        
         if(!pojoHandler.isInitialized()){
        	 ModelDefinition definition = pojoHandler.findModelDefinition(clazz);
        	 pojoHandler.initialize(line, (T) definition.createModel());
         }
         pojo = pojoHandler.newInstance();
         for (int i = 0; i < headers.length; i++) {
            pojoHandler.setField(pojo, headers[i], line[i].trim());
         }
      }
      return pojo;
   }

   /**
    * Read all lines from the CSV file and return a list of pojos representing the data
    * 
    * @return A list of pojos representing the CSV file
    * @throws IOException
    */
   public List<T> readAll() throws IOException {
      List<T> list = new ArrayList<T>();
      for (T pojo = readNext(); pojo != null; pojo = readNext()) {
         list.add(pojo);
      }
      return list;
   }
}
