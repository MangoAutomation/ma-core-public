/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockMangoProperties;
import com.serotonin.m2m2.SimulationTimerProvider;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherDefinition;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherVO;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;
import com.serotonin.util.DirectoryInfo;
import com.serotonin.util.DirectoryUtils;
import com.serotonin.util.properties.MangoProperties;

/**
 * It was noticed that H2 grows when updating the rt data if it changes
 *
 * @author Terry Packer
 */
public class H2SizeBugTest extends MangoTestBase {

    protected static final File baseTestDir = new File("junit");
    protected String compress = "DEFAULT"; //Options DEFAULT NO LZF DEFLATE (Only for pagestore)
    protected int useTrace = -1; //-1=default 0=none, 1=error, 2=info, 3=debug
    protected boolean useMvStore = false;
    protected boolean useCheckpoint = false; //Flush to disk
    protected boolean useDao = true;
    protected boolean useShutdownCompact = false;
    protected boolean closeOnExit = false;
    protected boolean printSize = false;
    protected boolean enableWeb = false;
    
    @BeforeClass
    public static void addDefinitions() throws IOException {
        delete(baseTestDir);
        List<ModuleElementDefinition> definitions = new ArrayList<>();
        definitions.add(new MockPublisherDefinition());
        addModule("mockPublisher", definitions);
    }
    
    @Test
    public void testH2Growth() throws Exception {
        try {
            if(!"DEFAULT".equals(compress))
                Common.databaseProxy.runScript(new String[] {"SET COMPRESS_LOB " + compress + ";"}, System.out);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        MockPublisherVO vo = new MockPublisherVO();
        vo.setDefinition(new MockPublisherDefinition());
        vo.setXid("PUB_TEST1");
        vo.setEnabled(false);
        
        PublisherDao.instance.savePublisher(vo);
        
        Map<Integer, Long> data = getData(10000, 0);
        Map<String, Object> rtData = new HashMap<String, Object>();
        rtData.put("RT_DATA", data);
        int id = vo.getId();
        for(int i=0; i<200; i++) {
            id = updateDatabase(id, rtData);
            modifyData(data);
            if(i%10 == 0) {
                if(useCheckpoint)
                    Common.databaseProxy.runScript(new String[] {"CHECKPOINT;"}, System.out);
                long size = dbSize();
                if(printSize)
                    System.out.println("DB Size: " + DirectoryUtils.bytesDescription(size));
                if(size > 20000000)
                    fail("Database grew over 20MB.");
            }
        }
        System.out.print("DONE");
    }
    
    /**
     * @param data
     * @throws SQLException 
     * @throws IOException 
     */
    private int updateDatabase(int id, Map<String, Object> rtData) throws SQLException, IOException {

        //Test without DAO Layer (Not the bug)
        if(!useDao) {
            String sql = "update publishers set rtData=? where id=?";
            JdbcConnectionPool pool = (JdbcConnectionPool)Common.databaseProxy.getDataSource();
            Connection conn = pool.getConnection();
            conn.setAutoCommit(true);
            PreparedStatement ps = conn.prepareStatement(sql);
            //ps.setObject(1, SerializationHelper.writeObject(rtData));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(rtData);
            oos.flush();
            oos.close();
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            ps.setBinaryStream(1, is);
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();
            conn.close();
            return id;
        }else {
            PublisherDao.instance.savePersistentData(id, rtData);
            return id;
        }
    }

    private Map<Integer,Long> getData(int size, long initialValue){
        Map<Integer, Long> data = new HashMap<>(size);
        for(int i=0; i<size; i++) {
            data.put(i, initialValue);
        }
        return data;
    }
    
    private void modifyData(Map<Integer, Long> map) {
        for(Entry<Integer, Long> entry : map.entrySet())
            entry.setValue(entry.getValue() + 1L);
    }
    
    private long dbSize() {
        DirectoryInfo fileDatainfo = DirectoryUtils.getSize(baseTestDir);
        return fileDatainfo.getSize();
    }
    
    @After
    @Override
    public void after() {
        SimulationTimerProvider provider = (SimulationTimerProvider) Providers.get(TimerProvider.class);
        provider.reset();
        Common.runtimeManager.terminate();
        Common.runtimeManager.joinTermination();
//        try {
//            delete(baseTestDir);
//        } catch (IOException e) {
//            fail(e.getMessage());
//        }
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.MangoTestBase#getLifecycle()
     */
    @Override
    protected MockMangoLifecycle getLifecycle() {
        Common.databaseProxy = new H2Proxy();
        return new H2MangoLifecycle(modules, enableH2Web, h2WebPort);
    }
    
    class H2MangoLifecycle extends MockMangoLifecycle {

        /**
         * @param modules
         * @param enableWebConsole
         * @param webPort
         */
        public H2MangoLifecycle(List<Module> modules, boolean enableWebConsole, int webPort) {
            super(modules, enableWebConsole, webPort);
        }
        
        /* (non-Javadoc)
         * @see com.serotonin.m2m2.MockMangoLifecycle#getEnvProps()
         */
        @Override
        protected MangoProperties getEnvProps() {
            MockMangoProperties props = new MockMangoProperties();
            props.setDefaultValue("db.type", "h2");
            String url = "jdbc:h2:" + baseTestDir.getAbsolutePath() + "/databases/mah2-test;LOB_TIMEOUT=50";
            if(useTrace >= 0)
                url += ";TRACE_LEVEL_FILE=" + useTrace;
            if(useMvStore)
                url += ";MV_STORE=TRUE";
            if(closeOnExit)
                url += ";DB_CLOSE_ON_EXIT=TRUE";
            
            props.setDefaultValue("db.url", url);
            props.setDefaultValue("db.location", baseTestDir.getAbsolutePath() +"/databases/mah2-test");
            if(useShutdownCompact)
                props.setDefaultValue("db.h2.shutdownCompact", "true");
            
            if(enableWeb)
                props.setDefaultValue("db.web.start", "true");
                props.setDefaultValue("db.web.port","8091");
            
            return props;
        }
        
        @Override
        public void terminate() {
            Common.databaseProxy.terminate(true);
            
            //Check size here
        }
    }
}
