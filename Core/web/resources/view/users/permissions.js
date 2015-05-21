/**
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

require(['jquery', 'dgrid/OnDemandGrid', 'dstore/Rest', 'dstore/Request', 'dstore/Trackable', 'dojo/domReady!'],
function(OnDemandGrid, Rest, Request, Trackable) {
	
	var pointStore = new Rest({
	    target: '/rest/v1/data-points',
	    idProperty: 'xid'
	});
	
	var pointsGrid = new OnDemandGrid({
	    collection: pointStore,
	    columns: {
	        xid: 'XID',
	        name: 'name',
	        dataSourceXid: {
	            label: 'Data Source',
	        },
	    },
	    sort: [{property: 'dataSourceXid', descending: true}]
	}, 'points-grid');
	
});