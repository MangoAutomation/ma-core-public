package com.serotonin.json.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker annotation for a class to indicate that the class should not be treated as a bean. The primary use case for
 * this is for when a class implements JsonSerialiable, and JsonProperty annotations exist. If the class is not
 * annotated with this, it will be treated as a bean.
 * 
 * @author Matthew Lohbihler
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonEntity {
    // no op - Marker annotation only
}
