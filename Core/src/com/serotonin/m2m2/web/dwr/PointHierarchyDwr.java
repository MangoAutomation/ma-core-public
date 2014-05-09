package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.Map;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;
import com.serotonin.m2m2.vo.hierarchy.PointFolder;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchy;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

public class PointHierarchyDwr extends BaseDwr {
    @DwrPermission(admin = true)
    public PointFolder getPointHierarchy() {
        DataPointDao dataPointDao = new DataPointDao();
        PointHierarchy ph = dataPointDao.getPointHierarchy(false);
        return ph.getRoot();
    }

    @DwrPermission(admin = true)
    public PointFolder savePointHierarchy(PointFolder rootFolder) {
        new DataPointDao().savePointHierarchy(rootFolder);
        return rootFolder;
    }
    
    @DwrPermission(admin = true)
    public ProcessResult saveEdits(int[] points, Map<String, String> values) {
    	ProcessResult pr = new ProcessResult();
    	DataPointDao dpd = new DataPointDao();
    	DataPointVO[] vos = new DataPointVO[points.length];
    	for(int k = 0; k < points.length; k+=1) {
    		DataPointVO vo = dpd.get(points[k]);
    		vo.setEventDetectors(new ArrayList<PointEventDetectorVO>());
    		vos[k] = vo;
    	}
    	
    	for(DataPointVO vo : vos) {
    		for(String propStr : values.keySet()) {
    			int prop = Integer.parseInt(propStr);
    			switch(prop) {
    			case DataPointVO.Properties.NAME :
    				vo.setName(values.get(propStr));
				break;
    			case DataPointVO.Properties.XID :
    				vo.setXid(values.get(propStr));
				break;
    			case DataPointVO.Properties.DATA_SOURCE_XID :
    				vo.setDataSourceXid(values.get(propStr));
    			break;
    			case DataPointVO.Properties.DEFAULT_CACHE_SIZE :
    				vo.setDefaultCacheSize(Integer.parseInt(values.get(propStr)));
				break;
    			case DataPointVO.Properties.DEVICE_NAME :
    				vo.setDeviceName(values.get(propStr));
				break;
    			case DataPointVO.Properties.DISCARD_EXTREME_VALUES :
    				vo.setDiscardExtremeValues(Boolean.parseBoolean(values.get(propStr)));
				break;
    			case DataPointVO.Properties.DISCARD_HIGH_LIMIT :
    				vo.setDiscardHighLimit(Double.parseDouble(values.get(propStr)));
				break;
    			case DataPointVO.Properties.DISCARD_LOW_LIMIT :
    				vo.setDiscardLowLimit(Double.parseDouble(values.get(propStr)));
				break;
    			case DataPointVO.Properties.INTERVAL_LOGGING_PERIOD :
    				vo.setIntervalLoggingPeriod(Integer.parseInt(values.get(propStr)));
				break;
    			case DataPointVO.Properties.INTERVAL_LOGGING_PERIOD_TYPE :
    				vo.setIntervalLoggingPeriodType(Integer.parseInt(values.get(propStr)));
				break;
    			case DataPointVO.Properties.INTERVAL_LOGGING_TYPE :
    				vo.setIntervalLoggingType(Integer.parseInt(values.get(propStr)));
				break;
    			case DataPointVO.Properties.LOGGING_TYPE :
    				vo.setLoggingType(Integer.parseInt(values.get(propStr)));
				break;
    			}
    		}
    		dpd.updateDataPointShallow(vo);
    	}
    		
    	return pr;
    }
}
