/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.infiniteautomation.mango.monitor.IntegerMonitor;
import com.infiniteautomation.mango.monitor.ValueMonitorOwner;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.db.WideQueryCallback;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.io.StreamUtils;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.ImageSaveException;
import com.serotonin.m2m2.db.DatabaseProxy.DatabaseType;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedIdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.IAnnotated;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.ImageValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.vo.pair.LongPair;
import com.serotonin.metrics.EventHistogram;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.util.CollectionUtils;
import com.serotonin.util.queue.ObjectQueue;

public class PointValueDaoSQL extends BaseDao implements PointValueDao {
    
    private static final Log LOG = LogFactory.getLog(PointValueDao.class);
    
    private static List<UnsavedPointValue> UNSAVED_POINT_VALUES = new ArrayList<UnsavedPointValue>();

    private static final String POINT_VALUE_INSERT_START = "insert into pointValues (dataPointId, dataType, pointValue, ts) values ";
    private static final String POINT_VALUE_INSERT_VALUES = "(?,?,?,?)";
    private static final int POINT_VALUE_INSERT_VALUES_COUNT = 4;
    private static final String POINT_VALUE_INSERT = POINT_VALUE_INSERT_START + POINT_VALUE_INSERT_VALUES;
    private static final String POINT_VALUE_ANNOTATION_INSERT = "insert into pointValueAnnotations "
            + "(pointValueId, textPointValueShort, textPointValueLong, sourceMessage) values (?,?,?,?)";

    /**
     * Only the PointValueCache should call this method during runtime. Only use if the data point is not running.
     */
    @Override
    public PointValueTime savePointValueSync(int pointId, PointValueTime pointValue, SetPointSource source) {
        long id = savePointValueImpl(pointId, pointValue, source, false, null);

        PointValueTime savedPointValue;
        int retries = 5;
        while (true) {
            try {
                savedPointValue = getPointValue(id);
                break;
            }
            catch (ConcurrencyFailureException e) {
                if (retries <= 0)
                    throw e;
                retries--;
            }
        }

        return savedPointValue;
    }

    /**
     * Only the PointValueCache should call this method during runtime. Only use if the data point is not running.
     */
    @Override
    public void savePointValueAsync(int pointId, PointValueTime pointValue, SetPointSource source, Consumer<Long> savedCallback) {
        savePointValueImpl(pointId, pointValue, source, true, savedCallback);
    }

    long savePointValueImpl(final int pointId, final PointValueTime pointValue, final SetPointSource source,
            boolean async, Consumer<Long> savedCallback) {
        DataValue value = pointValue.getValue();
        final int dataType = DataTypes.getDataType(value);
        double dvalue = 0;
        String svalue = null;

        if (dataType == DataTypes.IMAGE) {
            ImageValue imageValue = (ImageValue) value;
            dvalue = imageValue.getType();
            if (imageValue.isSaved())
                svalue = Long.toString(imageValue.getId());
        }
        else if (value.hasDoubleRepresentation())
            dvalue = value.getDoubleValue();
        else
            svalue = value.getStringValue();

        // Check if we need to create an annotation.
        long id;
        try {
            if (svalue != null || source != null || dataType == DataTypes.IMAGE)
                async = false;
            id = savePointValue(pointId, dataType, dvalue, pointValue.getTime(), svalue, source, async, savedCallback);
        }
        catch (ConcurrencyFailureException e) {
            // Still failed to insert after all of the retries. Store the data
            synchronized (UNSAVED_POINT_VALUES) {
                UNSAVED_POINT_VALUES.add(new UnsavedPointValue(pointId, pointValue, source, savedCallback));
            }
            return -1;
        }

        // Check if we need to save an image
        if (dataType == DataTypes.IMAGE) {
            ImageValue imageValue = (ImageValue) value;
            if (!imageValue.isSaved()) {
                imageValue.setId(id);

                File file = new File(Common.getFiledataPath(), imageValue.getFilename());

                // Write the file.
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(file);
                    StreamUtils.transfer(new ByteArrayInputStream(imageValue.getData()), out);
                }
                catch (IOException e) {
                    // Rethrow as an RTE
                    throw new ImageSaveException(e);
                }
                finally {
                    try {
                        if (out != null)
                            out.close();
                    }
                    catch (IOException e) {
                        // no op
                    }
                }

                // Allow the data to be GC'ed
                imageValue.setData(null);
            }
        }

        clearUnsavedPointValues();
        clearUnsavedPointUpdates();

