/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author Terry Packer
 *
 */
@JsonDeserialize(using = DataPointVoDeserializer.class)
@JsonSerialize(using = DataPointVoSerializer.class)
public abstract class DataPointVoMixin {

}
