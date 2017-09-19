package com.serotonin.m2m2.rt.script;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.Common;
import com.serotonin.web.http.HttpUtils4;

//import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;


public class HttpBuilderScriptUtility {
    public static final String CONTEXT_KEY = "HttpBuilder";
    
    //private final boolean permitted;
    private int retried = 0;
    private HttpUriRequest request = null;
    private HttpResponse response;
    private ScriptHttpCallback errorCallback;
    private ScriptHttpCallback responseCallback;
    private ScriptExceptionCallback exceptionCallback;
    private Exception thrown;
    private List<Integer> okayStatus = new ArrayList<Integer>(1);
    
    public HttpBuilderScriptUtility(ScriptPermissions permissions) {
        //permitted = permissions.getDataSourcePermissions().contains(SuperadminPermissionDefinition.GROUP_NAME);
        okayStatus.add(HttpStatus.OK.value());
    }
    
    public HttpResponse getResponse() {
        return response;
    }
    
    public int getRetried() {
        return retried;
    }
    
    public void setOkayStatusArray(int[] statusCodes) {
        okayStatus.clear();
        for(int i : statusCodes)
            okayStatus.add(i);
    }
    
    /*
     * Provided properly named functions and spicy JavaScript shorthands
     */
    public HttpBuilderScriptUtility err(ScriptHttpCallback errorCallback) {
        this.errorCallback = errorCallback;
        return this;
    }
    
    public HttpBuilderScriptUtility resp(ScriptHttpCallback responseCallback) {
        this.responseCallback = responseCallback;
        return this;
    }
    
    public HttpBuilderScriptUtility excp(ScriptExceptionCallback exceptionCallback) {
        this.exceptionCallback = exceptionCallback;
        return this;
    }
    
    public HttpBuilderScriptUtility setErrorCallback(ScriptHttpCallback errorCallback) {
        this.errorCallback = errorCallback;
        return this;
    }
    
    public HttpBuilderScriptUtility setResponseCallback(ScriptHttpCallback responseCallback) {
        this.responseCallback = responseCallback;
        return this;
    }
    
    public HttpBuilderScriptUtility setExceptionCallback(ScriptExceptionCallback exceptionCallback) {
        this.exceptionCallback = exceptionCallback;
        return this;
    }
    
    public HttpBuilderScriptUtility post(String loc, Map<String, String> headers, String content) {
//        if(!permitted)
//            throw new ScriptPermissionsException("");
        reset();
        URI uri = buildUri(loc);
        if(uri == null)
            return this;
        
        HttpPost post = new HttpPost(uri);
        putHeaders(post, headers);
        try {
            post.setEntity(new StringEntity(content));
            request = post;
        } catch(UnsupportedEncodingException e) {
            thrown = e;
        }
        return this;
    }
    
    public HttpBuilderScriptUtility get(String loc, Map<String, String> headers, Map<String, String> parameters) {
//      if(!permitted)
//      throw new ScriptPermissionsException("");
        reset();
        URI uri = buildUri(loc, parameters);
        if(uri == null)
            return this;
        
        HttpGet get = new HttpGet(uri);
        putHeaders(get, headers);
        request = get;
        return this;
    }
    
    public void execute() {
        if(thrown != null) {
            if(exceptionCallback != null) {
                exceptionCallback.exception(thrown);
                return;
            }
            if(errorCallback != null)
                errorCallback.invoke(-1, null, thrown.getMessage());
            return;
        }
        
        try {
            response = Common.getHttpClient().execute(request);
        } catch(IOException e) {
            if(exceptionCallback != null)
                exceptionCallback.exception(e);
            else if(errorCallback != null)
                errorCallback.invoke(-1, null, e.getMessage());
            return;
        }
        
        try {   
            for(Integer status : okayStatus)
                if(status.intValue() == response.getStatusLine().getStatusCode()) {
                    if(responseCallback != null)
                        responseCallback.invoke(status.intValue(), extractHeaders(response), HttpUtils4.readFullResponseBody(response));
                    return;
                }
            
            if(errorCallback != null)
                errorCallback.invoke(response.getStatusLine().getStatusCode(), extractHeaders(response), HttpUtils4.readFullResponseBody(response));
        }  catch(IOException e) {
            if(exceptionCallback != null)
                exceptionCallback.exception(e);
            if(errorCallback != null)
                errorCallback.invoke(response.getStatusLine().getStatusCode(), extractHeaders(response), "FAILED TO READ BODY: " + e.getMessage());
        }
    }
    
    private URI buildUri(String loc) {
        return buildUri(loc, null);
    }
    
    private URI buildUri(String loc, Map<String, String> parameters) {
        try {
            URIBuilder urib = new URIBuilder(loc);
            if(parameters != null) {
                for(Entry<String, String> entry : parameters.entrySet())
                    urib.addParameter(entry.getKey(), entry.getValue());
            }
            return urib.build();
        } catch(URISyntaxException e) {
            thrown = e;
            return null;
        }
    }
    
    private void putHeaders(HttpRequest request, Map<String, String> headers) {
        if(headers != null)
            for(Entry<String, String> header : headers.entrySet())
                request.addHeader(header.getKey(), header.getValue());
    }
    
    private Map<String, String> extractHeaders(HttpResponse response) {
        if(response == null)
            return new HashMap<String, String>();
        Map<String, String> headers = new HashMap<String, String>();
        for(Header h : response.getAllHeaders())
            headers.put(h.getName(), h.getValue());
        return headers;
    }
    
    private void reset() {
        retried = 0;
        errorCallback = null;
        responseCallback = null;
        exceptionCallback = null;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{ ");
        builder.append("post( url, headers, content ): set the POST, returns HttpBuilder\n");
        builder.append("get( url, headers, parameters ): set the GET, returns HttpBuilder\n");
        builder.append("resp( function( status, headers, content ){} ): set the response callback, returns HttpBuilder\n");
        builder.append("err( function( status, headers, content ){} ): set the error callback, returns HttpBuilder\n");
        builder.append("excp( function( exception ){} ): set the error callback, returns HttpBuilder\n");
        builder.append("execute(): execute the staged request and receive callbacks\n");
        builder.append("}");
        return builder.toString();
    }
}