        return id;
    }

    private void clearUnsavedPointValues() {
        if (!UNSAVED_POINT_VALUES.isEmpty()) {
            synchronized (UNSAVED_POINT_VALUES) {
                while (!UNSAVED_POINT_VALUES.isEmpty()) {
                    UnsavedPointValue data = UNSAVED_POINT_VALUES.remove(0);
                    savePointValueImpl(data.getPointId(), data.getPointValue(), data.getSource(), false, data.getSavedCallback());
                }
            }
        }
    }

    long savePointValue(final int pointId, final int dataType, double dvalue, final long time, final String svalue,
            final SetPointSource source, boolean async, Consumer<Long> savedCallback) {
        // Apply database specific bounds on double values.
        dvalue = Common.databaseProxy.applyBounds(dvalue);

        if (async) {
            BatchWriteBehind.add(new BatchWriteBehindEntry(pointId, dataType, dvalue, time, savedCallback), ejt);
            return -1;
        }

        int retries = 5;
        while (true) {
            try {
                return savePointValueImpl(pointId, dataType, dvalue, time, svalue, source);
            }
            catch (ConcurrencyFailureException e) {
                if (retries <= 0)
                    throw e;
                retries--;
            }
            catch (RuntimeException e) {
                throw new RuntimeException("Error saving point value: dataType=" + dataType + ", dvalue=" + dvalue, e);
            }
        }
    }

    private long savePointValueImpl(int pointId, int dataType, double dvalue, long time, String svalue,
            SetPointSource source) {
        long id = doInsertLong(POINT_VALUE_INSERT, new Object[] { pointId, dataType, dvalue, time });

        if (svalue == null && dataType == DataTypes.IMAGE)
            svalue = Long.toString(id);

        // Check if we need to create an annotation.
        TranslatableMessage sourceMessage = null;
        if (source != null)
            sourceMessage = source.getSetPointSourceMessage();

        if (svalue != null || sourceMessage != null) {
            String shortString = null;
            String longString = null;
            if (svalue != null) {
                if (svalue.length() > 128)
                    longString = svalue;
                else
                    shortString = svalue;
            }

            ejt.update(POINT_VALUE_ANNOTATION_INSERT, //
                    new Object[] { id, shortString, longString, writeTranslatableMessage(sourceMessage) }, //
                    new int[] { Types.INTEGER, Types.VARCHAR, Types.CLOB, Types.CLOB });
        }

        return id;
    }

    //Update Point Values
    private static List<UnsavedPointUpdate> UNSAVED_POINT_UPDATES = new ArrayList<UnsavedPointUpdate>();

    private static final String POINT_VALUE_UPDATE = "UPDATE pointValues SET dataType=?, pointValue=? ";
    private static final String POINT_VALUE_ANNOTATION_UPDATE = "UPDATE pointValueAnnotations SET "
            + "textPointValueShort=?, textPointValueLong=?, sourceMessage=? ";

    /**
     * Only the PointValueCache should call this method during runtime. Do not use.
     */
    @Override
    public PointValueTime updatePointValueSync(int dataPointId, PointValueTime pvt, SetPointSource source) {
        long id = updatePointValueImpl(dataPointId, pvt, source, false);

        PointValueTime savedPointValue;
        int retries = 5;
        while (true) {
            try {
                savedPointValue = getPointValue(id);
                break;
            }
            catch (ConcurrencyFailureException e) {
                if (retries <= 0)
                    throw e;
                retries--;
            }
        }

        return savedPointValue;
    }

    /**
     * Only the PointValueCache should call this method during runtime. Do not use.
     */
    @Override
    public void updatePointValueAsync(int pointId, PointValueTime pointValue, SetPointSource source) {
        updatePointValueImpl(pointId, pointValue, source, true);
    }

    long updatePointValueImpl(final int pointId, final PointValueTime pvt, final SetPointSource source, boolean async) {
        DataValue value = pvt.getValue();
        final int dataType = DataTypes.getDataType(value);
        double dvalue = 0;
        String svalue = null;

        if (dataType == DataTypes.IMAGE) {
            ImageValue imageValue = (ImageValue) value;
            dvalue = imageValue.getType();
            if (imageValue.isSaved())
                svalue = Long.toString(imageValue.getId());
        }
        else if (value.hasDoubleRepresentation())
            dvalue = value.getDoubleValue();
        else
            svalue = value.getStringValue();

        // Check if we need to create an annotation.
        long id;
        try {
            if (svalue != null || source != null || dataType == DataTypes.IMAGE)
                async = false;
            id = updatePointValue(pointId, dataType, dvalue, pvt.getTime(), svalue, source, async);
        }
        catch (ConcurrencyFailureException e) {
            // Still failed to insert after all of the retries. Store the data
            synchronized (UNSAVED_POINT_UPDATES) {
                UNSAVED_POINT_UPDATES.add(new UnsavedPointUpdate(pointId, pvt, source));
            }
            return -1;
        }

        // Check if we need to save an image
        if (dataType == DataTypes.IMAGE) {
            ImageValue imageValue = (ImageValue) value;
            if (!imageValue.isSaved()) {
                imageValue.setId(id);

                File file = new File(Common.getFiledataPath(), imageValue.getFilename());

                // Write the file.
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(file);
                    StreamUtils.transfer(new ByteArrayInputStream(imageValue.getData()), out);
                }
                catch (IOException e) {
                    // Rethrow as an RTE
                    throw new ImageSaveException(e);
                }
                finally {
                    try {
                        if (out != null)
                            out.close();
                    }
                    catch (IOException e) {
                        // no op
                    }
                }

                // Allow the data to be GC'ed
                imageValue.setData(null);
            }
        }

        clearUnsavedPointUpdates();

        return id;
    }

    private void clearUnsavedPointUpdates() {
        if (!UNSAVED_POINT_UPDATES.isEmpty()) {
            synchronized (UNSAVED_POINT_UPDATES) {
                while (!UNSAVED_POINT_UPDATES.isEmpty()) {
                    UnsavedPointUpdate data = UNSAVED_POINT_UPDATES.remove(0);
                    updatePointValueImpl(data.getPointId(), data.getPointValue(), data.getSource(), false);
                }
            }
        }
    }

    long updatePointValue(final int dataPointId, final int dataType, double dvalue, final long time,
            final String svalue, final SetPointSource source, boolean async) {
        // Apply database specific bounds on double values.
        dvalue = Common.databaseProxy.applyBounds(dvalue);

        if (async) {
            BatchUpdateBehind.add(new BatchUpdateBehindEntry(dataPointId, dataType, dvalue, time, svalue, source), ejt);
            return -1;
        }

        int retries = 5;
        while (true) {
            try {
                return updatePointValueImpl(dataPointId, dataType, dvalue, time, svalue, source);
            }
            catch (ConcurrencyFailureException e) {
                if (retries <= 0)
                    throw e;
                retries--;
            }
            catch (RuntimeException e) {
                throw new RuntimeException("Error saving point value: dataType=" + dataType + ", dvalue=" + dvalue, e);
            }
        }
    }

    private long updatePointValueImpl(int dataPointId, int dataType, double dvalue, long time, String svalue,
            SetPointSource source) {
        long id = doInsertLong(POINT_VALUE_UPDATE + " WHERE ts = ? AND dataPointId = ?", new Object[] { dataType,
                dvalue, time, dataPointId });

        this.updatePointValueAnnotation(id, dataType, svalue, source);

        return id;
    }

    private void updatePointValueAnnotation(long id, int dataType, String svalue, SetPointSource source) {

        if (svalue == null && dataType == DataTypes.IMAGE)
            svalue = Long.toString(id);

        // Check if we need to create an annotation.
        TranslatableMessage sourceMessage = null;
        if (source != null)
            sourceMessage = source.getSetPointSourceMessage();

        if (svalue != null || sourceMessage != null) {
            String shortString = null;
            String longString = null;
            if (svalue != null) {
                if (svalue.length() > 128)
                    longString = svalue;
                else
                    shortString = svalue;
            }

            ejt.update(POINT_VALUE_ANNOTATION_UPDATE + "WHERE pointValueId = ?", //
                    new Object[] { shortString, longString, writeTranslatableMessage(sourceMessage), id }, //
                    new int[] { Types.VARCHAR, Types.CLOB, Types.CLOB, Types.INTEGER });
        }

    }

    //
    //
    // Queries
    //
    private static final String POINT_VALUE_SELECT = //
    "select pv.dataType, pv.pointValue, pva.textPointValueShort, pva.textPointValueLong, pv.ts, pva.sourceMessage " //
            + "from pointValues pv " //
            + "  left join pointValueAnnotations pva on pv.id = pva.pointValueId";

    //
    //
    // Single point
    //
    @Override
    public PointValueTime getLatestPointValue(int dataPointId) {

        long maxTs = ejt.queryForLong("select max(ts) from pointValues where dataPointId=?",
                new Object[] { dataPointId }, 0);
        if (maxTs == 0)
            return null;
        return pointValueQuery(POINT_VALUE_SELECT + " where pv.dataPointId=? and pv.ts=?", new Object[] { dataPointId,
                maxTs });
    }

    private PointValueTime getPointValue(long id) {
        return pointValueQuery(POINT_VALUE_SELECT + " where pv.id=?", new Object[] { id });
    }

    @Override
    public PointValueTime getPointValueBefore(int dataPointId, long time) {
        Long valueTime = queryForObject("select max(ts) from pointValues where dataPointId=? and ts<?", new Object[] {
                dataPointId, time }, Long.class, null);
        if (valueTime == null)
            return null;
        return getPointValueAt(dataPointId, valueTime);
    }

    @Override
    public PointValueTime getPointValueAt(int dataPointId, long time) {
        return pointValueQuery(POINT_VALUE_SELECT + " where pv.dataPointId=? and pv.ts=?", new Object[] { dataPointId,
                time });
    }

    @Override
    public PointValueTime getPointValueAfter(int dataPointId, long time) {
        Long valueTime = queryForObject("select min(ts) from pointValues where dataPointId=? and ts>=?", new Object[] {
                dataPointId, time }, Long.class, null);
        if (valueTime == null)
            return null;
        return getPointValueAt(dataPointId, valueTime);
    }

    private PointValueTime pointValueQuery(String sql, Object[] params) {
        List<PointValueTime> result = pointValuesQuery(sql, params, 1);
        if (result.size() == 0)
            return null;
        return result.get(0);
    }

    //
    //
    // Values lists
    //
    @Override
    public List<PointValueTime> getPointValues(int dataPointId, long since) {
        return pointValuesQuery(POINT_VALUE_SELECT + " where pv.dataPointId=? and pv.ts >= ? order by ts",
                new Object[] { dataPointId, since }, 0);
    }

    @Override
    public List<PointValueTime> getPointValuesBetween(int dataPointId, long from, long to) {
        return pointValuesQuery(POINT_VALUE_SELECT + " where pv.dataPointId=? and pv.ts >= ? and pv.ts<? order by ts",
                new Object[] { dataPointId, from, to }, 0);
    }
    
    @Override
    public List<PointValueTime> getPointValuesBetween(int dataPointId, long from, long to, int limit) {
        return pointValuesQuery(POINT_VALUE_SELECT + " where pv.dataPointId=? and pv.ts >= ? and pv.ts<? order by ts",
                new Object[] { dataPointId, from, to }, limit);
    }

    @Override
    public List<PointValueTime> getLatestPointValues(int dataPointId, int limit) {
        if (limit == 0)
            return Collections.emptyList();
        if (limit == 1) {
            PointValueTime pvt = getLatestPointValue(dataPointId);
            if(pvt != null)
                return CollectionUtils.toList(pvt);
            else
                return new ArrayList<PointValueTime>();
        }
        return pointValuesQuery(POINT_VALUE_SELECT + " where pv.dataPointId=? order by pv.ts desc",
                new Object[] { dataPointId }, limit);
    }

    @Override
    public List<PointValueTime> getLatestPointValues(int dataPointId, int limit, long before) {
        return pointValuesQuery(POINT_VALUE_SELECT + " where pv.dataPointId=? and pv.ts<? order by pv.ts desc",
                new Object[] { dataPointId, before }, limit);
    }
    
    @Override
    public void getLatestPointValues(int dataPointId, long before, Integer limit, final PVTQueryCallback<PointValueTime> callback) {
        LatestSinglePointValuesPreparedStatementCreator stmt = new LatestSinglePointValuesPreparedStatementCreator(dataPointId, before, limit, callback);
        ejt.execute(stmt, stmt);
    }
    
    private List<PointValueTime> pointValuesQuery(String sql, Object[] params, int limit) {
        return Common.databaseProxy.doLimitQuery(this, sql, params, new PointValueRowMapper(), limit);
    }
    
    /**
     * Container for Latest Single Data Point Query with/without limits
     * 
     * @author Terry Packer
     */
    class LatestSinglePointValuesPreparedStatementCreator implements PreparedStatementCreator, PreparedStatementCallback<Integer> {
        
        final PointValueRowMapper mapper = new PointValueRowMapper();
        final int dataPointId;
        final long before;
        final Integer limit;
        final PVTQueryCallback<PointValueTime> callback;
        int counter;
        
        public LatestSinglePointValuesPreparedStatementCreator(Integer id, long before, Integer limit, PVTQueryCallback<PointValueTime> callback) {
            this.dataPointId = id;
            this.before = before;
            this.limit = limit;
            this.callback = callback;
            this.counter = 0;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection con)
                throws SQLException {
    
            Object[] args = new Object[] {dataPointId, before};
            String sql = POINT_VALUE_SELECT + " where pv.dataPointId=? and pv.ts<? order by pv.ts desc";
           
            if(limit != null) {
                sql = Common.databaseProxy.getLimitQuerySql(sql, limit);
            }
            
            PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ArgumentPreparedStatementSetter setter = new ArgumentPreparedStatementSetter(args);
            setter.setValues(stmt);
            return stmt;
        }
        
        @Override
        public Integer doInPreparedStatement(PreparedStatement ps)
                throws SQLException, DataAccessException {
            ResultSet rs = null;
            try {
                ps.execute();
                rs = ps.getResultSet();
                while(rs.next()) {
                    PointValueTime value = mapper.mapRow(rs, counter);
                    callback.row(value, counter);
                    counter++;
                }
            }catch(IOException e) {
                LOG.warn("Cancelling Latest Point Value Query.", e);
                ps.cancel();
            }finally {
                JdbcUtils.closeResultSet(rs);
            }
            return counter;
        }
    }
    
    /**
     * Container for Latest Single Data Point Query with/without limits
     * 
     * @author Terry Packer
     */
    class LatestSingleIdPointValuesPreparedStatementCreator implements PreparedStatementCreator, PreparedStatementCallback<Integer>{
        
        final AnnotatedIdPointValueRowMapper mapper = new AnnotatedIdPointValueRowMapper();

        final List<Integer> ids;
        final long before;
        final Integer limit;
        final PVTQueryCallback<IdPointValueTime> callback;
        final MutableInt counter;
        
        public LatestSingleIdPointValuesPreparedStatementCreator(Integer id, long before, Integer limit, PVTQueryCallback<IdPointValueTime> callback, MutableInt counter) {
            this.ids = new ArrayList<Integer>();
            this.ids.add(id);
            this.before = before;
            this.limit = limit;
            this.callback = callback;
            this.counter = counter;
        }
        
        public LatestSingleIdPointValuesPreparedStatementCreator(List<Integer> ids, long before, Integer limit, PVTQueryCallback<IdPointValueTime> callback, MutableInt counter) {
            this.ids = ids;
            this.before = before;
            this.limit = limit;
            this.callback = callback;
            this.counter = counter;
        }
        
        @Override
        public PreparedStatement createPreparedStatement(Connection con)
                throws SQLException {
            if(ids.size() != 1)
                throw new RuntimeException("Wrong base query.");
            
            Object[] args = new Object[] {ids.get(0), before};
            String sql = ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId = ? and pv.ts < ? order by pv.ts desc";

            if(limit != null) {
                sql = Common.databaseProxy.getLimitQuerySql(sql, limit);
            }
            
            PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ArgumentPreparedStatementSetter setter = new ArgumentPreparedStatementSetter(args);
            setter.setValues(stmt);
            return stmt;
        }
        
        @Override
        public Integer doInPreparedStatement(PreparedStatement ps)
                throws SQLException, DataAccessException {
            ResultSet rs = null;
            try {
                ps.execute();
                rs = ps.getResultSet();
                while(rs.next()) {
                    IdPointValueTime value = mapper.mapRow(rs, counter.getValue());
                    callback.row(value, counter.getValue());
                    counter.increment();
                }
            }catch(IOException e) {
                LOG.warn("Cancelling Latest Point Value Query.", e);
                ps.cancel();
            }finally {
                JdbcUtils.closeResultSet(rs);
            }
            return counter.getValue();
        }
    }
    
    /**
     * Query for the latest values for many data points in time descending order with an optional limit, 
     * the limit is applied to the query such that you may not get data for all points
     *
     * @author Terry Packer
     */
    class LatestMultiplePointsValuesPreparedStatementCreator extends LatestSingleIdPointValuesPreparedStatementCreator{
        
        final AnnotatedIdPointValueRowMapper mapper = new AnnotatedIdPointValueRowMapper();
        
        public LatestMultiplePointsValuesPreparedStatementCreator(List<Integer> ids, long before,
                Integer limit, PVTQueryCallback<IdPointValueTime> callback) {
            super(ids, before, limit, callback, new MutableInt(0));
        }
        
        @Override
        public PreparedStatement createPreparedStatement(Connection con)
                throws SQLException {
            
            if(ids.size() == 1)
                return super.createPreparedStatement(con);

            Object[] args = new Object[] {before};
            String dataPointIds = createDelimitedList(ids, ",", null);
            String sql = ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId in (" + dataPointIds + ") and pv.ts < ? order by pv.ts desc";
            
            if(limit != null) {
                sql = Common.databaseProxy.getLimitQuerySql(sql, limit);
            }   
            
            PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ArgumentPreparedStatementSetter setter = new ArgumentPreparedStatementSetter(args);
            setter.setValues(stmt);
            return stmt;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValues(java.util.List, long, boolean, java.lang.Integer, com.infiniteautomation.mango.db.query.PVTQueryCallback)
     */
    @Override
    public void getLatestPointValues(List<Integer> ids, long before, boolean orderById, Integer limit, PVTQueryCallback<IdPointValueTime> callback){
        if(ids.size() == 0)
            return;
        if(orderById) {
            //Limit results of each data point to size limit, i.e. loop over all points and query with limit
            MutableInt counter = new MutableInt(0);
            for(Integer id: ids) {
                LatestSingleIdPointValuesPreparedStatementCreator c = new LatestSingleIdPointValuesPreparedStatementCreator(
                        id, before, limit, callback, counter);
                ejt.execute(c,c);
            }
        }else {
            //Limit total results to limit
            LatestMultiplePointsValuesPreparedStatementCreator lmpvpsc = new LatestMultiplePointsValuesPreparedStatementCreator(ids, before, limit, callback);
            ejt.execute(lmpvpsc, lmpvpsc);
        }        
    }

    //
    //
    // Query with callback
    //
    @Override
    public void getPointValuesBetween(int dataPointId, long from, long to, MappedRowCallback<PointValueTime> callback) {
        query(POINT_VALUE_SELECT + " where pv.dataPointId=? and pv.ts >= ? and pv.ts<? order by ts", new Object[] {
                dataPointId, from, to }, new PointValueRowMapper(), callback);
    }
    
    /**
     * Single point value time range logic
     *
     * @author Terry Packer
     */
    class TimeRangeSinglePointValuesPreparedStatementCreator<T extends PVTQueryCallback<IdPointValueTime>> implements PreparedStatementCreator, PreparedStatementCallback<Integer>{
        
        final AnnotatedIdPointValueRowMapper mapper = new AnnotatedIdPointValueRowMapper();
        
        final List<Integer> ids;
        final long from;
        final long to;
        final Integer limit;
        final T callback;
        MutableInt counter;
        
        public TimeRangeSinglePointValuesPreparedStatementCreator(Integer id, long from, long to, Integer limit, T callback, MutableInt counter) {
            this.ids = new ArrayList<>();
            this.ids.add(id);
            this.from = from;
            this.to = to;
            this.limit = limit;
            this.callback = callback;
            this.counter = counter;
        }
        
        public TimeRangeSinglePointValuesPreparedStatementCreator(List<Integer> ids, long from, long to, Integer limit, T callback, MutableInt counter) {
            this.ids = ids;
            this.from = from;
            this.to = to;
            this.limit = limit;
            this.callback = callback;
            this.counter = counter;
        }
        
        @Override
        public PreparedStatement createPreparedStatement(Connection con)
                throws SQLException {
            if(ids.size() != 1)
                throw new RuntimeException("Wrong base query.");
            
            List<Object> args = new ArrayList<>();
            String sql = ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId = ? and pv.ts >= ? and pv.ts<? order by pv.ts asc";
            args.add(ids.get(0));
            args.add(from);
            args.add(to);
            if(limit != null) {
                sql += " limit ?";
                args.add(limit);
            }
            
            PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ArgumentPreparedStatementSetter setter = new ArgumentPreparedStatementSetter(args.toArray(new Object[args.size()]));
            setter.setValues(stmt);
            return stmt;
        }
        
        @Override
        public Integer doInPreparedStatement(PreparedStatement ps)
                throws SQLException, DataAccessException {
            ResultSet rs = null;
            try {
                ps.execute();
                rs = ps.getResultSet();
                while(rs.next()) {
                    IdPointValueTime value = mapper.mapRow(rs, counter.getValue());
                    callback.row(value, counter.getValue());
                    counter.increment();
                }
            }catch(IOException e) {
                LOG.warn("Cancelling Time Range Point Value Query.", e);
                ps.cancel();
            }finally {
                JdbcUtils.closeResultSet(rs);
            }
            return counter.getValue();
        }
    }
    
    /**
     * Query multiple points over a range
     *
     * @author Terry Packer
     */
    class TimeRangeMultiplePointsValuesPreparedStatementCreator<T extends PVTQueryCallback<IdPointValueTime>> extends TimeRangeSinglePointValuesPreparedStatementCreator<T> {

        public TimeRangeMultiplePointsValuesPreparedStatementCreator(List<Integer> ids, long from, long to,
                Integer limit, T callback) {
            super(ids, from, to, limit, callback, new MutableInt(0));
        }
        
        @Override
        public PreparedStatement createPreparedStatement(Connection con)
                throws SQLException {
            
            if(ids.size() == 1)
                return super.createPreparedStatement(con);
            
            List<Object> args = new ArrayList<>();
            String dataPointIds = createDelimitedList(ids, ",", null);
            String sql = ANNOTATED_POINT_ID_VALUE_SELECT + " WHERE pv.dataPointId in (" + dataPointIds + ") AND pv.ts >= ? AND pv.ts<? ORDER BY pv.ts ASC";
            args.add(from);
            args.add(to);
            
            if(limit != null) {
                sql += " limit ?";
                args.add(limit);
            }
            
            PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ArgumentPreparedStatementSetter setter = new ArgumentPreparedStatementSetter(args.toArray(new Object[args.size()]));
            setter.setValues(stmt);
            return stmt;
        }
    }
    
    @Override
    public void getPointValuesBetween(List<Integer> ids, long from, long to, boolean orderById, Integer limit, PVTQueryCallback<IdPointValueTime> callback) {
        if(ids.size() == 0)
            return;
        if(orderById) {
            //Limit results of each data point to size limit, i.e. loop over all points and query with limit
            MutableInt counter = new MutableInt(0);
            for(Integer id: ids) {
                TimeRangeSinglePointValuesPreparedStatementCreator<PVTQueryCallback<IdPointValueTime>> c = 
                        new TimeRangeSinglePointValuesPreparedStatementCreator<PVTQueryCallback<IdPointValueTime>>(id, from, to, limit, callback, counter);
                ejt.execute(c,c);
            }
        }else {
            //Limit total results to limit
            TimeRangeMultiplePointsValuesPreparedStatementCreator<PVTQueryCallback<IdPointValueTime>> c = 
                    new TimeRangeMultiplePointsValuesPreparedStatementCreator<PVTQueryCallback<IdPointValueTime>>(ids, from, to, limit, callback);
            ejt.execute(c, c);
        }  
    }

	@Override
	public void wideQuery(int pointId, long from, long to, WideQueryCallback<PointValueTime> callback) {
        // TODO Improve performance by using one statement and using the exceptions to cancel the results
	    PointValueTime pvt = this.getPointValueBefore(pointId, from);
	    if(pvt != null)
	        callback.preQuery(pvt);
        this.getPointValuesBetween(pointId, from, to, new MappedRowCallback<PointValueTime>() {
            @Override
            public void row(PointValueTime value, int index) {
                callback.row(value, index);
            }
        });
        pvt = this.getPointValueAfter(pointId, to);
        if(pvt != null)
            callback.postQuery(pvt);
	}
    
	
    private static final String ANNOTATED_POINT_ID_VALUE_SELECT = "select pv.dataPointId, pv.dataType, pv.pointValue, " //
            + "pva.textPointValueShort, pva.textPointValueLong, pv.ts, pva.sourceMessage "
            + "from pointValues pv "
            + "  left join pointValueAnnotations pva on pv.id = pva.pointValueId";


    /**
     * Process a cancellable bookend time range query
     *
     * @author Terry Packer
     */
    class BookendSinglePointValuesPreparedStatementCreator extends TimeRangeSinglePointValuesPreparedStatementCreator<BookendQueryCallback<IdPointValueTime>> {
        
        //Single Value Queue for writing values in order
        protected final Map<Integer, IdPointValueTime> values;
        //Create a statement specifically for fetching the first values.
        protected PreparedStatement firstValuesSelect;
        
        public BookendSinglePointValuesPreparedStatementCreator(Integer id, long from, long to,
                Integer limit, BookendQueryCallback<IdPointValueTime> callback, MutableInt counter) {
            super(id, from, to, limit, callback, counter);
            this.values = new HashMap<>(1);
        }
        
        public BookendSinglePointValuesPreparedStatementCreator(List<Integer> ids, long from, long to,
                Integer limit, BookendQueryCallback<IdPointValueTime> callback, MutableInt counter) {
            super(ids, from, to, limit, callback, counter);
            this.values = new HashMap<>(ids.size());
        }
        
        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            if(ids.size() != 1)
                throw new RuntimeException("Wrong base query.");
            
            String startValueSql = "";
            List<Object> startValueArgs = new ArrayList<>(2);
            startValueSql = startValueSql + ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId=? AND pv.ts <= ? ORDER BY ts DESC LIMIT 1 ";
            startValueArgs.add(ids.get(0));
            startValueArgs.add(from);
            
            firstValuesSelect = con.prepareStatement(startValueSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ArgumentPreparedStatementSetter setter = new ArgumentPreparedStatementSetter(startValueArgs.toArray(new Object[startValueArgs.size()]));
            setter.setValues(firstValuesSelect);
            
            List<Object> args = new ArrayList<>(3); //pv.ts > ? because firstValueSelect is special
            String sql = ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId = ? and pv.ts > ? and pv.ts<? order by pv.ts asc";
            args.add(ids.get(0));
            args.add(from);
            args.add(to);
            if(limit != null) {
                sql += " limit ?";
                args.add(limit);
            }
            
            PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            setter = new ArgumentPreparedStatementSetter(args.toArray(new Object[args.size()]));
            setter.setValues(stmt);
            return stmt;
        }
        
        @Override
        public Integer doInPreparedStatement(PreparedStatement ps)
                throws SQLException, DataAccessException {
            ResultSet rs = null;
            MutableInt realSamples = new MutableInt(0);
            try {
                firstValuesSelect.execute();
                rs = firstValuesSelect.getResultSet();
                while(rs.next()) {
                    IdPointValueTime current = mapper.mapRow(rs, counter.getValue());
                    if(current.getTime() == from) {
                        callback.firstValue(current, counter.getAndIncrement(), false);
                        realSamples.increment();
                    } else {
                        IdPointValueTime fakeValue;
                        if(current instanceof IAnnotated)
                            fakeValue = new AnnotatedIdPointValueTime(current.getId(), current.getValue(), from, 
                                    ((IAnnotated)current).getSourceMessage());
                        else
                            fakeValue = new IdPointValueTime(current.getId(), current.getValue(), from);
                        callback.firstValue(fakeValue, counter.getAndIncrement(), true);
                    }
                    values.put(current.getId(), current);
                }
                
                for(Integer id : ids)
                    if(!values.containsKey(id))
                        callback.firstValue(new IdPointValueTime(id, null, from), counter.getAndIncrement(), true);
            } catch(IOException e) {
                LOG.warn("Cancelling Time Range Point Value Query.", e);
                firstValuesSelect.cancel();
                ps.cancel();
                return counter.getValue();
            } finally {
                JdbcUtils.closeResultSet(rs);
            }
            try {
                ps.execute();
                rs = ps.getResultSet();
                
                //Process the data in time order, saving the current value for use in the lastValue callback at the end.
                while(rs.next()) {
                    IdPointValueTime current = mapper.mapRow(rs, counter.getValue());
                    values.put(current.getId(), current);
                    callback.row(current, counter.getAndIncrement());
                    if(limit != null && realSamples.incrementAndGet() == limit)
                        break;
                }
                
                for(IdPointValueTime current : values.values()) {
                    IdPointValueTime fakeValue;
                    if(current instanceof IAnnotated)
                        fakeValue = new AnnotatedIdPointValueTime(current.getId(), current.getValue(), to, 
                                ((IAnnotated)current).getSourceMessage());
                    else
                        fakeValue = new IdPointValueTime(current.getId(), current.getValue(), to);
                    callback.lastValue(fakeValue, counter.getAndIncrement(), true);
                }
                
                for(Integer id : ids) {
                    if(!values.containsKey(id))
                        callback.lastValue(new IdPointValueTime(id, null, to), counter.getAndIncrement(), true);
                }
            }catch(IOException e) {
                LOG.warn("Cancelling Time Range Point Value Query.", e);
                ps.cancel();
            }finally {
                JdbcUtils.closeResultSet(rs);
            }
            return counter.getValue();
        }
    }

    /**
     * Bookend for mulitple points
     *
     * @author Terry Packer
     */
    class BookendMultiplePointValuesPreparedStatementCreator extends BookendSinglePointValuesPreparedStatementCreator {
        
        
        public BookendMultiplePointValuesPreparedStatementCreator(List<Integer> ids, long from, long to,
                Integer limit, BookendQueryCallback<IdPointValueTime> callback) {
            super(ids, from, to, limit, callback, new MutableInt(0));
        }
        
        public PreparedStatement createPreparedStatement(Connection con)
                throws SQLException {
            
            if(ids.size() == 1)
                return super.createPreparedStatement(con);
            
            boolean first = true;
            String startValueSql = "";
            List<Object> startValueArgs = new ArrayList<>(ids.size()*2);
            for(Integer seriesId : ids) {
                if(first)
                    first = false;
                else
                    startValueSql += " UNION ";
                startValueSql = startValueSql + "(" + ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId=? AND pv.ts <= ? ORDER BY ts DESC LIMIT 1) ";
                startValueArgs.add(seriesId);
                startValueArgs.add(from);
            }
            
            firstValuesSelect = con.prepareStatement(startValueSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ArgumentPreparedStatementSetter setter = new ArgumentPreparedStatementSetter(startValueArgs.toArray(new Object[startValueArgs.size()]));
            setter.setValues(firstValuesSelect);
            
            List<Object> args = new ArrayList<>(ids.size()*2);
            String dataPointIds = createDelimitedList(ids, ",", null);
            String sql = ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId in (" + dataPointIds + ") AND pv.ts >= ? AND pv.ts<? ORDER BY pv.ts ASC";
            args.add(from+1); //handle from in the startValueSql
            args.add(to);
            
            if(limit != null) {
                sql += " limit ?";
                args.add(limit);
            }
            
            PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            setter = new ArgumentPreparedStatementSetter(args.toArray(new Object[args.size()]));
            setter.setValues(stmt);
            return stmt;
        }
    }
    
    
    @Override
    public void wideBookendQuery(List<Integer> ids, long from, long to, boolean orderById, Integer limit, BookendQueryCallback<IdPointValueTime> callback){
        if(ids.size() == 0)
            return;
        if(orderById) {
            //Limit results of each data point to size limit, i.e. loop over all points and query with limit
            MutableInt counter = new MutableInt(0);
            for(Integer id: ids) {
                BookendSinglePointValuesPreparedStatementCreator c = 
                        new BookendSinglePointValuesPreparedStatementCreator(id, from, to, limit, callback, counter);
                ejt.execute(c,c);
            }
        }else {
            //Limit total results to limit
            BookendMultiplePointValuesPreparedStatementCreator c = 
                    new BookendMultiplePointValuesPreparedStatementCreator(ids, from, to, limit, callback);
            ejt.execute(c, c);
        }     
    }
    
    class PointValueRowMapper implements RowMapper<PointValueTime> {
        @Override
        public PointValueTime mapRow(ResultSet rs, int rowNum) throws SQLException {
            DataValue value = createDataValue(rs, 1);
            long time = rs.getLong(5);

            TranslatableMessage sourceMessage = BaseDao.readTranslatableMessage(rs, 6);
            if (sourceMessage == null)
                // No annotations, just return a point value.
                return new PointValueTime(value, time);

            // There was a source for the point value, so return an annotated version.
            return new AnnotatedPointValueTime(value, time, sourceMessage);
        }
    }

    DataValue createDataValue(ResultSet rs, int firstParameter) throws SQLException {
        int dataType = rs.getInt(firstParameter);
        DataValue value;
        switch (dataType) {
        case (DataTypes.NUMERIC):
            value = new NumericValue(rs.getDouble(firstParameter + 1));
            break;
        case (DataTypes.BINARY):
            value = new BinaryValue(rs.getDouble(firstParameter + 1) == 1);
            break;
        case (DataTypes.MULTISTATE):
            value = new MultistateValue(rs.getInt(firstParameter + 1));
            break;
        case (DataTypes.ALPHANUMERIC):
            String s = rs.getString(firstParameter + 2);
            if (s == null)
                s = rs.getString(firstParameter + 3);
            value = new AlphanumericValue(s);
            break;
        case (DataTypes.IMAGE):
            value = new ImageValue(Integer.parseInt(rs.getString(firstParameter + 2)), rs.getInt(firstParameter + 1));
            break;
        default:
            value = null;
        }
        return value;
    }

    //
    //
    // Multiple-point callback for point history replays
    //
    @Override
    public void getPointValuesBetween(List<Integer> dataPointIds, long from, long to,
            MappedRowCallback<IdPointValueTime> callback) {
        String ids = createDelimitedList(dataPointIds, ",", null);
        query(ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId in (" + ids + ") and pv.ts >= ? and pv.ts<? order by ts",
                new Object[] { from, to }, new AnnotatedIdPointValueRowMapper(), callback);
    }
    
    /**
     * Bring across source translation
     *
     * @author Terry Packer
     */
    class AnnotatedIdPointValueRowMapper implements RowMapper<IdPointValueTime> {
        @Override
        public IdPointValueTime mapRow(ResultSet rs, int rowNum) throws SQLException {
            int dataPointId = rs.getInt(1);
            DataValue value = createDataValue(rs, 2);
            long time = rs.getLong(6);
            TranslatableMessage sourceMessage = BaseDao.readTranslatableMessage(rs, 7);
                if (sourceMessage != null)
                    return new AnnotatedIdPointValueTime(dataPointId, value, time, sourceMessage);
                else
                    return new IdPointValueTime(dataPointId, value, time);
        }
    }

    //
    //
    // Point value deletions
    //

    public long deletePointValue(int pointValueId) {
        return deletePointValues("delete from pointValues where id = ?", new Object[] { pointValueId }, 0, 0);
    }

    @Override
    public long deletePointValue(int dataPointId, long ts) {
        return deletePointValues("delete from pointValues where dataPointId = ? AND ts = ?", new Object[] {
                dataPointId, ts }, 0, 0);
    }

    @Override
    public long deletePointValuesBefore(int dataPointId, long time) {
        return deletePointValues("delete from pointValues where dataPointId=? and ts<?", new Object[] { dataPointId,
                time }, 0, 0);
    }
    
    @Override
    public long deletePointValuesBetween(int dataPointId, long startTime, long endTime) {
        return deletePointValues("delete from pointValues where dataPointId=? and ts>=? and ts<?", new Object[] { dataPointId,
                startTime, endTime }, 0, 0);
    }

    /*
     * (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValuesBeforeWithoutCount(int, long)
     */
    @Override
    public boolean deletePointValuesBeforeWithoutCount(int dataPointId, long time){
    	return deletePointValuesBefore(dataPointId, time) > 0;
    }
    
    @Override
    public long deletePointValues(int dataPointId) {
        return deletePointValues("delete from pointValues where dataPointId=?", new Object[] { dataPointId }, 0, 0);
    }

    public boolean deletePointValuesWithoutCount(int dataPointId) {
        return deletePointValues("delete from pointValues where dataPointId=?", new Object[] { dataPointId }, 0, 0) > 0;
    }
    
    @Override
    public long deleteAllPointData() {
        return deletePointValues("delete from pointValues", null, 0, 0);
    }
    
    /*
     * (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteAllPointDataWithoutCount()
     */
    @Override
    public void deleteAllPointDataWithoutCount() {
        deletePointValues("delete from pointValues", null, 0, 0);
    }

    @Override
    public long deleteOrphanedPointValues() {
        return deletePointValues("DELETE FROM pointValues WHERE dataPointId NOT IN (SELECT ID FROM dataPoints)", null,
                5000, 100000);
    }

    @Override
    public void deleteOrphanedPointValuesWithoutCount() {
        deletePointValues("DELETE FROM pointValues WHERE dataPointId NOT IN (SELECT ID FROM dataPoints)", null,
                5000, 100000);
    }
    
    @Override
    public void deleteOrphanedPointValueAnnotations() {
        RowMapper<Long> rm = new RowMapper<Long>() {
            @Override
            public Long mapRow(ResultSet rs, int row) throws SQLException {
                return rs.getLong(1);
            }
        };
        int limit = 1000;
        while (true) {
            List<Long> ids = Common.databaseProxy.doLimitQuery(this,
                    "select pointValueId from pointValueAnnotations pa "
                            + "left join pointValues p on pa.pointValueId=p.id where p.id is null", null, rm, limit);

            if (ids.isEmpty())
                break;

            String idStr = createDelimitedList(ids, ",", null);
            ejt.update("delete from pointValueAnnotations where pointValueId in (" + idStr + ")");
            if (ids.size() < limit)
                break;
        }
    }

    private long deletePointValues(String sql, Object[] params, int chunkWait, int limit) {
        long cnt = Common.databaseProxy.doLimitDelete(ejt, sql, params, 1000, chunkWait, limit);
        clearUnsavedPointValues();
        return cnt;
    }

    /**
     * There WAS a bug here where the end date should be exclusive! The TCP Persistent publisher expects it to be
     * exclusive,
     * but as for what ramifications it will have to other modules who knows.
     * 
     * For example if one uses this method to count a range and then a select point values between, the results can be
     * different!
     * 
     * This has been changed to be exclusive of End time as the NoSQL DB uses exclusive queries and this needs to 
     * match for the Persistent TCP Module to work across various Data stores.
     * 
     */
    @Override
    public long dateRangeCount(int dataPointId, long from, long to) {
        return ejt.queryForLong("select count(*) from pointValues where dataPointId=? and ts>=? and ts<?",
                new Object[] { dataPointId, from, to }, 0l);
    }

    @Override
    public long getInceptionDate(int dataPointId) {
        return ejt
                .queryForLong("select min(ts) from pointValues where dataPointId=?", new Object[] { dataPointId }, -1);
    }

    @Override
    public long getStartTime(List<Integer> dataPointIds) {
        if (dataPointIds.isEmpty())
            return -1;
        return ejt.queryForLong("select min(ts) from pointValues where dataPointId in ("
                + createDelimitedList(dataPointIds, ",", null) + ")", null, 0l);
    }

    @Override
    public long getEndTime(List<Integer> dataPointIds) {
        if (dataPointIds.isEmpty())
            return -1;
        return ejt.queryForLong("select max(ts) from pointValues where dataPointId in ("
                + createDelimitedList(dataPointIds, ",", null) + ")", null, -1l);
    }

    @Override
    public LongPair getStartAndEndTime(List<Integer> dataPointIds) {
        if (dataPointIds.isEmpty())
            return null;
        return queryForObject(
                "select min(ts),max(ts) from pointValues where dataPointId in ("
                        + createDelimitedList(dataPointIds, ",", null) + ")", null, new RowMapper<LongPair>() {
                    @Override
                    public LongPair mapRow(ResultSet rs, int index) throws SQLException {
                        long l = rs.getLong(1);
                        if (rs.wasNull())
                            return null;
                        return new LongPair(l, rs.getLong(2));
                    }
                }, null);
    }

    @Override
    public List<Long> getFiledataIds(int dataPointId) {
        return queryForList("select id from pointValues where dataPointId=? and dataType=? ", new Object[] {
                dataPointId, DataTypes.IMAGE }, Long.class);
    }

    /**
     * Class that stored point value data when it could not be saved to the database due to concurrency errors.
     * 
     * @author Matthew Lohbihler
     */
    class UnsavedPointValue {
        private final int pointId;
        private final PointValueTime pointValue;
        private final SetPointSource source;
        private final Consumer<Long> savedCallback;
        
        public UnsavedPointValue(int pointId, PointValueTime pointValue, SetPointSource source, Consumer<Long> savedCallback) {
            this.pointId = pointId;
            this.pointValue = pointValue;
            this.source = source;
            this.savedCallback = savedCallback;
        }

        public int getPointId() {
            return pointId;
        }

        public PointValueTime getPointValue() {
            return pointValue;
        }

        public SetPointSource getSource() {
            return source;
        }

        public Consumer<Long> getSavedCallback() {
            return savedCallback;
        }
    }

    /**
     * Class that stored point value data when it could not be saved to the database due to concurrency errors.
     * 
     * @author Matthew Lohbihler
     */
    class UnsavedPointUpdate {
        private final int pointId;
        private final PointValueTime pointValue;
        private final SetPointSource source;

        public UnsavedPointUpdate(int pointId, PointValueTime pointValue, SetPointSource source) {
            this.pointId = pointId;
            this.pointValue = pointValue;
            this.source = source;
        }

        public int getPointId() {
            return pointId;
        }

        public PointValueTime getPointValue() {
            return pointValue;
        }

        public SetPointSource getSource() {
            return source;
        }
    }

    class BatchWriteBehindEntry {
        private final int pointId;
        private final int dataType;
        private final double dvalue;
        private final long time;
        private final Consumer<Long> savedCallback;

        public BatchWriteBehindEntry(int pointId, int dataType, double dvalue, long time, Consumer<Long> savedCallback) {
            this.pointId = pointId;
            this.dataType = dataType;
            this.dvalue = dvalue;
            this.time = time;
            this.savedCallback = savedCallback;
        }

        public void writeInto(Object[] params, int index) {
            index *= POINT_VALUE_INSERT_VALUES_COUNT;
            params[index++] = pointId;
            params[index++] = dataType;
            params[index++] = dvalue;
            params[index++] = time;
        }
        
        public void callback() {
            if(savedCallback != null)
                savedCallback.accept(time);
        }
    }

    public static final String ENTRIES_MONITOR_ID = "com.serotonin.m2m2.db.dao.PointValueDao$BatchWriteBehind.ENTRIES_MONITOR";
    public static final String INSTANCES_MONITOR_ID = "com.serotonin.m2m2.db.dao.PointValueDao$BatchWriteBehind.INSTANCES_MONITOR";
    public static final String BATCH_WRITE_SPEED_MONITOR_ID = "com.serotonin.m2m2.db.dao.PointValueDao$BatchWriteBehind.BATCH_WRITE_SPEED_MONITOR";
    final static EventHistogram writesPerSecond = new EventHistogram(5000, 2);

    private static final ValueMonitorOwner valueOwner = new ValueMonitorOwner(){

		@Override
		public void reset(String id) {
			switch(id){
				case ENTRIES_MONITOR_ID:
					synchronized (BatchWriteBehind.ENTRIES) {
						BatchWriteBehind.ENTRIES_MONITOR.setValue(BatchWriteBehind.ENTRIES.size());
					}
				break;
				case INSTANCES_MONITOR_ID:
					BatchWriteBehind.INSTANCES_MONITOR.setValue(BatchWriteBehind.instances.size());
				break;
				case BATCH_WRITE_SPEED_MONITOR_ID:
					//No-Op since we can't see the speed, perhaps set to 0?
				break;
				case ENTRIES_UPDATE_MONITOR_ID:
					synchronized(BatchUpdateBehind.ENTRIES){
						BatchUpdateBehind.ENTRIES_MONITOR.setValue(BatchUpdateBehind.ENTRIES.size());
					}
				break;
				case INSTANCES_UPDATE_MONITOR_ID:
					BatchUpdateBehind.INSTANCES_MONITOR.setValue(BatchUpdateBehind.instances.size());
				break;
			}
		}
    	
    };
    
    static class BatchWriteBehind implements WorkItem {
        private static final ObjectQueue<BatchWriteBehindEntry> ENTRIES = new ObjectQueue<PointValueDaoSQL.BatchWriteBehindEntry>();
        private static final CopyOnWriteArrayList<BatchWriteBehind> instances = new CopyOnWriteArrayList<BatchWriteBehind>();
        private static Log LOG = LogFactory.getLog(BatchWriteBehind.class);
        private static final int SPAWN_THRESHOLD = 10000;
        private static final int MAX_INSTANCES = 5;
        private static int MAX_ROWS = 1000;
        private static final IntegerMonitor ENTRIES_MONITOR = new IntegerMonitor(ENTRIES_MONITOR_ID,
        		new TranslatableMessage("internal.monitor.BATCH_ENTRIES"), valueOwner);
        private static final IntegerMonitor INSTANCES_MONITOR = new IntegerMonitor(INSTANCES_MONITOR_ID,
        		new TranslatableMessage("internal.monitor.BATCH_INSTANCES"), valueOwner);
        //TODO Create DoubleMonitor but will need to upgrade the Internal data source to do this
        private static final IntegerMonitor BATCH_WRITE_SPEED_MONITOR = new IntegerMonitor(
                BATCH_WRITE_SPEED_MONITOR_ID, new TranslatableMessage("internal.monitor.BATCH_WRITE_SPEED_MONITOR"), valueOwner);

        private static List<Class<? extends RuntimeException>> retriedExceptions = new ArrayList<Class<? extends RuntimeException>>();

        static {
            if (Common.databaseProxy.getType() == DatabaseType.DERBY)
                // This has not been tested to be optimal
                MAX_ROWS = 1000;
            else if (Common.databaseProxy.getType() == DatabaseType.H2)
                // This has not been tested to be optimal
                MAX_ROWS = 1000;
            else if (Common.databaseProxy.getType() == DatabaseType.MSSQL)
                // MSSQL has max rows of 1000, and max parameters of 2100. In this case that works out to...
                MAX_ROWS = 524;
            else if (Common.databaseProxy.getType() == DatabaseType.MYSQL)
                // This appears to be an optimal value
                MAX_ROWS = 2000;
            else if (Common.databaseProxy.getType() == DatabaseType.POSTGRES)
                // This appears to be an optimal value
                MAX_ROWS = 2000;
            else
                throw new ShouldNeverHappenException("Unknown database type: " + Common.databaseProxy.getType());

            Common.MONITORED_VALUES.addIfMissingStatMonitor(ENTRIES_MONITOR);
            Common.MONITORED_VALUES.addIfMissingStatMonitor(INSTANCES_MONITOR);
            Common.MONITORED_VALUES.addIfMissingStatMonitor(BATCH_WRITE_SPEED_MONITOR);

            retriedExceptions.add(RecoverableDataAccessException.class);
            retriedExceptions.add(TransientDataAccessException.class);
            retriedExceptions.add(TransientDataAccessResourceException.class);
            retriedExceptions.add(CannotGetJdbcConnectionException.class);
        }

        static void add(BatchWriteBehindEntry e, ExtendedJdbcTemplate ejt) {
            synchronized (ENTRIES) {
                ENTRIES.push(e);
                ENTRIES_MONITOR.setValue(ENTRIES.size());
                if (ENTRIES.size() > instances.size() * SPAWN_THRESHOLD) {
                    if (instances.size() < MAX_INSTANCES) {
                        BatchWriteBehind bwb = new BatchWriteBehind(ejt);
                        instances.add(bwb);
                        INSTANCES_MONITOR.setValue(instances.size());
                        try {
                            Common.backgroundProcessing.addWorkItem(bwb);
                        }
                        catch (RejectedExecutionException ree) {
                            instances.remove(bwb);
                            INSTANCES_MONITOR.setValue(instances.size());
                            throw ree;
                        }
                    }
                }
            }
        }

        private final ExtendedJdbcTemplate ejt;

        public BatchWriteBehind(ExtendedJdbcTemplate ejt) {
            this.ejt = ejt;
        }

        @Override
        public void execute() {
            try {
                BatchWriteBehindEntry[] inserts;
                while (true) {
                    synchronized (ENTRIES) {
                        if (ENTRIES.size() == 0)
                            break;

                        inserts = new BatchWriteBehindEntry[ENTRIES.size() < MAX_ROWS ? ENTRIES.size() : MAX_ROWS];
                        ENTRIES.pop(inserts);
                        ENTRIES_MONITOR.setValue(ENTRIES.size());
                    }

                    // Create the sql and parameters
                    Object[] params = new Object[inserts.length * POINT_VALUE_INSERT_VALUES_COUNT];
                    StringBuilder sb = new StringBuilder();
                    sb.append(POINT_VALUE_INSERT_START);
                    for (int i = 0; i < inserts.length; i++) {
                        if (i > 0)
                            sb.append(',');
                        sb.append(POINT_VALUE_INSERT_VALUES);
                        inserts[i].writeInto(params, i);
                    }

                    // Insert the data
                    int retries = 10;
                    while (true) {
                        try {
                            ejt.update(sb.toString(), params);
                            writesPerSecond.hitMultiple(inserts.length);
                            BATCH_WRITE_SPEED_MONITOR.setValue(writesPerSecond.getEventCounts()[0] / 5);
                            for(BatchWriteBehindEntry entry : inserts)
                                entry.callback();
                            break;
                        }
                        catch (RuntimeException e) {
                            if (retriedExceptions.contains(e.getClass())) {
                                if (retries <= 0) {
                                    LOG.error("Concurrency failure saving " + inserts.length
                                            + " batch inserts after 10 tries. Data lost.");
                                    break;
                                }

                                int wait = (10 - retries) * 100;
                                try {
                                    if (wait > 0) {
                                        synchronized (this) {
                                            wait(wait);
                                        }
                                    }
                                }
                                catch (InterruptedException ie) {
                                    // no op
                                }

                                retries--;
                            }
                            else {
                                LOG.error("Error saving " + inserts.length + " batch inserts. Data lost.", e);
                                break;
                            }
                        }
                    }
                }
            }
            finally {
                instances.remove(this);
                INSTANCES_MONITOR.setValue(instances.size());
            }
        }

        @Override
        public int getPriority() {
            return WorkItem.PRIORITY_HIGH;
        }

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getDescription()
		 */
		@Override
		public String getDescription() {
			return "Batch Writing from batch of size: " + ENTRIES.size(); 
		}

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getTaskId()
		 */
		@Override
		public String getTaskId() {
			return "BWB";
		}
		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getQueueSize()
		 */
		@Override
		public int getQueueSize() {
			return 0;
		}
		
		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#rejected(com.serotonin.timer.RejectedTaskReason)
		 */
		@Override
		public void rejected(RejectedTaskReason reason) { 
			instances.remove(this);
            INSTANCES_MONITOR.setValue(instances.size());
		}
    }

    //
    //Batch Updating
    //
    //
    class BatchUpdateBehindEntry {
        private final int dataPointId;
        private final int dataType;
        private final double dvalue;
        private final long time;
        private final String svalue;
        private final SetPointSource source;

        public BatchUpdateBehindEntry(int dataPointId, int dataType, double dvalue, long time, String svalue,
                SetPointSource source) {
            this.dataPointId = dataPointId;
            this.dataType = dataType;
            this.dvalue = dvalue;
            this.time = time;
            this.svalue = svalue;
            this.source = source;
        }

        public void writeInto(Object[] params, int index) {

            params[index++] = dataType;
            params[index++] = dvalue;
            params[index++] = time;
            params[index++] = dataPointId;
            params[index++] = svalue;
            params[index++] = source;
        }
    }

    public static final String ENTRIES_UPDATE_MONITOR_ID = "com.serotonin.m2m2.db.dao.PointValueDao$BatchUpdateBehind.ENTRIES_MONITOR";
    public static final String INSTANCES_UPDATE_MONITOR_ID = "com.serotonin.m2m2.db.dao.PointValueDao$BatchUpdateBehind.INSTANCES_MONITOR";

    static class BatchUpdateBehind implements WorkItem {
        private static final ObjectQueue<BatchUpdateBehindEntry> ENTRIES = new ObjectQueue<PointValueDaoSQL.BatchUpdateBehindEntry>();
        private static final CopyOnWriteArrayList<BatchUpdateBehind> instances = new CopyOnWriteArrayList<BatchUpdateBehind>();
        private static Log LOG = LogFactory.getLog(BatchUpdateBehind.class);
        private static final int SPAWN_THRESHOLD = 10000;
        private static final int MAX_INSTANCES = 5;
        private static int MAX_ROWS = 1000;
        private static final IntegerMonitor ENTRIES_MONITOR = new IntegerMonitor(ENTRIES_UPDATE_MONITOR_ID,
        		new TranslatableMessage("internal.monitor.BATCH_ENTRIES"), valueOwner);
        private static final IntegerMonitor INSTANCES_MONITOR = new IntegerMonitor(INSTANCES_UPDATE_MONITOR_ID,
        		new TranslatableMessage("internal.monitor.BATCH_INSTANCES"), valueOwner);

        private static List<Class<? extends RuntimeException>> retriedExceptions = new ArrayList<Class<? extends RuntimeException>>();

        static {
            if (Common.databaseProxy.getType() == DatabaseType.DERBY)
                // This has not been tested to be optimal
                MAX_ROWS = 1000;
            else if (Common.databaseProxy.getType() == DatabaseType.H2)
                // This has not been tested to be optimal
                MAX_ROWS = 1000;
            else if (Common.databaseProxy.getType() == DatabaseType.MSSQL)
                // MSSQL has max rows of 1000, and max parameters of 2100. In this case that works out to...
                MAX_ROWS = 524;
            else if (Common.databaseProxy.getType() == DatabaseType.MYSQL)
                // This appears to be an optimal value
                MAX_ROWS = 2000;
            else if (Common.databaseProxy.getType() == DatabaseType.POSTGRES)
                // This appears to be an optimal value
                MAX_ROWS = 2000;
            else
                throw new ShouldNeverHappenException("Unknown database type: " + Common.databaseProxy.getType());

            Common.MONITORED_VALUES.addIfMissingStatMonitor(ENTRIES_MONITOR);
            Common.MONITORED_VALUES.addIfMissingStatMonitor(INSTANCES_MONITOR);

            retriedExceptions.add(RecoverableDataAccessException.class);
            retriedExceptions.add(TransientDataAccessException.class);
            retriedExceptions.add(TransientDataAccessResourceException.class);
            retriedExceptions.add(CannotGetJdbcConnectionException.class);
        }

        static void add(BatchUpdateBehindEntry e, ExtendedJdbcTemplate ejt) {
            synchronized (ENTRIES) {
                ENTRIES.push(e);
                ENTRIES_MONITOR.setValue(ENTRIES.size());
                if (ENTRIES.size() > instances.size() * SPAWN_THRESHOLD) {
                    if (instances.size() < MAX_INSTANCES) {
                        BatchUpdateBehind bwb = new BatchUpdateBehind(ejt);
                        instances.add(bwb);
                        INSTANCES_MONITOR.setValue(instances.size());
                        try {
                            Common.backgroundProcessing.addWorkItem(bwb);
                        }
                        catch (RejectedExecutionException ree) {
                            instances.remove(bwb);
                            INSTANCES_MONITOR.setValue(instances.size());
                            throw ree;
                        }
                    }
                }
            }
        }

        private final ExtendedJdbcTemplate ejt;

        public BatchUpdateBehind(ExtendedJdbcTemplate ejt) {
            this.ejt = ejt;
        }

        @Override
        public void execute() {
            try {
                BatchUpdateBehindEntry[] updates;
                while (true) {
                    synchronized (ENTRIES) {
                        if (ENTRIES.size() == 0)
                            break;

                        updates = new BatchUpdateBehindEntry[ENTRIES.size() < MAX_ROWS ? ENTRIES.size() : MAX_ROWS];
                        ENTRIES.pop(updates);
                        ENTRIES_MONITOR.setValue(ENTRIES.size());
                    }

                    // Create the sql and parameters
                    final int batchSize = updates.length;
                    final Object[][] params = new Object[updates.length][6];
                    for (int i = 0; i < updates.length; i++) {
                        updates[i].writeInto(params[i], 0);
                    }

                    // Insert the data
                    int retries = 10;
                    while (true) {
                        try {
                            int[] updatedIds = ejt.batchUpdate(
                                    POINT_VALUE_UPDATE + " WHERE ts = ? AND dataPointId = ?",
                                    new BatchPreparedStatementSetter() {
                                        @Override
                                        public int getBatchSize() {
                                            return batchSize;
                                        }

                                        @Override
                                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                                            ps.setInt(1, (Integer) params[i][0]);
                                            ps.setDouble(2, (Double) params[i][1]);
                                            ps.setLong(3, (Long) params[i][2]);
                                            ps.setInt(4, (Integer) params[i][3]); //Update 
                                        }
                                    });

                            //Now if we have Annotation updates we need to apply those
                            if (updatedIds.length != params.length) {
                                LOG.fatal("Updated rows doesn't match necessary rows to update annotations!");
                            }
                            else {
                                PointValueDaoSQL dao = new PointValueDaoSQL();
                                for (int i = 0; i < updatedIds.length; i++) {

                                    if ((params[i][4] != null) || (params[i][5] != null)) {
                                        //Do the update
                                        dao.updatePointValueAnnotation(updatedIds[i], (Integer) params[i][0],
                                                (String) params[i][4], (SetPointSource) params[i][5]);
                                    }

                                }

                            }//end else we can do the update

                            break;
                        }
                        catch (RuntimeException e) {
                            if (retriedExceptions.contains(e.getClass())) {
                                if (retries <= 0) {
                                    LOG.error("Concurrency failure updating " + updates.length
                                            + " batch updates after 10 tries. Data lost.");
                                    break;
                                }

                                int wait = (10 - retries) * 100;
                                try {
                                    if (wait > 0) {
                                        synchronized (this) {
                                            wait(wait);
                                        }
                                    }
                                }
                                catch (InterruptedException ie) {
                                    // no op
                                }

                                retries--;
                            }
                            else {
                                LOG.error("Error saving " + updates.length + " batch updates. Data lost.", e);
                                break;
                            }
                        }
                    }
                }
            }
            finally {
                instances.remove(this);
                INSTANCES_MONITOR.setValue(instances.size());
            }
        }

        @Override
        public int getPriority() {
            return WorkItem.PRIORITY_HIGH;
        }

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getDescription()
		 */
		@Override
		public String getDescription() {
			return "Batch Updating from batch of size: " + ENTRIES.size();
		}

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getTaskId()
		 */
		@Override
		public String getTaskId() {
			return "BUB";
		}
		
		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getQueueSize()
		 */
		@Override
		public int getQueueSize() {
			return 0;
		}
		
		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#rejected(com.serotonin.timer.RejectedTaskReason)
		 */
		@Override
		public void rejected(RejectedTaskReason reason) { 
			instances.remove(this);
            INSTANCES_MONITOR.setValue(instances.size());
		}
    }

}
