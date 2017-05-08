/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.db.spring;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Matthew Lohbihler
 */
public interface ConnectionCallbackVoid {
    void doInConnection(Connection conn) throws SQLException;
}
