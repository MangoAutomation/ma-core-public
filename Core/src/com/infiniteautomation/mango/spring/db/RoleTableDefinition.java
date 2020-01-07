/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * @author Terry Packer
 *
 */
@Component
public class RoleTableDefinition extends AbstractTableDefinition {

    public static final String TABLE_NAME = "roles";

    public RoleTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("r"));
    }

}
