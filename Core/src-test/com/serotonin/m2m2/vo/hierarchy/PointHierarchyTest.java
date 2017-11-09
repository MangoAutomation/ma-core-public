/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.vo.hierarchy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.serotonin.json.JsonException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockRuntimeManager;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * 
 * https://github.com/bwaldvogel/h2-lob-issue
 * 
 *
 * @author Terry Packer
 */
public class PointHierarchyTest extends MangoTestBase{
    
    @Before
    public void setupRuntimeManager() {
        Common.runtimeManager = new MockRuntimeManager(true);
        
        try {
            loadDefaultConfiguration();
        } catch (JsonException | IOException | URISyntaxException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void testSynchronization() {
        
       PointHierarchy ph = DataPointDao.instance.getPointHierarchy(true);

        //First confirm the PH is correct
        assertEquals(0, ph.getRoot().getPoints().size());
        assertEquals(5, ph.getRoot().getSubfolder("Demo").getPoints().size());
        assertEquals(13, ph.getRoot().getSubfolder("Demo").getSubfolder("Meters").getSubfolder("Meter 1").getPoints().size());
        assertEquals(13, ph.getRoot().getSubfolder("Demo").getSubfolder("Meters").getSubfolder("Meter 2").getPoints().size());
        assertEquals(13, ph.getRoot().getSubfolder("Demo").getSubfolder("Meters").getSubfolder("Meter 3").getPoints().size());
        
        //Test synchronization
        // fire off threads that try to modify 
        // and save the ph
        AtomicInteger threads = new AtomicInteger();
        for(int i=0; i<5; i++) {
            threads.incrementAndGet();
            new PointHierarchyRandomizer(threads).start();
        }
        
        while(threads.get() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    class PointHierarchyModifier extends Thread {
        
        private AtomicInteger threads;
        
        public PointHierarchyModifier(AtomicInteger threads) {
            this.threads = threads;
        }
        
        public void run() {
            try {
                for(int i=0; i<10; i++) {
                    
                    PointHierarchy ph = DataPointDao.instance.getPointHierarchy(false);
                    
                    //Mess around by traversing the hierarchy
                    traverse(ph.getRoot());
                    
                    //If the root has no subfolders, create some
                    if(ph.getRoot().getSubfolders().size() == 0) {
                       List<DataPointSummary> points = ph.getRoot().getPoints();
                       int partitionSize = points.size() / 3;
                       List<List<DataPointSummary>> partitions = Lists.partition(points, partitionSize);
                       
                       ph = new PointHierarchy();
                       int pointFolderId = 1;
                       for(List<DataPointSummary> pointPartition : partitions) {
                           
                           PointFolder f = new PointFolder();
                           f.setName("Folder " + pointFolderId);
                           for(DataPointSummary summary : pointPartition)
                               f.addDataPoint(summary);
                           ph.addPointFolder(f, 0);
                           pointFolderId++;
                       }
                       
                    }else {
                        ph = new PointHierarchy();
                        List<DataPointVO> dataPoints = DataPointDao.instance.getAll();
                        for(DataPointVO vo : dataPoints)
                            ph.getRoot().addDataPoint(new DataPointSummary(vo));
                    }
                    
                    DataPointDao.instance.savePointHierarchy(ph.getRoot());
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }catch(Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }finally {
                threads.decrementAndGet();
            }
        }

        
        /**
         * Little method to traverse the ph and touch every point
         * @param f
         */
        private void traverse(PointFolder f) {
            for(DataPointSummary summary : f.getPoints())
                if(summary.getId() %2 == 0) {
                    //Do nothing
                }
            for(PointFolder pf : f.getSubfolders())
                traverse(pf);
        }
    }
    
    /**
     * Randomly move points around into folders,
     * simulates the Persistent TCP Data Source
     *
     * @author Terry Packer
     */
    class PointHierarchyRandomizer extends Thread {
        
        private AtomicInteger threads;
        private Random rand = new Random();
        
        public PointHierarchyRandomizer(AtomicInteger threads) {
            this.threads = threads;
        }
        
        public void run() {
            try {
                for(int i=0; i<10; i++) {
                    
                    PointHierarchy pointHierarchy = DataPointDao.instance.getPointHierarchy(false);
                    List<DataPointVO> points = DataPointDao.instance.getAll();
                    boolean changes = false;
                    
                    for(DataPointVO vo : points) {
                        List<String> path = generateRandomPath(10);
                        List<PointFolder> folders = pointHierarchy.getFolderPath(vo.getId());
                        PointFolder oldFolder = folders.get(folders.size() - 1);
    
                        // Get the new folder for the point. Create new folders as necessary.
                        PointFolder newFolder = pointHierarchy.getRoot();
                        for (String s : path) {
                            PointFolder sub = newFolder.getSubfolder(s);
                            if (sub == null) {
                                // Add the folder
                                sub = new PointFolder();
                                sub.setName(s);
                                newFolder.addSubfolder(sub);
                            }
                            newFolder = sub;
                        }
    
                        if (oldFolder != newFolder) {
                            oldFolder.removeDataPoint(vo.getId());
                            newFolder.addDataPoint(new DataPointSummary(vo));
                            changes = true;
                        }
                    }
    
                    if(changes)
                        DataPointDao.instance.savePointHierarchy(pointHierarchy.getRoot());
    
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }catch(Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }finally {
                threads.decrementAndGet();
            }
        }
        
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        
        private List<String> generateRandomPath(int maxLength){
            
            int length = rand.nextInt(maxLength);
            List<String> path = new ArrayList<String>(length);
            for(int i=0; i<length; i++) {
                path.add(nextRandomString());
            }
            
            return path;
        }
        
        private String nextRandomString() {
            StringBuilder salt = new StringBuilder();
            Random rnd = new Random();
            while (salt.length() < 18) { // length of the random string.
                int index = (int) (rnd.nextFloat() * SALTCHARS.length());
                salt.append(SALTCHARS.charAt(index));
            }
            String saltStr = salt.toString();
            return saltStr;
        }
    }
    
}
