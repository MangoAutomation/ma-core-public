/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.web;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;

/**
 * Test for a Mango Client 
 * 
 * TODO: implement client fully
 * 
 * @author Terry Packer
 *
 */
public class LoadTestRequests {
    
    //@Test
    public void testDos() throws ClientProtocolException, IOException, URISyntaxException {
        
        //TODO Setup using env properties
        
        try(MangoHttpClient client = new MangoHttpClient("localhost", 8081, false)){

            CloseableHttpResponse response = client.login("admin", "admin");
            response.close();
            
            for(int i=0; i<200; i++) {
                response = client.get("/rest/latest/users/current");
                if(response.getStatusLine().getStatusCode() != 200) {
                    fail(response.getStatusLine().getReasonPhrase());
                }
                response.close();
            }
        }
    }
    
    //TODO setup URL on testing v2 controller
    public void testAsyncException () throws ClientProtocolException, IOException, URISyntaxException {
        try(MangoHttpClient client = new MangoHttpClient("localhost", 8081, false)){

            CloseableHttpResponse response = client.login("admin", "admin");
            response.close();
            
            for(int i=0; i<200; i++) {
                response = client.get("/rest/latest/users/current");
                if(response.getStatusLine().getStatusCode() != 200) {
                    fail(response.getStatusLine().getReasonPhrase());
                }
                response.close();
            }
        }
    }
    
    //@Test
    public void testQoSviaServer() throws ClientProtocolException, IOException, URISyntaxException {
        int webQosMaxRequests = 10; //Set to the value of the env property
        int webQosWaitMs = 10000; //Set to the value of the env property
        int webQosSuspendMs = 30000; //Set to the value of the env property

        //Track running threads
        AtomicInteger running = new AtomicInteger();
        List<String> failures = new ArrayList<>();    
        for(int i=0; i<webQosMaxRequests + 1; i++) {
            Thread t = new Thread() {
                
                public void run() {
                    try(MangoHttpClient client = new MangoHttpClient("localhost", 8081, false)){

                        CloseableHttpResponse response = client.login("admin", "admin");
                        response.close();
                        
                        int delay = webQosWaitMs * 2;
                        response = client.get("/rest/latest/example/delay-response/" + delay);
                        if(response.getStatusLine().getStatusCode() != 200) {
                            failures.add(response.getStatusLine().getReasonPhrase());
                        }else {
                            //TODO Convert response content
                            response.getEntity().getContent();
                        }
                        response.close();
                    } catch (IOException | URISyntaxException e) {
                        e.printStackTrace();
                        failures.add(e.getMessage());
                    } finally {
                        running.decrementAndGet();
                    }
                };
            };
            running.incrementAndGet();
            t.start();
        }
        //Rough estimate of max runtime will be near webQosSuspendTime
        int maxRuntime = (int)(webQosSuspendMs + (webQosSuspendMs * .8));
        int runtime = 0;
        while(running.get() > 0) {
            try { Thread.sleep(50); } catch(InterruptedException e) { }
            runtime += 50;
            if(runtime > maxRuntime) {
                fail("QoS delay too long");
            }
        }
        if(!failures.isEmpty()) {
            String message = "" + failures.size() + " failures:\n";
            for(String failure : failures) {
                message += failure + "\n";
            }
            fail(message);
        }
    }
    
    //This test doesn't quite work, not sure how to delay/chunk the string body
    public void testQoSviaClient() throws ClientProtocolException, IOException, URISyntaxException {
        int webQosMaxRequests = 10; //Set to the value of the env property
        int webQosWaitMs = 50; //Set to the value of the env property
        int webQosSuspendMs = 30000; //Set to the value of the env property

        //Track running threads
        AtomicInteger running = new AtomicInteger();
        
        try(MangoHttpClient client = new MangoHttpClient("localhost", 8081, false)){

            CloseableHttpResponse response = client.login("admin", "admin");
            response.close();
            
            for(int i=0; i<webQosMaxRequests + 1; i++) {
                new Thread() {
                    public void run() {
                        running.incrementAndGet();
                        try {
                            int keepAliveMs = webQosWaitMs * 2;
                            //Build a string of keep alive for ms length
                            StringBuilder sb = new StringBuilder();
                            for(long i=0; i<keepAliveMs; i++) {
                                sb.append("0");
                            }
                            
                            HttpPost request = new HttpPost(client.createURI("/rest/latest/example/log-info-message"));
                            request.setEntity(new StringEntity(sb.toString(), ContentType.TEXT_PLAIN) {
    
                                @Override
                                public InputStream getContent() throws IOException, UnsupportedOperationException {
                                    AtomicInteger count = new AtomicInteger();
                                    return new InputStream() {
                                        @Override
                                        public int read() throws IOException {
                                            try { Thread.sleep(10); } catch(InterruptedException e) { }
                                            return content[count.getAndIncrement()];
                                        }
                                    };
                                }
    
                                @Override
                                public void writeTo(OutputStream outstream) throws IOException {
                                    for(int i =0; i<content.length; i++) {
                                        try { Thread.sleep(10); } catch(InterruptedException e) { }
                                        outstream.write(content[i]);
                                    }
                                    outstream.flush();
                                }
                                
                                @Override
                                public boolean isStreaming() {
                                    return true;
                                }
                                @Override
                                public boolean isChunked() {
                                    return true;
                                }
                            });
                        

                            CloseableHttpResponse response = client.execute(request, true);
                            if(response.getStatusLine().getStatusCode() != 200) {
                                fail(response.getStatusLine().getReasonPhrase());
                            }
                            response.close();
                        }catch(IOException | URISyntaxException e) {
                            fail(e.getMessage());
                        }finally {
                            running.decrementAndGet();
                        }
                    };
                }.start();
            }
            //Rough estimate of max runtime will be near webQosSuspendTime
            int maxRuntime = (int)(webQosSuspendMs + (webQosSuspendMs * .8));
            long startTime = System.currentTimeMillis();
            long runtime = 0;
            while(running.get() > 0) {
                try { Thread.sleep(50); } catch(InterruptedException e) { }
                runtime = System.currentTimeMillis() - startTime;
                if(runtime > maxRuntime) {
                    fail("QoS delay too long");
                }
            }
            if(runtime < (webQosMaxRequests + 1) * (webQosWaitMs * 2)) {
                fail("QoS delay too short");
            }
        }
    }
    
    //@Test
    public void uploadFileManyTimes() throws InterruptedException, ExecutionException, TimeoutException, ClientProtocolException, IOException, URISyntaxException {
        try(MangoHttpClient client = new MangoHttpClient("localhost", 8081, false)){
            
            CloseableHttpResponse response = client.login("admin", "admin");
            response.close();
            
            boolean doPost = false;
            for(int i=0; i<200; i++) {
                if(!doPost) {
                    response = client.get("/rest/latest/users/current");
                }else {
                    InputStream is = this.getClass().getClassLoader().getResourceAsStream("testMangoConfig.json");
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    builder.addBinaryBody("upfile", is, ContentType.DEFAULT_BINARY, "testConfig.json");
                    response = client.post("/rest/latest/file-stores/DATA_FILE_TEMPLATE", builder.build(), new BasicNameValuePair("overwrite", "true"));
                }
                if(response.getStatusLine().getStatusCode() != 200) {
                    fail(response.getStatusLine().getReasonPhrase());
                }
                doPost = !doPost;
                response.close();
            }
        }
   }
}
