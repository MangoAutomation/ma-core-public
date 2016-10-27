package com.serotonin.m2m2.dao;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.springframework.jdbc.core.RowMapper;

import com.infiniteautomation.mango.db.query.SQLStatement;
import com.infiniteautomation.mango.db.query.StreamableRowCallback;
import com.infiniteautomation.mango.db.query.StreamableSqlQuery;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.vo.AbstractVO;

public class TestStreamableSqlQuery extends MangoTestBase {

	@Before
    public void setup() {
    	this.initMaHome();
    	this.initEnvProperties("test-env");
    	this.initTimer();
//    	File db = new File(new File("junit"), "databases");
//    	this.configureH2Proxy(db);
    	this.configureMySqlProxy("10.55.55.8", 3306, "mango");
    			
//        MockitoAnnotations.initMocks(this);
    }
	
//	@Test
	public void testStreamableSqlQuery() {
		OutputStream deadEnd = new OutputStream(){
			@Override
			public void write(int b) throws IOException {}
		};
		
		System.out.println("Max memory: " + Runtime.getRuntime().totalMemory());
		
		//Run this test with a low memory overhead, watch for oom
		String[] createTable = new String[]{
				"DROP TABLE IF EXISTS streamTest;",
				"CREATE TABLE streamTest ( id int not null auto_increment, testData longtext, primary key (id) ) engine=InnoDB;",
		};
		try {
			Common.databaseProxy.runScript(createTable, deadEnd);
			String[] insert = new String[]{ "INSERT INTO streamTest (testData) values ('01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567'),('01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567'),('01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567'),('01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567'),('01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567');" };
			for(int k = 0; k < 50000; k += 1) {//Insert a million dummy records
				Common.databaseProxy.runScript(insert, deadEnd);
			}
		} catch(Exception e) {
			e.printStackTrace();
			fail("Failed to create test database.");
		}
		StreamTestDao std = new StreamTestDao("streamTest");
		StreamableSqlQuery<StreamTestData> ssq = new StreamableSqlQuery<StreamTestData>(std, std.TEST_SELECT_ALL,
				new StreamableRowCallback<StreamTestData>() {
					boolean logged = false;
					@Override
					public void row(StreamTestData row, int index) throws Exception {
//						System.out.println("Got test data" + row.getTestData() + " on row " + index);
						if(!logged) {
							System.gc();
							System.out.println("Current free memory: " + Runtime.getRuntime().freeMemory());
							logged = true;
						}
					}
			
		}, null);
		
		System.gc();
		System.out.println("Pre-query free memory: " + Runtime.getRuntime().freeMemory());
		try {
			ssq.query();
		} catch(Exception e) {
			fail("Didn't execute query successfully.");
		}
		System.gc();
		System.out.println("Post-query free memory: " + Runtime.getRuntime().freeMemory());
		
		String[] dropTable = new String[]{
				"drop table streamTest;"
		};
		
		try {
			Common.databaseProxy.runScript(dropTable, deadEnd);
		} catch(Exception e) {
			fail("Failed to delete table when done");
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
	}
	
	class StreamTestDao extends AbstractDao<StreamTestData> {

		public final SQLStatement TEST_SELECT_ALL = new SQLStatement("select testData ", "select count(id) ",null, "streamTest", null, false);
		
		protected StreamTestDao(String typeName) {
			super(typeName);
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
