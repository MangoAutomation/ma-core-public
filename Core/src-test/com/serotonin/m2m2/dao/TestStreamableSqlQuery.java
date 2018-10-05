/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.RowMapper;

import com.infiniteautomation.mango.db.query.Index;
import com.infiniteautomation.mango.db.query.SQLStatement;
import com.infiniteautomation.mango.db.query.StreamableRowCallback;
import com.infiniteautomation.mango.db.query.StreamableSqlQuery;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.DatabaseProxy.DatabaseType;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * TODO Test stream=true in StreamableSqlQuery constructor
 * @author Terry Packer
 *
 */
public class TestStreamableSqlQuery extends MangoTestBase {

    private final OutputStream deadEnd = new OutputStream(){
        @Override
        public void write(int b) throws IOException {}
    };
    
    private final  String[] createTable = new String[]{
            "DROP TABLE IF EXISTS streamTest;",
            "CREATE TABLE streamTest ( id int not null auto_increment, testData longtext, primary key (id) ) engine=InnoDB;",
    };
    
    private final String[] dropTable = new String[]{
            "drop table streamTest;"
    };
    
    private final String[] insert = new String[]{ "INSERT INTO streamTest (testData) values ('01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567'),('01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567'),('01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567'),('01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567'),('01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567');" };

    
    @Before
    @Override
    public void before() {
        super.before();
        try {
            Common.databaseProxy.runScript(createTable, deadEnd);
            for(int k = 0; k < 50000; k += 1) {//Insert a million dummy records
                Common.databaseProxy.runScript(insert, deadEnd);
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail("Failed to create test database.");
        }
    }
    
    @After
    @Override
    public void after() {
        try {
            Common.databaseProxy.runScript(dropTable, deadEnd);
        } catch(Exception e) {
            fail("Failed to delete table when done");
        }
        super.after();
    }
    
    @Test
    public void testQuery() {
        int idleConnectionCount = Common.databaseProxy.getIdleConnections();
        
        StreamTestDao std = new StreamTestDao("streamTest");
        StreamableSqlQuery<StreamTestData> ssq = new StreamableSqlQuery<StreamTestData>(std, false, std.TEST_SELECT_ALL,
                new StreamableRowCallback<StreamTestData>() {
                    @Override
                    public void row(StreamTestData row, int index) throws Exception {
                        //No-op
                    }
            
        }, new StreamableRowCallback<Long>() {

            @Override
            public void row(Long row, int index) throws Exception {
                //No-op
            }
            
        });
        
        for(int i=0; i<idleConnectionCount + 1; i++) {
            try {
                ssq.query();
                assertEquals(0, Common.databaseProxy.getActiveConnections());
            } catch(Exception e) {
                fail("Unexpected failure: " + e.getMessage());
            }
        }
        
        //Ensure we don't leave open connections around
        assertEquals(idleConnectionCount, Common.databaseProxy.getIdleConnections());
        
        //Test the count
        for(int i=0; i<idleConnectionCount + 1; i++) {
            try {
                ssq.count();
                assertEquals(0, Common.databaseProxy.getActiveConnections());
            } catch(Exception e) {
                fail("Unexpected failure: " + e.getMessage());
            }
        }
        
        //Ensure we don't leave open connections around
        assertEquals(idleConnectionCount, Common.databaseProxy.getIdleConnections());
    }

    
    @Test
    public void testQueryException() {
        int idleConnectionCount = Common.databaseProxy.getIdleConnections();
        
        StreamTestDao std = new StreamTestDao("streamTest");
        StreamableSqlQuery<StreamTestData> ssq = new StreamableSqlQuery<StreamTestData>(std, false, std.TEST_SELECT_ALL,
                new StreamableRowCallback<StreamTestData>() {
                    @Override
                    public void row(StreamTestData row, int index) throws Exception {
                        System.out.println("Got test data " + row.getTestData() + " on row " + index);
                        if(index > 2)
                            throw new StreamTestException();
                    }
            
        }, new StreamableRowCallback<Long>() {
            @Override
            public void row(Long row, int index) throws Exception {
                throw new StreamTestException();
            }
            
        });
        
        for(int i=0; i<idleConnectionCount + 1; i++) {
            try {
                ssq.query();
            } catch(IOException e) {
                if(e.getCause() instanceof StreamTestException) {
                    //Expect our Exception, test the connection pool size
                    assertEquals(0, Common.databaseProxy.getActiveConnections());
                }else {
                    fail("Unexpected failure: " + e.getMessage());
                }
                
            } catch(Exception e) {
                fail("Unexpected failure: " + e.getMessage());
            }
        }
        
        //Ensure we don't leave open connections around
        assertEquals(idleConnectionCount, Common.databaseProxy.getIdleConnections());
        
        //Test Count
        for(int i=0; i<idleConnectionCount + 1; i++) {
            try {
                ssq.count();
            } catch(IOException e) {
                if(e.getCause() instanceof StreamTestException) {
                    //Expect our Exception, test the connection pool size
                    assertEquals(0, Common.databaseProxy.getActiveConnections());
                }else {
                    fail("Unexpected failure: " + e.getMessage());
                }
                
            } catch(Exception e) {
                fail("Unexpected failure: " + e.getMessage());
            }
        }
        
        //Ensure we don't leave open connections around
        assertEquals(idleConnectionCount, Common.databaseProxy.getIdleConnections());
    }
	
	
	class StreamTestException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public StreamTestException() {
            super("Stream Test Exception");
        }
	}
	
	class StreamTestData extends AbstractVO<StreamTestData> {
		private static final long serialVersionUID = 1L;
		String testData;
		
		@Override
		public String getTypeKey() {
			return "streamTest";
		}
		
		public String getTestData() {
			return testData;
		}
		
		public void setTestData(String testData) {
			this.testData = testData;
		}

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.vo.AbstractVO#getDao()
		 */
		@Override
		protected AbstractDao<StreamTestData> getDao() {
			return null;
		}
	}
	
	
	class StreamTestDao extends AbstractDao<StreamTestData> {

		public final SQLStatement TEST_SELECT_ALL = new SQLStatement("select testData from ", "select count(id) from ",null, "streamTest", null, false, false, new ArrayList<Index>(), DatabaseType.H2);
		
		protected StreamTestDao(String typeName) {
			super(typeName, null);
			TEST_SELECT_ALL.build();
		}

		@Override
		protected String getXidPrefix() {
			return "StreamTest_";
		}

		@Override
		public StreamTestData getNewVo() {
			return new StreamTestData();
		}

		@Override
		protected String getTableName() {
			return "streamTest";
		}

		@Override
		protected Object[] voToObjectArray(StreamTestData vo) {
			return new Object[]{vo.getTestData()};
		}

		@Override
		protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
			LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
			map.put("testData", Types.LONGVARCHAR);
			return map;
		}

		@Override
		protected Map<String, IntStringPair> getPropertiesMap() {
			return new HashMap<>();
		}

		@Override
		public RowMapper<StreamTestData> getRowMapper() {
			return new RowMapper<StreamTestData>() {

				@Override
				public StreamTestData mapRow(ResultSet rs, int rowNum) throws SQLException {
					StreamTestData ans = new StreamTestData();
					ans.setTestData(rs.getString(1));
					return ans;
				}
				
			};
		}
		
	}
}
