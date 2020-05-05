package com.serotonin.json.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Order the properties of a class during serialization.
 *
 * @author Terry Packer
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonPropertyOrder
{
    /**
     * Order in which properties of annotated object are to be serialized in.
     */
    public String[] value() default { };

}
