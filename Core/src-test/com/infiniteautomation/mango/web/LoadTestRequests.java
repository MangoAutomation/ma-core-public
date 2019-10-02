/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.web;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
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
    public void login() throws ClientProtocolException, IOException, URISyntaxException {
        try(MangoHttpClient client = new MangoHttpClient("localhost", 8081, false)){

            CloseableHttpResponse response = client.login("admin", "admin");
            response.close();
            
            for(int i=0; i<200; i++) {
                response = client.get("/rest/v2/users/current");
                if(response.getStatusLine().getStatusCode() != 200) {
                    fail(response.getStatusLine().getReasonPhrase());
                }
                response.close();
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
                    response = client.get("/rest/v2/users/current");
                }else {
                    InputStream is = this.getClass().getClassLoader().getResourceAsStream("testMangoConfig.json");
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    builder.addBinaryBody("upfile", is, ContentType.DEFAULT_BINARY, "testConfig.json");
                    response = client.post("/rest/v2/file-stores/DATA_FILE_TEMPLATE", builder.build(), new BasicNameValuePair("overwrite", "true"));
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
