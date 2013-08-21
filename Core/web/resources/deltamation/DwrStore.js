/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

define(["dojo/_base/declare", "dojo/Deferred", "deltamation/ArrayTester",
        "dojo/store/util/QueryResults", "dojo/store/util/SimpleQueryEngine"],
function(declare, Deferred, ArrayTester, QueryResults, SimpleQueryEngine) {

// No base class, but for purposes of documentation, the base class is dojo/store/api/Store
var base = null;

return declare("deltamation.DwrStore", base, {
    // summary:
    //        This is a basic in-memory object store. It implements dojo/store/api/Store.
    constructor: function(options) {
        // summary:
        //        Creates a memory object store.
        // options: dojo/store/Memory
        //        This provides any configuration information that will be mixed into the store.
        //        This should generally include the data property to provide the starting set of data.
        for(var i in options){
            this[i] = options[i];
        }
    },
    
    // dwr: Dwr object
    // must be created from a com.deltamation.mango.downtime.web.AbstractDwr
    // i.e. supports a load() method that returns response.data.list
    // also has get(id), save(vo), and remove(id)
    dwr: null,
    
    // controls whether sql is done using or or and
    or: true,

    // idProperty: String
    //        Indicates the property to use as the identity property. The values of this
    //        property should be unique.
    idProperty: "id",
    
    // queryEngine: Function
    //        Defines the query engine to use for querying the data store
    queryEngine: SimpleQueryEngine,
    
    // set this to true to fetch a full result set from Dwr and then filter/sort locally
    // using the engine set in queryEngine
    queryLocally: false,
    
    // Dwr timeout
    dwrTimeout: 5000,
    
    get: function(id) {
        // summary:
        //        Retrieves an object by its identity
        // id: Number
        //        The identity to use to lookup the object
        // returns: Object
        //        The object in the store that matches the given id.
        
        if (!id)
            return null;
        
        var deferred = new Deferred();
        
        this.dwr.getFull(id, {
            callback:function(response) {
                if (response.data.vo)
                    deferred.resolve(response.data.vo);
                else
                    deferred.reject(mangoTranslate("table.error.wrongId", [id]));
            },
            errorHandler: function(message) {
                message.dwrError = true;
                deferred.reject(mangoTranslate("table.error.dwr", [message]));
            },
            timeout: this.dwrTimeout
        });
        
        return deferred;
    },
    
    getIdentity: function(object) {
        // summary:
        //        Returns an object's identity
        // object: Object
        //        The object to get the identity from
        // returns: Number
        return object[this.idProperty];
    },
    
    put: function(object, options) {
        // summary:
        //        Stores an object
        // object: Object
        //        The object to store.
        // options: dojo/store/api/Store.PutDirectives?
        //        Additional metadata for storing the data.  Includes an "id"
        //        property if a specific id is to be used.
        // returns: Number
        options = options || {};
        
        if ("id" in options) {
            object[this.idProperty] = options.id;
        }
        if (!(this.idProperty in object)) {
            object[this.idProperty] = -1;
        }
        
        if (object[this.idProperty] !== -1 && !options.overwrite) {
            throw new Error("Object already exists");
        }
        
        if (typeof this.dwr.saveFull !== 'function')
            return object;
            
        var deferred = new Deferred();
        
        this.dwr.saveFull(object, {
            callback: function(response) {
                if (response.hasMessages) {
                    deferred.reject(response);
                }
                else {
                    deferred.resolve(response.data.vo);
                }
            },
            errorHandler: function(message) {
                message.dwrError = true;
                deferred.reject(mangoTranslate("table.error.dwr", [message]));
            },
            timeout: this.dwrTimeout
        });
        
        return deferred;
    },
    
    add: function(object, options) {
        // summary:
        //        Creates an object, throws an error if the object already exists
        // object: Object
        //        The object to store.
        // options: dojo/store/api/Store.PutDirectives?
        //        Additional metadata for storing the data.  Includes an "id"
        //        property if a specific id is to be used.
        // returns: Number
        (options = options || {}).overwrite = false;
        // call put with overwrite being false
        return this.put(object, options);
    },
    
    remove: function(id) {
        // summary:
        //        Deletes an object by its identity
        // id: Number
        //        The identity to use to delete the object
        // returns: Boolean
        //        Returns true if an object was removed, false (undefined) if no object matched the id
        
        if (typeof this.dwr.remove !== 'function')
            return true;
        
        if (!id)
            return false;
        
        var deferred = new Deferred();
        
        this.dwr.remove(id, {
            callback:function(response) {
                if (response.hasMessages) {
                    deferred.reject(response);
                }
                else {
                    deferred.resolve(true);
                }
            },
            errorHandler: function(message) {
                message.dwrError = true;
                deferred.reject(mangoTranslate("table.error.dwr", [message]));
            },
            timeout: this.dwrTimeout
        });
        return deferred;
    },
    
    query: function(query, options) {
        // summary:
        //        Queries the store for objects.
        // query: Object
        //        The query to use for retrieving objects from the store.
        // options: dojo/store/api/Store.QueryOptions?
        //        The optional arguments to apply to the resultset.
        // returns: dojo/store/api/Store.QueryResults
        //        The results of the query, extended with iterative methods.
        //
        // example:
        //        Given the following store:
        //
        //     |    var store = new Memory({
        //     |        data: [
        //     |            {id: 1, name: "one", prime: false },
        //    |            {id: 2, name: "two", even: true, prime: true},
        //    |            {id: 3, name: "three", prime: true},
        //    |            {id: 4, name: "four", even: true, prime: false},
        //    |            {id: 5, name: "five", prime: true}
        //    |        ]
        //    |    });
        //
        //    ...find all items where "prime" is true:
        //
        //    |    var results = store.query({ prime: true });
        //
        //    ...or find all items where "even" is true:
        //
        //    |    var results = store.query({ even: true });
        var results;
        if (this.queryLocally)
            results = this.localQuery(query, options);
        else
            results = this.remoteQuery(query, options);
        return results;
    },
    
    localQuery: function(query, options) {
        var deferred = new Deferred();
        var queryEngine = this.queryEngine;
        
        this.dwr.loadFull({
            callback:function(response) {
                // was getting some weird closure problems with using the function parameters directly here
                // query was coming up with a endTime property
                var results = queryEngine(query, options)(response.data.list);
                deferred.resolve(results);
            },
            errorHandler: function(message) {
                message.dwrError = true;
                deferred.reject(mangoTranslate("table.error.dwr", [message]));
            },
            timeout: this.dwrTimeout
        });
        return QueryResults(deferred);
    },
    
    remoteQuery: function(query, options) {
        var _this = this;
        var deferred = new Deferred();
        deferred.total = new Deferred();
        options = options || {};
        
        var sortArray, op;
        if (typeof options.sort === 'string') {
            op = new SortOption();
            op.attribute = options.sort;
            op.desc = false;
            sortArray = [op];
        }
        else if (options.sort && typeof options.sort.length === 'number') {
            sortArray = [];
            for (var i = 0; i < options.sort.length; i++) {
                op = new SortOption();
                op.attribute = options.sort[i].attribute;
                op.desc = options.sort[i].descending || false;
                sortArray.push(op);
            }
        }
        
        var filterMap = {};
        for (var prop in query) {
            var conditions = query[prop];
            // allow specific regex queries
            if (conditions instanceof RegExp) {
                // anything
                //if (conditions.source === '^.*$')
                //    continue;
                filterMap[prop] = 'RegExp:' + conditions.source;
                break;
            }
            if (conditions instanceof ArrayTester) {
                conditions = conditions.data;
            }
            if (typeof conditions === 'string' || typeof conditions === 'number')
                conditions = [conditions];
            filterMap[prop] = conditions.join();
        }
        
        var start = (isFinite(options.start)) ? options.start : null;
        var count = (isFinite(options.count)) ? options.count : null;
        
        this.dwr.dojoQuery(filterMap, sortArray, start, count, this.or, {
            callback: function(response) {
                // sets a query id so that a query can be retrieved from a cache
                if (typeof _this.queryCallback === 'function') {
                    _this.queryCallback(response);
                }

                deferred.resolve(response.data.list);
                deferred.total.resolve(response.data.total);
            },
            errorHandler: function(message) {
                message.dwrError = true;
                var msg = mangoTranslate("table.error.dwr", [message]);
                deferred.reject(msg);
                deferred.total.reject(msg);
            },
            timeout: this.dwrTimeout
        });
        
        return QueryResults(deferred);
    },
    
    queryCallback: null
});

});
