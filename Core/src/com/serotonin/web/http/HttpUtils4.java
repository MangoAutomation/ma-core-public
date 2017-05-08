/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultHttpClient;

import com.serotonin.io.StreamUtils;

/**
 * @author Matthew Lohbihler
 */
public class HttpUtils4 {
    public static HttpClient getHttpClient(int timeoutMs) {
        return getHttpClient(timeoutMs, timeoutMs);
    }

    public static HttpClient getHttpClient(int connectMS, int readMS) {
        DefaultHttpClient client = new DefaultHttpClient();
        client.getParams().setParameter("http.connection.timeout", connectMS);
        client.getParams().setParameter("http.connection-manager.timeout", connectMS);
        client.getParams().setParameter("http.socket.timeout", readMS);
        client.getParams().setParameter("http.protocol.head-body-timeout", readMS);
        return client;
    }

    public static String readFullResponseBody(HttpResponse response) throws IOException {
        return readResponseBody(response, -1);
    }

    public static String readResponseBody(HttpResponse response) throws IOException {
        return readResponseBody(response, 1024 * 1024);
    }

    public static String readResponseBody(HttpResponse response, int limit) throws IOException {
        InputStream in = response.getEntity().getContent();
        if (in == null)
            return null;

        Charset charset = ContentType.getOrDefault(response.getEntity()).getCharset();
        if (charset == null)
            charset = Consts.ISO_8859_1;
        InputStreamReader reader = new InputStreamReader(in, charset);
        StringWriter writer = new StringWriter();

        StreamUtils.transfer(reader, writer, limit);

        return writer.toString();
    }

    public static <T, E extends Exception> T handleResponse(HttpClient client, HttpRequestBase request,
            HttpResponseHandler4<T, E> handler) throws IOException, E {
        try {
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Invalid response status: " + response.getStatusLine().getStatusCode()
                        + ", reason=" + response.getStatusLine().getReasonPhrase());
            return handler.handle(response);
        }
        finally {
            request.reset();
        }
    }

    public static void execute(HttpClient client, HttpRequestBase request) throws IOException {
        try {
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Invalid response status: " + response.getStatusLine().getStatusCode()
                        + ", reason=" + response.getStatusLine().getReasonPhrase());
        }
        finally {
            request.reset();
        }
    }

    public static void execute(HttpClient client, HttpRequestBase request, OutputStream out) throws IOException {
        try {
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Invalid response status: " + response.getStatusLine().getStatusCode()
                        + ", reason=" + response.getStatusLine().getReasonPhrase() + ", content="
                        + readResult(response.getEntity().getContent()));
            StreamUtils.transfer(response.getEntity().getContent(), out);
        }
        finally {
            request.reset();
        }
    }

    public static String getTextContent(String uri) throws HttpException, IOException {
        return getTextContent(uri, 0);
    }

    public static String getTextContent(String uri, int retries) throws HttpException, IOException {
        return getTextContent(getHttpClient(30000), uri, retries);
    }

    public static String getTextContent(HttpClient httpClient, String uri) throws HttpException, IOException {
        return getTextContent(httpClient, uri, 0);
    }

    public static String getTextContent(HttpClient httpClient, String uri, int retries) throws HttpException,
            IOException {
        return getTextContent(httpClient, new HttpGet(uri), retries);
    }

    public static String getTextContent(HttpClient client, HttpRequestBase request, int retries) throws HttpException,
            IOException {
        try {
            HttpResponse response = executeWithRetries(client, request, retries);
            String body = readFullResponseBody(response);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new HttpException("Invalid response code " + response.getStatusLine().getStatusCode()
                        + ", reason=" + response.getStatusLine().getReasonPhrase() + ": " + body);
            return body;
        }
        finally {
            request.reset();
        }
    }

    public static void dumpHeaders(HttpResponse response) {
        for (Header header : response.getAllHeaders())
            System.out.println(header.getName() + "=" + header.getValue());
    }

    public static void transferResponse(HttpClient client, String uri, OutputStream out) throws HttpException,
            IOException {
        HttpGet request = new HttpGet(uri);
        try {
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new HttpException("Invalid response code " + response.getStatusLine().getStatusCode()
                        + ", reason=" + response.getStatusLine().getReasonPhrase() + " for uri " + uri);

            InputStream in = response.getEntity().getContent();
            if (in == null)
                return;

            StreamUtils.transfer(in, out);
        }
        finally {
            request.reset();
        }
    }

    private static HttpResponse executeWithRetries(HttpClient client, HttpRequestBase request, int retries)
            throws IOException {
        HttpResponse response;
        while (true) {
            try {
                response = client.execute(request);
                break;
            }
            catch (SocketTimeoutException e) {
                if (retries <= 0)
                    throw e;
                retries--;
            }
        }
        return response;
    }

    public static String readResult(InputStream in) throws IOException {
        StringWriter out = new StringWriter(1000);
        StreamUtils.transfer(new InputStreamReader(in), out, 1000);
        return out.toString();
    }

    public static URI updatePath(URI uri, String newPath) throws URISyntaxException {
        StringBuilder sb = new StringBuilder();
        sb.append(uri.getScheme()).append("://");
        sb.append(uri.getHost());
        if (uri.getPort() != -1)
            sb.append(":").append(uri.getPort());

        if (!newPath.startsWith("/")) {
            String path = uri.getPath();
            int pos = path.lastIndexOf("/");
            if (pos == -1)
                sb.append("/");
            else
                sb.append(path.substring(0, pos + 1));
        }
        sb.append(newPath);

        return new URI(sb.toString());
    }

    public static void addBasicAuth(HttpRequestBase request, String username, String password) {
        String credStr = username + ":" + password;
        String creds = new String(Base64.encodeBase64(credStr.getBytes()));
        request.setHeader("Authorization", "Basic " + creds);
    }
}
