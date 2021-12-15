package com.serotonin.json.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation with which properties can be marked as being convertible. Can be applied to fields or methods. If any
 * such annotations are found in a class, it will not be treated as a bean. This works in conjunction with the
 * JsonSerializable interface, such that any properties that cannot be simply dealt with by annotating can be handled in
 * the JsonSerializable methods.
 *
 * @author Matthew Lohbihler
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonProperty {
    /**
     * Whether the property should be *read from the JSON* and *written into the object*, AKA deserialized.
     *
     * @return true if the property is readable. Defaults to true.
     */
    boolean read() default true;

    /**
     * Whether the property should be *read from the object* and *written into the JSON*, AKA serialized.
     *
     * @return true if the property is writable. Defaults to true.
     */
    boolean write() default true;

    /**
     * An alias for the property in JSON. Used for both reading and writing.
     *
     * @return the alias to use. Defaults to the name of the property in the Java code.
     */
    String alias() default "";

    /**
     * Determines whether the property should be written to the JSON if the value is a default value. Default values
     * are:
     *
     * boolean == false
     *
     * number = 0
     *
     * object = null
     *
     * @return true if the property should be suppressed if its value is the default value.
     */
    boolean suppressDefaultValue() default false;

    /**
     * The include hints for which this property will be read and written. If not provided this is considered to be "*",
     * or equivalent to all hints. If provided, this property will only be used if the reader or writer's include hint
     * is one of the hints in the array.
     *
     * @return the include hints for this property.
     */
    String[] includeHints() default {};

    /**
     * Any past field names that should be treated to refer to this property, useful when a field name has been changed
     */
    String[] readAliases() default {};
}
