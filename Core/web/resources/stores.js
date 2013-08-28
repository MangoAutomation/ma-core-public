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
    stores.dataPoint.dwr.or = false; //Use AND in Queries to restrict to DataSource of interest
}

if (typeof DataPointDetailsDwr !== 'undefined') {
    stores.dataPointDetails = new CachedDwrStore(DataPointDetailsDwr, "DataPointDetailsDwr");
    // only a small list, get full on each and sort/filter locally
    stores.dataPointDetails.dwr.queryLocally = false;
}


}); // require
