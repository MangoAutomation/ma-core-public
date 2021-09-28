package com.serotonin.db;

import java.util.Collection;
import java.util.Iterator;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.springframework.transaction.PlatformTransactionManager;

import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseType;

public class DaoUtils implements TransactionCapable {
    protected final DataSource dataSource;
    protected final PlatformTransactionManager transactionManager;
    protected final ExtendedJdbcTemplate ejt;

    // Print out times and SQL for RQL Queries
    protected final boolean useMetrics;
    protected final long metricsThreshold;

    protected final DatabaseType databaseType;
    protected final DSLContext create;

    public DaoUtils(DataSource dataSource, PlatformTransactionManager transactionManager, DatabaseType databaseType, DSLContext context, ExtendedJdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
        this.databaseType = databaseType;
        this.useMetrics = Common.envProps.getBoolean("db.useMetrics", false);
        this.metricsThreshold = Common.envProps.getLong("db.metricsThreshold", 0L);
        this.ejt = jdbcTemplate;
        this.create = context;
    }

    //
    // Delimited lists
    /**
     * Bad practice, should be using prepared statements. This is being used to do WHERE x IN(a,b,c)
     */
    @Deprecated
    protected String createDelimitedList(Collection<?> values, String delimiter, String quote) {
        StringBuilder sb = new StringBuilder();
        Iterator<?> iterator = values.iterator();
        boolean first = true;
        while (iterator.hasNext()) {
            if (first)
                first = false;
            else
                sb.append(delimiter);

            if (quote != null)
                sb.append(quote);

            sb.append(iterator.next());

            if (quote != null)
                sb.append(quote);
        }
        return sb.toString();
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

}
