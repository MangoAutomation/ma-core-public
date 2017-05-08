package com.serotonin.db.spring;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.ParameterDisposer;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.StatementCreatorUtils;

/**
 * This is a copy of the class from spring, recreated here because the original is package private.
 */
public class ArgTypePreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {

    private final Object[] args;

    private final int[] argTypes;

    /**
     * Create a new ArgTypePreparedStatementSetter for the given arguments.
     * 
     * @param args
     * @param argTypes
     */
    public ArgTypePreparedStatementSetter(Object[] args, int[] argTypes) {
        if ((args != null && argTypes == null) || (args == null && argTypes != null)
                || (args != null && args.length != argTypes.length)) {
            throw new InvalidDataAccessApiUsageException("args and argTypes parameters must match");
        }
        this.args = args;
        this.argTypes = argTypes;
    }

    public void setValues(PreparedStatement ps) throws SQLException {
        int argIndx = 1;
        if (this.args != null) {
            for (int i = 0; i < this.args.length; i++) {
                Object arg = this.args[i];
                if (arg instanceof Collection<?> && this.argTypes[i] != Types.ARRAY) {
                    Collection<?> entries = (Collection<?>) arg;
                    for (Iterator<?> it = entries.iterator(); it.hasNext();) {
                        Object entry = it.next();
                        StatementCreatorUtils.setParameterValue(ps, argIndx++, this.argTypes[i], null, entry);
                    }
                }
                else if (arg instanceof ByteArrayInputStream && this.argTypes[i] == Types.BINARY) {
                    ByteArrayInputStream bain = (ByteArrayInputStream) arg;
                    ps.setBinaryStream(argIndx++, bain, bain.available());
                }
                else if (arg instanceof InputStream && this.argTypes[i] == Types.BLOB) {
                    ps.setBlob(argIndx++, (InputStream) arg);
                }
                else {
                    StatementCreatorUtils.setParameterValue(ps, argIndx++, this.argTypes[i], null, arg);
                }
            }
        }
    }

    public void cleanupParameters() {
        StatementCreatorUtils.cleanupParameters(this.args);
    }

}
