/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;

/**
 * @author Terry Packer
 *
 */
@Component
public class RoleTableDefinition extends AbstractTableDefinition {

    public static final String TABLE_NAME = "roles";
    public static final String ROLE_INHERITANCE_TABLE = "roleInheritance";

    public RoleTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("r"));
    }

    //Role Inheritance table
    public static final Name roleIdFieldName = DSL.name("roleId");
    public static final Field<Integer> roleIdField = DSL.field(roleIdFieldName, SQLDataType.INTEGER.nullable(false));
    public static final Table<? extends Record> roleInheritanceTable = DSL.table(ROLE_INHERITANCE_TABLE);
    public static final Name roleInheritanceTableAlias = DSL.name("rh");
    public static final Table<? extends Record> roleInheritanceTableAsAlias = roleInheritanceTable.as(roleInheritanceTableAlias);

    public static final Field<Integer> roleInheritanceTableRoleIdFieldAlias = DSL.field(roleInheritanceTableAlias.append(roleIdFieldName), SQLDataType.INTEGER.nullable(false));

    public static final Name roleInheritanceTableInheritedRoleIdName = DSL.name("inheritedRoleId");
    public static final Field<Integer> roleInheritanceTableInheritedRoleIdField = DSL.field(roleInheritanceTableInheritedRoleIdName, SQLDataType.INTEGER);
    public static final Field<Integer> roleInheritanceTableInheritedRoleIdFieldAlias = DSL.field(roleInheritanceTableAlias.append(roleInheritanceTableInheritedRoleIdName), SQLDataType.INTEGER);


}
