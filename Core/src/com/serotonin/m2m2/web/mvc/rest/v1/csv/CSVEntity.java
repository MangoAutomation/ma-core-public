package com.serotonin.m2m2.web.mvc.rest.v1.csv;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * Annotation saying that this object is a CSV Pojo.
 *
 * All fields will be written execpt transient fields and
 * fields annotated with CSVColumn and specified to be hidden
 *
 * Based on work done by Staffan Friberg
 * @author Terry Packer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CSVEntity {
	
	   /**
	    * This value should be the TYPE NAME to match the data type to be represented in the following fields.
	    *
	    *
	    * @return The matching typeName name for this field
	    */
	   String typeName() default "";
	   
	   /**
	    * Indicates that this annotation is derived by a typeName column lookup
	    * TODO Add more documentation about this feature
	    * @return
	    */
	   boolean derived() default false; 
}
