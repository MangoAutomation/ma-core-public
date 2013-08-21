/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

var stores = {};

require(["deltamation/CachedDwrStore"],
function(CachedDwrStore) {

if (typeof DataSourceDwr !== 'undefined') {
    stores.dataSource = new CachedDwrStore(DataSourceDwr, "DataSourceDwr");
    // only a small list, get full on each and sort/filter locally
    stores.dataSource.dwr.queryLocally = true;
}

}); // require
