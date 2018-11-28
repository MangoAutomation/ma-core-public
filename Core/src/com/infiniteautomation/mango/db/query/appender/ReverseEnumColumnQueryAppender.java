/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.db.query.appender;

import java.util.ArrayList;
import java.util.List;

import com.infiniteautomation.mango.db.query.ComparisonEnum;
import com.infiniteautomation.mango.db.query.SQLQueryColumn;
import com.infiniteautomation.mango.util.ReverseEnum;

/**
 * @author Jared Wiltshire
 */
public class ReverseEnumColumnQueryAppender<X, E extends Enum<E> & ReverseEnum<X>> extends GenericSQLColumnQueryAppender {

    private final Class<E> enumClass;

    public ReverseEnumColumnQueryAppender(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public void appendSQL(SQLQueryColumn column, StringBuilder selectSql, StringBuilder countSql,
            List<Object> selectArgs, List<Object> columnArgs, ComparisonEnum comparison) {

        if ((columnArgs.size() == 1) && (columnArgs.get(0) == null)) {
            //Catchall for null comparisons
            appendSQL(column.getName(), IS_SQL, selectSql, countSql);
            selectArgs.add(null);
            return;
        }

        // map the String name to the value of the reverse enum
        List<Object> arguments = new ArrayList<Object>();
        for (Object o : columnArgs) {
            if (o instanceof String) {
                E enumValue = Enum.valueOf(enumClass, (String) o);
                arguments.add(enumValue.value());
            } else {
                arguments.add(o);
            }
        }

        super.appendSQL(column, selectSql, countSql, selectArgs, arguments, comparison);
    }

}
