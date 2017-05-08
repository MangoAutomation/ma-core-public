/*
 * Created on 26-Jul-2006
 */
package com.serotonin.web.content;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Matthew Lohbihler
 */
public class MockServletResponse implements HttpServletResponse {
    private final StringWriter writer = new StringWriter();
    private final MockServletOutputStream outputStream = new MockServletOutputStream();

    public PrintWriter getWriter() {
        return new PrintWriter(writer);
    }

    public String getContent() {
        return writer.toString();
    }

    public void setContentType(String type) { /* no op */
    }

    public String getContentType() {
        return null;
    }

    public String getCharacterEncoding() {
        return null;
    }

    public void setCharacterEncoding(String charEncoding) { /* no op */
    }

    public int getBufferSize() {
        return -1;
    }

    public void setBufferSize(int size) { /* no op */
    }

    public void setLocale(Locale locale) { /* no op */
    }

    public Locale getLocale() {
        return null;
    }

    // public OutputStream getStream() { return null; }
    public ServletOutputStream getOutputStream() {
        return outputStream;
    }

    public boolean isCommitted() {
        return false;
    }

    public void resetBuffer() { /* no op */
    }

    public void reset() { /* no op */
    }

    public void setContentLength(int length) { /* no op */
    }

    public void flushBuffer() { /* no op */
    }

    public void sendError(int status) { /* no op */
    }

    public String encodeURL(String url) {
        return null;
    }

    public String encodeRedirectURL(String url) {
        return null;
    }

    public void addCookie(Cookie cookie) { /* no op */
    }

    public String encodeUrl(String url) {
        return null;
    }

    public void sendRedirect(String location) { /* no op */
    }

    public void addDateHeader(String name, long value) { /* no op */
    }

    public void setIntHeader(String name, int value) { /* no op */
    }

    public String encodeRedirectUrl(String url) {
        return null;
    }

    public void setStatus(int status) { /* no op */
    }

    public void addHeader(String name, String value) { /* no op */
    }

    public boolean containsHeader(String name) {
        return false;
    }

    public void sendError(int status, String message) { /* no op */
    }

    public void addIntHeader(String name, int value) { /* no op */
    }

    public void setDateHeader(String name, long value) { /* no op */
    }

    public void setHeader(String name, String value) { /* no op */
    }

    public void setStatus(int status, String message) { /* no op */
    }

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#setContentLengthLong(long)
	 */
	@Override
	public void setContentLengthLong(long len) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#getStatus()
	 */
	@Override
	public int getStatus() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#getHeader(java.lang.String)
	 */
	@Override
	public String getHeader(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#getHeaders(java.lang.String)
	 */
	@Override
	public Collection<String> getHeaders(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#getHeaderNames()
	 */
	@Override
	public Collection<String> getHeaderNames() {
		// TODO Auto-generated method stub
		return null;
	}
}
