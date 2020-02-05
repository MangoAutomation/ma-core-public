/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
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
    public static final String ROLE_MAPPING_TABLE = "roleMappings";

    public RoleTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("r"));
    }

    public static final Table<? extends Record> roleMappingTable = DSL.table(ROLE_MAPPING_TABLE);
    public static final Name roleMappingTableAlias = DSL.name("rm");
    public static final Table<? extends Record> roleMappingTableAsAlias = roleMappingTable.as(roleMappingTableAlias);

    public static final Name roleIdFieldName = DSL.name("roleId");
    public static final Field<Integer> roleIdField = DSL.field(roleIdFieldName, SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> roleIdFieldAlias = DSL.field(roleMappingTableAlias.append(roleIdFieldName), SQLDataType.INTEGER.nullable(false));

    public static final Name voIdFieldName = DSL.name("voId");
    public static final Field<Integer> voIdField = DSL.field(voIdFieldName, SQLDataType.INTEGER);
    public static final Field<Integer> voIdFieldAlias = DSL.field(roleMappingTableAlias.append(voIdFieldName), SQLDataType.INTEGER);

    public static final Name voTypeFieldName = DSL.name("voType");
    public static final Field<String> voTypeField = DSL.field(voTypeFieldName, SQLDataType.VARCHAR(255));
    public static final Field<String> voTypeFieldAlias = DSL.field(roleMappingTableAlias.append(voTypeFieldName), SQLDataType.VARCHAR(255));

    public static final Name permissionTypeFieldName = DSL.name("permissionType");
    public static final Field<String> permissionTypeField = DSL.field(permissionTypeFieldName, SQLDataType.VARCHAR(255).nullable(false));
    public static final Field<String> permissionTypeFieldAlias = DSL.field(roleMappingTableAlias.append(permissionTypeFieldName), SQLDataType.VARCHAR(255).nullable(false));

    /**
     * Join on the mapping table via conditions (use aliases in the condition)
     * @param query
     * @param condition
     * @return
     */
    public SelectJoinStep<Record> joinRoleMappings(SelectJoinStep<Record> query, Condition condition) {
        return query.join(roleMappingTableAsAlias).on(condition);
    }

}
