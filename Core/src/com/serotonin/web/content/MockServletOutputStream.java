package com.serotonin.web.content;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

public class MockServletOutputStream extends ServletOutputStream {
    @Override
    public void write(int b) {
        // no op
    }

	/* (non-Javadoc)
	 * @see javax.servlet.ServletOutputStream#isReady()
	 */
	@Override
	public boolean isReady() {
		return true;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletOutputStream#setWriteListener(javax.servlet.WriteListener)
	 */
	@Override
	public void setWriteListener(WriteListener writeListener) {
		
	}
}
