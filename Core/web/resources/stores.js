/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

var stores = {};


require(["deltamation/CachedDwrStore", "dojo/Deferred","dojo/store/util/QueryResults"],
function(CachedDwrStore,QueryResults) {

if (typeof DataSourceDwr !== 'undefined') {
    stores.dataSource = new CachedDwrStore(DataSourceDwr, "DataSourceDwr");
    // only a small list, get full on each and sort/filter locally
    stores.dataSource.dwr.queryLocally = false;
}

if (typeof DataPointDwr !== 'undefined') {
	DataPointDwr.loadFull = DataPointDwr.getPoints;
    stores.dataPoint = new CachedDwrStore(DataPointDwr, "DataPointDwr");
    // only a small list, get full on each and sort/filter locally
    stores.dataPoint.dwr.queryLocally = false;
    stores.dataPoint.dwr.loadData = false;
}

}); // require
