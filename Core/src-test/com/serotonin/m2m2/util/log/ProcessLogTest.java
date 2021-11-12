/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util.log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MockMangoProperties;
import com.serotonin.provider.Providers;
import com.serotonin.util.properties.MangoProperties;

/**
 *
 * @author Terry Packer
 */
public class ProcessLogTest {

    @BeforeClass
    public static void staticSetup() throws IOException{

        //Setup Mango properties Provider as we indirectly access Common
        Providers.add(MangoProperties.class, new MockMangoProperties());
    }

    @Test
    public void testProcessLogStringWriter() {
        final StringWriter scriptOut = new StringWriter();
        final PrintWriter writer = new PrintWriter(scriptOut);
        LogLevel level = LogLevel.TRACE;
        try(ProcessLog log = new ProcessLog("1", level, true, writer)){
            String message = "string writer test";
            log.trace(message);
            assertMatch(message, level, scriptOut.toString());
        }
    }

    @Test
    public void testRollingProcessLog() throws IOException {
        //Delete any existing files for our test
        cleanLogs("processLog.test-rolling");
        LogLevel level = LogLevel.TRACE;
        try(ProcessLog log = new ProcessLog("processLog.", "test-rolling", level, true, 100, 10)){
            String message = "rolling test ";
            for(int i=1; i<11; i++)
                log.trace(message + i);

            File[] logFiles = log.getFiles();
            Assert.assertEquals(10, logFiles.length);
            for(File file : logFiles) {
                String result = getLogContents(file);
                String [] parts = file.getName().split("\\.log.");
                if(parts.length == 1) {
                    //Current logfile should be rolling test 10
                    assertMatch("rolling test 10", level, result);
                }else {
                    int index = Integer.parseInt(parts[1]);
                    assertMatch("rolling test " + (10 - index), level, result);
                }
            }

            //Write another log message, generate files 1-10
            log.trace("rolling test " + 11);
            logFiles = log.getFiles();
            Assert.assertEquals(11, logFiles.length);
            for(File file : logFiles) {
                String result = getLogContents(file);
                String [] parts = file.getName().split("\\.log.");
                if(parts.length == 1) {
                    //Current logfile should be rolling test 10
                    assertMatch("rolling test 11", level, result);
                }else {
                    int index = Integer.parseInt(parts[1]);
                    assertMatch("rolling test " + (10 - index + 1), level, result);
                }
            }

            //Write a final log messages to shuffle the oldest log out
            log.trace("rolling test " + 12);
            logFiles = log.getFiles();
            Assert.assertEquals(11, logFiles.length);
            for(File file : logFiles) {
                String result = getLogContents(file);
                String [] parts = file.getName().split("\\.log.");
                if(parts.length == 1) {
                    //Current logfile should be rolling test 10
                    assertMatch("rolling test 12", level, result);
                }else {
                    int index = Integer.parseInt(parts[1]);
                    assertMatch("rolling test " + (10 - index + 2), level, result);
                }
            }
        }
        cleanLogs("processLog.test-rolling");
    }

    //TODO Test writing null as String and Object

    @Test
    public void testProcessLogNulls() {
        StringWriter scriptOut = new StringWriter();
        PrintWriter writer = new PrintWriter(scriptOut);
        LogLevel level = LogLevel.TRACE;
        try(ProcessLog log = new ProcessLog("1", level, true, writer)){
            String message = null;
            log.trace(message);
            assertMatch("null", level, scriptOut.toString());
        }
    }


    @Test
    public void testRollingProcessLogMultiThreadedSingleFile() throws InterruptedException, IOException {
        cleanLogs("processLog.test-multi-thread");
        LogLevel level = LogLevel.TRACE;
        try(ProcessLog log = new ProcessLog("processLog.", "test-multi-thread", level, true, 100000000, 10)){
            AtomicInteger count = new AtomicInteger();
            AtomicBoolean running = new AtomicBoolean(true);
            AtomicInteger active = new AtomicInteger();
            int threadCount = 10;
            for(int i=0; i<threadCount; i++) {
                new Thread() {
                    /* (non-Javadoc)
                     * @see java.lang.Thread#run()
                     */
                    @Override
                    public void run() {
                        active.incrementAndGet();
                        while(running.get()) {
                            log.info("Writing message " + count.getAndIncrement());
                            try{ Thread.sleep(10);}catch(InterruptedException e) {}
                        }
                        active.decrementAndGet();
                    }
                }.start();
            }

            Thread.sleep(2000);
            running.set(false);
            while(active.get() > 0) {
                Thread.sleep(100);
            }
            //All messages should be written
            String result = getLogContents(log.getFile());
            String[] messages = result.split("\\n");
            Assert.assertEquals(count.get(), messages.length); //The last log message has a newline so the split will actually contain log messages + 1 elements
        }
        cleanLogs("processLog.test-multi-thread");
    }

    @Test
    public void testRollingProcessLogMultiThreadedMultiFile() throws InterruptedException, IOException {
        cleanLogs("processLog.test-multi-thread-multi-file");
        LogLevel level = LogLevel.TRACE;
        try(ProcessLog log = new ProcessLog("processLog.", "test-multi-thread-multi-file", level, true, 10000, 100)){
            AtomicInteger count = new AtomicInteger();
            AtomicBoolean running = new AtomicBoolean(true);
            AtomicInteger active = new AtomicInteger();
            int threadCount = 10;
            for(int i=0; i<threadCount; i++) {
                new Thread() {
                    /* (non-Javadoc)
                     * @see java.lang.Thread#run()
                     */
                    @Override
                    public void run() {
                        active.incrementAndGet();
                        while(running.get()) {
                            log.info("Writing message " + count.getAndIncrement());
                            try{ Thread.sleep(10);}catch(InterruptedException e) {}
                        }
                        active.decrementAndGet();
                    }
                }.start();
            }

            Thread.sleep(2000);
            running.set(false);
            while(active.get() > 0) {
                Thread.sleep(100);
            }
            //All messages should be written
            File[] files = log.getFiles();
            int messageCount = 0;
            for(int i=0; i<files.length; i++) {
                String result = getLogContents(files[i]);
                messageCount += result.split("\\n").length;
            }


            Assert.assertEquals(count.get(), messageCount); //The last log message has a newline so the split will actually contain log messages + 1 elements
        }
        //cleanLogs("processLog.test-multi-thread-multi-file");
    }

    private final String logRegex = "(\\D.*) \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3} \\((.*):(\\d.*)\\) - (.*)" + System.lineSeparator();
    private void assertMatch(String message, LogLevel level, String result) {
        //Messages of the form:
        //LEVEL yyyy-MM-dd HH:mm:SS,sss (location:line) - message
        System.out.println(result);
        Pattern p = Pattern.compile(logRegex);
        Matcher m = p.matcher(result);
        Assert.assertEquals(true, m.matches());
        String foundLevel = m.group(1);
        //String className = m.group(2);
        //String line = m.group(3);
        String foundMessage = m.group(4);

        Assert.assertEquals(level.name(), foundLevel);
        Assert.assertEquals(message, foundMessage);
    }

    /**
     * @param file
     * @return
     * @throws IOException
     */
    private String getLogContents(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    private void cleanLogs(String prefix) {
        File files[] = Common.getLogsDir().listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if(name.startsWith(prefix))
                    return true;
                else
                    return false;
            }

        });
        for(File file : files)
            file.delete();
    }

}
