/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.spring.esb;

import org.springframework.messaging.converter.ProtobufMessageConverter;
import org.springframework.stereotype.Component;

/**
 * Spring Messaging converter for Protobuf
 */
@Component
public class PiProtobufMessageConverter extends ProtobufMessageConverter {

}
