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
    protected final Field<String> xidField;
    /**
     * tablePrefix.xid
     */
    protected final Field<String> xidAlias;

    /**
     * Field for name column
     */
    protected final Field<String> nameField;
    /**
     * tablePrefix.name
     */
    protected final Field<String> nameAlias;
    
    
    public AbstractTableDefinition(Table<? extends Record> table, Name alias) {
        super(table, alias);
        this.xidField = getXidField();
        this.xidAlias = getXidAlias();
        this.nameField = getNameField();
        this.nameAlias = getNameAlias();
    }


    @Override
    protected void addFields(List<Field<?>> fields) {
        if(this.xidField != null) {
            fields.add(xidField);
        }
        if(this.nameField != null) {
            fields.add(nameField);
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
    
}
