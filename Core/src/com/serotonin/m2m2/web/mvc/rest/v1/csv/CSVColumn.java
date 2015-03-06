package com.serotonin.m2m2.web.mvc.rest.v1.csv;

import java.beans.PropertyEditorManager;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to show that a field in a class is a column in a CSV file.
 * 
 * Default values are simply set directly to the field and will be used if a field is empty.
 * Based on work done by Staffan Friberg
 * @author Terry Packer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface CSVColumn {

   /**
    * This value should match the header in the csv file to read/written.
    *
    * If not set the header is assumed to have the same name as the field.
    *
    * @return The matching header name for this field
    */
   String header() default "";

   /**
    * The CSVPropertyEditor to use when converting values.
    * The editor has to have a default constructor.
    *
    * By default an automatic check will try to determine what editor to use.
    * See PropertyEditorManager for available default editors.
    *
    * @see PropertyEditorManager
    * @return The CSVPropertyEditor to use for reading/writing values from/to the csv file.
    */
   Class<? extends CSVPropertyEditor> editor() default CSVPropertyEditor.class;

   /**
    * Lets the readers/writers know this is a hidden field that shouldn't be used in the csv file.
    * The same effect can be used without the annotation using transient.
    *
    * @return Boolean say if this field is hidden or not (default false)
    */
   boolean hidden() default false;
   
   int order() default 0;
}
