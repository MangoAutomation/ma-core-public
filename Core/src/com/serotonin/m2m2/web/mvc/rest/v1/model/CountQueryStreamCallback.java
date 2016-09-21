/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.io.IOException;

/**
 * Class that can be used
 * with a PageQueryStream when objects are
 * filtered out of the stream after the query.
 * 
 * Usage:
 * 
 * 1.  Set filtered count
 * 2.  Perform query tied to this callback
 * 3.  Output will contain the query's count less the filtered count
 * 
 * @author Terry Packer
 *
 */
public class CountQueryStreamCallback extends FilteredQueryStreamCallback<Long> {

	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.QueryStreamCallback#write(java.lang.Object)
	 */
	@Override
	protected void write(Long vo) throws IOException {
		//Remove our filtered count
		vo = vo - this.filtered;
		super.write(vo);
	}
}
