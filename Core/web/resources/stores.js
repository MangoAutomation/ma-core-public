/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

var stores = {};


require(["deltamation/CachedDwrStore", "dojo/Deferred","dojo/store/util/QueryResults"],
function(CachedDwrStore,QueryResults) {

if (typeof DataSourceDwr !== 'undefined') {
    stores.dataSource = new CachedDwrStore(DataSourceDwr, "DataSourceDwr");
    stores.dataSource.dwr.queryLocally = false;
}

if (typeof DataPointDwr !== 'undefined') {
	DataPointDwr.loadFull = DataPointDwr.getPoints;
    stores.dataPoint = new CachedDwrStore(DataPointDwr, "DataPointDwr");
    stores.dataPoint.dwr.queryLocally = false;
    stores.dataPoint.dwr.loadData = false;
    stores.dataPoint.dwr.or = false; //Use AND in Queries to restrict to DataSource of interest

    stores.allDataPoints = new CachedDwrStore(DataPointDwr, "DataPointDwr");
    stores.allDataPoints.dwr.queryLocally = false;
    stores.allDataPoints.dwr.loadData = true;
    stores.allDataPoints.dwr.or = false; //Use AND in Queries to restrict to DataSource of interest


}

if (typeof DataPointDetailsDwr !== 'undefined') {
    stores.dataPointDetails = new CachedDwrStore(DataPointDetailsDwr, "DataPointDetailsDwr");
    stores.dataPointDetails.dwr.queryLocally = false;
}

if (typeof EventInstanceDwr !== 'undefined') {
    stores.eventInstances = new CachedDwrStore(EventInstanceDwr, "EventInstanceDwr");
    stores.eventInstances.dwr.queryLocally = false;
    stores.eventInstances.dwr.or = false; //Use AND in Queries to combine filters
    stores.eventInstances.dwr.dwrTimeout = 20000; //Default to use 20s timeout for larger Tables
}




}); // require
