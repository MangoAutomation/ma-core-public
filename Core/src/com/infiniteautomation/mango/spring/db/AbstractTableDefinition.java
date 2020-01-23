/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import java.util.List;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * @author Terry Packer
 *
 */
public class AbstractTableDefinition extends AbstractBasicTableDefinition {

    /**
     * Field for xid column
     */
    public final Field<String> XID;

    /**
     * tablePrefix.xid
     */
    public final Field<String> XID_ALIAS;

    /**
     * Field for name column
     */
    public final Field<String> NAME;
    /**
     * tablePrefix.name
     */
    public final Field<String> NAME_ALIAS;

    public AbstractTableDefinition(Table<? extends Record> table, Name alias) {
        super(table, alias);
        this.XID = getXidField();
        this.XID_ALIAS = getXidAlias();
        this.NAME = getNameField();
        this.NAME_ALIAS = getNameAlias();
    }


    @Override
    protected void addFields(List<Field<?>> fields) {
        if(this.XID != null) {
            fields.add(XID);
        }
        if(this.NAME != null) {
            fields.add(NAME);
        }
    }

    /**
     * tableAlias.xid
     * @return
     */
    public Field<String> getXidAlias() {
        Name fieldName = getXidFieldName();
        if(fieldName == null) {
            return null;
        }else {
            return DSL.field(this.alias.append(fieldName), SQLDataType.VARCHAR(getXidFieldLength()).nullable(false));
        }
    }

    /**
     * Override as necessary Can be null if no xid column Exists
     *
     * @return String name of xid Column
     */
    public Field<String> getXidField() {
        Name fieldName = getXidFieldName();
        if(fieldName == null) {
            return null;
        }else {
            return DSL.field(getXidFieldName(), SQLDataType.VARCHAR(getXidFieldLength()).nullable(false));
        }
    }

    /**
     * Optionally override the name of the Xid column.
     * @return Name of xid column or null if there is not one
     */
    protected Name getXidFieldName() {
        return DSL.name("xid");
    }

    /**
     * Optionally override the length of the XID field
     * @return
     */
    protected int getXidFieldLength() {
        return 100;
    }

    /**
     * tableAlias.id
     * @return
     */
    public Field<String> getNameAlias() {
        Name fieldName = getNameFieldName();
        if(fieldName == null) {
            return null;
        }else {
            return DSL.field(this.alias.append(getNameFieldName()), SQLDataType.VARCHAR(getNameFieldLength()).nullable(false));
        }
    }

    /**
     * Override as necessary Can be null if no name Exists
     *
     * @return Field representing the 'name' Column
     */
    public Field<String> getNameField() {
        Name fieldName = getNameFieldName();
        if(fieldName == null) {
            return null;
        }else {
            return DSL.field(getNameFieldName(), SQLDataType.VARCHAR(getNameFieldLength()).nullable(false));
        }
    }

    /**
     * Optionally override the name of the Xid column
     * @return Name of name column or null if there is not one
     */
    protected Name getNameFieldName() {
        return DSL.name("name");
    }

    /**
     * Optionally override the length of the name column
     * @return
     */
    protected int getNameFieldLength() {
        return 255;
    }

    /**
     * Create a aliased version of this field for this table
     * @param <T>
     * @param field
     * @return
     */
    public <T extends Object> Field<T> getAlias(Field<T> field) {
        return DSL.field(this.alias.append(field.getName()), field.getDataType());
    }

}
