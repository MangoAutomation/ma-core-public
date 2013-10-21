require({cache:{
'dgrid/Selection':function(){
define(["dojo/_base/kernel", "dojo/_base/declare", "dojo/_base/Deferred", "dojo/on", "dojo/has", "dojo/aspect", "./List", "dojo/has!touch?./util/touch", "put-selector/put", "dojo/query", "dojo/_base/sniff"],
function(kernel, declare, Deferred, on, has, aspect, List, touchUtil, put){

has.add("mspointer", function(global, doc, element){
	return "onmspointerdown" in element;
});

// Add feature test for user-select CSS property for optionally disabling
// text selection.
// (Can't use dom.setSelectable prior to 1.8.2 because of bad sniffs, see #15990)
has.add("css-user-select", function(global, doc, element){
	var style = element.style,
		prefixes = ["Khtml", "O", "ms", "Moz", "Webkit"],
		i = prefixes.length,
		name = "userSelect";

	// Iterate prefixes from most to least likely
	do{
		if(typeof style[name] !== "undefined"){
			// Supported; return property name
			return name;
		}
	}while(i-- && (name = prefixes[i] + "UserSelect"));

	// Not supported if we didn't return before now
	return false;
});

// Also add a feature test for the onselectstart event, which offers a more
// graceful fallback solution than node.unselectable.
has.add("dom-selectstart", typeof document.onselectstart !== "undefined");

var ctrlEquiv = has("mac") ? "metaKey" : "ctrlKey",
	hasUserSelect = has("css-user-select"),
	downType = has("mspointer") ? "MSPointerDown" : "mousedown",
	upType = has("mspointer") ? "MSPointerUp" : "mouseup";

function makeUnselectable(node, unselectable){
	// Utility function used in fallback path for recursively setting unselectable
	var value = node.unselectable = unselectable ? "on" : "",
		elements = node.getElementsByTagName("*"),
		i = elements.length;
	
	while(--i){
		if(elements[i].tagName === "INPUT" || elements[i].tagName === "TEXTAREA"){
			continue; // Don't prevent text selection in text input fields.
		}
		elements[i].unselectable = value;
	}
}

function setSelectable(grid, selectable){
	// Alternative version of dojo/dom.setSelectable based on feature detection.
	
	// For FF < 21, use -moz-none, which will respect -moz-user-select: text on
	// child elements (e.g. form inputs).  In FF 21, none behaves the same.
	// See https://developer.mozilla.org/en-US/docs/CSS/user-select
	var node = grid.bodyNode,
		value = selectable ? "text" : has("ff") < 21 ? "-moz-none" : "none";
	
	if(hasUserSelect){
		node.style[hasUserSelect] = value;
	}else if(has("dom-selectstart")){
		// For browsers that don't support user-select but support selectstart (IE<10),
		// we can hook up an event handler as necessary.  Since selectstart bubbles,
		// it will handle any child elements as well.
		// Note, however, that both this and the unselectable fallback below are
		// incapable of preventing text selection from outside the targeted node.
		if(!selectable && !grid._selectstartHandle){
			grid._selectstartHandle = on(node, "selectstart", function(evt){
				var tag = evt.target && evt.target.tagName;
				
				// Prevent selection except where a text input field is involved.
				if(tag !== "INPUT" && tag !== "TEXTAREA"){
					evt.preventDefault();
				}
			});
		}else if(selectable && grid._selectstartHandle){
			grid._selectstartHandle.remove();
			delete grid._selectstartHandle;
		}
	}else{
		// For browsers that don't support either user-select or selectstart (Opera),
		// we need to resort to setting the unselectable attribute on all nodes
		// involved.  Since this doesn't automatically apply to child nodes, we also
		// need to re-apply it whenever rows are rendered.
		makeUnselectable(node, !selectable);
		if(!selectable && !grid._unselectableHandle){
			grid._unselectableHandle = aspect.after(grid, "renderRow", function(row){
				makeUnselectable(row, true);
				return row;
			});
		}else if(selectable && grid._unselectableHandle){
			grid._unselectableHandle.remove();
			delete grid._unselectableHandle;
		}
	}
}

return declare(null, {
	// summary:
	//		Add selection capabilities to a grid. The grid will have a selection property and
	//		fire "dgrid-select" and "dgrid-deselect" events.
	
	// selectionDelegate: String
	//		Selector to delegate to as target of selection events.
	selectionDelegate: ".dgrid-row",
	
	// selectionEvents: String
	//		Event (or events, comma-delimited) to listen on to trigger select logic.
	//		Note: this is ignored in the case of touch devices.
	selectionEvents: downType + "," + upType + ",dgrid-cellfocusin",
	
	// deselectOnRefresh: Boolean
	//		If true, the selection object will be cleared when refresh is called.
	deselectOnRefresh: true,
	
	// allowSelectAll: Boolean
	//		If true, allow ctrl/cmd+A to select all rows.
	//		Also consulted by the selector plugin for showing select-all checkbox.
	allowSelectAll: false,
	
	// selection:
	//		An object where the property names correspond to 
	//		object ids and values are true or false depending on whether an item is selected
	selection: {},
	
	// selectionMode: String
	//		The selection mode to use, can be "none", "multiple", "single", or "extended".
	selectionMode: "extended",
	
	// allowTextSelection: Boolean
	//		Whether to still allow text within cells to be selected.  The default
	//		behavior is to allow text selection only when selectionMode is none;
	//		setting this property to either true or false will explicitly set the
	//		behavior regardless of selectionMode.
	allowTextSelection: undefined,
	
	create: function(){
		this.selection = {};
		return this.inherited(arguments);
	},
	postCreate: function(){
		this.inherited(arguments);
		
		// Force selectionMode setter to run.
		var selectionMode = this.selectionMode;
		this.selectionMode = "";
		this._setSelectionMode(selectionMode);
		
		this._initSelectionEvents(); // first time; set up event hooks
	},
	
	destroy: function(){
		this.inherited(arguments);
		
		// Remove any extra handles added by Selection.
		if(this._selectstartHandle){ this._selectstartHandle.remove(); }
		if(this._unselectableHandle){ this._unselectableHandle.remove(); }
		if(this._deselectionHandle){ this._deselectionHandle.remove(); }
	},
	
	_setSelectionMode: function(mode){
		// summary:
		//		Updates selectionMode, resetting necessary variables.
		if(mode == this.selectionMode){ return; } // prevent unnecessary spinning
		
		// Start selection fresh when switching mode.
		this.clearSelection();
		
		this.selectionMode = mode;
		
		// Compute name of selection handler for this mode once
		// (in the form of _fooSelectionHandler)
		this._selectionHandlerName = "_" + mode + "SelectionHandler";
		
		// Also re-run allowTextSelection setter in case it is in automatic mode.
		this._setAllowTextSelection(this.allowTextSelection);
	},
	setSelectionMode: function(mode){
		kernel.deprecated("setSelectionMode(...)", 'use set("selectionMode", ...) instead', "dgrid 0.4");
		this.set("selectionMode", mode);
	},
	
	_setAllowTextSelection: function(allow){
		if(typeof allow !== "undefined"){
			setSelectable(this, allow);
		}else{
			setSelectable(this, this.selectionMode === "none");
		}
		this.allowTextSelection = allow;
	},
	
	_handleSelect: function(event, target){
		// Don't run if selection mode doesn't have a handler (incl. "none"),
		// or if coming from a dgrid-cellfocusin from a mousedown
		if(!this[this._selectionHandlerName] ||
				(event.type === "dgrid-cellfocusin" && event.parentType === "mousedown") ||
				(event.type === upType && target != this._waitForMouseUp)){
			return;
		}
		this._waitForMouseUp = null;
		this._selectionTriggerEvent = event;
		
		// Don't call select handler for ctrl+navigation
		if(!event.keyCode || !event.ctrlKey || event.keyCode == 32){
			// If clicking a selected item, wait for mouseup so that drag n' drop
			// is possible without losing our selection
			if(!event.shiftKey && event.type === downType && this.isSelected(target)){
				this._waitForMouseUp = target;
			}else{
				this[this._selectionHandlerName](event, target);
			}
		}
		this._selectionTriggerEvent = null;
	},
	
	_singleSelectionHandler: function(event, target){
		// summary:
		//		Selection handler for "single" mode, where only one target may be
		//		selected at a time.
		
		var ctrlKey = event.keyCode ? event.ctrlKey : event[ctrlEquiv];
		if(this._lastSelected === target){
			// Allow ctrl to toggle selection, even within single select mode.
			this.select(target, null, !ctrlKey || !this.isSelected(target));
		}else{
			this.clearSelection();
			this.select(target);
			this._lastSelected = target;
		}
	},
	
	_multipleSelectionHandler: function(event, target){
		// summary:
		//		Selection handler for "multiple" mode, where shift can be held to
		//		select ranges, ctrl/cmd can be held to toggle, and clicks/keystrokes
		//		without modifier keys will add to the current selection.
		
		var lastRow = this._lastSelected,
			ctrlKey = event.keyCode ? event.ctrlKey : event[ctrlEquiv],
			value;
		
		if(!event.shiftKey){
			// Toggle if ctrl is held; otherwise select
			value = ctrlKey ? null : true;
			lastRow = null;
		}
		this.select(target, lastRow, value);

		if(!lastRow){
			// Update reference for potential subsequent shift+select
			// (current row was already selected above)
			this._lastSelected = target;
		}
	},
	
	_extendedSelectionHandler: function(event, target){
		// summary:
		//		Selection handler for "extended" mode, which is like multiple mode
		//		except that clicks/keystrokes without modifier keys will clear
		//		the previous selection.
		
		// Clear selection first for right-clicks outside selection and non-ctrl-clicks;
		// otherwise, extended mode logic is identical to multiple mode
		if(event.button === 2 ? !this.isSelected(target) :
				!(event.keyCode ? event.ctrlKey : event[ctrlEquiv])){
			this.clearSelection(null, true);
		}
		this._multipleSelectionHandler(event, target);
	},
	
	_toggleSelectionHandler: function(event, target){
		// summary:
		//		Selection handler for "toggle" mode which simply toggles the selection
		//		of the given target.  Primarily useful for touch input.
		
		this.select(target, null, null);
	},

	_initSelectionEvents: function(){
		// summary:
		//		Performs first-time hookup of event handlers containing logic
		//		required for selection to operate.
		
		var grid = this,
			selector = this.selectionDelegate;
		
		if(has("touch")){
			// listen for touch taps if available
			on(this.contentNode, touchUtil.selector(selector, touchUtil.tap), function(evt){
				grid._handleSelect(evt, this);
			});
		}else{
			// listen for actions that should cause selections
			on(this.contentNode, on.selector(selector, this.selectionEvents), function(event){
				grid._handleSelect(event, this);
			});
		}
		
		// Also hook up spacebar (for ctrl+space)
		if(this.addKeyHandler){
			this.addKeyHandler(32, function(event){
				grid._handleSelect(event, event.target);
			});
		}
		
		// If allowSelectAll is true, allow ctrl/cmd+A to (de)select all rows.
		// (Handler further checks against _allowSelectAll, which may be updated
		// if selectionMode is changed post-init.)
		if(this.allowSelectAll){
			this.on("keydown", function(event) {
				if (event[ctrlEquiv] && event.keyCode == 65) {
					event.preventDefault();
					grid[grid.allSelected ? "clearSelection" : "selectAll"]();
				}
			});
		}
		
		// Update aspects if there is a store change
		if(this._setStore){
			aspect.after(this, "_setStore", function(){
				grid._updateDeselectionAspect();
			});
		}
		this._updateDeselectionAspect();
	},
	
	_updateDeselectionAspect: function(){
		// summary:
		//		Hooks up logic to handle deselection of removed items.
		//		Aspects to an observable store's notify method if applicable,
		//		or to the list/grid's removeRow method otherwise.
		
		var self = this,
			store = this.store;
		
		// Remove anything previously configured
		if(this._deselectionSignal){
			this._deselectionSignal.remove();
		}
		
		// Is there currently an observable store?
		if(store && store.notify){
			this._deselectionSignal = aspect.after(store, "notify", function(object, idToUpdate){
				var id = idToUpdate || (object && object[this.idProperty || "id"]);
				if (id) {
					var row = self.row(id),
						selection = row && self.selection[row.id];
					// Is the row currently in the selection list.
					if(selection){
						// Ensure consistency between DOM and state on add/put;
						// prune from selection on remove
						self[(object ? "" : "de") + "select"](row, null, selection);
					}
				}
			}, true);
		}else{
			this._deselectionSignal = aspect.before(this, "removeRow", function(rowElement, justCleanup){
				var row;
				if(!justCleanup){
					row = this.row(rowElement);
					// if it is a real row removal for a selected item, deselect it
					if(row && (row.id in this.selection)){
						this.deselect(row);
					}
				}
			});
		}
	},
	
	allowSelect: function(row){
		// summary:
		//		A method that can be overriden to determine whether or not a row (or 
		//		cell) can be selected. By default, all rows (or cells) are selectable.
		return true;
	},
	
	_selectionEventQueue: function(value, type){
		var grid = this,
			event = "dgrid-" + (value ? "select" : "deselect"),
			rows = this[event], // current event queue (actually cells for CellSelection)
			trigger = this._selectionTriggerEvent;
		
		if (trigger) {
			// If selection was triggered by another event, we want to know its type
			// to report later.  Grab it ahead of the timeout to avoid
			// "member not found" errors in IE < 9.
			trigger = trigger.type;
		}
		
		if(rows){ return rows; } // return existing queue, allowing to push more
		
		// Create a timeout to fire an event for the accumulated rows once everything is done.
		// We expose the callback in case the event needs to be fired immediately.
		setTimeout(this._fireSelectionEvent = function(){
			if(!rows){ return; } // rows will be set only the first time this is called
			
			var eventObject = {
				bubbles: true,
				grid: grid
			};
			if(trigger){ eventObject.parentType = trigger; }
			eventObject[type] = rows;
			on.emit(grid.contentNode, event, eventObject);
			rows = null;
			// clear the queue, so we create a new one as needed
			delete grid[event];
		}, 0);
		return (rows = this[event] = []);
	},
	select: function(row, toRow, value){
		if(value === undefined){
			// default to true
			value = true;
		} 
		if(!row.element){
			row = this.row(row);
		}
		
		// Check whether we're allowed to select the given row before proceeding.
		// If a deselect operation is being performed, this check is skipped,
		// to avoid errors when changing column definitions, and since disabled
		// rows shouldn't ever be selected anyway.
		if(value === false || this.allowSelect(row)){
			var selection = this.selection;
			var previousValue = selection[row.id];
			if(value === null){
				// indicates a toggle
				value = !previousValue;
			}
			var element = row.element;
			if(!value && !this.allSelected){
				delete this.selection[row.id];
			}else{
				selection[row.id] = value;
			}
			if(element){
				// add or remove classes as appropriate
				if(value){
					put(element, ".dgrid-selected.ui-state-active");
				}else{
					put(element, "!dgrid-selected!ui-state-active");
				}
			}
			if(value != previousValue && element){
				// add to the queue of row events
				this._selectionEventQueue(value, "rows").push(row);
			}
			
			if(toRow){
				if(!toRow.element){
					toRow = this.row(toRow);
				}
				var toElement = toRow.element;
				var fromElement = row.element;
				// find if it is earlier or later in the DOM
				var traverser = (toElement && (toElement.compareDocumentPosition ? 
					toElement.compareDocumentPosition(fromElement) == 2 :
					toElement.sourceIndex > fromElement.sourceIndex)) ? "down" : "up";
				while(row.element != toElement && (row = this[traverser](row))){
					this.select(row, null, value);
				}
			}
		}
	},
	deselect: function(row, toRow){
		this.select(row, toRow, false);
	},
	clearSelection: function(exceptId, dontResetLastSelected){
		// summary:
		//		Deselects any currently-selected items.
		// exceptId: Mixed?
		//		If specified, the given id will not be deselected.
		
		this.allSelected = false;
		for(var id in this.selection){
			if(exceptId !== id){
				this.deselect(id);
			}
		}
		if(!dontResetLastSelected){
			this._lastSelected = null;
		}
	},
	selectAll: function(){
		this.allSelected = true;
		this.selection = {}; // we do this to clear out pages from previous sorts
		for(var i in this._rowIdToObject){
			var row = this.row(this._rowIdToObject[i]);
			this.select(row.id);
		}
	},
	isSelected: function(object){
		// summary:
		//		Returns true if the indicated row is selected.
		
		if(typeof object === "undefined" || object === null){
			return false;
		}
		if(!object.element){
			object = this.row(object);
		}
		
		// First check whether the given row is indicated in the selection hash;
		// failing that, check if allSelected is true (testing against the
		// allowSelect method if possible)
		return (object.id in this.selection) ? !!this.selection[object.id] :
			this.allSelected && (!object.data || this.allowSelect(object));
	},
	
	refresh: function(){
		if(this.deselectOnRefresh){
			this.clearSelection();
			// Need to fire the selection event now because after the refresh,
			// the nodes that we will fire for will be gone.
			this._fireSelectionEvent && this._fireSelectionEvent();
		}
		this._lastSelected = null;
		return this.inherited(arguments);
	},
	
	renderArray: function(){
		var grid = this,
			rows = this.inherited(arguments);
		
		Deferred.when(rows, function(rows){
			var selection = grid.selection,
				i, row, selected;
			for(i = 0; i < rows.length; i++){
				row = grid.row(rows[i]);
				selected = row.id in selection ? selection[row.id] : grid.allSelected;
				if(selected){
					grid.select(row, null, selected);
				}
			}
		});
		return rows;
	}
});

});

},
'dojo/_base/Deferred':function(){
define([
	"./kernel",
	"../Deferred",
	"../promise/Promise",
	"../errors/CancelError",
	"../has",
	"./lang",
	"../when"
], function(dojo, NewDeferred, Promise, CancelError, has, lang, when){
	// module:
	//		dojo/_base/Deferred

	var mutator = function(){};
	var freeze = Object.freeze || function(){};
	// A deferred provides an API for creating and resolving a promise.
	var Deferred = dojo.Deferred = function(/*Function?*/ canceller){
		// summary:
		//		Deprecated.   This module defines the legacy dojo/_base/Deferred API.
		//		New code should use dojo/Deferred instead.
		// description:
		//		The Deferred API is based on the concept of promises that provide a
		//		generic interface into the eventual completion of an asynchronous action.
		//		The motivation for promises fundamentally is about creating a
		//		separation of concerns that allows one to achieve the same type of
		//		call patterns and logical data flow in asynchronous code as can be
		//		achieved in synchronous code. Promises allows one
		//		to be able to call a function purely with arguments needed for
		//		execution, without conflating the call with concerns of whether it is
		//		sync or async. One shouldn't need to alter a call's arguments if the
		//		implementation switches from sync to async (or vice versa). By having
		//		async functions return promises, the concerns of making the call are
		//		separated from the concerns of asynchronous interaction (which are
		//		handled by the promise).
		//
		//		The Deferred is a type of promise that provides methods for fulfilling the
		//		promise with a successful result or an error. The most important method for
		//		working with Dojo's promises is the then() method, which follows the
		//		CommonJS proposed promise API. An example of using a Dojo promise:
		//
		//		|	var resultingPromise = someAsyncOperation.then(function(result){
		//		|		... handle result ...
		//		|	},
		//		|	function(error){
		//		|		... handle error ...
		//		|	});
		//
		//		The .then() call returns a new promise that represents the result of the
		//		execution of the callback. The callbacks will never affect the original promises value.
		//
		//		The Deferred instances also provide the following functions for backwards compatibility:
		//
		//		- addCallback(handler)
		//		- addErrback(handler)
		//		- callback(result)
		//		- errback(result)
		//
		//		Callbacks are allowed to return promises themselves, so
		//		you can build complicated sequences of events with ease.
		//
		//		The creator of the Deferred may specify a canceller.  The canceller
		//		is a function that will be called if Deferred.cancel is called
		//		before the Deferred fires. You can use this to implement clean
		//		aborting of an XMLHttpRequest, etc. Note that cancel will fire the
		//		deferred with a CancelledError (unless your canceller returns
		//		another kind of error), so the errbacks should be prepared to
		//		handle that error for cancellable Deferreds.
		// example:
		//	|	var deferred = new Deferred();
		//	|	setTimeout(function(){ deferred.callback({success: true}); }, 1000);
		//	|	return deferred;
		// example:
		//		Deferred objects are often used when making code asynchronous. It
		//		may be easiest to write functions in a synchronous manner and then
		//		split code using a deferred to trigger a response to a long-lived
		//		operation. For example, instead of register a callback function to
		//		denote when a rendering operation completes, the function can
		//		simply return a deferred:
		//
		//		|	// callback style:
		//		|	function renderLotsOfData(data, callback){
		//		|		var success = false
		//		|		try{
		//		|			for(var x in data){
		//		|				renderDataitem(data[x]);
		//		|			}
		//		|			success = true;
		//		|		}catch(e){ }
		//		|		if(callback){
		//		|			callback(success);
		//		|		}
		//		|	}
		//
		//		|	// using callback style
		//		|	renderLotsOfData(someDataObj, function(success){
		//		|		// handles success or failure
		//		|		if(!success){
		//		|			promptUserToRecover();
		//		|		}
		//		|	});
		//		|	// NOTE: no way to add another callback here!!
		// example:
		//		Using a Deferred doesn't simplify the sending code any, but it
		//		provides a standard interface for callers and senders alike,
		//		providing both with a simple way to service multiple callbacks for
		//		an operation and freeing both sides from worrying about details
		//		such as "did this get called already?". With Deferreds, new
		//		callbacks can be added at any time.
		//
		//		|	// Deferred style:
		//		|	function renderLotsOfData(data){
		//		|		var d = new Deferred();
		//		|		try{
		//		|			for(var x in data){
		//		|				renderDataitem(data[x]);
		//		|			}
		//		|			d.callback(true);
		//		|		}catch(e){
		//		|			d.errback(new Error("rendering failed"));
		//		|		}
		//		|		return d;
		//		|	}
		//
		//		|	// using Deferred style
		//		|	renderLotsOfData(someDataObj).then(null, function(){
		//		|		promptUserToRecover();
		//		|	});
		//		|	// NOTE: addErrback and addCallback both return the Deferred
		//		|	// again, so we could chain adding callbacks or save the
		//		|	// deferred for later should we need to be notified again.
		// example:
		//		In this example, renderLotsOfData is synchronous and so both
		//		versions are pretty artificial. Putting the data display on a
		//		timeout helps show why Deferreds rock:
		//
		//		|	// Deferred style and async func
		//		|	function renderLotsOfData(data){
		//		|		var d = new Deferred();
		//		|		setTimeout(function(){
		//		|			try{
		//		|				for(var x in data){
		//		|					renderDataitem(data[x]);
		//		|				}
		//		|				d.callback(true);
		//		|			}catch(e){
		//		|				d.errback(new Error("rendering failed"));
		//		|			}
		//		|		}, 100);
		//		|		return d;
		//		|	}
		//
		//		|	// using Deferred style
		//		|	renderLotsOfData(someDataObj).then(null, function(){
		//		|		promptUserToRecover();
		//		|	});
		//
		//		Note that the caller doesn't have to change his code at all to
		//		handle the asynchronous case.

		var result, finished, canceled, fired, isError, head, nextListener;
		var promise = (this.promise = new Promise());

		function complete(value){
			if(finished){
				throw new Error("This deferred has already been resolved");
			}
			result = value;
			finished = true;
			notify();
		}
		function notify(){
			var mutated;
			while(!mutated && nextListener){
				var listener = nextListener;
				nextListener = nextListener.next;
				if((mutated = (listener.progress == mutator))){ // assignment and check
					finished = false;
				}

				var func = (isError ? listener.error : listener.resolved);
				if(has("config-useDeferredInstrumentation")){
					if(isError && NewDeferred.instrumentRejected){
						NewDeferred.instrumentRejected(result, !!func);
					}
				}
				if(func){
					try{
						var newResult = func(result);
						if (newResult && typeof newResult.then === "function"){
							newResult.then(lang.hitch(listener.deferred, "resolve"), lang.hitch(listener.deferred, "reject"), lang.hitch(listener.deferred, "progress"));
							continue;
						}
						var unchanged = mutated && newResult === undefined;
						if(mutated && !unchanged){
							isError = newResult instanceof Error;
						}
						listener.deferred[unchanged && isError ? "reject" : "resolve"](unchanged ? result : newResult);
					}catch(e){
						listener.deferred.reject(e);
					}
				}else{
					if(isError){
						listener.deferred.reject(result);
					}else{
						listener.deferred.resolve(result);
					}
				}
			}
		}

		this.isResolved = promise.isResolved = function(){
			// summary:
			//		Checks whether the deferred has been resolved.
			// returns: Boolean

			return fired == 0;
		};

		this.isRejected = promise.isRejected = function(){
			// summary:
			//		Checks whether the deferred has been rejected.
			// returns: Boolean

			return fired == 1;
		};

		this.isFulfilled = promise.isFulfilled = function(){
			// summary:
			//		Checks whether the deferred has been resolved or rejected.
			// returns: Boolean

			return fired >= 0;
		};

		this.isCanceled = promise.isCanceled = function(){
			// summary:
			//		Checks whether the deferred has been canceled.
			// returns: Boolean

			return canceled;
		};

		// calling resolve will resolve the promise
		this.resolve = this.callback = function(value){
			// summary:
			//		Fulfills the Deferred instance successfully with the provide value
			this.fired = fired = 0;
			this.results = [value, null];
			complete(value);
		};


		// calling error will indicate that the promise failed
		this.reject = this.errback = function(error){
			// summary:
			//		Fulfills the Deferred instance as an error with the provided error
			isError = true;
			this.fired = fired = 1;
			if(has("config-useDeferredInstrumentation")){
				if(NewDeferred.instrumentRejected){
					NewDeferred.instrumentRejected(error, !!nextListener);
				}
			}
			complete(error);
			this.results = [null, error];
		};
		// call progress to provide updates on the progress on the completion of the promise
		this.progress = function(update){
			// summary:
			//		Send progress events to all listeners
			var listener = nextListener;
			while(listener){
				var progress = listener.progress;
				progress && progress(update);
				listener = listener.next;
			}
		};
		this.addCallbacks = function(callback, errback){
			// summary:
			//		Adds callback and error callback for this deferred instance.
			// callback: Function?
			//		The callback attached to this deferred object.
			// errback: Function?
			//		The error callback attached to this deferred object.
			// returns:
			//		Returns this deferred object.
			this.then(callback, errback, mutator);
			return this;	// Deferred
		};
		// provide the implementation of the promise
		promise.then = this.then = function(/*Function?*/resolvedCallback, /*Function?*/errorCallback, /*Function?*/progressCallback){
			// summary:
			//		Adds a fulfilledHandler, errorHandler, and progressHandler to be called for
			//		completion of a promise. The fulfilledHandler is called when the promise
			//		is fulfilled. The errorHandler is called when a promise fails. The
			//		progressHandler is called for progress events. All arguments are optional
			//		and non-function values are ignored. The progressHandler is not only an
			//		optional argument, but progress events are purely optional. Promise
			//		providers are not required to ever create progress events.
			//
			//		This function will return a new promise that is fulfilled when the given
			//		fulfilledHandler or errorHandler callback is finished. This allows promise
			//		operations to be chained together. The value returned from the callback
			//		handler is the fulfillment value for the returned promise. If the callback
			//		throws an error, the returned promise will be moved to failed state.
			//
			// returns:
			//		Returns a new promise that represents the result of the
			//		execution of the callback. The callbacks will never affect the original promises value.
			// example:
			//		An example of using a CommonJS compliant promise:
			//		|	asyncComputeTheAnswerToEverything().
			//		|		then(addTwo).
			//		|		then(printResult, onError);
			//		|	>44
			//
			var returnDeferred = progressCallback == mutator ? this : new Deferred(promise.cancel);
			var listener = {
				resolved: resolvedCallback,
				error: errorCallback,
				progress: progressCallback,
				deferred: returnDeferred
			};
			if(nextListener){
				head = head.next = listener;
			}
			else{
				nextListener = head = listener;
			}
			if(finished){
				notify();
			}
			return returnDeferred.promise; // Promise
		};
		var deferred = this;
		promise.cancel = this.cancel = function(){
			// summary:
			//		Cancels the asynchronous operation
			if(!finished){
				var error = canceller && canceller(deferred);
				if(!finished){
					if (!(error instanceof Error)){
						error = new CancelError(error);
					}
					error.log = false;
					deferred.reject(error);
				}
			}
			canceled = true;
		};
		freeze(promise);
	};
	lang.extend(Deferred, {
		addCallback: function(/*Function*/ callback){
			// summary:
			//		Adds successful callback for this deferred instance.
			// returns:
			//		Returns this deferred object.
			return this.addCallbacks(lang.hitch.apply(dojo, arguments));	// Deferred
		},

		addErrback: function(/*Function*/ errback){
			// summary:
			//		Adds error callback for this deferred instance.
			// returns:
			//		Returns this deferred object.
			return this.addCallbacks(null, lang.hitch.apply(dojo, arguments));	// Deferred
		},

		addBoth: function(/*Function*/ callback){
			// summary:
			//		Add handler as both successful callback and error callback for this deferred instance.
			// returns:
			//		Returns this deferred object.
			var enclosed = lang.hitch.apply(dojo, arguments);
			return this.addCallbacks(enclosed, enclosed);	// Deferred
		},
		fired: -1
	});

	Deferred.when = dojo.when = when;

	return Deferred;
});

},
'dojo/_base/window':function(){
define(["./kernel", "./lang", "../sniff"], function(dojo, lang, has){
// module:
//		dojo/_base/window

var ret = {
	// summary:
	//		API to save/set/restore the global/document scope.

	global: dojo.global,
	/*=====
	 global: {
		 // summary:
		 //		Alias for the current window. 'global' can be modified
		 //		for temporary context shifting. See also withGlobal().
		 // description:
		 //		Use this rather than referring to 'window' to ensure your code runs
		 //		correctly in managed contexts.
	 },
	 =====*/

	doc: this["document"] || null,
	/*=====
	doc: {
		// summary:
		//		Alias for the current document. 'doc' can be modified
		//		for temporary context shifting. See also withDoc().
		// description:
		//		Use this rather than referring to 'window.document' to ensure your code runs
		//		correctly in managed contexts.
		// example:
		//	|	n.appendChild(dojo.doc.createElement('div'));
	},
	=====*/

	body: function(/*Document?*/ doc){
		// summary:
		//		Return the body element of the specified document or of dojo/_base/window::doc.
		// example:
		//	|	win.body().appendChild(dojo.doc.createElement('div'));

		// Note: document.body is not defined for a strict xhtml document
		// Would like to memoize this, but dojo.doc can change vi dojo.withDoc().
		doc = doc || dojo.doc;
		return doc.body || doc.getElementsByTagName("body")[0]; // Node
	},

	setContext: function(/*Object*/ globalObject, /*DocumentElement*/ globalDocument){
		// summary:
		//		changes the behavior of many core Dojo functions that deal with
		//		namespace and DOM lookup, changing them to work in a new global
		//		context (e.g., an iframe). The varibles dojo.global and dojo.doc
		//		are modified as a result of calling this function and the result of
		//		`dojo.body()` likewise differs.
		dojo.global = ret.global = globalObject;
		dojo.doc = ret.doc = globalDocument;
	},

	withGlobal: function(	/*Object*/ globalObject,
							/*Function*/ callback,
							/*Object?*/ thisObject,
							/*Array?*/ cbArguments){
		// summary:
		//		Invoke callback with globalObject as dojo.global and
		//		globalObject.document as dojo.doc.
		// description:
		//		Invoke callback with globalObject as dojo.global and
		//		globalObject.document as dojo.doc. If provided, globalObject
		//		will be executed in the context of object thisObject
		//		When callback() returns or throws an error, the dojo.global
		//		and dojo.doc will be restored to its previous state.

		var oldGlob = dojo.global;
		try{
			dojo.global = ret.global = globalObject;
			return ret.withDoc.call(null, globalObject.document, callback, thisObject, cbArguments);
		}finally{
			dojo.global = ret.global = oldGlob;
		}
	},

	withDoc: function(	/*DocumentElement*/ documentObject,
						/*Function*/ callback,
						/*Object?*/ thisObject,
						/*Array?*/ cbArguments){
		// summary:
		//		Invoke callback with documentObject as dojo/_base/window::doc.
		// description:
		//		Invoke callback with documentObject as dojo/_base/window::doc. If provided,
		//		callback will be executed in the context of object thisObject
		//		When callback() returns or throws an error, the dojo/_base/window::doc will
		//		be restored to its previous state.

		var oldDoc = ret.doc,
			oldQ = has("quirks"),
			oldIE = has("ie"), isIE, mode, pwin;

		try{
			dojo.doc = ret.doc = documentObject;
			// update dojo.isQuirks and the value of the has feature "quirks".
			// remove setting dojo.isQuirks and dojo.isIE for 2.0
			dojo.isQuirks = has.add("quirks", dojo.doc.compatMode == "BackCompat", true, true); // no need to check for QuirksMode which was Opera 7 only

			if(has("ie")){
				if((pwin = documentObject.parentWindow) && pwin.navigator){
					// re-run IE detection logic and update dojo.isIE / has("ie")
					// (the only time parentWindow/navigator wouldn't exist is if we were not
					// passed an actual legitimate document object)
					isIE = parseFloat(pwin.navigator.appVersion.split("MSIE ")[1]) || undefined;
					mode = documentObject.documentMode;
					if(mode && mode != 5 && Math.floor(isIE) != mode){
						isIE = mode;
					}
					dojo.isIE = has.add("ie", isIE, true, true);
				}
			}

			if(thisObject && typeof callback == "string"){
				callback = thisObject[callback];
			}

			return callback.apply(thisObject, cbArguments || []);
		}finally{
			dojo.doc = ret.doc = oldDoc;
			dojo.isQuirks = has.add("quirks", oldQ, true, true);
			dojo.isIE = has.add("ie", oldIE, true, true);
		}
	}
};

 1  && lang.mixin(dojo, ret);

return ret;

});

},
'dojo/promise/Promise':function(){
define([
	"../_base/lang"
], function(lang){
	"use strict";

	// module:
	//		dojo/promise/Promise

	function throwAbstract(){
		throw new TypeError("abstract");
	}

	return lang.extend(function Promise(){
		// summary:
		//		The public interface to a deferred.
		// description:
		//		The public interface to a deferred. All promises in Dojo are
		//		instances of this class.
	}, {
		then: function(callback, errback, progback){
			// summary:
			//		Add new callbacks to the promise.
			// description:
			//		Add new callbacks to the deferred. Callbacks can be added
			//		before or after the deferred is fulfilled.
			// callback: Function?
			//		Callback to be invoked when the promise is resolved.
			//		Receives the resolution value.
			// errback: Function?
			//		Callback to be invoked when the promise is rejected.
			//		Receives the rejection error.
			// progback: Function?
			//		Callback to be invoked when the promise emits a progress
			//		update. Receives the progress update.
			// returns: dojo/promise/Promise
			//		Returns a new promise for the result of the callback(s).
			//		This can be used for chaining many asynchronous operations.

			throwAbstract();
		},

		cancel: function(reason, strict){
			// summary:
			//		Inform the deferred it may cancel its asynchronous operation.
			// description:
			//		Inform the deferred it may cancel its asynchronous operation.
			//		The deferred's (optional) canceler is invoked and the
			//		deferred will be left in a rejected state. Can affect other
			//		promises that originate with the same deferred.
			// reason: any
			//		A message that may be sent to the deferred's canceler,
			//		explaining why it's being canceled.
			// strict: Boolean?
			//		If strict, will throw an error if the deferred has already
			//		been fulfilled and consequently cannot be canceled.
			// returns: any
			//		Returns the rejection reason if the deferred was canceled
			//		normally.

			throwAbstract();
		},

		isResolved: function(){
			// summary:
			//		Checks whether the promise has been resolved.
			// returns: Boolean

			throwAbstract();
		},

		isRejected: function(){
			// summary:
			//		Checks whether the promise has been rejected.
			// returns: Boolean

			throwAbstract();
		},

		isFulfilled: function(){
			// summary:
			//		Checks whether the promise has been resolved or rejected.
			// returns: Boolean

			throwAbstract();
		},

		isCanceled: function(){
			// summary:
			//		Checks whether the promise has been canceled.
			// returns: Boolean

			throwAbstract();
		},

		always: function(callbackOrErrback){
			// summary:
			//		Add a callback to be invoked when the promise is resolved
			//		or rejected.
			// callbackOrErrback: Function?
			//		A function that is used both as a callback and errback.
			// returns: dojo/promise/Promise
			//		Returns a new promise for the result of the callback/errback.

			return this.then(callbackOrErrback, callbackOrErrback);
		},

		otherwise: function(errback){
			// summary:
			//		Add new errbacks to the promise.
			// errback: Function?
			//		Callback to be invoked when the promise is rejected.
			// returns: dojo/promise/Promise
			//		Returns a new promise for the result of the errback.

			return this.then(null, errback);
		},

		trace: function(){
			return this;
		},

		traceRejected: function(){
			return this;
		},

		toString: function(){
			// returns: string
			//		Returns `[object Promise]`.

			return "[object Promise]";
		}
	});
});

},
'dojo/query':function(){
define(["./_base/kernel", "./has", "./dom", "./on", "./_base/array", "./_base/lang", "./selector/_loader", "./selector/_loader!default"],
	function(dojo, has, dom, on, array, lang, loader, defaultEngine){

	"use strict";

	has.add("array-extensible", function(){
		// test to see if we can extend an array (not supported in old IE)
		return lang.delegate([], {length: 1}).length == 1 && !has("bug-for-in-skips-shadowed");
	});
	
	var ap = Array.prototype, aps = ap.slice, apc = ap.concat, forEach = array.forEach;

	var tnl = function(/*Array*/ a, /*dojo/NodeList?*/ parent, /*Function?*/ NodeListCtor){
		// summary:
		//		decorate an array to make it look like a `dojo/NodeList`.
		// a:
		//		Array of nodes to decorate.
		// parent:
		//		An optional parent NodeList that generated the current
		//		list of nodes. Used to call _stash() so the parent NodeList
		//		can be accessed via end() later.
		// NodeListCtor:
		//		An optional constructor function to use for any
		//		new NodeList calls. This allows a certain chain of
		//		NodeList calls to use a different object than dojo/NodeList.
		var nodeList = new (NodeListCtor || this._NodeListCtor || nl)(a);
		return parent ? nodeList._stash(parent) : nodeList;
	};

	var loopBody = function(f, a, o){
		a = [0].concat(aps.call(a, 0));
		o = o || dojo.global;
		return function(node){
			a[0] = node;
			return f.apply(o, a);
		};
	};

	// adapters

	var adaptAsForEach = function(f, o){
		// summary:
		//		adapts a single node function to be used in the forEach-type
		//		actions. The initial object is returned from the specialized
		//		function.
		// f: Function
		//		a function to adapt
		// o: Object?
		//		an optional context for f
		return function(){
			this.forEach(loopBody(f, arguments, o));
			return this;	// Object
		};
	};

	var adaptAsMap = function(f, o){
		// summary:
		//		adapts a single node function to be used in the map-type
		//		actions. The return is a new array of values, as via `dojo.map`
		// f: Function
		//		a function to adapt
		// o: Object?
		//		an optional context for f
		return function(){
			return this.map(loopBody(f, arguments, o));
		};
	};

	var adaptAsFilter = function(f, o){
		// summary:
		//		adapts a single node function to be used in the filter-type actions
		// f: Function
		//		a function to adapt
		// o: Object?
		//		an optional context for f
		return function(){
			return this.filter(loopBody(f, arguments, o));
		};
	};

	var adaptWithCondition = function(f, g, o){
		// summary:
		//		adapts a single node function to be used in the map-type
		//		actions, behaves like forEach() or map() depending on arguments
		// f: Function
		//		a function to adapt
		// g: Function
		//		a condition function, if true runs as map(), otherwise runs as forEach()
		// o: Object?
		//		an optional context for f and g
		return function(){
			var a = arguments, body = loopBody(f, a, o);
			if(g.call(o || dojo.global, a)){
				return this.map(body);	// self
			}
			this.forEach(body);
			return this;	// self
		};
	};

	var NodeList = function(array){
		// summary:
		//		Array-like object which adds syntactic
		//		sugar for chaining, common iteration operations, animation, and
		//		node manipulation. NodeLists are most often returned as the
		//		result of dojo.query() calls.
		// description:
		//		NodeList instances provide many utilities that reflect
		//		core Dojo APIs for Array iteration and manipulation, DOM
		//		manipulation, and event handling. Instead of needing to dig up
		//		functions in the dojo.* namespace, NodeLists generally make the
		//		full power of Dojo available for DOM manipulation tasks in a
		//		simple, chainable way.
		// example:
		//		create a node list from a node
		//		|	new query.NodeList(dojo.byId("foo"));
		// example:
		//		get a NodeList from a CSS query and iterate on it
		//		|	var l = dojo.query(".thinger");
		//		|	l.forEach(function(node, index, nodeList){
		//		|		console.log(index, node.innerHTML);
		//		|	});
		// example:
		//		use native and Dojo-provided array methods to manipulate a
		//		NodeList without needing to use dojo.* functions explicitly:
		//		|	var l = dojo.query(".thinger");
		//		|	// since NodeLists are real arrays, they have a length
		//		|	// property that is both readable and writable and
		//		|	// push/pop/shift/unshift methods
		//		|	console.log(l.length);
		//		|	l.push(dojo.create("span"));
		//		|
		//		|	// dojo's normalized array methods work too:
		//		|	console.log( l.indexOf(dojo.byId("foo")) );
		//		|	// ...including the special "function as string" shorthand
		//		|	console.log( l.every("item.nodeType == 1") );
		//		|
		//		|	// NodeLists can be [..] indexed, or you can use the at()
		//		|	// function to get specific items wrapped in a new NodeList:
		//		|	var node = l[3]; // the 4th element
		//		|	var newList = l.at(1, 3); // the 2nd and 4th elements
		// example:
		//		the style functions you expect are all there too:
		//		|	// style() as a getter...
		//		|	var borders = dojo.query(".thinger").style("border");
		//		|	// ...and as a setter:
		//		|	dojo.query(".thinger").style("border", "1px solid black");
		//		|	// class manipulation
		//		|	dojo.query("li:nth-child(even)").addClass("even");
		//		|	// even getting the coordinates of all the items
		//		|	var coords = dojo.query(".thinger").coords();
		// example:
		//		DOM manipulation functions from the dojo.* namespace area also available:
		//		|	// remove all of the elements in the list from their
		//		|	// parents (akin to "deleting" them from the document)
		//		|	dojo.query(".thinger").orphan();
		//		|	// place all elements in the list at the front of #foo
		//		|	dojo.query(".thinger").place("foo", "first");
		// example:
		//		Event handling couldn't be easier. `dojo.connect` is mapped in,
		//		and shortcut handlers are provided for most DOM events:
		//		|	// like dojo.connect(), but with implicit scope
		//		|	dojo.query("li").connect("onclick", console, "log");
		//		|
		//		|	// many common event handlers are already available directly:
		//		|	dojo.query("li").onclick(console, "log");
		//		|	var toggleHovered = dojo.hitch(dojo, "toggleClass", "hovered");
		//		|	dojo.query("p")
		//		|		.onmouseenter(toggleHovered)
		//		|		.onmouseleave(toggleHovered);
		// example:
		//		chainability is a key advantage of NodeLists:
		//		|	dojo.query(".thinger")
		//		|		.onclick(function(e){ /* ... */ })
		//		|		.at(1, 3, 8) // get a subset
		//		|			.style("padding", "5px")
		//		|			.forEach(console.log);
		var isNew = this instanceof nl && has("array-extensible");
		if(typeof array == "number"){
			array = Array(array);
		}
		var nodeArray = (array && "length" in array) ? array : arguments;
		if(isNew || !nodeArray.sort){
			// make sure it's a real array before we pass it on to be wrapped 
			var target = isNew ? this : [],
				l = target.length = nodeArray.length;
			for(var i = 0; i < l; i++){
				target[i] = nodeArray[i];
			}
			if(isNew){
				// called with new operator, this means we are going to use this instance and push
				// the nodes on to it. This is usually much faster since the NodeList properties
				//	don't need to be copied (unless the list of nodes is extremely large).
				return target;
			}
			nodeArray = target;
		}
		// called without new operator, use a real array and copy prototype properties,
		// this is slower and exists for back-compat. Should be removed in 2.0.
		lang._mixin(nodeArray, nlp);
		nodeArray._NodeListCtor = function(array){
			// call without new operator to preserve back-compat behavior
			return nl(array);
		};
		return nodeArray;
	};
	
	var nl = NodeList, nlp = nl.prototype = 
		has("array-extensible") ? [] : {};// extend an array if it is extensible

	// expose adapters and the wrapper as private functions

	nl._wrap = nlp._wrap = tnl;
	nl._adaptAsMap = adaptAsMap;
	nl._adaptAsForEach = adaptAsForEach;
	nl._adaptAsFilter  = adaptAsFilter;
	nl._adaptWithCondition = adaptWithCondition;

	// mass assignment

	// add array redirectors
	forEach(["slice", "splice"], function(name){
		var f = ap[name];
		//Use a copy of the this array via this.slice() to allow .end() to work right in the splice case.
		// CANNOT apply ._stash()/end() to splice since it currently modifies
		// the existing this array -- it would break backward compatibility if we copy the array before
		// the splice so that we can use .end(). So only doing the stash option to this._wrap for slice.
		nlp[name] = function(){ return this._wrap(f.apply(this, arguments), name == "slice" ? this : null); };
	});
	// concat should be here but some browsers with native NodeList have problems with it

	// add array.js redirectors
	forEach(["indexOf", "lastIndexOf", "every", "some"], function(name){
		var f = array[name];
		nlp[name] = function(){ return f.apply(dojo, [this].concat(aps.call(arguments, 0))); };
	});

	lang.extend(NodeList, {
		// copy the constructors
		constructor: nl,
		_NodeListCtor: nl,
		toString: function(){
			// Array.prototype.toString can't be applied to objects, so we use join
			return this.join(",");
		},
		_stash: function(parent){
			// summary:
			//		private function to hold to a parent NodeList. end() to return the parent NodeList.
			//
			// example:
			//		How to make a `dojo/NodeList` method that only returns the third node in
			//		the dojo/NodeList but allows access to the original NodeList by using this._stash:
			//	|	dojo.extend(NodeList, {
			//	|		third: function(){
			//	|			var newNodeList = NodeList(this[2]);
			//	|			return newNodeList._stash(this);
			//	|		}
			//	|	});
			//	|	// then see how _stash applies a sub-list, to be .end()'ed out of
			//	|	dojo.query(".foo")
			//	|		.third()
			//	|			.addClass("thirdFoo")
			//	|		.end()
			//	|		// access to the orig .foo list
			//	|		.removeClass("foo")
			//	|
			//
			this._parent = parent;
			return this; // dojo/NodeList
		},

		on: function(eventName, listener){
			// summary:
			//		Listen for events on the nodes in the NodeList. Basic usage is:
			//		| query(".my-class").on("click", listener);
			//		This supports event delegation by using selectors as the first argument with the event names as
			//		pseudo selectors. For example:
			//		| dojo.query("#my-list").on("li:click", listener);
			//		This will listen for click events within `<li>` elements that are inside the `#my-list` element.
			//		Because on supports CSS selector syntax, we can use comma-delimited events as well:
			//		| dojo.query("#my-list").on("li button:mouseover, li:click", listener);
			var handles = this.map(function(node){
				return on(node, eventName, listener); // TODO: apply to the NodeList so the same selector engine is used for matches
			});
			handles.remove = function(){
				for(var i = 0; i < handles.length; i++){
					handles[i].remove();
				}
			};
			return handles;
		},

		end: function(){
			// summary:
			//		Ends use of the current `NodeList` by returning the previous NodeList
			//		that generated the current NodeList.
			// description:
			//		Returns the `NodeList` that generated the current `NodeList`. If there
			//		is no parent NodeList, an empty NodeList is returned.
			// example:
			//	|	dojo.query("a")
			//	|		.filter(".disabled")
			//	|			// operate on the anchors that only have a disabled class
			//	|			.style("color", "grey")
			//	|		.end()
			//	|		// jump back to the list of anchors
			//	|		.style(...)
			//
			if(this._parent){
				return this._parent;
			}else{
				//Just return empty list.
				return new this._NodeListCtor(0);
			}
		},

		// http://developer.mozilla.org/en/docs/Core_JavaScript_1.5_Reference:Global_Objects:Array#Methods

		// FIXME: handle return values for #3244
		//		http://trac.dojotoolkit.org/ticket/3244

		// FIXME:
		//		need to wrap or implement:
		//			join (perhaps w/ innerHTML/outerHTML overload for toString() of items?)
		//			reduce
		//			reduceRight

		/*=====
		slice: function(begin, end){
			// summary:
			//		Returns a new NodeList, maintaining this one in place
			// description:
			//		This method behaves exactly like the Array.slice method
			//		with the caveat that it returns a dojo/NodeList and not a
			//		raw Array. For more details, see Mozilla's [slice
			//		documentation](https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Array/slice)
			// begin: Integer
			//		Can be a positive or negative integer, with positive
			//		integers noting the offset to begin at, and negative
			//		integers denoting an offset from the end (i.e., to the left
			//		of the end)
			// end: Integer?
			//		Optional parameter to describe what position relative to
			//		the NodeList's zero index to end the slice at. Like begin,
			//		can be positive or negative.
			return this._wrap(a.slice.apply(this, arguments));
		},

		splice: function(index, howmany, item){
			// summary:
			//		Returns a new NodeList, manipulating this NodeList based on
			//		the arguments passed, potentially splicing in new elements
			//		at an offset, optionally deleting elements
			// description:
			//		This method behaves exactly like the Array.splice method
			//		with the caveat that it returns a dojo/NodeList and not a
			//		raw Array. For more details, see Mozilla's [splice
			//		documentation](https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Array/splice)
			//		For backwards compatibility, calling .end() on the spliced NodeList
			//		does not return the original NodeList -- splice alters the NodeList in place.
			// index: Integer
			//		begin can be a positive or negative integer, with positive
			//		integers noting the offset to begin at, and negative
			//		integers denoting an offset from the end (i.e., to the left
			//		of the end)
			// howmany: Integer?
			//		Optional parameter to describe what position relative to
			//		the NodeList's zero index to end the slice at. Like begin,
			//		can be positive or negative.
			// item: Object...?
			//		Any number of optional parameters may be passed in to be
			//		spliced into the NodeList
			return this._wrap(a.splice.apply(this, arguments));	// dojo/NodeList
		},

		indexOf: function(value, fromIndex){
			// summary:
			//		see dojo.indexOf(). The primary difference is that the acted-on
			//		array is implicitly this NodeList
			// value: Object
			//		The value to search for.
			// fromIndex: Integer?
			//		The location to start searching from. Optional. Defaults to 0.
			// description:
			//		For more details on the behavior of indexOf, see Mozilla's
			//		[indexOf
			//		docs](https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Array/indexOf)
			// returns:
			//		Positive Integer or 0 for a match, -1 of not found.
			return d.indexOf(this, value, fromIndex); // Integer
		},

		lastIndexOf: function(value, fromIndex){
			// summary:
			//		see dojo.lastIndexOf(). The primary difference is that the
			//		acted-on array is implicitly this NodeList
			// description:
			//		For more details on the behavior of lastIndexOf, see
			//		Mozilla's [lastIndexOf
			//		docs](https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Array/lastIndexOf)
			// value: Object
			//		The value to search for.
			// fromIndex: Integer?
			//		The location to start searching from. Optional. Defaults to 0.
			// returns:
			//		Positive Integer or 0 for a match, -1 of not found.
			return d.lastIndexOf(this, value, fromIndex); // Integer
		},

		every: function(callback, thisObject){
			// summary:
			//		see `dojo.every()` and the [Array.every
			//		docs](https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Array/every).
			//		Takes the same structure of arguments and returns as
			//		dojo.every() with the caveat that the passed array is
			//		implicitly this NodeList
			// callback: Function
			//		the callback
			// thisObject: Object?
			//		the context
			return d.every(this, callback, thisObject); // Boolean
		},

		some: function(callback, thisObject){
			// summary:
			//		Takes the same structure of arguments and returns as
			//		`dojo.some()` with the caveat that the passed array is
			//		implicitly this NodeList.  See `dojo.some()` and Mozilla's
			//		[Array.some
			//		documentation](https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Array/some).
			// callback: Function
			//		the callback
			// thisObject: Object?
			//		the context
			return d.some(this, callback, thisObject); // Boolean
		},
		=====*/

		concat: function(item){
			// summary:
			//		Returns a new NodeList comprised of items in this NodeList
			//		as well as items passed in as parameters
			// description:
			//		This method behaves exactly like the Array.concat method
			//		with the caveat that it returns a `NodeList` and not a
			//		raw Array. For more details, see the [Array.concat
			//		docs](https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Array/concat)
			// item: Object?
			//		Any number of optional parameters may be passed in to be
			//		spliced into the NodeList

			//return this._wrap(apc.apply(this, arguments));
			// the line above won't work for the native NodeList, or for Dojo NodeLists either :-(

			// implementation notes:
			// Array.concat() doesn't recognize native NodeLists or Dojo NodeLists
			// as arrays, and so does not inline them into a unioned array, but
			// appends them as single entities. Both the original NodeList and the
			// items passed in as parameters must be converted to raw Arrays
			// and then the concatenation result may be re-_wrap()ed as a Dojo NodeList.

			var t = aps.call(this, 0),
				m = array.map(arguments, function(a){
					return aps.call(a, 0);
				});
			return this._wrap(apc.apply(t, m), this);	// dojo/NodeList
		},

		map: function(/*Function*/ func, /*Function?*/ obj){
			// summary:
			//		see dojo.map(). The primary difference is that the acted-on
			//		array is implicitly this NodeList and the return is a
			//		NodeList (a subclass of Array)
			return this._wrap(array.map(this, func, obj), this); // dojo/NodeList
		},

		forEach: function(callback, thisObj){
			// summary:
			//		see `dojo.forEach()`. The primary difference is that the acted-on
			//		array is implicitly this NodeList. If you want the option to break out
			//		of the forEach loop, use every() or some() instead.
			forEach(this, callback, thisObj);
			// non-standard return to allow easier chaining
			return this; // dojo/NodeList
		},
		filter: function(/*String|Function*/ filter){
			// summary:
			//		"masks" the built-in javascript filter() method (supported
			//		in Dojo via `dojo.filter`) to support passing a simple
			//		string filter in addition to supporting filtering function
			//		objects.
			// filter:
			//		If a string, a CSS rule like ".thinger" or "div > span".
			// example:
			//		"regular" JS filter syntax as exposed in dojo.filter:
			//		|	dojo.query("*").filter(function(item){
			//		|		// highlight every paragraph
			//		|		return (item.nodeName == "p");
			//		|	}).style("backgroundColor", "yellow");
			// example:
			//		the same filtering using a CSS selector
			//		|	dojo.query("*").filter("p").styles("backgroundColor", "yellow");

			var a = arguments, items = this, start = 0;
			if(typeof filter == "string"){ // inline'd type check
				items = query._filterResult(this, a[0]);
				if(a.length == 1){
					// if we only got a string query, pass back the filtered results
					return items._stash(this); // dojo/NodeList
				}
				// if we got a callback, run it over the filtered items
				start = 1;
			}
			return this._wrap(array.filter(items, a[start], a[start + 1]), this);	// dojo/NodeList
		},
		instantiate: function(/*String|Object*/ declaredClass, /*Object?*/ properties){
			// summary:
			//		Create a new instance of a specified class, using the
			//		specified properties and each node in the NodeList as a
			//		srcNodeRef.
			// example:
			//		Grabs all buttons in the page and converts them to dijit/form/Button's.
			//	|	var buttons = query("button").instantiate(Button, {showLabel: true});
			var c = lang.isFunction(declaredClass) ? declaredClass : lang.getObject(declaredClass);
			properties = properties || {};
			return this.forEach(function(node){
				new c(properties, node);
			});	// dojo/NodeList
		},
		at: function(/*===== index =====*/){
			// summary:
			//		Returns a new NodeList comprised of items in this NodeList
			//		at the given index or indices.
			//
			// index: Integer...
			//		One or more 0-based indices of items in the current
			//		NodeList. A negative index will start at the end of the
			//		list and go backwards.
			//
			// example:
			//	Shorten the list to the first, second, and third elements
			//	|	query("a").at(0, 1, 2).forEach(fn);
			//
			// example:
			//	Retrieve the first and last elements of a unordered list:
			//	|	query("ul > li").at(0, -1).forEach(cb);
			//
			// example:
			//	Do something for the first element only, but end() out back to
			//	the original list and continue chaining:
			//	|	query("a").at(0).onclick(fn).end().forEach(function(n){
			//	|		console.log(n); // all anchors on the page.
			//	|	})

			var t = new this._NodeListCtor(0);
			forEach(arguments, function(i){
				if(i < 0){ i = this.length + i; }
				if(this[i]){ t.push(this[i]); }
			}, this);
			return t._stash(this); // dojo/NodeList
		}
	});

	function queryForEngine(engine, NodeList){
		var query = function(/*String*/ query, /*String|DOMNode?*/ root){
			// summary:
			//		Returns nodes which match the given CSS selector, searching the
			//		entire document by default but optionally taking a node to scope
			//		the search by. Returns an instance of NodeList.
			if(typeof root == "string"){
				root = dom.byId(root);
				if(!root){
					return new NodeList([]);
				}
			}
			var results = typeof query == "string" ? engine(query, root) : query ? (query.end && query.on) ? query : [query] : [];
			if(results.end && results.on){
				// already wrapped
				return results;
			}
			return new NodeList(results);
		};
		query.matches = engine.match || function(node, selector, root){
			// summary:
			//		Test to see if a node matches a selector
			return query.filter([node], selector, root).length > 0;
		};
		// the engine provides a filtering function, use it to for matching
		query.filter = engine.filter || function(nodes, selector, root){
			// summary:
			//		Filters an array of nodes. Note that this does not guarantee to return a NodeList, just an array.
			return query(selector, root).filter(function(node){
				return array.indexOf(nodes, node) > -1;
			});
		};
		if(typeof engine != "function"){
			var search = engine.search;
			engine = function(selector, root){
				// Slick does it backwards (or everyone else does it backwards, probably the latter)
				return search(root || document, selector);
			};
		}
		return query;
	}
	var query = queryForEngine(defaultEngine, NodeList);
	/*=====
	query = function(selector, context){
		// summary:
		//		This modules provides DOM querying functionality. The module export is a function
		//		that can be used to query for DOM nodes by CSS selector and returns a NodeList
		//		representing the matching nodes.
		// selector: String
		//		A CSS selector to search for.
		// context: String|DomNode?
		//		An optional context to limit the searching scope. Only nodes under `context` will be
		//		scanned.
		// example:
		//		add an onclick handler to every submit button in the document
		//		which causes the form to be sent via Ajax instead:
		//	|	require(["dojo/query"], function(query){
		//	|		query("input[type='submit']").on("click", function(e){
		//	|			dojo.stopEvent(e); // prevent sending the form
		//	|			var btn = e.target;
		//	|			dojo.xhrPost({
		//	|				form: btn.form,
		//	|				load: function(data){
		//	|					// replace the form with the response
		//	|					var div = dojo.doc.createElement("div");
		//	|					dojo.place(div, btn.form, "after");
		//	|					div.innerHTML = data;
		//	|					dojo.style(btn.form, "display", "none");
		//	|				}
		//	|			});
		//	|		});
		// |	});
		//
		// description:
		//		dojo/query is responsible for loading the appropriate query engine and wrapping
		//		its results with a `NodeList`. You can use dojo/query with a specific selector engine
		//		by using it as a plugin. For example, if you installed the sizzle package, you could
		//		use it as the selector engine with:
		//		|	require(["dojo/query!sizzle"], function(query){
		//		|		query("div")...
		//
		//		The id after the ! can be a module id of the selector engine or one of the following values:
		//
		//		- acme: This is the default engine used by Dojo base, and will ensure that the full
		//		Acme engine is always loaded.
		//
		//		- css2: If the browser has a native selector engine, this will be used, otherwise a
		//		very minimal lightweight selector engine will be loaded that can do simple CSS2 selectors
		//		(by #id, .class, tag, and [name=value] attributes, with standard child or descendant (>)
		//		operators) and nothing more.
		//
		//		- css2.1: If the browser has a native selector engine, this will be used, otherwise the
		//		full Acme engine will be loaded.
		//
		//		- css3: If the browser has a native selector engine with support for CSS3 pseudo
		//		selectors (most modern browsers except IE8), this will be used, otherwise the
		//		full Acme engine will be loaded.
		//
		//		- Or the module id of a selector engine can be used to explicitly choose the selector engine
		//
		//		For example, if you are using CSS3 pseudo selectors in module, you can specify that
		//		you will need support them with:
		//		|	require(["dojo/query!css3"], function(query){
		//		|		query('#t > h3:nth-child(odd)')...
		//
		//		You can also choose the selector engine/load configuration by setting the query-selector:
		//		For example:
		//		|	<script data-dojo-config="query-selector:'css3'" src="dojo.js"></script>
		//
		return new NodeList(); // dojo/NodeList
	 };
	 =====*/

	// the query that is returned from this module is slightly different than dojo.query,
	// because dojo.query has to maintain backwards compatibility with returning a
	// true array which has performance problems. The query returned from the module
	// does not use true arrays, but rather inherits from Array, making it much faster to
	// instantiate.
	dojo.query = queryForEngine(defaultEngine, function(array){
		// call it without the new operator to invoke the back-compat behavior that returns a true array
		return NodeList(array);	// dojo/NodeList
	});

	query.load = function(id, parentRequire, loaded){
		// summary:
		//		can be used as AMD plugin to conditionally load new query engine
		// example:
		//	|	require(["dojo/query!custom"], function(qsa){
		//	|		// loaded selector/custom.js as engine
		//	|		qsa("#foobar").forEach(...);
		//	|	});
		loader.load(id, parentRequire, function(engine){
			loaded(queryForEngine(engine, NodeList));
		});
	};

	dojo._filterQueryResult = query._filterResult = function(nodes, selector, root){
		return new NodeList(query.filter(nodes, selector, root));
	};
	dojo.NodeList = query.NodeList = NodeList;
	return query;
});

},
'dojo/selector/_loader':function(){
define(["../has", "require"],
		function(has, require){

"use strict";
var testDiv = document.createElement("div");
has.add("dom-qsa2.1", !!testDiv.querySelectorAll);
has.add("dom-qsa3", function(){
			// test to see if we have a reasonable native selector engine available
			try{
				testDiv.innerHTML = "<p class='TEST'></p>"; // test kind of from sizzle
				// Safari can't handle uppercase or unicode characters when
				// in quirks mode, IE8 can't handle pseudos like :empty
				return testDiv.querySelectorAll(".TEST:empty").length == 1;
			}catch(e){}
		});
var fullEngine;
var acme = "./acme", lite = "./lite";
return {
	// summary:
	//		This module handles loading the appropriate selector engine for the given browser

	load: function(id, parentRequire, loaded, config){
		var req = require;
		// here we implement the default logic for choosing a selector engine
		id = id == "default" ? has("config-selectorEngine") || "css3" : id;
		id = id == "css2" || id == "lite" ? lite :
				id == "css2.1" ? has("dom-qsa2.1") ? lite : acme :
				id == "css3" ? has("dom-qsa3") ? lite : acme :
				id == "acme" ? acme : (req = parentRequire) && id;
		if(id.charAt(id.length-1) == '?'){
			id = id.substring(0,id.length - 1);
			var optionalLoad = true;
		}
		// the query engine is optional, only load it if a native one is not available or existing one has not been loaded
		if(optionalLoad && (has("dom-compliant-qsa") || fullEngine)){
			return loaded(fullEngine);
		}
		// load the referenced selector engine
		req([id], function(engine){
			if(id != "./lite"){
				fullEngine = engine;
			}
			loaded(engine);
		});
	}
};
});

},
'xstyle/has-class':function(){
define(["dojo/has"], function(has){
	var tested = {};
	return function(){
		var test, args = arguments;
		for(var i = 0; i < args.length; i++){
			var test = args[i];
			if(!tested[test]){
				tested[test] = true;
				var parts = test.match(/^(no-)?(.+?)((-[\d\.]+)(-[\d\.]+)?)?$/), // parse the class name
					hasResult = has(parts[2]), // the actual has test
					lower = -parts[4]; // lower bound if it is in the form of test-4 or test-4-6 (would be 4)
				if((lower > 0 ? lower <= hasResult && (-parts[5] || lower) >= hasResult :  // if it has a range boundary, compare to see if we are in it
						!!hasResult) == !parts[1]){ // parts[1] is the no- prefix that can negate the result
					document.documentElement.className += ' has-' + test;
				}
			}
		}
	}
});
},
'dojo/errors/create':function(){
define(["../_base/lang"], function(lang){
	return function(name, ctor, base, props){
		base = base || Error;

		var ErrorCtor = function(message){
			if(base === Error){
				if(Error.captureStackTrace){
					Error.captureStackTrace(this, ErrorCtor);
				}

				// Error.call() operates on the returned error
				// object rather than operating on |this|
				var err = Error.call(this, message),
					prop;

				// Copy own properties from err to |this|
				for(prop in err){
					if(err.hasOwnProperty(prop)){
						this[prop] = err[prop];
					}
				}

				// messsage is non-enumerable in ES5
				this.message = message;
				// stack is non-enumerable in at least Firefox
				this.stack = err.stack;
			}else{
				base.apply(this, arguments);
			}
			if(ctor){
				ctor.apply(this, arguments);
			}
		};

		ErrorCtor.prototype = lang.delegate(base.prototype, props);
		ErrorCtor.prototype.name = name;
		ErrorCtor.prototype.constructor = ErrorCtor;

		return ErrorCtor;
	};
});

},
'dojo/_base/array':function(){
define(["./kernel", "../has", "./lang"], function(dojo, has, lang){
	// module:
	//		dojo/_base/array

	// our old simple function builder stuff
	var cache = {}, u;

	function buildFn(fn){
		return cache[fn] = new Function("item", "index", "array", fn); // Function
	}
	// magic snippet: if(typeof fn == "string") fn = cache[fn] || buildFn(fn);

	// every & some

	function everyOrSome(some){
		var every = !some;
		return function(a, fn, o){
			var i = 0, l = a && a.length || 0, result;
			if(l && typeof a == "string") a = a.split("");
			if(typeof fn == "string") fn = cache[fn] || buildFn(fn);
			if(o){
				for(; i < l; ++i){
					result = !fn.call(o, a[i], i, a);
					if(some ^ result){
						return !result;
					}
				}
			}else{
				for(; i < l; ++i){
					result = !fn(a[i], i, a);
					if(some ^ result){
						return !result;
					}
				}
			}
			return every; // Boolean
		};
	}

	// indexOf, lastIndexOf

	function index(up){
		var delta = 1, lOver = 0, uOver = 0;
		if(!up){
			delta = lOver = uOver = -1;
		}
		return function(a, x, from, last){
			if(last && delta > 0){
				// TODO: why do we use a non-standard signature? why do we need "last"?
				return array.lastIndexOf(a, x, from);
			}
			var l = a && a.length || 0, end = up ? l + uOver : lOver, i;
			if(from === u){
				i = up ? lOver : l + uOver;
			}else{
				if(from < 0){
					i = l + from;
					if(i < 0){
						i = lOver;
					}
				}else{
					i = from >= l ? l + uOver : from;
				}
			}
			if(l && typeof a == "string") a = a.split("");
			for(; i != end; i += delta){
				if(a[i] == x){
					return i; // Number
				}
			}
			return -1; // Number
		};
	}

	var array = {
		// summary:
		//		The Javascript v1.6 array extensions.

		every: everyOrSome(false),
		/*=====
		 every: function(arr, callback, thisObject){
			 // summary:
			 //		Determines whether or not every item in arr satisfies the
			 //		condition implemented by callback.
			 // arr: Array|String
			 //		the array to iterate on. If a string, operates on individual characters.
			 // callback: Function|String
			 //		a function is invoked with three arguments: item, index,
			 //		and array and returns true if the condition is met.
			 // thisObject: Object?
			 //		may be used to scope the call to callback
			 // returns: Boolean
			 // description:
			 //		This function corresponds to the JavaScript 1.6 Array.every() method, with one difference: when
			 //		run over sparse arrays, this implementation passes the "holes" in the sparse array to
			 //		the callback function with a value of undefined. JavaScript 1.6's every skips the holes in the sparse array.
			 //		For more details, see:
			 //		https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Objects/Array/every
			 // example:
			 //	|	// returns false
			 //	|	array.every([1, 2, 3, 4], function(item){ return item>1; });
			 // example:
			 //	|	// returns true
			 //	|	array.every([1, 2, 3, 4], function(item){ return item>0; });
		 },
		 =====*/

		some: everyOrSome(true),
		/*=====
		some: function(arr, callback, thisObject){
			// summary:
			//		Determines whether or not any item in arr satisfies the
			//		condition implemented by callback.
			// arr: Array|String
			//		the array to iterate over. If a string, operates on individual characters.
			// callback: Function|String
			//		a function is invoked with three arguments: item, index,
			//		and array and returns true if the condition is met.
			// thisObject: Object?
			//		may be used to scope the call to callback
			// returns: Boolean
			// description:
			//		This function corresponds to the JavaScript 1.6 Array.some() method, with one difference: when
			//		run over sparse arrays, this implementation passes the "holes" in the sparse array to
			//		the callback function with a value of undefined. JavaScript 1.6's some skips the holes in the sparse array.
			//		For more details, see:
			//		https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Objects/Array/some
			// example:
			//	| // is true
			//	| array.some([1, 2, 3, 4], function(item){ return item>1; });
			// example:
			//	| // is false
			//	| array.some([1, 2, 3, 4], function(item){ return item<1; });
		},
		=====*/

		indexOf: index(true),
		/*=====
		indexOf: function(arr, value, fromIndex, findLast){
			// summary:
			//		locates the first index of the provided value in the
			//		passed array. If the value is not found, -1 is returned.
			// description:
			//		This method corresponds to the JavaScript 1.6 Array.indexOf method, with two differences:
			//
			//		1. when run over sparse arrays, the Dojo function invokes the callback for every index
			//		   whereas JavaScript 1.6's indexOf skips the holes in the sparse array.
			//		2. uses equality (==) rather than strict equality (===)
			//
			//		For details on this method, see:
			//		https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Objects/Array/indexOf
			// arr: Array
			// value: Object
			// fromIndex: Integer?
			// findLast: Boolean?
			//		Makes indexOf() work like lastIndexOf().  Used internally; not meant for external usage.
			// returns: Number
		},
		=====*/

		lastIndexOf: index(false),
		/*=====
		lastIndexOf: function(arr, value, fromIndex){
			// summary:
			//		locates the last index of the provided value in the passed
			//		array. If the value is not found, -1 is returned.
			// description:
		 	//		This method corresponds to the JavaScript 1.6 Array.lastIndexOf method, with two differences:
		 	//
		 	//		1. when run over sparse arrays, the Dojo function invokes the callback for every index
		 	//		   whereas JavaScript 1.6's lasIndexOf skips the holes in the sparse array.
		 	//		2. uses equality (==) rather than strict equality (===)
		 	//
		 	//		For details on this method, see:
		 	//		https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Objects/Array/lastIndexOf
			// arr: Array,
			// value: Object,
			// fromIndex: Integer?
			// returns: Number
		},
		=====*/

		forEach: function(arr, callback, thisObject){
			// summary:
			//		for every item in arr, callback is invoked. Return values are ignored.
			//		If you want to break out of the loop, consider using array.every() or array.some().
			//		forEach does not allow breaking out of the loop over the items in arr.
			// arr:
			//		the array to iterate over. If a string, operates on individual characters.
			// callback:
			//		a function is invoked with three arguments: item, index, and array
			// thisObject:
			//		may be used to scope the call to callback
			// description:
			//		This function corresponds to the JavaScript 1.6 Array.forEach() method, with one difference: when
			//		run over sparse arrays, this implementation passes the "holes" in the sparse array to
			//		the callback function with a value of undefined. JavaScript 1.6's forEach skips the holes in the sparse array.
			//		For more details, see:
			//		https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Objects/Array/forEach
			// example:
			//	| // log out all members of the array:
			//	| array.forEach(
			//	|		[ "thinger", "blah", "howdy", 10 ],
			//	|		function(item){
			//	|			console.log(item);
			//	|		}
			//	| );
			// example:
			//	| // log out the members and their indexes
			//	| array.forEach(
			//	|		[ "thinger", "blah", "howdy", 10 ],
			//	|		function(item, idx, arr){
			//	|			console.log(item, "at index:", idx);
			//	|		}
			//	| );
			// example:
			//	| // use a scoped object member as the callback
			//	|
			//	| var obj = {
			//	|		prefix: "logged via obj.callback:",
			//	|		callback: function(item){
			//	|			console.log(this.prefix, item);
			//	|		}
			//	| };
			//	|
			//	| // specifying the scope function executes the callback in that scope
			//	| array.forEach(
			//	|		[ "thinger", "blah", "howdy", 10 ],
			//	|		obj.callback,
			//	|		obj
			//	| );
			//	|
			//	| // alternately, we can accomplish the same thing with lang.hitch()
			//	| array.forEach(
			//	|		[ "thinger", "blah", "howdy", 10 ],
			//	|		lang.hitch(obj, "callback")
			//	| );
			// arr: Array|String
			// callback: Function|String
			// thisObject: Object?

			var i = 0, l = arr && arr.length || 0;
			if(l && typeof arr == "string") arr = arr.split("");
			if(typeof callback == "string") callback = cache[callback] || buildFn(callback);
			if(thisObject){
				for(; i < l; ++i){
					callback.call(thisObject, arr[i], i, arr);
				}
			}else{
				for(; i < l; ++i){
					callback(arr[i], i, arr);
				}
			}
		},

		map: function(arr, callback, thisObject, Ctr){
			// summary:
			//		applies callback to each element of arr and returns
			//		an Array with the results
			// arr: Array|String
			//		the array to iterate on. If a string, operates on
			//		individual characters.
			// callback: Function|String
			//		a function is invoked with three arguments, (item, index,
			//		array),	 and returns a value
			// thisObject: Object?
			//		may be used to scope the call to callback
			// returns: Array
			// description:
			//		This function corresponds to the JavaScript 1.6 Array.map() method, with one difference: when
			//		run over sparse arrays, this implementation passes the "holes" in the sparse array to
			//		the callback function with a value of undefined. JavaScript 1.6's map skips the holes in the sparse array.
			//		For more details, see:
			//		https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Objects/Array/map
			// example:
			//	| // returns [2, 3, 4, 5]
			//	| array.map([1, 2, 3, 4], function(item){ return item+1 });

			// TODO: why do we have a non-standard signature here? do we need "Ctr"?
			var i = 0, l = arr && arr.length || 0, out = new (Ctr || Array)(l);
			if(l && typeof arr == "string") arr = arr.split("");
			if(typeof callback == "string") callback = cache[callback] || buildFn(callback);
			if(thisObject){
				for(; i < l; ++i){
					out[i] = callback.call(thisObject, arr[i], i, arr);
				}
			}else{
				for(; i < l; ++i){
					out[i] = callback(arr[i], i, arr);
				}
			}
			return out; // Array
		},

		filter: function(arr, callback, thisObject){
			// summary:
			//		Returns a new Array with those items from arr that match the
			//		condition implemented by callback.
			// arr: Array
			//		the array to iterate over.
			// callback: Function|String
			//		a function that is invoked with three arguments (item,
			//		index, array). The return of this function is expected to
			//		be a boolean which determines whether the passed-in item
			//		will be included in the returned array.
			// thisObject: Object?
			//		may be used to scope the call to callback
			// returns: Array
			// description:
			//		This function corresponds to the JavaScript 1.6 Array.filter() method, with one difference: when
			//		run over sparse arrays, this implementation passes the "holes" in the sparse array to
			//		the callback function with a value of undefined. JavaScript 1.6's filter skips the holes in the sparse array.
			//		For more details, see:
			//		https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Objects/Array/filter
			// example:
			//	| // returns [2, 3, 4]
			//	| array.filter([1, 2, 3, 4], function(item){ return item>1; });

			// TODO: do we need "Ctr" here like in map()?
			var i = 0, l = arr && arr.length || 0, out = [], value;
			if(l && typeof arr == "string") arr = arr.split("");
			if(typeof callback == "string") callback = cache[callback] || buildFn(callback);
			if(thisObject){
				for(; i < l; ++i){
					value = arr[i];
					if(callback.call(thisObject, value, i, arr)){
						out.push(value);
					}
				}
			}else{
				for(; i < l; ++i){
					value = arr[i];
					if(callback(value, i, arr)){
						out.push(value);
					}
				}
			}
			return out; // Array
		},

		clearCache: function(){
			cache = {};
		}
	};


	 1  && lang.mixin(dojo, array);

	return array;
});

},
'dgrid/OnDemandGrid':function(){
define(["dojo/_base/declare", "./Grid", "./OnDemandList"], function(declare, Grid, OnDemandList){
	return declare([Grid, OnDemandList], {});
});
},
'dojo/selector/acme':function(){
define([
	"../dom", "../sniff", "../_base/array", "../_base/lang", "../_base/window"
], function(dom, has, array, lang, win){

	// module:
	//		dojo/selector/acme

/*
	acme architectural overview:

		acme is a relatively full-featured CSS3 query library. It is
		designed to take any valid CSS3 selector and return the nodes matching
		the selector. To do this quickly, it processes queries in several
		steps, applying caching where profitable.

		The steps (roughly in reverse order of the way they appear in the code):
			1.) check to see if we already have a "query dispatcher"
				- if so, use that with the given parameterization. Skip to step 4.
			2.) attempt to determine which branch to dispatch the query to:
				- JS (optimized DOM iteration)
				- native (FF3.1+, Safari 3.1+, IE 8+)
			3.) tokenize and convert to executable "query dispatcher"
				- this is where the lion's share of the complexity in the
					system lies. In the DOM version, the query dispatcher is
					assembled as a chain of "yes/no" test functions pertaining to
					a section of a simple query statement (".blah:nth-child(odd)"
					but not "div div", which is 2 simple statements). Individual
					statement dispatchers are cached (to prevent re-definition)
					as are entire dispatch chains (to make re-execution of the
					same query fast)
			4.) the resulting query dispatcher is called in the passed scope
					(by default the top-level document)
				- for DOM queries, this results in a recursive, top-down
					evaluation of nodes based on each simple query section
				- for native implementations, this may mean working around spec
					bugs. So be it.
			5.) matched nodes are pruned to ensure they are unique (if necessary)
*/


	////////////////////////////////////////////////////////////////////////
	// Toolkit aliases
	////////////////////////////////////////////////////////////////////////

	// if you are extracting acme for use in your own system, you will
	// need to provide these methods and properties. No other porting should be
	// necessary, save for configuring the system to use a class other than
	// dojo/NodeList as the return instance instantiator
	var trim = 			lang.trim;
	var each = 			array.forEach;

	var getDoc = function(){ return win.doc; };
	// NOTE(alex): the spec is idiotic. CSS queries should ALWAYS be case-sensitive, but nooooooo
	var cssCaseBug = (getDoc().compatMode) == "BackCompat";

	////////////////////////////////////////////////////////////////////////
	// Global utilities
	////////////////////////////////////////////////////////////////////////


	var specials = ">~+";

	// global thunk to determine whether we should treat the current query as
	// case sensitive or not. This switch is flipped by the query evaluator
	// based on the document passed as the context to search.
	var caseSensitive = false;

	// how high?
	var yesman = function(){ return true; };

	////////////////////////////////////////////////////////////////////////
	// Tokenizer
	////////////////////////////////////////////////////////////////////////

	var getQueryParts = function(query){
		// summary:
		//		state machine for query tokenization
		// description:
		//		instead of using a brittle and slow regex-based CSS parser,
		//		acme implements an AST-style query representation. This
		//		representation is only generated once per query. For example,
		//		the same query run multiple times or under different root nodes
		//		does not re-parse the selector expression but instead uses the
		//		cached data structure. The state machine implemented here
		//		terminates on the last " " (space) character and returns an
		//		ordered array of query component structures (or "parts"). Each
		//		part represents an operator or a simple CSS filtering
		//		expression. The structure for parts is documented in the code
		//		below.


		// NOTE:
		//		this code is designed to run fast and compress well. Sacrifices
		//		to readability and maintainability have been made.  Your best
		//		bet when hacking the tokenizer is to put The Donnas on *really*
		//		loud (may we recommend their "Spend The Night" release?) and
		//		just assume you're gonna make mistakes. Keep the unit tests
		//		open and run them frequently. Knowing is half the battle ;-)
		if(specials.indexOf(query.slice(-1)) >= 0){
			// if we end with a ">", "+", or "~", that means we're implicitly
			// searching all children, so make it explicit
			query += " * ";
		}else{
			// if you have not provided a terminator, one will be provided for
			// you...
			query += " ";
		}

		var ts = function(/*Integer*/ s, /*Integer*/ e){
			// trim and slice.

			// take an index to start a string slice from and an end position
			// and return a trimmed copy of that sub-string
			return trim(query.slice(s, e));
		};

		// the overall data graph of the full query, as represented by queryPart objects
		var queryParts = [];


		// state keeping vars
		var inBrackets = -1, inParens = -1, inMatchFor = -1,
			inPseudo = -1, inClass = -1, inId = -1, inTag = -1, currentQuoteChar,
			lc = "", cc = "", pStart;

		// iteration vars
		var x = 0, // index in the query
			ql = query.length,
			currentPart = null, // data structure representing the entire clause
			_cp = null; // the current pseudo or attr matcher

		// several temporary variables are assigned to this structure during a
		// potential sub-expression match:
		//		attr:
		//			a string representing the current full attribute match in a
		//			bracket expression
		//		type:
		//			if there's an operator in a bracket expression, this is
		//			used to keep track of it
		//		value:
		//			the internals of parenthetical expression for a pseudo. for
		//			:nth-child(2n+1), value might be "2n+1"

		var endTag = function(){
			// called when the tokenizer hits the end of a particular tag name.
			// Re-sets state variables for tag matching and sets up the matcher
			// to handle the next type of token (tag or operator).
			if(inTag >= 0){
				var tv = (inTag == x) ? null : ts(inTag, x); // .toLowerCase();
				currentPart[ (specials.indexOf(tv) < 0) ? "tag" : "oper" ] = tv;
				inTag = -1;
			}
		};

		var endId = function(){
			// called when the tokenizer might be at the end of an ID portion of a match
			if(inId >= 0){
				currentPart.id = ts(inId, x).replace(/\\/g, "");
				inId = -1;
			}
		};

		var endClass = function(){
			// called when the tokenizer might be at the end of a class name
			// match. CSS allows for multiple classes, so we augment the
			// current item with another class in its list
			if(inClass >= 0){
				currentPart.classes.push(ts(inClass + 1, x).replace(/\\/g, ""));
				inClass = -1;
			}
		};

		var endAll = function(){
			// at the end of a simple fragment, so wall off the matches
			endId();
			endTag();
			endClass();
		};

		var endPart = function(){
			endAll();
			if(inPseudo >= 0){
				currentPart.pseudos.push({ name: ts(inPseudo + 1, x) });
			}
			// hint to the selector engine to tell it whether or not it
			// needs to do any iteration. Many simple selectors don't, and
			// we can avoid significant construction-time work by advising
			// the system to skip them
			currentPart.loops = (
					currentPart.pseudos.length ||
					currentPart.attrs.length ||
					currentPart.classes.length	);

			currentPart.oquery = currentPart.query = ts(pStart, x); // save the full expression as a string


			// otag/tag are hints to suggest to the system whether or not
			// it's an operator or a tag. We save a copy of otag since the
			// tag name is cast to upper-case in regular HTML matches. The
			// system has a global switch to figure out if the current
			// expression needs to be case sensitive or not and it will use
			// otag or tag accordingly
			currentPart.otag = currentPart.tag = (currentPart["oper"]) ? null : (currentPart.tag || "*");

			if(currentPart.tag){
				// if we're in a case-insensitive HTML doc, we likely want
				// the toUpperCase when matching on element.tagName. If we
				// do it here, we can skip the string op per node
				// comparison
				currentPart.tag = currentPart.tag.toUpperCase();
			}

			// add the part to the list
			if(queryParts.length && (queryParts[queryParts.length-1].oper)){
				// operators are always infix, so we remove them from the
				// list and attach them to the next match. The evaluator is
				// responsible for sorting out how to handle them.
				currentPart.infixOper = queryParts.pop();
				currentPart.query = currentPart.infixOper.query + " " + currentPart.query;
				/*
				console.debug(	"swapping out the infix",
								currentPart.infixOper,
								"and attaching it to",
								currentPart);
				*/
			}
			queryParts.push(currentPart);

			currentPart = null;
		};

		// iterate over the query, character by character, building up a
		// list of query part objects
		for(; lc=cc, cc=query.charAt(x), x < ql; x++){
			//		cc: the current character in the match
			//		lc: the last character (if any)

			// someone is trying to escape something, so don't try to match any
			// fragments. We assume we're inside a literal.
			if(lc == "\\"){ continue; }
			if(!currentPart){ // a part was just ended or none has yet been created
				// NOTE: I hate all this alloc, but it's shorter than writing tons of if's
				pStart = x;
				//	rules describe full CSS sub-expressions, like:
				//		#someId
				//		.className:first-child
				//	but not:
				//		thinger > div.howdy[type=thinger]
				//	the indidual components of the previous query would be
				//	split into 3 parts that would be represented a structure like:
				//		[
				//			{
				//				query: "thinger",
				//				tag: "thinger",
				//			},
				//			{
				//				query: "div.howdy[type=thinger]",
				//				classes: ["howdy"],
				//				infixOper: {
				//					query: ">",
				//					oper: ">",
				//				}
				//			},
				//		]
				currentPart = {
					query: null, // the full text of the part's rule
					pseudos: [], // CSS supports multiple pseud-class matches in a single rule
					attrs: [],	// CSS supports multi-attribute match, so we need an array
					classes: [], // class matches may be additive, e.g.: .thinger.blah.howdy
					tag: null,	// only one tag...
					oper: null, // ...or operator per component. Note that these wind up being exclusive.
					id: null,	// the id component of a rule
					getTag: function(){
						return caseSensitive ? this.otag : this.tag;
					}
				};

				// if we don't have a part, we assume we're going to start at
				// the beginning of a match, which should be a tag name. This
				// might fault a little later on, but we detect that and this
				// iteration will still be fine.
				inTag = x;
			}

			// Skip processing all quoted characters.
			// If we are inside quoted text then currentQuoteChar stores the character that began the quote,
			// thus that character that will end it.
			if(currentQuoteChar){
				if(cc == currentQuoteChar){
					currentQuoteChar = null;
				}
				continue;
			}else if (cc == "'" || cc == '"'){
				currentQuoteChar = cc;
				continue;
			}

			if(inBrackets >= 0){
				// look for a the close first
				if(cc == "]"){ // if we're in a [...] clause and we end, do assignment
					if(!_cp.attr){
						// no attribute match was previously begun, so we
						// assume this is an attribute existence match in the
						// form of [someAttributeName]
						_cp.attr = ts(inBrackets+1, x);
					}else{
						// we had an attribute already, so we know that we're
						// matching some sort of value, as in [attrName=howdy]
						_cp.matchFor = ts((inMatchFor||inBrackets+1), x);
					}
					var cmf = _cp.matchFor;
					if(cmf){
						// try to strip quotes from the matchFor value. We want
						// [attrName=howdy] to match the same
						//	as [attrName = 'howdy' ]
						if(	(cmf.charAt(0) == '"') || (cmf.charAt(0) == "'") ){
							_cp.matchFor = cmf.slice(1, -1);
						}
					}
					// remove backslash escapes from an attribute match, since DOM
					// querying will get attribute values without backslashes
					if(_cp.matchFor){
						_cp.matchFor = _cp.matchFor.replace(/\\/g, "");
					}

					// end the attribute by adding it to the list of attributes.
					currentPart.attrs.push(_cp);
					_cp = null; // necessary?
					inBrackets = inMatchFor = -1;
				}else if(cc == "="){
					// if the last char was an operator prefix, make sure we
					// record it along with the "=" operator.
					var addToCc = ("|~^$*".indexOf(lc) >=0 ) ? lc : "";
					_cp.type = addToCc+cc;
					_cp.attr = ts(inBrackets+1, x-addToCc.length);
					inMatchFor = x+1;
				}
				// now look for other clause parts
			}else if(inParens >= 0){
				// if we're in a parenthetical expression, we need to figure
				// out if it's attached to a pseudo-selector rule like
				// :nth-child(1)
				if(cc == ")"){
					if(inPseudo >= 0){
						_cp.value = ts(inParens+1, x);
					}
					inPseudo = inParens = -1;
				}
			}else if(cc == "#"){
				// start of an ID match
				endAll();
				inId = x+1;
			}else if(cc == "."){
				// start of a class match
				endAll();
				inClass = x;
			}else if(cc == ":"){
				// start of a pseudo-selector match
				endAll();
				inPseudo = x;
			}else if(cc == "["){
				// start of an attribute match.
				endAll();
				inBrackets = x;
				// provide a new structure for the attribute match to fill-in
				_cp = {
					/*=====
					attr: null, type: null, matchFor: null
					=====*/
				};
			}else if(cc == "("){
				// we really only care if we've entered a parenthetical
				// expression if we're already inside a pseudo-selector match
				if(inPseudo >= 0){
					// provide a new structure for the pseudo match to fill-in
					_cp = {
						name: ts(inPseudo+1, x),
						value: null
					};
					currentPart.pseudos.push(_cp);
				}
				inParens = x;
			}else if(
				(cc == " ") &&
				// if it's a space char and the last char is too, consume the
				// current one without doing more work
				(lc != cc)
			){
				endPart();
			}
		}
		return queryParts;
	};


	////////////////////////////////////////////////////////////////////////
	// DOM query infrastructure
	////////////////////////////////////////////////////////////////////////

	var agree = function(first, second){
		// the basic building block of the yes/no chaining system. agree(f1,
		// f2) generates a new function which returns the boolean results of
		// both of the passed functions to a single logical-anded result. If
		// either are not passed, the other is used exclusively.
		if(!first){ return second; }
		if(!second){ return first; }

		return function(){
			return first.apply(window, arguments) && second.apply(window, arguments);
		};
	};

	var getArr = function(i, arr){
		// helps us avoid array alloc when we don't need it
		var r = arr||[]; // FIXME: should this be 'new d._NodeListCtor()' ?
		if(i){ r.push(i); }
		return r;
	};

	var _isElement = function(n){ return (1 == n.nodeType); };

	// FIXME: need to coalesce _getAttr with defaultGetter
	var blank = "";
	var _getAttr = function(elem, attr){
		if(!elem){ return blank; }
		if(attr == "class"){
			return elem.className || blank;
		}
		if(attr == "for"){
			return elem.htmlFor || blank;
		}
		if(attr == "style"){
			return elem.style.cssText || blank;
		}
		return (caseSensitive ? elem.getAttribute(attr) : elem.getAttribute(attr, 2)) || blank;
	};

	var attrs = {
		"*=": function(attr, value){
			return function(elem){
				// E[foo*="bar"]
				//		an E element whose "foo" attribute value contains
				//		the substring "bar"
				return (_getAttr(elem, attr).indexOf(value)>=0);
			};
		},
		"^=": function(attr, value){
			// E[foo^="bar"]
			//		an E element whose "foo" attribute value begins exactly
			//		with the string "bar"
			return function(elem){
				return (_getAttr(elem, attr).indexOf(value)==0);
			};
		},
		"$=": function(attr, value){
			// E[foo$="bar"]
			//		an E element whose "foo" attribute value ends exactly
			//		with the string "bar"
			return function(elem){
				var ea = " "+_getAttr(elem, attr);
				var lastIndex = ea.lastIndexOf(value);
				return lastIndex > -1 && (lastIndex==(ea.length-value.length));
			};
		},
		"~=": function(attr, value){
			// E[foo~="bar"]
			//		an E element whose "foo" attribute value is a list of
			//		space-separated values, one of which is exactly equal
			//		to "bar"

			// return "[contains(concat(' ',@"+attr+",' '), ' "+ value +" ')]";
			var tval = " "+value+" ";
			return function(elem){
				var ea = " "+_getAttr(elem, attr)+" ";
				return (ea.indexOf(tval)>=0);
			};
		},
		"|=": function(attr, value){
			// E[hreflang|="en"]
			//		an E element whose "hreflang" attribute has a
			//		hyphen-separated list of values beginning (from the
			//		left) with "en"
			var valueDash = value+"-";
			return function(elem){
				var ea = _getAttr(elem, attr);
				return (
					(ea == value) ||
					(ea.indexOf(valueDash)==0)
				);
			};
		},
		"=": function(attr, value){
			return function(elem){
				return (_getAttr(elem, attr) == value);
			};
		}
	};

	// avoid testing for node type if we can. Defining this in the negative
	// here to avoid negation in the fast path.
	var _noNES = (typeof getDoc().firstChild.nextElementSibling == "undefined");
	var _ns = !_noNES ? "nextElementSibling" : "nextSibling";
	var _ps = !_noNES ? "previousElementSibling" : "previousSibling";
	var _simpleNodeTest = (_noNES ? _isElement : yesman);

	var _lookLeft = function(node){
		// look left
		while(node = node[_ps]){
			if(_simpleNodeTest(node)){ return false; }
		}
		return true;
	};

	var _lookRight = function(node){
		// look right
		while(node = node[_ns]){
			if(_simpleNodeTest(node)){ return false; }
		}
		return true;
	};

	var getNodeIndex = function(node){
		var root = node.parentNode;
		root = root.nodeType != 7 ? root : root.nextSibling; // PROCESSING_INSTRUCTION_NODE
		var i = 0,
			tret = root.children || root.childNodes,
			ci = (node["_i"]||node.getAttribute("_i")||-1),
			cl = (root["_l"]|| (typeof root.getAttribute !== "undefined" ? root.getAttribute("_l") : -1));

		if(!tret){ return -1; }
		var l = tret.length;

		// we calculate the parent length as a cheap way to invalidate the
		// cache. It's not 100% accurate, but it's much more honest than what
		// other libraries do
		if( cl == l && ci >= 0 && cl >= 0 ){
			// if it's legit, tag and release
			return ci;
		}

		// else re-key things
		if(has("ie") && typeof root.setAttribute !== "undefined"){
			root.setAttribute("_l", l);
		}else{
			root["_l"] = l;
		}
		ci = -1;
		for(var te = root["firstElementChild"]||root["firstChild"]; te; te = te[_ns]){
			if(_simpleNodeTest(te)){
				if(has("ie")){
					te.setAttribute("_i", ++i);
				}else{
					te["_i"] = ++i;
				}
				if(node === te){
					// NOTE:
					//	shortcutting the return at this step in indexing works
					//	very well for benchmarking but we avoid it here since
					//	it leads to potential O(n^2) behavior in sequential
					//	getNodexIndex operations on a previously un-indexed
					//	parent. We may revisit this at a later time, but for
					//	now we just want to get the right answer more often
					//	than not.
					ci = i;
				}
			}
		}
		return ci;
	};

	var isEven = function(elem){
		return !((getNodeIndex(elem)) % 2);
	};

	var isOdd = function(elem){
		return ((getNodeIndex(elem)) % 2);
	};

	var pseudos = {
		"checked": function(name, condition){
			return function(elem){
				return !!("checked" in elem ? elem.checked : elem.selected);
			};
		},
		"disabled": function(name, condition){
			return function(elem){
				return elem.disabled;
			};
		},
		"enabled": function(name, condition){
			return function(elem){
				return !elem.disabled;
			};
		},
		"first-child": function(){ return _lookLeft; },
		"last-child": function(){ return _lookRight; },
		"only-child": function(name, condition){
			return function(node){
				return _lookLeft(node) && _lookRight(node);
			};
		},
		"empty": function(name, condition){
			return function(elem){
				// DomQuery and jQuery get this wrong, oddly enough.
				// The CSS 3 selectors spec is pretty explicit about it, too.
				var cn = elem.childNodes;
				var cnl = elem.childNodes.length;
				// if(!cnl){ return true; }
				for(var x=cnl-1; x >= 0; x--){
					var nt = cn[x].nodeType;
					if((nt === 1)||(nt == 3)){ return false; }
				}
				return true;
			};
		},
		"contains": function(name, condition){
			var cz = condition.charAt(0);
			if( cz == '"' || cz == "'" ){ //remove quote
				condition = condition.slice(1, -1);
			}
			return function(elem){
				return (elem.innerHTML.indexOf(condition) >= 0);
			};
		},
		"not": function(name, condition){
			var p = getQueryParts(condition)[0];
			var ignores = { el: 1 };
			if(p.tag != "*"){
				ignores.tag = 1;
			}
			if(!p.classes.length){
				ignores.classes = 1;
			}
			var ntf = getSimpleFilterFunc(p, ignores);
			return function(elem){
				return (!ntf(elem));
			};
		},
		"nth-child": function(name, condition){
			var pi = parseInt;
			// avoid re-defining function objects if we can
			if(condition == "odd"){
				return isOdd;
			}else if(condition == "even"){
				return isEven;
			}
			// FIXME: can we shorten this?
			if(condition.indexOf("n") != -1){
				var tparts = condition.split("n", 2);
				var pred = tparts[0] ? ((tparts[0] == '-') ? -1 : pi(tparts[0])) : 1;
				var idx = tparts[1] ? pi(tparts[1]) : 0;
				var lb = 0, ub = -1;
				if(pred > 0){
					if(idx < 0){
						idx = (idx % pred) && (pred + (idx % pred));
					}else if(idx>0){
						if(idx >= pred){
							lb = idx - idx % pred;
						}
						idx = idx % pred;
					}
				}else if(pred<0){
					pred *= -1;
					// idx has to be greater than 0 when pred is negative;
					// shall we throw an error here?
					if(idx > 0){
						ub = idx;
						idx = idx % pred;
					}
				}
				if(pred > 0){
					return function(elem){
						var i = getNodeIndex(elem);
						return (i>=lb) && (ub<0 || i<=ub) && ((i % pred) == idx);
					};
				}else{
					condition = idx;
				}
			}
			var ncount = pi(condition);
			return function(elem){
				return (getNodeIndex(elem) == ncount);
			};
		}
	};

	var defaultGetter = (has("ie") < 9 || has("ie") == 9 && has("quirks")) ? function(cond){
		var clc = cond.toLowerCase();
		if(clc == "class"){ cond = "className"; }
		return function(elem){
			return (caseSensitive ? elem.getAttribute(cond) : elem[cond]||elem[clc]);
		};
	} : function(cond){
		return function(elem){
			return (elem && elem.getAttribute && elem.hasAttribute(cond));
		};
	};

	var getSimpleFilterFunc = function(query, ignores){
		// generates a node tester function based on the passed query part. The
		// query part is one of the structures generated by the query parser
		// when it creates the query AST. The "ignores" object specifies which
		// (if any) tests to skip, allowing the system to avoid duplicating
		// work where it may have already been taken into account by other
		// factors such as how the nodes to test were fetched in the first
		// place
		if(!query){ return yesman; }
		ignores = ignores||{};

		var ff = null;

		if(!("el" in ignores)){
			ff = agree(ff, _isElement);
		}

		if(!("tag" in ignores)){
			if(query.tag != "*"){
				ff = agree(ff, function(elem){
					return (elem && ((caseSensitive ? elem.tagName : elem.tagName.toUpperCase()) == query.getTag()));
				});
			}
		}

		if(!("classes" in ignores)){
			each(query.classes, function(cname, idx, arr){
				// get the class name
				/*
				var isWildcard = cname.charAt(cname.length-1) == "*";
				if(isWildcard){
					cname = cname.substr(0, cname.length-1);
				}
				// I dislike the regex thing, even if memoized in a cache, but it's VERY short
				var re = new RegExp("(?:^|\\s)" + cname + (isWildcard ? ".*" : "") + "(?:\\s|$)");
				*/
				var re = new RegExp("(?:^|\\s)" + cname + "(?:\\s|$)");
				ff = agree(ff, function(elem){
					return re.test(elem.className);
				});
				ff.count = idx;
			});
		}

		if(!("pseudos" in ignores)){
			each(query.pseudos, function(pseudo){
				var pn = pseudo.name;
				if(pseudos[pn]){
					ff = agree(ff, pseudos[pn](pn, pseudo.value));
				}
			});
		}

		if(!("attrs" in ignores)){
			each(query.attrs, function(attr){
				var matcher;
				var a = attr.attr;
				// type, attr, matchFor
				if(attr.type && attrs[attr.type]){
					matcher = attrs[attr.type](a, attr.matchFor);
				}else if(a.length){
					matcher = defaultGetter(a);
				}
				if(matcher){
					ff = agree(ff, matcher);
				}
			});
		}

		if(!("id" in ignores)){
			if(query.id){
				ff = agree(ff, function(elem){
					return (!!elem && (elem.id == query.id));
				});
			}
		}

		if(!ff){
			if(!("default" in ignores)){
				ff = yesman;
			}
		}
		return ff;
	};

	var _nextSibling = function(filterFunc){
		return function(node, ret, bag){
			while(node = node[_ns]){
				if(_noNES && (!_isElement(node))){ continue; }
				if(
					(!bag || _isUnique(node, bag)) &&
					filterFunc(node)
				){
					ret.push(node);
				}
				break;
			}
			return ret;
		};
	};

	var _nextSiblings = function(filterFunc){
		return function(root, ret, bag){
			var te = root[_ns];
			while(te){
				if(_simpleNodeTest(te)){
					if(bag && !_isUnique(te, bag)){
						break;
					}
					if(filterFunc(te)){
						ret.push(te);
					}
				}
				te = te[_ns];
			}
			return ret;
		};
	};

	// get an array of child *elements*, skipping text and comment nodes
	var _childElements = function(filterFunc){
		filterFunc = filterFunc||yesman;
		return function(root, ret, bag){
			// get an array of child elements, skipping text and comment nodes
			var te, x = 0, tret = root.children || root.childNodes;
			while(te = tret[x++]){
				if(
					_simpleNodeTest(te) &&
					(!bag || _isUnique(te, bag)) &&
					(filterFunc(te, x))
				){
					ret.push(te);
				}
			}
			return ret;
		};
	};

	// test to see if node is below root
	var _isDescendant = function(node, root){
		var pn = node.parentNode;
		while(pn){
			if(pn == root){
				break;
			}
			pn = pn.parentNode;
		}
		return !!pn;
	};

	var _getElementsFuncCache = {};

	var getElementsFunc = function(query){
		var retFunc = _getElementsFuncCache[query.query];
		// if we've got a cached dispatcher, just use that
		if(retFunc){ return retFunc; }
		// else, generate a new on

		// NOTE:
		//		this function returns a function that searches for nodes and
		//		filters them.  The search may be specialized by infix operators
		//		(">", "~", or "+") else it will default to searching all
		//		descendants (the " " selector). Once a group of children is
		//		found, a test function is applied to weed out the ones we
		//		don't want. Many common cases can be fast-pathed. We spend a
		//		lot of cycles to create a dispatcher that doesn't do more work
		//		than necessary at any point since, unlike this function, the
		//		dispatchers will be called every time. The logic of generating
		//		efficient dispatchers looks like this in pseudo code:
		//
		//		# if it's a purely descendant query (no ">", "+", or "~" modifiers)
		//		if infixOperator == " ":
		//			if only(id):
		//				return def(root):
		//					return d.byId(id, root);
		//
		//			elif id:
		//				return def(root):
		//					return filter(d.byId(id, root));
		//
		//			elif cssClass && getElementsByClassName:
		//				return def(root):
		//					return filter(root.getElementsByClassName(cssClass));
		//
		//			elif only(tag):
		//				return def(root):
		//					return root.getElementsByTagName(tagName);
		//
		//			else:
		//				# search by tag name, then filter
		//				return def(root):
		//					return filter(root.getElementsByTagName(tagName||"*"));
		//
		//		elif infixOperator == ">":
		//			# search direct children
		//			return def(root):
		//				return filter(root.children);
		//
		//		elif infixOperator == "+":
		//			# search next sibling
		//			return def(root):
		//				return filter(root.nextElementSibling);
		//
		//		elif infixOperator == "~":
		//			# search rightward siblings
		//			return def(root):
		//				return filter(nextSiblings(root));

		var io = query.infixOper;
		var oper = (io ? io.oper : "");
		// the default filter func which tests for all conditions in the query
		// part. This is potentially inefficient, so some optimized paths may
		// re-define it to test fewer things.
		var filterFunc = getSimpleFilterFunc(query, { el: 1 });
		var qt = query.tag;
		var wildcardTag = ("*" == qt);
		var ecs = getDoc()["getElementsByClassName"];

		if(!oper){
			// if there's no infix operator, then it's a descendant query. ID
			// and "elements by class name" variants can be accelerated so we
			// call them out explicitly:
			if(query.id){
				// testing shows that the overhead of yesman() is acceptable
				// and can save us some bytes vs. re-defining the function
				// everywhere.
				filterFunc = (!query.loops && wildcardTag) ?
					yesman :
					getSimpleFilterFunc(query, { el: 1, id: 1 });

				retFunc = function(root, arr){
					var te = dom.byId(query.id, (root.ownerDocument||root));
					if(!te || !filterFunc(te)){ return; }
					if(9 == root.nodeType){ // if root's a doc, we just return directly
						return getArr(te, arr);
					}else{ // otherwise check ancestry
						if(_isDescendant(te, root)){
							return getArr(te, arr);
						}
					}
				};
			}else if(
				ecs &&
				// isAlien check. Workaround for Prototype.js being totally evil/dumb.
				/\{\s*\[native code\]\s*\}/.test(String(ecs)) &&
				query.classes.length &&
				!cssCaseBug
			){
				// it's a class-based query and we've got a fast way to run it.

				// ignore class and ID filters since we will have handled both
				filterFunc = getSimpleFilterFunc(query, { el: 1, classes: 1, id: 1 });
				var classesString = query.classes.join(" ");
				retFunc = function(root, arr, bag){
					var ret = getArr(0, arr), te, x=0;
					var tret = root.getElementsByClassName(classesString);
					while((te = tret[x++])){
						if(filterFunc(te, root) && _isUnique(te, bag)){
							ret.push(te);
						}
					}
					return ret;
				};

			}else if(!wildcardTag && !query.loops){
				// it's tag only. Fast-path it.
				retFunc = function(root, arr, bag){
					var ret = getArr(0, arr), te, x=0;
					var tag = query.getTag(),
						tret = tag ? root.getElementsByTagName(tag) : [];
					while((te = tret[x++])){
						if(_isUnique(te, bag)){
							ret.push(te);
						}
					}
					return ret;
				};
			}else{
				// the common case:
				//		a descendant selector without a fast path. By now it's got
				//		to have a tag selector, even if it's just "*" so we query
				//		by that and filter
				filterFunc = getSimpleFilterFunc(query, { el: 1, tag: 1, id: 1 });
				retFunc = function(root, arr, bag){
					var ret = getArr(0, arr), te, x=0;
					// we use getTag() to avoid case sensitivity issues
					var tag = query.getTag(),
						tret = tag ? root.getElementsByTagName(tag) : [];
					while((te = tret[x++])){
						if(filterFunc(te, root) && _isUnique(te, bag)){
							ret.push(te);
						}
					}
					return ret;
				};
			}
		}else{
			// the query is scoped in some way. Instead of querying by tag we
			// use some other collection to find candidate nodes
			var skipFilters = { el: 1 };
			if(wildcardTag){
				skipFilters.tag = 1;
			}
			filterFunc = getSimpleFilterFunc(query, skipFilters);
			if("+" == oper){
				retFunc = _nextSibling(filterFunc);
			}else if("~" == oper){
				retFunc = _nextSiblings(filterFunc);
			}else if(">" == oper){
				retFunc = _childElements(filterFunc);
			}
		}
		// cache it and return
		return _getElementsFuncCache[query.query] = retFunc;
	};

	var filterDown = function(root, queryParts){
		// NOTE:
		//		this is the guts of the DOM query system. It takes a list of
		//		parsed query parts and a root and finds children which match
		//		the selector represented by the parts
		var candidates = getArr(root), qp, x, te, qpl = queryParts.length, bag, ret;

		for(var i = 0; i < qpl; i++){
			ret = [];
			qp = queryParts[i];
			x = candidates.length - 1;
			if(x > 0){
				// if we have more than one root at this level, provide a new
				// hash to use for checking group membership but tell the
				// system not to post-filter us since we will already have been
				// guaranteed to be unique
				bag = {};
				ret.nozip = true;
			}
			var gef = getElementsFunc(qp);
			for(var j = 0; (te = candidates[j]); j++){
				// for every root, get the elements that match the descendant
				// selector, adding them to the "ret" array and filtering them
				// via membership in this level's bag. If there are more query
				// parts, then this level's return will be used as the next
				// level's candidates
				gef(te, ret, bag);
			}
			if(!ret.length){ break; }
			candidates = ret;
		}
		return ret;
	};

	////////////////////////////////////////////////////////////////////////
	// the query runner
	////////////////////////////////////////////////////////////////////////

	// these are the primary caches for full-query results. The query
	// dispatcher functions are generated then stored here for hash lookup in
	// the future
	var _queryFuncCacheDOM = {},
		_queryFuncCacheQSA = {};

	// this is the second level of splitting, from full-length queries (e.g.,
	// "div.foo .bar") into simple query expressions (e.g., ["div.foo",
	// ".bar"])
	var getStepQueryFunc = function(query){
		var qparts = getQueryParts(trim(query));

		// if it's trivial, avoid iteration and zipping costs
		if(qparts.length == 1){
			// we optimize this case here to prevent dispatch further down the
			// chain, potentially slowing things down. We could more elegantly
			// handle this in filterDown(), but it's slower for simple things
			// that need to be fast (e.g., "#someId").
			var tef = getElementsFunc(qparts[0]);
			return function(root){
				var r = tef(root, []);
				if(r){ r.nozip = true; }
				return r;
			};
		}

		// otherwise, break it up and return a runner that iterates over the parts recursively
		return function(root){
			return filterDown(root, qparts);
		};
	};

	// NOTES:
	//	* we can't trust QSA for anything but document-rooted queries, so
	//	  caching is split into DOM query evaluators and QSA query evaluators
	//	* caching query results is dirty and leak-prone (or, at a minimum,
	//	  prone to unbounded growth). Other toolkits may go this route, but
	//	  they totally destroy their own ability to manage their memory
	//	  footprint. If we implement it, it should only ever be with a fixed
	//	  total element reference # limit and an LRU-style algorithm since JS
	//	  has no weakref support. Caching compiled query evaluators is also
	//	  potentially problematic, but even on large documents the size of the
	//	  query evaluators is often < 100 function objects per evaluator (and
	//	  LRU can be applied if it's ever shown to be an issue).
	//	* since IE's QSA support is currently only for HTML documents and even
	//	  then only in IE 8's "standards mode", we have to detect our dispatch
	//	  route at query time and keep 2 separate caches. Ugg.

	// we need to determine if we think we can run a given query via
	// querySelectorAll or if we'll need to fall back on DOM queries to get
	// there. We need a lot of information about the environment and the query
	// to make the determination (e.g. does it support QSA, does the query in
	// question work in the native QSA impl, etc.).

	// IE QSA queries may incorrectly include comment nodes, so we throw the
	// zipping function into "remove" comments mode instead of the normal "skip
	// it" which every other QSA-clued browser enjoys
	var noZip = has("ie") ? "commentStrip" : "nozip";

	var qsa = "querySelectorAll";
	var qsaAvail = !!getDoc()[qsa];

	//Don't bother with n+3 type of matches, IE complains if we modify those.
	var infixSpaceRe = /\\[>~+]|n\+\d|([^ \\])?([>~+])([^ =])?/g;
	var infixSpaceFunc = function(match, pre, ch, post){
		return ch ? (pre ? pre + " " : "") + ch + (post ? " " + post : "") : /*n+3*/ match;
	};
	
	//Don't apply the infixSpaceRe to attribute value selectors
	var attRe = /([^[]*)([^\]]*])?/g;
	var attFunc = function(match, nonAtt, att){
		return nonAtt.replace(infixSpaceRe, infixSpaceFunc) + (att||"");
	};
	var getQueryFunc = function(query, forceDOM){
		//Normalize query. The CSS3 selectors spec allows for omitting spaces around
		//infix operators, >, ~ and +
		//Do the work here since detection for spaces is used as a simple "not use QSA"
		//test below.
		query = query.replace(attRe, attFunc);

		if(qsaAvail){
			// if we've got a cached variant and we think we can do it, run it!
			var qsaCached = _queryFuncCacheQSA[query];
			if(qsaCached && !forceDOM){ return qsaCached; }
		}

		// else if we've got a DOM cached variant, assume that we already know
		// all we need to and use it
		var domCached = _queryFuncCacheDOM[query];
		if(domCached){ return domCached; }

		// TODO:
		//		today we're caching DOM and QSA branches separately so we
		//		recalc useQSA every time. If we had a way to tag root+query
		//		efficiently, we'd be in good shape to do a global cache.

		var qcz = query.charAt(0);
		var nospace = (-1 == query.indexOf(" "));

		// byId searches are wicked fast compared to QSA, even when filtering
		// is required
		if( (query.indexOf("#") >= 0) && (nospace) ){
			forceDOM = true;
		}

		var useQSA = (
			qsaAvail && (!forceDOM) &&
			// as per CSS 3, we can't currently start w/ combinator:
			//		http://www.w3.org/TR/css3-selectors/#w3cselgrammar
			(specials.indexOf(qcz) == -1) &&
			// IE's QSA impl sucks on pseudos
			(!has("ie") || (query.indexOf(":") == -1)) &&

			(!(cssCaseBug && (query.indexOf(".") >= 0))) &&

			// FIXME:
			//		need to tighten up browser rules on ":contains" and "|=" to
			//		figure out which aren't good
			//		Latest webkit (around 531.21.8) does not seem to do well with :checked on option
			//		elements, even though according to spec, selected options should
			//		match :checked. So go nonQSA for it:
			//		http://bugs.dojotoolkit.org/ticket/5179
			(query.indexOf(":contains") == -1) && (query.indexOf(":checked") == -1) &&
			(query.indexOf("|=") == -1) // some browsers don't grok it
		);

		// TODO:
		//		if we've got a descendant query (e.g., "> .thinger" instead of
		//		just ".thinger") in a QSA-able doc, but are passed a child as a
		//		root, it should be possible to give the item a synthetic ID and
		//		trivially rewrite the query to the form "#synid > .thinger" to
		//		use the QSA branch


		if(useQSA){
			var tq = (specials.indexOf(query.charAt(query.length-1)) >= 0) ?
						(query + " *") : query;
			return _queryFuncCacheQSA[query] = function(root){
				try{
					// the QSA system contains an egregious spec bug which
					// limits us, effectively, to only running QSA queries over
					// entire documents.  See:
					//		http://ejohn.org/blog/thoughts-on-queryselectorall/
					//	despite this, we can also handle QSA runs on simple
					//	selectors, but we don't want detection to be expensive
					//	so we're just checking for the presence of a space char
					//	right now. Not elegant, but it's cheaper than running
					//	the query parser when we might not need to
					if(!((9 == root.nodeType) || nospace)){ throw ""; }
					var r = root[qsa](tq);
					// skip expensive duplication checks and just wrap in a NodeList
					r[noZip] = true;
					return r;
				}catch(e){
					// else run the DOM branch on this query, ensuring that we
					// default that way in the future
					return getQueryFunc(query, true)(root);
				}
			};
		}else{
			// DOM branch
			var parts = query.match(/([^\s,](?:"(?:\\.|[^"])+"|'(?:\\.|[^'])+'|[^,])*)/g);
			return _queryFuncCacheDOM[query] = ((parts.length < 2) ?
				// if not a compound query (e.g., ".foo, .bar"), cache and return a dispatcher
				getStepQueryFunc(query) :
				// if it *is* a complex query, break it up into its
				// constituent parts and return a dispatcher that will
				// merge the parts when run
				function(root){
					var pindex = 0, // avoid array alloc for every invocation
						ret = [],
						tp;
					while((tp = parts[pindex++])){
						ret = ret.concat(getStepQueryFunc(tp)(root));
					}
					return ret;
				}
			);
		}
	};

	var _zipIdx = 0;

	// NOTE:
	//		this function is Moo inspired, but our own impl to deal correctly
	//		with XML in IE
	var _nodeUID = has("ie") ? function(node){
		if(caseSensitive){
			// XML docs don't have uniqueID on their nodes
			return (node.getAttribute("_uid") || node.setAttribute("_uid", ++_zipIdx) || _zipIdx);

		}else{
			return node.uniqueID;
		}
	} :
	function(node){
		return (node._uid || (node._uid = ++_zipIdx));
	};

	// determine if a node in is unique in a "bag". In this case we don't want
	// to flatten a list of unique items, but rather just tell if the item in
	// question is already in the bag. Normally we'd just use hash lookup to do
	// this for us but IE's DOM is busted so we can't really count on that. On
	// the upside, it gives us a built in unique ID function.
	var _isUnique = function(node, bag){
		if(!bag){ return 1; }
		var id = _nodeUID(node);
		if(!bag[id]){ return bag[id] = 1; }
		return 0;
	};

	// attempt to efficiently determine if an item in a list is a dupe,
	// returning a list of "uniques", hopefully in document order
	var _zipIdxName = "_zipIdx";
	var _zip = function(arr){
		if(arr && arr.nozip){ return arr; }

		if(!arr || !arr.length){ return []; }
		if(arr.length < 2){ return [arr[0]]; }

		var ret = [];

		_zipIdx++;

		// we have to fork here for IE and XML docs because we can't set
		// expandos on their nodes (apparently). *sigh*
		var x, te;
		if(has("ie") && caseSensitive){
			var szidx = _zipIdx+"";
			for(x = 0; x < arr.length; x++){
				if((te = arr[x]) && te.getAttribute(_zipIdxName) != szidx){
					ret.push(te);
					te.setAttribute(_zipIdxName, szidx);
				}
			}
		}else if(has("ie") && arr.commentStrip){
			try{
				for(x = 0; x < arr.length; x++){
					if((te = arr[x]) && _isElement(te)){
						ret.push(te);
					}
				}
			}catch(e){ /* squelch */ }
		}else{
			for(x = 0; x < arr.length; x++){
				if((te = arr[x]) && te[_zipIdxName] != _zipIdx){
					ret.push(te);
					te[_zipIdxName] = _zipIdx;
				}
			}
		}
		return ret;
	};

	// the main executor
	var query = function(/*String*/ query, /*String|DOMNode?*/ root){
		// summary:
		//		Returns nodes which match the given CSS3 selector, searching the
		//		entire document by default but optionally taking a node to scope
		//		the search by. Returns an array.
		// description:
		//		dojo.query() is the swiss army knife of DOM node manipulation in
		//		Dojo. Much like Prototype's "$$" (bling-bling) function or JQuery's
		//		"$" function, dojo.query provides robust, high-performance
		//		CSS-based node selector support with the option of scoping searches
		//		to a particular sub-tree of a document.
		//
		//		Supported Selectors:
		//		--------------------
		//
		//		acme supports a rich set of CSS3 selectors, including:
		//
		//		- class selectors (e.g., `.foo`)
		//		- node type selectors like `span`
		//		- ` ` descendant selectors
		//		- `>` child element selectors
		//		- `#foo` style ID selectors
		//		- `*` universal selector
		//		- `~`, the preceded-by sibling selector
		//		- `+`, the immediately preceded-by sibling selector
		//		- attribute queries:
		//			- `[foo]` attribute presence selector
		//			- `[foo='bar']` attribute value exact match
		//			- `[foo~='bar']` attribute value list item match
		//			- `[foo^='bar']` attribute start match
		//			- `[foo$='bar']` attribute end match
		//			- `[foo*='bar']` attribute substring match
		//		- `:first-child`, `:last-child`, and `:only-child` positional selectors
		//		- `:empty` content emtpy selector
		//		- `:checked` pseudo selector
		//		- `:nth-child(n)`, `:nth-child(2n+1)` style positional calculations
		//		- `:nth-child(even)`, `:nth-child(odd)` positional selectors
		//		- `:not(...)` negation pseudo selectors
		//
		//		Any legal combination of these selectors will work with
		//		`dojo.query()`, including compound selectors ("," delimited).
		//		Very complex and useful searches can be constructed with this
		//		palette of selectors and when combined with functions for
		//		manipulation presented by dojo/NodeList, many types of DOM
		//		manipulation operations become very straightforward.
		//
		//		Unsupported Selectors:
		//		----------------------
		//
		//		While dojo.query handles many CSS3 selectors, some fall outside of
		//		what's reasonable for a programmatic node querying engine to
		//		handle. Currently unsupported selectors include:
		//
		//		- namespace-differentiated selectors of any form
		//		- all `::` pseduo-element selectors
		//		- certain pseudo-selectors which don't get a lot of day-to-day use:
		//			- `:root`, `:lang()`, `:target`, `:focus`
		//		- all visual and state selectors:
		//			- `:root`, `:active`, `:hover`, `:visited`, `:link`,
		//				  `:enabled`, `:disabled`
		//			- `:*-of-type` pseudo selectors
		//
		//		dojo.query and XML Documents:
		//		-----------------------------
		//
		//		`dojo.query` (as of dojo 1.2) supports searching XML documents
		//		in a case-sensitive manner. If an HTML document is served with
		//		a doctype that forces case-sensitivity (e.g., XHTML 1.1
		//		Strict), dojo.query() will detect this and "do the right
		//		thing". Case sensitivity is dependent upon the document being
		//		searched and not the query used. It is therefore possible to
		//		use case-sensitive queries on strict sub-documents (iframes,
		//		etc.) or XML documents while still assuming case-insensitivity
		//		for a host/root document.
		//
		//		Non-selector Queries:
		//		---------------------
		//
		//		If something other than a String is passed for the query,
		//		`dojo.query` will return a new `dojo/NodeList` instance
		//		constructed from that parameter alone and all further
		//		processing will stop. This means that if you have a reference
		//		to a node or NodeList, you can quickly construct a new NodeList
		//		from the original by calling `dojo.query(node)` or
		//		`dojo.query(list)`.
		//
		// query:
		//		The CSS3 expression to match against. For details on the syntax of
		//		CSS3 selectors, see <http://www.w3.org/TR/css3-selectors/#selectors>
		// root:
		//		A DOMNode (or node id) to scope the search from. Optional.
		// returns: Array
		// example:
		//		search the entire document for elements with the class "foo":
		//	|	dojo.query(".foo");
		//		these elements will match:
		//	|	<span class="foo"></span>
		//	|	<span class="foo bar"></span>
		//	|	<p class="thud foo"></p>
		// example:
		//		search the entire document for elements with the classes "foo" *and* "bar":
		//	|	dojo.query(".foo.bar");
		//		these elements will match:
		//	|	<span class="foo bar"></span>
		//		while these will not:
		//	|	<span class="foo"></span>
		//	|	<p class="thud foo"></p>
		// example:
		//		find `<span>` elements which are descendants of paragraphs and
		//		which have a "highlighted" class:
		//	|	dojo.query("p span.highlighted");
		//		the innermost span in this fragment matches:
		//	|	<p class="foo">
		//	|		<span>...
		//	|			<span class="highlighted foo bar">...</span>
		//	|		</span>
		//	|	</p>
		// example:
		//		set an "odd" class on all odd table rows inside of the table
		//		`#tabular_data`, using the `>` (direct child) selector to avoid
		//		affecting any nested tables:
		//	|	dojo.query("#tabular_data > tbody > tr:nth-child(odd)").addClass("odd");
		// example:
		//		remove all elements with the class "error" from the document
		//		and store them in a list:
		//	|	var errors = dojo.query(".error").orphan();
		// example:
		//		add an onclick handler to every submit button in the document
		//		which causes the form to be sent via Ajax instead:
		//	|	dojo.query("input[type='submit']").onclick(function(e){
		//	|		dojo.stopEvent(e); // prevent sending the form
		//	|		var btn = e.target;
		//	|		dojo.xhrPost({
		//	|			form: btn.form,
		//	|			load: function(data){
		//	|				// replace the form with the response
		//	|				var div = dojo.doc.createElement("div");
		//	|				dojo.place(div, btn.form, "after");
		//	|				div.innerHTML = data;
		//	|				dojo.style(btn.form, "display", "none");
		//	|			}
		//	|		});
		//	|	});

		root = root || getDoc();

		// throw the big case sensitivity switch
		var od = root.ownerDocument || root;	// root is either Document or a node inside the document
		caseSensitive = (od.createElement("div").tagName === "div");

		// NOTE:
		//		adding "true" as the 2nd argument to getQueryFunc is useful for
		//		testing the DOM branch without worrying about the
		//		behavior/performance of the QSA branch.
		var r = getQueryFunc(query)(root);

		// FIXME:
		//		need to investigate this branch WRT #8074 and #8075
		if(r && r.nozip){
			return r;
		}
		return _zip(r); // dojo/NodeList
	};
	query.filter = function(/*Node[]*/ nodeList, /*String*/ filter, /*String|DOMNode?*/ root){
		// summary:
		//		function for filtering a NodeList based on a selector, optimized for simple selectors
		var tmpNodeList = [],
			parts = getQueryParts(filter),
			filterFunc =
				(parts.length == 1 && !/[^\w#\.]/.test(filter)) ?
				getSimpleFilterFunc(parts[0]) :
				function(node){
					return array.indexOf(query(filter, dom.byId(root)), node) != -1;
				};
		for(var x = 0, te; te = nodeList[x]; x++){
			if(filterFunc(te)){ tmpNodeList.push(te); }
		}
		return tmpNodeList;
	};
	return query;
});

},
'dgrid/Grid':function(){
define(["dojo/_base/kernel", "dojo/_base/declare", "dojo/on", "dojo/has", "put-selector/put", "./List", "./util/misc", "dojo/_base/sniff"],
function(kernel, declare, listen, has, put, List, miscUtil){
	var contentBoxSizing = has("ie") < 8 && !has("quirks");
	var invalidClassChars = /[^\._a-zA-Z0-9-]/g;
	function appendIfNode(parent, subNode){
		if(subNode && subNode.nodeType){
			parent.appendChild(subNode);
		}
	}
	
	var Grid = declare(List, {
		columns: null,
		// cellNavigation: Boolean
		//		This indicates that focus is at the cell level. This may be set to false to cause
		//		focus to be at the row level, which is useful if you want only want row-level
		//		navigation.
		cellNavigation: true,
		tabableHeader: true,
		showHeader: true,
		column: function(target){
			// summary:
			//		Get the column object by node, or event, or a columnId
			if(typeof target == "string"){
				return this.columns[target];
			}else{
				return this.cell(target).column;
			}
		},
		listType: "grid",
		cell: function(target, columnId){
			// summary:
			//		Get the cell object by node, or event, id, plus a columnId
			
			if(target.column && target.element){ return target; }
			
			if(target.target && target.target.nodeType){
				// event
				target = target.target;
			}
			var element;
			if(target.nodeType){
				var object;
				do{
					if(this._rowIdToObject[target.id]){
						break;
					}
					var colId = target.columnId;
					if(colId){
						columnId = colId;
						element = target;
						break;
					}
					target = target.parentNode;
				}while(target && target != this.domNode);
			}
			if(!element && typeof columnId != "undefined"){
				var row = this.row(target),
					rowElement = row && row.element;
				if(rowElement){
					var elements = rowElement.getElementsByTagName("td");
					for(var i = 0; i < elements.length; i++){
						if(elements[i].columnId == columnId){
							element = elements[i];
							break;
						}
					}
				}
			}
			if(target != null){
				return {
					row: row || this.row(target),
					column: columnId && this.column(columnId),
					element: element
				};
			}
		},
		
		createRowCells: function(tag, each, subRows){
			// summary:
			//		Generates the grid for each row (used by renderHeader and and renderRow)
			var row = put("table.dgrid-row-table[role=presentation]"),
				cellNavigation = this.cellNavigation,
				// IE < 9 needs an explicit tbody; other browsers do not
				tbody = (has("ie") < 9 || has("quirks")) ? put(row, "tbody") : row,
				tr,
				si, sl, i, l, // iterators
				subRow, column, id, extraClassName, cell, innerCell, colSpan, rowSpan; // used inside loops
			
			// Allow specification of custom/specific subRows, falling back to
			// those defined on the instance.
			subRows = subRows || this.subRows;
			
			for(si = 0, sl = subRows.length; si < sl; si++){
				subRow = subRows[si];
				// for single-subrow cases in modern browsers, TR can be skipped
				// http://jsperf.com/table-without-trs
				tr = put(tbody, "tr");
				if(subRow.className){
					put(tr, "." + subRow.className);
				}
				
				for(i = 0, l = subRow.length; i < l; i++){
					// iterate through the columns
					column = subRow[i];
					id = column.id;
					extraClassName = column.className || (column.field && "field-" + column.field);
					cell = put(tag + (
							".dgrid-cell.dgrid-cell-padding" +
							(id ? ".dgrid-column-" + id : "") +
							(extraClassName ? "." + extraClassName : "")
						).replace(invalidClassChars,"-") +
						"[role=" + (tag === "th" ? "columnheader" : "gridcell") + "]");
					cell.columnId = id;
					if(contentBoxSizing){
						// The browser (IE7-) does not support box-sizing: border-box, so we emulate it with a padding div
						innerCell = put(cell, "!dgrid-cell-padding div.dgrid-cell-padding");// remove the dgrid-cell-padding, and create a child with that class
						cell.contents = innerCell;
					}else{
						innerCell = cell;
					}
					colSpan = column.colSpan;
					if(colSpan){
						cell.colSpan = colSpan;
					}
					rowSpan = column.rowSpan;
					if(rowSpan){
						cell.rowSpan = rowSpan;
					}
					each(innerCell, column);
					// add the td to the tr at the end for better performance
					tr.appendChild(cell);
				}
			}
			return row;
		},
		
		left: function(cell, steps){
			if(!cell.element){ cell = this.cell(cell); }
			return this.cell(this._move(cell, -(steps || 1), "dgrid-cell"));
		},
		right: function(cell, steps){
			if(!cell.element){ cell = this.cell(cell); }
			return this.cell(this._move(cell, steps || 1, "dgrid-cell"));
		},
		
		renderRow: function(object, options){
			var self = this;
			var row = this.createRowCells("td", function(td, column){
				var data = object;
				// Support get function or field property (similar to DataGrid)
				if(column.get){
					data = column.get(object);
				}else if("field" in column && column.field != "_item"){
					data = data[column.field];
				}
				
				if(column.renderCell){
					// A column can provide a renderCell method to do its own DOM manipulation,
					// event handling, etc.
					appendIfNode(td, column.renderCell(object, data, td, options));
				}else{
					defaultRenderCell.call(column, object, data, td, options);
				}
			}, options && options.subRows);
			// row gets a wrapper div for a couple reasons:
			//	1. So that one can set a fixed height on rows (heights can't be set on <table>'s AFAICT)
			// 2. So that outline style can be set on a row when it is focused, and Safari's outline style is broken on <table>
			return put("div[role=row]>", row);
		},
		renderHeader: function(){
			// summary:
			//		Setup the headers for the grid
			var
				grid = this,
				columns = this.columns,
				headerNode = this.headerNode,
				i = headerNode.childNodes.length;
			
			headerNode.setAttribute("role", "row");
			
			// clear out existing header in case we're resetting
			while(i--){
				put(headerNode.childNodes[i], "!");
			}
			
			var row = this.createRowCells("th", function(th, column){
				var contentNode = column.headerNode = th;
				if(contentBoxSizing){
					// we're interested in the th, but we're passed the inner div
					th = th.parentNode;
				}
				var field = column.field;
				if(field){
					th.field = field;
				}
				// allow for custom header content manipulation
				if(column.renderHeaderCell){
					appendIfNode(contentNode, column.renderHeaderCell(contentNode));
				}else if("label" in column || column.field){
					contentNode.appendChild(document.createTextNode(
						"label" in column ? column.label : column.field));
				}
				if(column.sortable !== false && field && field != "_item"){
					th.sortable = true;
					th.className += " dgrid-sortable";
				}
			}, this.subRows && this.subRows.headerRows);
			this._rowIdToObject[row.id = this.id + "-header"] = this.columns;
			headerNode.appendChild(row);
			
			// If the columns are sortable, re-sort on clicks.
			// Use a separate listener property to be managed by renderHeader in case
			// of subsequent calls.
			if(this._sortListener){
				this._sortListener.remove();
			}
			this._sortListener = listen(row, "click,keydown", function(event){
				// respond to click, space keypress, or enter keypress
				if(event.type == "click" || event.keyCode == 32 /* space bar */ || (!has("opera") && event.keyCode == 13) /* enter */){
					var target = event.target,
						field, sort, newSort, eventObj;
					do{
						if(target.sortable){
							// If the click is on the same column as the active sort,
							// reverse sort direction
							newSort = [{
								attribute: (field = target.field || target.columnId),
								descending: (sort = grid._sort[0]) && sort.attribute == field &&
									!sort.descending
							}];
							
							// Emit an event with the new sort
							eventObj = {
								bubbles: true,
								cancelable: true,
								grid: grid,
								parentType: event.type,
								sort: newSort
							};
							
							if (listen.emit(event.target, "dgrid-sort", eventObj)){
								// Stash node subject to DOM manipulations,
								// to be referenced then removed by sort()
								grid._sortNode = target;
								grid.set("sort", newSort);
							}
							
							break;
						}
					}while((target = target.parentNode) && target != headerNode);
				}
			});
		},
		
		resize: function(){
			// extension of List.resize to allow accounting for
			// column sizes larger than actual grid area
			var
				headerTableNode = this.headerNode.firstChild,
				contentNode = this.contentNode,
				width;
			
			this.inherited(arguments);
			
			if(!has("ie") || (has("ie") > 7 && !has("quirks"))){
				// Force contentNode width to match up with header width.
				// (Old IEs don't have a problem due to how they layout.)
				
				contentNode.style.width = ""; // reset first
				
				if(contentNode && headerTableNode){
					if((width = headerTableNode.offsetWidth) != contentNode.offsetWidth){
						// update size of content node if necessary (to match size of rows)
						// (if headerTableNode can't be found, there isn't much we can do)
						contentNode.style.width = width + "px";
					}
				}
			}
		},
		
		destroy: function(){
			// Run _destroyColumns first to perform any column plugin tear-down logic.
			this._destroyColumns();
			if(this._sortListener){
				this._sortListener.remove();
			}
			
			this.inherited(arguments);
		},
		
		_setSort: function(property, descending){
			// summary:
			//		Extension of List.js sort to update sort arrow in UI
			
			// Normalize _sort first via inherited logic, then update the sort arrow
			this.inherited(arguments);
			this.updateSortArrow(this._sort);
		},
		
		updateSortArrow: function(sort, updateSort){
			// summary:
			//		Method responsible for updating the placement of the arrow in the
			//		appropriate header cell.  Typically this should not be called (call
			//		set("sort", ...) when actually updating sort programmatically), but
			//		this method may be used by code which is customizing sort (e.g.
			//		by reacting to the dgrid-sort event, canceling it, then
			//		performing logic and calling this manually).
			// sort: Array
			//		Standard sort parameter - array of object(s) containing attribute
			//		and optionally descending property
			// updateSort: Boolean?
			//		If true, will update this._sort based on the passed sort array
			//		(i.e. to keep it in sync when custom logic is otherwise preventing
			//		it from being updated); defaults to false
			
			// Clean up UI from any previous sort
			if(this._lastSortedArrow){
				// Remove the sort classes from the parent node
				put(this._lastSortedArrow, "<!dgrid-sort-up!dgrid-sort-down");
				// Destroy the lastSortedArrow node
				put(this._lastSortedArrow, "!");
				delete this._lastSortedArrow;
			}
			
			if(updateSort){ this._sort = sort; }
			if(!sort[0]){ return; } // nothing to do if no sort is specified
			
			var prop = sort[0].attribute,
				desc = sort[0].descending,
				target = this._sortNode, // stashed if invoked from header click
				columns, column, i;
			
			delete this._sortNode;
			
			if(!target){
				columns = this.columns;
				for(i in columns){
					column = columns[i];
					if(column.field == prop){
						target = column.headerNode;
						break;
					}
				}
			}
			// skip this logic if field being sorted isn't actually displayed
			if(target){
				target = target.contents || target;
				// place sort arrow under clicked node, and add up/down sort class
				this._lastSortedArrow = put(target.firstChild, "-div.dgrid-sort-arrow.ui-icon[role=presentation]");
				this._lastSortedArrow.innerHTML = "&nbsp;";
				put(target, desc ? ".dgrid-sort-down" : ".dgrid-sort-up");
				// call resize in case relocation of sort arrow caused any height changes
				this.resize();
			}
		},
		
		styleColumn: function(colId, css){
			// summary:
			//		Dynamically creates a stylesheet rule to alter a column's style.
			
			return this.addCssRule("#" + miscUtil.escapeCssIdentifier(this.domNode.id) +
				" .dgrid-column-" + colId, css);
		},
		
		/*=====
		_configColumn: function(column, columnId, rowColumns, prefix){
			// summary:
			//		Method called when normalizing base configuration of a single
			//		column.  Can be used as an extension point for behavior requiring
			//		access to columns when a new configuration is applied.
		},=====*/
		
		_configColumns: function(prefix, rowColumns){
			// configure the current column
			var subRow = [],
				isArray = rowColumns instanceof Array;
			
			function configColumn(column, columnId){
				if(typeof column == "string"){
					rowColumns[columnId] = column = {label:column};
				}
				if(!isArray && !column.field){
					column.field = columnId;
				}
				columnId = column.id = column.id || (isNaN(columnId) ? columnId : (prefix + columnId));
				if(isArray){ this.columns[columnId] = column; }
				
				// allow further base configuration in subclasses
				if(this._configColumn){
					this._configColumn(column, columnId, rowColumns, prefix);
				}
				
				// add grid reference to each column object for potential use by plugins
				column.grid = this;
				if(typeof column.init === "function"){ column.init(); }
				
				subRow.push(column); // make sure it can be iterated on
			}
			
			miscUtil.each(rowColumns, configColumn, this);
			return isArray ? rowColumns : subRow;
		},
		
		_destroyColumns: function(){
			// summary:
			//		Iterates existing subRows looking for any column definitions with
			//		destroy methods (defined by plugins) and calls them.  This is called
			//		immediately before configuring a new column structure.
			
			var subRows = this.subRows,
				// if we have column sets, then we don't need to do anything with the missing subRows, ColumnSet will handle it
				subRowsLength = subRows && subRows.length,
				i, j, column, len;
			
			// First remove rows (since they'll be refreshed after we're done),
			// so that anything aspected onto removeRow by plugins can run.
			// (cleanup will end up running again, but with nothing to iterate.)
			this.cleanup();
			
			for(i = 0; i < subRowsLength; i++){
				for(j = 0, len = subRows[i].length; j < len; j++){
					column = subRows[i][j];
					if(typeof column.destroy === "function"){ column.destroy(); }
				}
			}
		},
		
		configStructure: function(){
			// configure the columns and subRows
			var subRows = this.subRows,
				columns = this._columns = this.columns;
			
			// Reset this.columns unless it was already passed in as an object
			this.columns = !columns || columns instanceof Array ? {} : columns;
			
			if(subRows){
				// Process subrows, which will in turn populate the this.columns object
				for(var i = 0; i < subRows.length; i++){
					subRows[i] = this._configColumns(i + "-", subRows[i]);
				}
			}else{
				this.subRows = [this._configColumns("", columns)];
			}
		},
		
		_getColumns: function(){
			// _columns preserves what was passed to set("columns"), but if subRows
			// was set instead, columns contains the "object-ified" version, which
			// was always accessible in the past, so maintain that accessibility going
			// forward.
			return this._columns || this.columns;
		},
		_setColumns: function(columns){
			this._destroyColumns();
			// reset instance variables
			this.subRows = null;
			this.columns = columns;
			// re-run logic
			this._updateColumns();
		},
		
		_setSubRows: function(subrows){
			this._destroyColumns();
			this.subRows = subrows;
			this._updateColumns();
		},
		
		setColumns: function(columns){
			kernel.deprecated("setColumns(...)", 'use set("columns", ...) instead', "dgrid 0.4");
			this.set("columns", columns);
		},
		setSubRows: function(subrows){
			kernel.deprecated("setSubRows(...)", 'use set("subRows", ...) instead', "dgrid 0.4");
			this.set("subRows", subrows);
		},
		
		_updateColumns: function(){
			// summary:
			//		Called when columns, subRows, or columnSets are reset
			
			this.configStructure();
			this.renderHeader();
			
			this.refresh();
			// re-render last collection if present
			this._lastCollection && this.renderArray(this._lastCollection);
			
			// After re-rendering the header, re-apply the sort arrow if needed.
			if(this._started){
				if(this._sort && this._sort.length){
					this.updateSortArrow(this._sort);
				} else {
					// Only call resize directly if we didn't call updateSortArrow,
					// since that calls resize itself when it updates.
					this.resize();
				}
			}
		}
	});
	
	function defaultRenderCell(object, data, td, options){
		if(this.formatter){
			// Support formatter, with or without formatterScope
			var formatter = this.formatter,
				formatterScope = this.grid.formatterScope;
			td.innerHTML = typeof formatter === "string" && formatterScope ?
				formatterScope[formatter](data, object) : formatter(data, object);
		}else if(data != null){
			td.appendChild(document.createTextNode(data)); 
		}
	}
	
	// expose appendIfNode and default implementation of renderCell,
	// e.g. for use by column plugins
	Grid.appendIfNode = appendIfNode;
	Grid.defaultRenderCell = defaultRenderCell;
	
	return Grid;
});

},
'dojo/dom':function(){
define(["./sniff", "./_base/window"],
		function(has, win){
	// module:
	//		dojo/dom

	// FIXME: need to add unit tests for all the semi-public methods

	if(has("ie") <= 7){
		try{
			document.execCommand("BackgroundImageCache", false, true);
		}catch(e){
			// sane browsers don't have cache "issues"
		}
	}

	// =============================
	// DOM Functions
	// =============================

	// the result object
	var dom = {
		// summary:
		//		This module defines the core dojo DOM API.
	};

	if(has("ie")){
		dom.byId = function(id, doc){
			if(typeof id != "string"){
				return id;
			}
			var _d = doc || win.doc, te = id && _d.getElementById(id);
			// attributes.id.value is better than just id in case the
			// user has a name=id inside a form
			if(te && (te.attributes.id.value == id || te.id == id)){
				return te;
			}else{
				var eles = _d.all[id];
				if(!eles || eles.nodeName){
					eles = [eles];
				}
				// if more than 1, choose first with the correct id
				var i = 0;
				while((te = eles[i++])){
					if((te.attributes && te.attributes.id && te.attributes.id.value == id) || te.id == id){
						return te;
					}
				}
			}
		};
	}else{
		dom.byId = function(id, doc){
			// inline'd type check.
			// be sure to return null per documentation, to match IE branch.
			return ((typeof id == "string") ? (doc || win.doc).getElementById(id) : id) || null; // DOMNode
		};
	}
	/*=====
	 dom.byId = function(id, doc){
		 // summary:
		 //		Returns DOM node with matching `id` attribute or falsy value (ex: null or undefined)
		 //		if not found.  If `id` is a DomNode, this function is a no-op.
		 //
		 // id: String|DOMNode
		 //		A string to match an HTML id attribute or a reference to a DOM Node
		 //
		 // doc: Document?
		 //		Document to work in. Defaults to the current value of
		 //		dojo.doc.  Can be used to retrieve
		 //		node references from other documents.
		 //
		 // example:
		 //		Look up a node by ID:
		 //	|	var n = dojo.byId("foo");
		 //
		 // example:
		 //		Check if a node exists, and use it.
		 //	|	var n = dojo.byId("bar");
		 //	|	if(n){ doStuff() ... }
		 //
		 // example:
		 //		Allow string or DomNode references to be passed to a custom function:
		 //	|	var foo = function(nodeOrId){
		 //	|		nodeOrId = dojo.byId(nodeOrId);
		 //	|		// ... more stuff
		 //	|	}
	 };
	 =====*/

	dom.isDescendant = function(/*DOMNode|String*/ node, /*DOMNode|String*/ ancestor){
		// summary:
		//		Returns true if node is a descendant of ancestor
		// node: DOMNode|String
		//		string id or node reference to test
		// ancestor: DOMNode|String
		//		string id or node reference of potential parent to test against
		//
		// example:
		//		Test is node id="bar" is a descendant of node id="foo"
		//	|	if(dojo.isDescendant("bar", "foo")){ ... }

		try{
			node = dom.byId(node);
			ancestor = dom.byId(ancestor);
			while(node){
				if(node == ancestor){
					return true; // Boolean
				}
				node = node.parentNode;
			}
		}catch(e){ /* squelch, return false */ }
		return false; // Boolean
	};


	// TODO: do we need setSelectable in the base?

	// Add feature test for user-select CSS property
	// (currently known to work in all but IE < 10 and Opera)
	has.add("css-user-select", function(global, doc, element){
		// Avoid exception when dom.js is loaded in non-browser environments
		if(!element){ return false; }
		
		var style = element.style;
		var prefixes = ["Khtml", "O", "ms", "Moz", "Webkit"],
			i = prefixes.length,
			name = "userSelect",
			prefix;

		// Iterate prefixes from most to least likely
		do{
			if(typeof style[name] !== "undefined"){
				// Supported; return property name
				return name;
			}
		}while(i-- && (name = prefixes[i] + "UserSelect"));

		// Not supported if we didn't return before now
		return false;
	});

	/*=====
	dom.setSelectable = function(node, selectable){
		// summary:
		//		Enable or disable selection on a node
		// node: DOMNode|String
		//		id or reference to node
		// selectable: Boolean
		//		state to put the node in. false indicates unselectable, true
		//		allows selection.
		// example:
		//		Make the node id="bar" unselectable
		//	|	dojo.setSelectable("bar");
		// example:
		//		Make the node id="bar" selectable
		//	|	dojo.setSelectable("bar", true);
	};
	=====*/

	var cssUserSelect = has("css-user-select");
	dom.setSelectable = cssUserSelect ? function(node, selectable){
		// css-user-select returns a (possibly vendor-prefixed) CSS property name
		dom.byId(node).style[cssUserSelect] = selectable ? "" : "none";
	} : function(node, selectable){
		node = dom.byId(node);

		// (IE < 10 / Opera) Fall back to setting/removing the
		// unselectable attribute on the element and all its children
		var nodes = node.getElementsByTagName("*"),
			i = nodes.length;

		if(selectable){
			node.removeAttribute("unselectable");
			while(i--){
				nodes[i].removeAttribute("unselectable");
			}
		}else{
			node.setAttribute("unselectable", "on");
			while(i--){
				nodes[i].setAttribute("unselectable", "on");
			}
		}
	};

	return dom;
});

},
'dgrid/_StoreMixin':function(){
define(["dojo/_base/kernel", "dojo/_base/declare", "dojo/_base/lang", "dojo/_base/Deferred", "dojo/on", "dojo/aspect", "put-selector/put"],
function(kernel, declare, lang, Deferred, listen, aspect, put){
	// This module isolates the base logic required by store-aware list/grid
	// components, e.g. OnDemandList/Grid and the Pagination extension.
	
	// Noop function, needed for _trackError when callback due to a bug in 1.8
	// (see http://bugs.dojotoolkit.org/ticket/16667)
	function noop(value){ return value; }
	
	function emitError(err){
		// called by _trackError in context of list/grid, if an error is encountered
		if(typeof err !== "object"){
			// Ensure we actually have an error object, so we can attach a reference.
			err = new Error(err);
		}else if(err.dojoType === "cancel"){
			// Don't fire dgrid-error events for errors due to canceled requests
			// (unfortunately, the Deferred instrumentation will still log them)
			return;
		}
		// TODO: remove this @ 0.4 (prefer grid property directly on event object)
		err.grid = this;
		
		if(listen.emit(this.domNode, "dgrid-error", {
				grid: this,
				error: err,
				cancelable: true,
				bubbles: true })){
			console.error(err);
		}
	}
	
	return declare(null, {
		// store: Object
		//		The object store (implementing the dojo/store API) from which data is
		//		to be fetched.
		store: null,
		
		// query: Object
		//		Specifies query parameter(s) to pass to store.query calls.
		query: null,
		
		// queryOptions: Object
		//		Specifies additional query options to mix in when calling store.query;
		//		sort, start, and count are already handled.
		queryOptions: null,
		
		// getBeforePut: boolean
		//		If true, a get request will be performed to the store before each put
		//		as a baseline when saving; otherwise, existing row data will be used.
		getBeforePut: true,
		
		// noDataMessage: String
		//		Message to be displayed when no results exist for a query, whether at
		//		the time of the initial query or upon subsequent observed changes.
		//		Defined by _StoreMixin, but to be implemented by subclasses.
		noDataMessage: "",
		
		// loadingMessage: String
		//		Message displayed when data is loading.
		//		Defined by _StoreMixin, but to be implemented by subclasses.
		loadingMessage: "",
		
		constructor: function(){
			// Create empty objects on each instance, not the prototype
			this.query = {};
			this.queryOptions = {};
			this.dirty = {};
			this._updating = {}; // Tracks rows that are mid-update
			this._columnsWithSet = {};

			// Reset _columnsWithSet whenever column configuration is reset
			aspect.before(this, "configStructure", lang.hitch(this, function(){
				this._columnsWithSet = {};
			}));
		},
		
		postCreate: function(){
			this.inherited(arguments);
			if(this.store){
				this._updateNotifyHandle(this.store);
			}
		},
		
		destroy: function(){
			this.inherited(arguments);
			if(this._notifyHandle){
				this._notifyHandle.remove();
			}
		},
		
		_configColumn: function(column){
			// summary:
			//		Implements extension point provided by Grid to store references to
			//		any columns with `set` methods, for use during `save`.
			if (column.set){
				this._columnsWithSet[column.field] = column;
			}
		},
		
		_updateNotifyHandle: function(store){
			// summary:
			//		Unhooks any previously-existing store.notify handle, and
			//		hooks up a new one for the given store.
			
			if(this._notifyHandle){
				// Unhook notify handler from previous store
				this._notifyHandle.remove();
				delete this._notifyHandle;
			}
			if(typeof store.notify === "function"){
				this._notifyHandle = aspect.after(store, "notify",
					lang.hitch(this, "_onNotify"), true);
			}
		},
		
		_setStore: function(store, query, queryOptions){
			// summary:
			//		Assigns a new store (and optionally query/queryOptions) to the list,
			//		and tells it to refresh.
			
			this._updateNotifyHandle(store);
			
			this.store = store;
			this.dirty = {}; // discard dirty map, as it applied to a previous store
			this.set("query", query, queryOptions);
		},
		_setQuery: function(query, queryOptions){
			// summary:
			//		Assigns a new query (and optionally queryOptions) to the list,
			//		and tells it to refresh.
			
			var sort = queryOptions && queryOptions.sort;
			
			this.query = query !== undefined ? query : this.query;
			this.queryOptions = queryOptions || this.queryOptions;
			
			// If we have new sort criteria, pass them through sort
			// (which will update _sort and call refresh in itself).
			// Otherwise, just refresh.
			sort ? this.set("sort", sort) : this.refresh();
		},
		setStore: function(store, query, queryOptions){
			kernel.deprecated("setStore(...)", 'use set("store", ...) instead', "dgrid 0.4");
			this.set("store", store, query, queryOptions);
		},
		setQuery: function(query, queryOptions){
			kernel.deprecated("setQuery(...)", 'use set("query", ...) instead', "dgrid 0.4");
			this.set("query", query, queryOptions);
		},
		
		_getQueryOptions: function(){
			// summary:
			//		Get a fresh queryOptions object, also including the current sort
			var options = lang.delegate(this.queryOptions, {});
			if(this._sort.length){
				// Prevents SimpleQueryEngine from doing unnecessary "null" sorts (which can
				// change the ordering in browsers that don't use a stable sort algorithm, eg Chrome)
				options.sort = this._sort;
			}
			return options;
		},
		_getQuery: function(){
			// summary:
			//		Implemented consistent with _getQueryOptions so that if query is
			//		an object, this returns a protected (delegated) object instead of
			//		the original.
			var q = this.query;
			return typeof q == "object" && q != null ? lang.delegate(q, {}) : q;
		},
		
		_setSort: function(property, descending){
			// summary:
			//		Sort the content
			
			// prevent default storeless sort logic as long as we have a store
			if(this.store){ this._lastCollection = null; }
			this.inherited(arguments);
		},
		
		_onNotify: function(object, existingId){
			// summary:
			//		Method called when the store's notify method is called.
			
			// Call inherited in case anything was mixed in earlier
			this.inherited(arguments);
			
			// For adds/puts, check whether any observers are hooked up;
			// if not, force a refresh to properly hook one up now that there is data
			if(object && this._numObservers < 1){
				this.refresh({ keepScrollPosition: true });
			}
		},
		
		insertRow: function(object, parent, beforeNode, i, options){
			var store = this.store,
				dirty = this.dirty,
				id = store && store.getIdentity(object),
				dirtyObj;
			
			if(id in dirty && !(id in this._updating)){ dirtyObj = dirty[id]; }
			if(dirtyObj){
				// restore dirty object as delegate on top of original object,
				// to provide protection for subsequent changes as well
				object = lang.delegate(object, dirtyObj);
			}
			return this.inherited(arguments);
		},
		
		updateDirty: function(id, field, value){
			// summary:
			//		Updates dirty data of a field for the item with the specified ID.
			var dirty = this.dirty,
				dirtyObj = dirty[id];
			
			if(!dirtyObj){
				dirtyObj = dirty[id] = {};
			}
			dirtyObj[field] = value;
		},
		setDirty: function(id, field, value){
			kernel.deprecated("setDirty(...)", "use updateDirty() instead", "dgrid 0.4");
			this.updateDirty(id, field, value);
		},
		
		save: function() {
			// Keep track of the store and puts
			var self = this,
				store = this.store,
				dirty = this.dirty,
				dfd = new Deferred(), promise = dfd.promise,
				getFunc = function(id){
					// returns a function to pass as a step in the promise chain,
					// with the id variable closured
					var data;
					return (self.getBeforePut || !(data = self.row(id).data)) ?
						function(){ return store.get(id); } :
						function(){ return data; };
				};
			
			// function called within loop to generate a function for putting an item
			function putter(id, dirtyObj) {
				// Return a function handler
				return function(object) {
					var colsWithSet = self._columnsWithSet,
						updating = self._updating,
						key, data;

					if (typeof object.set === "function") {
						object.set(dirtyObj);
					} else {
						// Copy dirty props to the original, applying setters if applicable
						for(key in dirtyObj){
							object[key] = dirtyObj[key];
						}
					}

					// Apply any set methods in column definitions.
					// Note that while in the most common cases column.set is intended
					// to return transformed data for the key in question, it is also
					// possible to directly modify the object to be saved.
					for(key in colsWithSet){
						data = colsWithSet[key].set(object);
						if(data !== undefined){ object[key] = data; }
					}
					
					updating[id] = true;
					// Put it in the store, returning the result/promise
					return Deferred.when(store.put(object), function() {
						// Clear the item now that it's been confirmed updated
						delete dirty[id];
						delete updating[id];
					});
				};
			}
			
			// For every dirty item, grab the ID
			for(var id in dirty) {
				// Create put function to handle the saving of the the item
				var put = putter(id, dirty[id]);
				
				// Add this item onto the promise chain,
				// getting the item from the store first if desired.
				promise = promise.then(getFunc(id)).then(put);
			}
			
			// Kick off and return the promise representing all applicable get/put ops.
			// If the success callback is fired, all operations succeeded; otherwise,
			// save will stop at the first error it encounters.
			dfd.resolve();
			return promise;
		},
		
		revert: function(){
			// summary:
			//		Reverts any changes since the previous save.
			this.dirty = {};
			this.refresh();
		},
		
		_trackError: function(func){
			// summary:
			//		Utility function to handle emitting of error events.
			// func: Function|String
			//		A function which performs some store operation, or a String identifying
			//		a function to be invoked (sans arguments) hitched against the instance.
			//		If sync, it can return a value, but may throw an error on failure.
			//		If async, it should return a promise, which would fire the error
			//		callback on failure.
			// tags:
			//		protected
			
			var result;
			
			if(typeof func == "string"){ func = lang.hitch(this, func); }
			
			try{
				result = func();
			}catch(err){
				// report sync error
				emitError.call(this, err);
			}
			
			// wrap in when call to handle reporting of potential async error
			return Deferred.when(result, noop, lang.hitch(this, emitError));
		},
		
		newRow: function(){
			// Override to remove no data message when a new row appears.
			if(this.noDataNode){
				put(this.noDataNode, "!");
				delete this.noDataNode;
			}
			return this.inherited(arguments);
		},
		removeRow: function(rowElement, justCleanup){
			var row = {element: rowElement};
			// Check to see if we are now empty...
			if(!justCleanup && this.noDataMessage &&
					(this.up(row).element === rowElement) &&
					(this.down(row).element === rowElement)){
				// ...we are empty, so show the no data message.
				this.noDataNode = put(this.contentNode, "div.dgrid-no-data");
				this.noDataNode.innerHTML = this.noDataMessage;
			}
			return this.inherited(arguments);
		}
	});
});

},
'put-selector/put':function(){
(function(define){
var forDocument, fragmentFasterHeuristic = /[-+,> ]/; // if it has any of these combinators, it is probably going to be faster with a document fragment 
define([], forDocument = function(doc, newFragmentFasterHeuristic){
"use strict";
	// module:
	//		put-selector/put
	// summary:
	//		This module defines a fast lightweight function for updating and creating new elements
	//		terse, CSS selector-based syntax. The single function from this module creates
	// 		new DOM elements and updates existing elements. See README.md for more information.
	//	examples:
	//		To create a simple div with a class name of "foo":
	//		|	put("div.foo");
	fragmentFasterHeuristic = newFragmentFasterHeuristic || fragmentFasterHeuristic;
	var selectorParse = /(?:\s*([-+ ,<>]))?\s*(\.|!\.?|#)?([-\w%$|]+)?(?:\[([^\]=]+)=?['"]?([^\]'"]*)['"]?\])?/g,
		undefined, namespaceIndex, namespaces = false,
		doc = doc || document,
		ieCreateElement = typeof doc.createElement == "object"; // telltale sign of the old IE behavior with createElement that does not support later addition of name 
	function insertTextNode(element, text){
		element.appendChild(doc.createTextNode(text));
	}
	function put(topReferenceElement){
		var fragment, lastSelectorArg, nextSibling, referenceElement, current,
			args = arguments,
			returnValue = args[0]; // use the first argument as the default return value in case only an element is passed in
		function insertLastElement(){
			// we perform insertBefore actions after the element is fully created to work properly with 
			// <input> tags in older versions of IE that require type attributes
			//	to be set before it is attached to a parent.
			// We also handle top level as a document fragment actions in a complex creation 
			// are done on a detached DOM which is much faster
			// Also if there is a parse error, we generally error out before doing any DOM operations (more atomic) 
			if(current && referenceElement && current != referenceElement){
				(referenceElement == topReferenceElement &&
					// top level, may use fragment for faster access 
					(fragment || 
						// fragment doesn't exist yet, check to see if we really want to create it 
						(fragment = fragmentFasterHeuristic.test(argument) && doc.createDocumentFragment()))
							// any of the above fails just use the referenceElement  
							 ? fragment : referenceElement).
								insertBefore(current, nextSibling || null); // do the actual insertion
			}
		}
		for(var i = 0; i < args.length; i++){
			var argument = args[i];
			if(typeof argument == "object"){
				lastSelectorArg = false;
				if(argument instanceof Array){
					// an array
					current = doc.createDocumentFragment();
					for(var key = 0; key < argument.length; key++){
						current.appendChild(put(argument[key]));
					}
					argument = current;
				}
				if(argument.nodeType){
					current = argument;
					insertLastElement();
					referenceElement = argument;
					nextSibling = 0;
				}else{
					// an object hash
					for(var key in argument){
						current[key] = argument[key];
					}				
				}
			}else if(lastSelectorArg){
				// a text node should be created
				// take a scalar value, use createTextNode so it is properly escaped
				// createTextNode is generally several times faster than doing an escaped innerHTML insertion: http://jsperf.com/createtextnode-vs-innerhtml/2
				lastSelectorArg = false;
				insertTextNode(current, argument);
			}else{
				if(i < 1){
					// if we are starting with a selector, there is no top element
					topReferenceElement = null;
				}
				lastSelectorArg = true;
				var leftoverCharacters = argument.replace(selectorParse, function(t, combinator, prefix, value, attrName, attrValue){
					if(combinator){
						// insert the last current object
						insertLastElement();
						if(combinator == '-' || combinator == '+'){
							// + or - combinator, 
							// TODO: add support for >- as a means of indicating before the first child?
							referenceElement = (nextSibling = (current || referenceElement)).parentNode;
							current = null;
							if(combinator == "+"){
								nextSibling = nextSibling.nextSibling;
							}// else a - operator, again not in CSS, but obvious in it's meaning (create next element before the current/referenceElement)
						}else{
							if(combinator == "<"){
								// parent combinator (not really in CSS, but theorized, and obvious in it's meaning)
								referenceElement = current = (current || referenceElement).parentNode;
							}else{
								if(combinator == ","){
									// comma combinator, start a new selector
									referenceElement = topReferenceElement;
								}else if(current){
									// else descendent or child selector (doesn't matter, treated the same),
									referenceElement = current;
								}
								current = null;
							}
							nextSibling = 0;
						}
						if(current){
							referenceElement = current;
						}
					}
					var tag = !prefix && value;
					if(tag || (!current && (prefix || attrName))){
						if(tag == "$"){
							// this is a variable to be replaced with a text node
							insertTextNode(referenceElement, args[++i]);
						}else{
							// Need to create an element
							tag = tag || put.defaultTag;
							var ieInputName = ieCreateElement && args[i +1] && args[i +1].name;
							if(ieInputName){
								// in IE, we have to use the crazy non-standard createElement to create input's that have a name 
								tag = '<' + tag + ' name="' + ieInputName + '">';
							}
							// we swtich between creation methods based on namespace usage
							current = namespaces && ~(namespaceIndex = tag.indexOf('|')) ?
								doc.createElementNS(namespaces[tag.slice(0, namespaceIndex)], tag.slice(namespaceIndex + 1)) : 
								doc.createElement(tag);
						}
					}
					if(prefix){
						if(value == "$"){
							value = args[++i];
						}
						if(prefix == "#"){
							// #id was specified
							current.id = value;
						}else{
							// we are in the className addition and removal branch
							var currentClassName = current.className;
							// remove the className (needed for addition or removal)
							// see http://jsperf.com/remove-class-name-algorithm/2 for some tests on this
							var removed = currentClassName && (" " + currentClassName + " ").replace(" " + value + " ", " ");
							if(prefix == "."){
								// addition, add the className
								current.className = currentClassName ? (removed + value).substring(1) : value;
							}else{
								// else a '!' class removal
								if(argument == "!"){
									var parentNode;
									// special signal to delete this element
									if(ieCreateElement){
										// use the ol' innerHTML trick to get IE to do some cleanup
										put("div", current, '<').innerHTML = "";
									}else if(parentNode = current.parentNode){ // intentional assigment
										// use a faster, and more correct (for namespaced elements) removal (http://jsperf.com/removechild-innerhtml)
										parentNode.removeChild(current);
									}
								}else{
									// we already have removed the class, just need to trim
									removed = removed.substring(1, removed.length - 1);
									// only assign if it changed, this can save a lot of time
									if(removed != currentClassName){
										current.className = removed;
									}
								}
							}
							// CSS class removal
						}
					}
					if(attrName){
						if(attrValue == "$"){
							attrValue = args[++i];
						}
						// [name=value]
						if(attrName == "style"){
							// handle the special case of setAttribute not working in old IE
							current.style.cssText = attrValue;
						}else{
							var method = attrName.charAt(0) == "!" ? (attrName = attrName.substring(1)) && 'removeAttribute' : 'setAttribute';
							attrValue = attrValue === '' ? attrName : attrValue;
							// determine if we need to use a namespace
							namespaces && ~(namespaceIndex = attrName.indexOf('|')) ?
								current[method + "NS"](namespaces[attrName.slice(0, namespaceIndex)], attrName.slice(namespaceIndex + 1), attrValue) :
								current[method](attrName, attrValue);
						}
					}
					return '';
				});
				if(leftoverCharacters){
					throw new SyntaxError("Unexpected char " + leftoverCharacters + " in " + argument);
				}
				insertLastElement();
				referenceElement = returnValue = current || referenceElement;
			}
		}
		if(topReferenceElement && fragment){
			// we now insert the top level elements for the fragment if it exists
			topReferenceElement.appendChild(fragment);
		}
		return returnValue;
	}
	put.addNamespace = function(name, uri){
		if(doc.createElementNS){
			(namespaces || (namespaces = {}))[name] = uri;
		}else{
			// for old IE
			doc.namespaces.add(name, uri);
		}
	};
	put.defaultTag = "div";
	put.forDocument = forDocument;
	return put;
});
})(function(id, deps, factory){
	factory = factory || deps;
	if(typeof define === "function"){
		// AMD loader
		define([], function(){
			return factory();
		});
	}else if(typeof window == "undefined"){
		// server side JavaScript, probably (hopefully) NodeJS
		require("./node-html")(module, factory);
	}else{
		// plain script in a browser
		put = factory();
	}
});

},
'dojo/promise/instrumentation':function(){
define([
	"./tracer",
	"../has",
	"../_base/lang",
	"../_base/array"
], function(tracer, has, lang, arrayUtil){
	function logError(error, rejection, deferred){
		var stack = "";
		if(error && error.stack){
			stack += error.stack;
		}
		if(rejection && rejection.stack){
			stack += "\n    ----------------------------------------\n    rejected" + rejection.stack.split("\n").slice(1).join("\n").replace(/^\s+/, " ");
		}
		if(deferred && deferred.stack){
			stack += "\n    ----------------------------------------\n" + deferred.stack;
		}
		console.error(error, stack);
	}

	function reportRejections(error, handled, rejection, deferred){
		if(!handled){
			logError(error, rejection, deferred);
		}
	}

	var errors = [];
	var activeTimeout = false;
	var unhandledWait = 1000;
	function trackUnhandledRejections(error, handled, rejection, deferred){
		if(handled){
			arrayUtil.some(errors, function(obj, ix){
				if(obj.error === error){
					errors.splice(ix, 1);
					return true;
				}
			});
		}else if(!arrayUtil.some(errors, function(obj){ return obj.error === error; })){
			errors.push({
				error: error,
				rejection: rejection,
				deferred: deferred,
				timestamp: new Date().getTime()
			});
		}

		if(!activeTimeout){
			activeTimeout = setTimeout(logRejected, unhandledWait);
		}
	}

	function logRejected(){
		var now = new Date().getTime();
		var reportBefore = now - unhandledWait;
		errors = arrayUtil.filter(errors, function(obj){
			if(obj.timestamp < reportBefore){
				logError(obj.error, obj.rejection, obj.deferred);
				return false;
			}
			return true;
		});

		if(errors.length){
			activeTimeout = setTimeout(logRejected, errors[0].timestamp + unhandledWait - now);
		}else{
			activeTimeout = false;
		}
	}

	return function(Deferred){
		// summary:
		//		Initialize instrumentation for the Deferred class.
		// description:
		//		Initialize instrumentation for the Deferred class.
		//		Done automatically by `dojo/Deferred` if the
		//		`deferredInstrumentation` and `useDeferredInstrumentation`
		//		config options are set.
		//
		//		Sets up `dojo/promise/tracer` to log to the console.
		//
		//		Sets up instrumentation of rejected deferreds so unhandled
		//		errors are logged to the console.

		var usage = has("config-useDeferredInstrumentation");
		if(usage){
			tracer.on("resolved", lang.hitch(console, "log", "resolved"));
			tracer.on("rejected", lang.hitch(console, "log", "rejected"));
			tracer.on("progress", lang.hitch(console, "log", "progress"));

			var args = [];
			if(typeof usage === "string"){
				args = usage.split(",");
				usage = args.shift();
			}
			if(usage === "report-rejections"){
				Deferred.instrumentRejected = reportRejections;
			}else if(usage === "report-unhandled-rejections" || usage === true || usage === 1){
				Deferred.instrumentRejected = trackUnhandledRejections;
				unhandledWait = parseInt(args[0], 10) || unhandledWait;
			}else{
				throw new Error("Unsupported instrumentation usage <" + usage + ">");
			}
		}
	};
});

},
'dgrid/OnDemandList':function(){
define(["./List", "./_StoreMixin", "dojo/_base/declare", "dojo/_base/lang", "dojo/_base/Deferred", "dojo/on", "./util/misc", "put-selector/put"],
function(List, _StoreMixin, declare, lang, Deferred, listen, miscUtil, put){

return declare([List, _StoreMixin], {
	// summary:
	//		Extends List to include virtual scrolling functionality, querying a
	//		dojo/store instance for the appropriate range when the user scrolls.
	
	// minRowsPerPage: Integer
	//		The minimum number of rows to request at one time.
	minRowsPerPage: 25,
	
	// maxRowsPerPage: Integer
	//		The maximum number of rows to request at one time.
	maxRowsPerPage: 250,
	
	// maxEmptySpace: Integer
	//		Defines the maximum size (in pixels) of unrendered space below the
	//		currently-rendered rows. Setting this to less than Infinity can be useful if you
	//		wish to limit the initial vertical scrolling of the grid so that the scrolling is
	// 		not excessively sensitive. With very large grids of data this may make scrolling
	//		easier to use, albiet it can limit the ability to instantly scroll to the end.
	maxEmptySpace: Infinity,	
	
	// bufferRows: Integer
	//	  The number of rows to keep ready on each side of the viewport area so that the user can
	//	  perform local scrolling without seeing the grid being built. Increasing this number can
	//	  improve perceived performance when the data is being retrieved over a slow network.
	bufferRows: 10,
	
	// farOffRemoval: Integer
	//		Defines the minimum distance (in pixels) from the visible viewport area
	//		rows must be in order to be removed.  Setting to Infinity causes rows
	//		to never be removed.
	farOffRemoval: 2000,
	
	// queryRowsOverlap: Integer
	//		Indicates the number of rows to overlap queries. This helps keep
	//		continuous data when underlying data changes (and thus pages don't
	//		exactly align)
	queryRowsOverlap: 1,
	
	// pagingMethod: String
	//		Method (from dgrid/util/misc) to use to either throttle or debounce
	//		requests.  Default is "debounce" which will cause the grid to wait until
	//		the user pauses scrolling before firing any requests; can be set to
	//		"throttleDelayed" instead to progressively request as the user scrolls,
	//		which generally incurs more overhead but might appear more responsive.
	pagingMethod: "debounce",
	
	// pagingDelay: Integer
	//		Indicates the delay (in milliseconds) imposed upon pagingMethod, to wait
	//		before paging in more data on scroll events. This can be increased to
	//		reduce client-side overhead or the number of requests sent to a server.
	pagingDelay: miscUtil.defaultDelay,
	
	// keepScrollPosition: Boolean
	//		When refreshing the list, controls whether the scroll position is
	//		preserved, or reset to the top.  This can also be overridden for
	//		specific calls to refresh.
	keepScrollPosition: false,
	
	rowHeight: 22,
	
	postCreate: function(){
		this.inherited(arguments);
		var self = this;
		// check visibility on scroll events
		listen(this.bodyNode, "scroll",
			miscUtil[this.pagingMethod](function(event){ self._processScroll(event); },
				null, this.pagingDelay));
	},
	
	renderQuery: function(query, preloadNode, options){
		// summary:
		//		Creates a preload node for rendering a query into, and executes the query
		//		for the first page of data. Subsequent data will be downloaded as it comes
		//		into view.
		var self = this,
			preload = {
				query: query,
				count: 0,
				node: preloadNode,
				options: options
			},
			priorPreload = this.preload,
			results;
		
		if(!preloadNode){
			// Initial query; set up top and bottom preload nodes
			var topPreload = {
				node: put(this.contentNode, "div.dgrid-preload", {
					rowIndex: 0
				}),
				count: 0,
				query: query,
				next: preload,
				options: options
			};
			topPreload.node.style.height = "0";
			preload.node = preloadNode = put(this.contentNode, "div.dgrid-preload");
			preload.previous = topPreload;
		}
		// this preload node is used to represent the area of the grid that hasn't been
		// downloaded yet
		preloadNode.rowIndex = this.minRowsPerPage;

		if(priorPreload){
			// the preload nodes (if there are multiple) are represented as a linked list, need to insert it
			if((preload.next = priorPreload.next) && 
					// check to make sure that the current scroll position is below this preload
					this.bodyNode.scrollTop >= priorPreload.node.offsetTop){ 
				// the prior preload is above/before in the linked list
				preload.previous = priorPreload;
			}else{
				// the prior preload is below/after in the linked list
				preload.next = priorPreload;
				preload.previous = priorPreload.previous;
			}
			// adjust the previous and next links so the linked list is proper
			preload.previous.next = preload;
			preload.next.previous = preload; 
		}else{
			this.preload = preload;
		}
		
		var loadingNode = put(preloadNode, "-div.dgrid-loading"),
			innerNode = put(loadingNode, "div.dgrid-below");
		innerNode.innerHTML = this.loadingMessage;

		function errback(err) {
			// Used as errback for when calls;
			// remove the loadingNode and re-throw if an error was passed
			put(loadingNode, "!");
			
			if(err){
				if(self._refreshDeferred){
					self._refreshDeferred.reject(err);
					delete self._refreshDeferred;
				}
				throw err;
			}
		}

		// Establish query options, mixing in our own.
		// (The getter returns a delegated object, so simply using mixin is safe.)
		options = lang.mixin(this.get("queryOptions"), options, 
			{start: 0, count: this.minRowsPerPage, query: query});
		
		// Protect the query within a _trackError call, but return the QueryResults
		this._trackError(function(){ return results = query(options); });
		
		if(typeof results === "undefined"){
			// Synchronous error occurred (but was caught by _trackError)
			errback();
			return;
		}
		
		// Render the result set
		Deferred.when(self.renderArray(results, preloadNode, options), function(trs){
			var total = typeof results.total === "undefined" ?
				results.length : results.total;
			return Deferred.when(total, function(total){
				var trCount = trs.length,
					parentNode = preloadNode.parentNode,
					noDataNode = self.noDataNode;
				
				put(loadingNode, "!");
				self._total = total;
				// now we need to adjust the height and total count based on the first result set
				if(total === 0){
					if(noDataNode){
						put(noDataNode, "!");
						delete self.noDataNode;
					}
					self.noDataNode = noDataNode = put("div.dgrid-no-data");
					parentNode.insertBefore(noDataNode, self._getFirstRowSibling(parentNode));
					noDataNode.innerHTML = self.noDataMessage;
				}
				var height = 0;
				for(var i = 0; i < trCount; i++){
					height += self._calcRowHeight(trs[i]);
				}
				// only update rowHeight if we actually got results and are visible
				if(trCount && height){ self.rowHeight = height / trCount; }
				
				total -= trCount;
				preload.count = total;
				preloadNode.rowIndex = trCount;
				if(total){
					preloadNode.style.height = Math.min(total * self.rowHeight, self.maxEmptySpace) + "px";
				}else{
					// if total is 0, IE quirks mode can't handle 0px height for some reason, I don't know why, but we are setting display: none for now
					preloadNode.style.display = "none";
				}
				
				if (self._previousScrollPosition) {
					// Restore position after a refresh operation w/ keepScrollPosition
					self.scrollTo(self._previousScrollPosition);
					delete self._previousScrollPosition;
				}
				
				// Redo scroll processing in case the query didn't fill the screen,
				// or in case scroll position was restored
				self._processScroll();
				
				// If _refreshDeferred is still defined after calling _processScroll,
				// resolve it now (_processScroll will remove it and resolve it itself
				// otherwise)
				if(self._refreshDeferred){
					self._refreshDeferred.resolve(results);
					delete self._refreshDeferred;
				}
				
				return trs;
			}, errback);
		}, errback);
		
		return results;
	},
	
	refresh: function(options){
		// summary:
		//		Refreshes the contents of the grid.
		// options: Object?
		//		Optional object, supporting the following parameters:
		//		* keepScrollPosition: like the keepScrollPosition instance property;
		//			specifying it in the options here will override the instance
		//			property's value for this specific refresh call only.
		
		var self = this,
			keep = (options && options.keepScrollPosition),
			dfd, results;
		
		// Fall back to instance property if option is not defined
		if(typeof keep === "undefined"){ keep = this.keepScrollPosition; }
		
		// Store scroll position to be restored after new total is received
		if(keep){ this._previousScrollPosition = this.getScrollPosition(); }
		
		this.inherited(arguments);
		if(this.store){
			// render the query
			dfd = this._refreshDeferred = new Deferred();
			
			// renderQuery calls _trackError internally
			results = self.renderQuery(function(queryOptions){
				return self.store.query(self.query, queryOptions);
			});
			if(typeof results === "undefined"){
				// Synchronous error occurred; reject the refresh promise.
				dfd.reject();
			}
			
			// Internally, _refreshDeferred will always be resolved with an object
			// containing `results` (QueryResults) and `rows` (the rendered rows);
			// externally the promise will resolve simply with the QueryResults, but
			// the event will be emitted with both under respective properties.
			return dfd.then(function(results){
				// Emit on a separate turn to enable event to be used consistently for
				// initial render, regardless of whether the backing store is async
				setTimeout(function() {
					listen.emit(self.domNode, "dgrid-refresh-complete", {
						bubbles: true,
						cancelable: false,
						grid: self,
						results: results // QueryResults object (may be a wrapped promise)
					});
				}, 0);
				
				// Delete the Deferred immediately so nothing tries to re-resolve
				delete self._refreshDeferred;
				
				// Resolve externally with just the QueryResults
				return results;
			}, function(err){
				delete self._refreshDeferred;
				throw err;
			});
		}
	},
	
	resize: function(){
		this.inherited(arguments);
		this._processScroll();
	},

	_getFirstRowSibling: function(container){
		// summary:
		//		Returns the DOM node that a new row should be inserted before
		//		when there are no other rows in the current result set.
		//		In the case of OnDemandList, this will always be the last child
		//		of the container (which will be a trailing preload node).
		return container.lastChild;
	},
	
	_calcRowHeight: function(rowElement){
		// summary:
		//		Calculate the height of a row. This is a method so it can be overriden for
		//		plugins that add connected elements to a row, like the tree
		
		var sibling = rowElement.previousSibling;
		return sibling && sibling.offsetTop != rowElement.offsetTop ?
			rowElement.offsetHeight : 0;
	},
	
	lastScrollTop: 0,
	_processScroll: function(evt){
		// summary:
		//		Checks to make sure that everything in the viewable area has been
		//		downloaded, and triggering a request for the necessary data when needed.
		var grid = this,
			scrollNode = grid.bodyNode,
			// grab current visible top from event if provided, otherwise from node
			visibleTop = (evt && evt.scrollTop) || this.getScrollPosition().y,
			visibleBottom = scrollNode.offsetHeight + visibleTop,
			priorPreload, preloadNode, preload = grid.preload,
			lastScrollTop = grid.lastScrollTop,
			requestBuffer = grid.bufferRows * grid.rowHeight,
			searchBuffer = requestBuffer - grid.rowHeight, // Avoid rounding causing multiple queries
			// References related to emitting dgrid-refresh-complete if applicable
			refreshDfd,
			lastResults,
			lastRows,
			preloadSearchNext = true;
		
		// XXX: I do not know why this happens.
		// munging the actual location of the viewport relative to the preload node by a few pixels in either
		// direction is necessary because at least WebKit on Windows seems to have an error that causes it to
		// not quite get the entire element being focused in the viewport during keyboard navigation,
		// which means it becomes impossible to load more data using keyboard navigation because there is
		// no more data to scroll to to trigger the fetch.
		// 1 is arbitrary and just gets it to work correctly with our current test cases; don’t wanna go
		// crazy and set it to a big number without understanding more about what is going on.
		// wondering if it has to do with border-box or something, but changing the border widths does not
		// seem to make it break more or less, so I do not know…
		var mungeAmount = 1;
		
		grid.lastScrollTop = visibleTop;

		function removeDistantNodes(preload, distanceOff, traversal, below){
			// we check to see the the nodes are "far off"
			var farOffRemoval = grid.farOffRemoval,
				preloadNode = preload.node;
			// by checking to see if it is the farOffRemoval distance away
			if(distanceOff > 2 * farOffRemoval){
				// ok, there is preloadNode that is far off, let's remove rows until we get to in the current viewpoint
				var row, nextRow = preloadNode[traversal];
				var reclaimedHeight = 0;
				var count = 0;
				var toDelete = [];
				while((row = nextRow)){
					var rowHeight = grid._calcRowHeight(row);
					if(reclaimedHeight + rowHeight + farOffRemoval > distanceOff || (nextRow.className.indexOf("dgrid-row") < 0 && nextRow.className.indexOf("dgrid-loading") < 0)){
						// we have reclaimed enough rows or we have gone beyond grid rows, let's call it good
						break;
					}
					var nextRow = row[traversal]; // have to do this before removing it
					reclaimedHeight += rowHeight;
					count += row.count || 1;
					// we just do cleanup here, as we will do a more efficient node destruction in the setTimeout below
					grid.removeRow(row, true);
					toDelete.push(row);
				}
				// now adjust the preloadNode based on the reclaimed space
				preload.count += count;
				if(below){
					preloadNode.rowIndex -= count;
					adjustHeight(preload);
				}else{
					// if it is above, we can calculate the change in exact row changes, which we must do to not mess with the scrolling
					preloadNode.style.height = (preloadNode.offsetHeight + reclaimedHeight) + "px";
				}
				// we remove the elements after expanding the preload node so that the contraction doesn't alter the scroll position
				var trashBin = put("div", toDelete);
				setTimeout(function(){
					// we can defer the destruction until later
					put(trashBin, "!");
				},1);
			}
		}
		
		function adjustHeight(preload, noMax){
			preload.node.style.height = Math.min(preload.count * grid.rowHeight, noMax ? Infinity : grid.maxEmptySpace) + "px";
		}
		function traversePreload(preload, moveNext){
			do{
				preload = moveNext ? preload.next : preload.previous;
			}while(preload && !preload.node.offsetWidth);// skip past preloads that are not currently connected
			return preload;
		}
		while(preload && !preload.node.offsetWidth){
			// skip past preloads that are not currently connected
			preload = preload.previous;
		}
		// there can be multiple preloadNodes (if they split, or multiple queries are created),
		//	so we can traverse them until we find whatever is in the current viewport, making
		//	sure we don't backtrack
		while(preload && preload != priorPreload){
			priorPreload = grid.preload;
			grid.preload = preload;
			preloadNode = preload.node;
			var preloadTop = preloadNode.offsetTop;
			var preloadHeight;
			
			if(visibleBottom + mungeAmount + searchBuffer < preloadTop){
				// the preload is below the line of sight
				preload = traversePreload(preload, (preloadSearchNext = false));
			}else if(visibleTop - mungeAmount - searchBuffer > (preloadTop + (preloadHeight = preloadNode.offsetHeight))){
				// the preload is above the line of sight
				preload = traversePreload(preload, (preloadSearchNext = true));
			}else{
				// the preload node is visible, or close to visible, better show it
				var offset = ((preloadNode.rowIndex ? visibleTop - requestBuffer : visibleBottom) - preloadTop) / grid.rowHeight;
				var count = (visibleBottom - visibleTop + 2 * requestBuffer) / grid.rowHeight;
				// utilize momentum for predictions
				var momentum = Math.max(Math.min((visibleTop - lastScrollTop) * grid.rowHeight, grid.maxRowsPerPage/2), grid.maxRowsPerPage/-2);
				count += Math.min(Math.abs(momentum), 10);
				if(preloadNode.rowIndex == 0){
					// at the top, adjust from bottom to top
					offset -= count;
				}
				offset = Math.max(offset, 0);
				if(offset < 10 && offset > 0 && count + offset < grid.maxRowsPerPage){
					// connect to the top of the preloadNode if possible to avoid excessive adjustments
					count += Math.max(0, offset);
					offset = 0;
				}
				count = Math.min(Math.max(count, grid.minRowsPerPage),
									grid.maxRowsPerPage, preload.count);
				
				if(count == 0){
					preload = traversePreload(preload, preloadSearchNext);
					continue;
				}
				
				count = Math.ceil(count);
				offset = Math.min(Math.floor(offset), preload.count - count);
				var options = lang.mixin(grid.get("queryOptions"), preload.options);
				preload.count -= count;
				var beforeNode = preloadNode,
					keepScrollTo, queryRowsOverlap = grid.queryRowsOverlap,
					below = preloadNode.rowIndex > 0 && preload; 
				if(below){
					// add new rows below
					var previous = preload.previous;
					if(previous){
						removeDistantNodes(previous, visibleTop - (previous.node.offsetTop + previous.node.offsetHeight), 'nextSibling');
						if(offset > 0 && previous.node == preloadNode.previousSibling){
							// all of the nodes above were removed
							offset = Math.min(preload.count, offset);
							preload.previous.count += offset;
							adjustHeight(preload.previous, true);
							preloadNode.rowIndex += offset;
							queryRowsOverlap = 0;
						}else{
							count += offset;
						}
						preload.count -= offset;
					}
					options.start = preloadNode.rowIndex - queryRowsOverlap;
					options.count = Math.min(count + queryRowsOverlap, grid.maxRowsPerPage);
					preloadNode.rowIndex = options.start + options.count;
				}else{
					// add new rows above
					if(preload.next){
						// remove out of sight nodes first
						removeDistantNodes(preload.next, preload.next.node.offsetTop - visibleBottom, 'previousSibling', true);
						var beforeNode = preloadNode.nextSibling;
						if(beforeNode == preload.next.node){
							// all of the nodes were removed, can position wherever we want
							preload.next.count += preload.count - offset;
							preload.next.node.rowIndex = offset + count;
							adjustHeight(preload.next);
							preload.count = offset;
							queryRowsOverlap = 0;
						}else{
							keepScrollTo = true;
						}
						
					}
					options.start = preload.count;
					options.count = Math.min(count + queryRowsOverlap, grid.maxRowsPerPage);
				}
				if(keepScrollTo && beforeNode && beforeNode.offsetWidth){
					keepScrollTo = beforeNode.offsetTop;
				}

				adjustHeight(preload);
				// create a loading node as a placeholder while the data is loaded
				var loadingNode = put(beforeNode, "-div.dgrid-loading[style=height:" + count * grid.rowHeight + "px]"),
					innerNode = put(loadingNode, "div.dgrid-" + (below ? "below" : "above"));
				innerNode.innerHTML = grid.loadingMessage;
				loadingNode.count = count;
				// use the query associated with the preload node to get the next "page"
				options.query = preload.query;
				
				// Avoid spurious queries (ideally this should be unnecessary...)
				if(options.start > grid._total || options.count < 0){
					console.log("Skipping query", options, grid._total);
					continue;
				}
				
				// Query now to fill in these rows.
				// Keep _trackError-wrapped results separate, since if results is a
				// promise, it will lose QueryResults functions when chained by `when`
				var results = preload.query(options),
					trackedResults = grid._trackError(function(){ return results; });
				
				if(trackedResults === undefined){
					// Sync query failed
					put(loadingNode, "!");
					return;
				}

				// Isolate the variables in case we make multiple requests
				// (which can happen if we need to render on both sides of an island of already-rendered rows)
				(function(loadingNode, scrollNode, below, keepScrollTo, results){
					lastRows = Deferred.when(grid.renderArray(results, loadingNode, options), function(rows){
						lastResults = results;
						
						// can remove the loading node now
						beforeNode = loadingNode.nextSibling;
						put(loadingNode, "!");
						if(keepScrollTo && beforeNode && beforeNode.offsetWidth){ // beforeNode may have been removed if the query results loading node was a removed as a distant node before rendering 
							// if the preload area above the nodes is approximated based on average
							// row height, we may need to adjust the scroll once they are filled in
							// so we don't "jump" in the scrolling position
							var pos = grid.getScrollPosition();
							grid.scrollTo({
								// Since we already had to query the scroll position,
								// include x to avoid TouchScroll querying it again on its end.
								x: pos.x,
								y: pos.y + beforeNode.offsetTop - keepScrollTo,
								// Don't kill momentum mid-scroll (for TouchScroll only).
								preserveMomentum: true
							});
						}
						
						Deferred.when(results.total || results.length, function(total){
							grid._total = total;
							if(below){
								// if it is below, we will use the total from the results to update
								// the count of the last preload in case the total changes as later pages are retrieved
								// (not uncommon when total counts are estimated for db perf reasons)
								
								// recalculate the count
								below.count = total - below.node.rowIndex;
								// readjust the height
								adjustHeight(below);
							}
						});
						
						// make sure we have covered the visible area
						grid._processScroll();
						return rows;
					}, function (e) {
						put(loadingNode, "!");
						throw e;
					});
				}).call(this, loadingNode, scrollNode, below, keepScrollTo, results);
				preload = preload.previous;
			}
		}
		
		// After iterating, if additional requests have been made mid-refresh,
		// resolve the refresh promise based on the latest results obtained
		if (lastRows && (refreshDfd = this._refreshDeferred)) {
			delete this._refreshDeferred;
			Deferred.when(lastRows, function() {
				refreshDfd.resolve(lastResults);
			});
		}
	},

	removeRow: function(rowElement, justCleanup){
		function chooseIndex(index1, index2){
			return index1 != null ? index1 : index2;
		}

		if(rowElement){
			// Clean up observers that need to be cleaned up.
			var previousNode = rowElement.previousSibling,
				nextNode = rowElement.nextSibling,
				prevIndex = previousNode && chooseIndex(previousNode.observerIndex, previousNode.previousObserverIndex),
				nextIndex = nextNode && chooseIndex(nextNode.observerIndex, nextNode.nextObserverIndex),
				thisIndex = rowElement.observerIndex;

			// Clear the observerIndex on the node being removed so it will not be considered any longer.
			rowElement.observerIndex = undefined;
			if(justCleanup){
				// Save the indexes from the siblings for future calls to removeRow.
				rowElement.nextObserverIndex = nextIndex;
				rowElement.previousObserverIndex = prevIndex;
			}

			// Is this row's observer index different than those on either side?
			if(thisIndex > -1 && thisIndex !== prevIndex && thisIndex !== nextIndex){
				// This is the last row that references the observer index.  Cancel the observer.
				var observers = this.observers;
				var observer = observers[thisIndex];
				if(observer){
					observer.cancel();
					this._numObservers--;
					observers[thisIndex] = 0; // remove it so we don't call cancel twice
				}
			}
		}
		// Finish the row removal.
		this.inherited(arguments);
	}
});

});

},
'dojo/promise/tracer':function(){
define([
	"../_base/lang",
	"./Promise",
	"../Evented"
], function(lang, Promise, Evented){
	"use strict";

	// module:
	//		dojo/promise/tracer

	/*=====
	return {
		// summary:
		//		Trace promise fulfillment.
		// description:
		//		Trace promise fulfillment. Calling `.trace()` or `.traceError()` on a
		//		promise enables tracing. Will emit `resolved`, `rejected` or `progress`
		//		events.

		on: function(type, listener){
			// summary:
			//		Subscribe to traces.
			// description:
			//		See `dojo/Evented#on()`.
			// type: String
			//		`resolved`, `rejected`, or `progress`
			// listener: Function
			//		The listener is passed the traced value and any arguments
			//		that were used with the `.trace()` call.
		}
	};
	=====*/

	var evented = new Evented;
	var emit = evented.emit;
	evented.emit = null;
	// Emit events asynchronously since they should not change the promise state.
	function emitAsync(args){
		setTimeout(function(){
			emit.apply(evented, args);
		}, 0);
	}

	Promise.prototype.trace = function(){
		// summary:
		//		Trace the promise.
		// description:
		//		Tracing allows you to transparently log progress,
		//		resolution and rejection of promises, without affecting the
		//		promise itself. Any arguments passed to `trace()` are
		//		emitted in trace events. See `dojo/promise/tracer` on how
		//		to handle traces.
		// returns: dojo/promise/Promise
		//		The promise instance `trace()` is called on.

		var args = lang._toArray(arguments);
		this.then(
			function(value){ emitAsync(["resolved", value].concat(args)); },
			function(error){ emitAsync(["rejected", error].concat(args)); },
			function(update){ emitAsync(["progress", update].concat(args)); }
		);
		return this;
	};

	Promise.prototype.traceRejected = function(){
		// summary:
		//		Trace rejection of the promise.
		// description:
		//		Tracing allows you to transparently log progress,
		//		resolution and rejection of promises, without affecting the
		//		promise itself. Any arguments passed to `trace()` are
		//		emitted in trace events. See `dojo/promise/tracer` on how
		//		to handle traces.
		// returns: dojo/promise/Promise
		//		The promise instance `traceRejected()` is called on.

		var args = lang._toArray(arguments);
		this.otherwise(function(error){
			emitAsync(["rejected", error].concat(args));
		});
		return this;
	};

	return evented;
});

},
'dgrid/List':function(){
define(["dojo/_base/kernel", "dojo/_base/declare", "dojo/on", "dojo/has", "./util/misc", "dojo/has!touch?./TouchScroll", "xstyle/has-class", "put-selector/put", "dojo/_base/sniff", "xstyle/css!./css/dgrid.css"], 
function(kernel, declare, listen, has, miscUtil, TouchScroll, hasClass, put){
	// Add user agent/feature CSS classes 
	hasClass("mozilla", "opera", "webkit", "ie", "ie-6", "ie-6-7", "quirks", "no-quirks", "touch");
	
	var oddClass = "dgrid-row-odd",
		evenClass = "dgrid-row-even",
		scrollbarWidth, scrollbarHeight;
	
	function byId(id){
		return document.getElementById(id);
	}
	
	function getScrollbarSize(node, dimension){
		// Used by has tests for scrollbar width/height
		var body = document.body,
			size;
		
		put(body, node, ".dgrid-scrollbar-measure");
		size = node["offset" + dimension] - node["client" + dimension];
		
		put(node, "!dgrid-scrollbar-measure");
		body.removeChild(node);
		
		return size;
	}
	has.add("dom-scrollbar-width", function(global, doc, element){
		return getScrollbarSize(element, "Width");
	});
	has.add("dom-scrollbar-height", function(global, doc, element){
		return getScrollbarSize(element, "Height");
	});
	
	// var and function for autogenerating ID when one isn't provided
	var autogen = 0;
	function generateId(){
		return "dgrid_" + autogen++;
	}
	
	// common functions for class and className setters/getters
	// (these are run in instance context)
	var spaceRx = / +/g;
	function setClass(cls){
		// Format input appropriately for use with put...
		var putClass = cls ? "." + cls.replace(spaceRx, ".") : "";
		
		// Remove any old classes, and add new ones.
		if(this._class){
			putClass = "!" + this._class.replace(spaceRx, "!") + putClass;
		}
		put(this.domNode, putClass);
		
		// Store for later retrieval/removal.
		this._class = cls;
	}
	function getClass(){
		return this._class;
	}
	
	// window resize event handler, run in context of List instance
	var winResizeHandler = has("ie") < 7 && !has("quirks") ? function(){
		// IE6 triggers window.resize on any element resize;
		// avoid useless calls (and infinite loop if height: auto).
		// The measurement logic here is based on dojo/window logic.
		var root, w, h, dims;
		
		if(!this._started){ return; } // no sense calling resize yet
		
		root = document.documentElement;
		w = root.clientWidth;
		h = root.clientHeight;
		dims = this._prevWinDims || [];
		if(dims[0] !== w || dims[1] !== h){
			this.resize();
			this._prevWinDims = [w, h];
		}
	} :
	function(){
		if(this._started){ this.resize(); }
	};
	
	// Desktop versions of functions, deferred to when there is no touch support,
	// or when the useTouchScroll instance property is set to false
	
	function desktopGetScrollPosition(){
		return {
			x: this.bodyNode.scrollLeft,
			y: this.bodyNode.scrollTop
		};
	}
	
	function desktopScrollTo(options){
		if(typeof options.x !== "undefined"){
			this.bodyNode.scrollLeft = options.x;
		}
		if(typeof options.y !== "undefined"){
			this.bodyNode.scrollTop = options.y;
		}
	}
	
	return declare(has("touch") ? TouchScroll : null, {
		tabableHeader: false,
		// showHeader: Boolean
		//		Whether to render header (sub)rows.
		showHeader: false,
		
		// showFooter: Boolean
		//		Whether to render footer area.  Extensions which display content
		//		in the footer area should set this to true.
		showFooter: false,
		
		// maintainOddEven: Boolean
		//		Whether to maintain the odd/even classes when new rows are inserted.
		//		This can be disabled to improve insertion performance if odd/even styling is not employed.
		maintainOddEven: true,
		
		// cleanAddedRules: Boolean
		//		Whether to track rules added via the addCssRule method to be removed
		//		when the list is destroyed.  Note this is effective at the time of
		//		the call to addCssRule, not at the time of destruction.
		cleanAddedRules: true,
		
		// useTouchScroll: Boolean
		//		If touch support is available, this determines whether to
		//		incorporate logic from the TouchScroll module (at the expense of
		//		normal desktop/mouse or native mobile scrolling functionality).
		useTouchScroll: true,
		
		postscript: function(params, srcNodeRef){
			// perform setup and invoke create in postScript to allow descendants to
			// perform logic before create/postCreate happen (a la dijit/_WidgetBase)
			var grid = this;
			
			(this._Row = function(id, object, element){
				this.id = id;
				this.data = object;
				this.element = element;
			}).prototype.remove = function(){
				grid.removeRow(this.element);
			};
			
			if(srcNodeRef){
				// normalize srcNodeRef and store on instance during create process.
				// Doing this in postscript is a bit earlier than dijit would do it,
				// but allows subclasses to access it pre-normalized during create.
				this.srcNodeRef = srcNodeRef =
					srcNodeRef.nodeType ? srcNodeRef : byId(srcNodeRef);
			}
			this.create(params, srcNodeRef);
		},
		listType: "list",
		
		create: function(params, srcNodeRef){
			var domNode = this.domNode = srcNodeRef || put("div"),
				cls;
			
			if(params){
				this.params = params;
				declare.safeMixin(this, params);
				
				// Check for initial class or className in params or on domNode
				cls = params["class"] || params.className || domNode.className;
				
				// handle sort param - TODO: revise @ 0.4 when _sort -> sort
				this._sort = params.sort || [];
				delete this.sort; // ensure back-compat method isn't shadowed
			}else{
				this._sort = [];
			}
			
			// ensure arrays and hashes are initialized
			this.observers = [];
			this._numObservers = 0;
			this._listeners = [];
			this._rowIdToObject = {};
			
			this.postMixInProperties && this.postMixInProperties();
			
			// Apply id to widget and domNode,
			// from incoming node, widget params, or autogenerated.
			this.id = domNode.id = domNode.id || this.id || generateId();
			
			// Perform initial rendering, and apply classes if any were specified.
			this.buildRendering();
			if(cls){ setClass.call(this, cls); }
			
			this.postCreate();
			
			// remove srcNodeRef instance property post-create
			delete this.srcNodeRef;
			// to preserve "it just works" behavior, call startup if we're visible
			if(this.domNode.offsetHeight){
				this.startup();
			}
		},
		buildRendering: function(){
			var domNode = this.domNode,
				self = this,
				headerNode, spacerNode, bodyNode, footerNode, isRTL;
			
			// Detect RTL on html/body nodes; taken from dojo/dom-geometry
			isRTL = this.isRTL = (document.body.dir || document.documentElement.dir ||
				document.body.style.direction).toLowerCase() == "rtl";
			
			// Clear out className (any pre-applied classes will be re-applied via the
			// class / className setter), then apply standard classes/attributes
			domNode.className = "";
			
			put(domNode, "[role=grid].ui-widget.dgrid.dgrid-" + this.listType);
			
			// Place header node (initially hidden if showHeader is false).
			headerNode = this.headerNode = put(domNode, 
				"div.dgrid-header.dgrid-header-row.ui-widget-header" +
				(this.showHeader ? "" : ".dgrid-header-hidden"));
			if(has("quirks") || has("ie") < 8){
				spacerNode = put(domNode, "div.dgrid-spacer");
			}
			bodyNode = this.bodyNode = put(domNode, "div.dgrid-scroller");
			
			// firefox 4 until at least 10 adds overflow: auto elements to the tab index by default for some
			// reason; force them to be not tabbable
			bodyNode.tabIndex = -1;
			
			this.headerScrollNode = put(domNode, "div.dgrid-header-scroll.dgrid-scrollbar-width.ui-widget-header");
			
			// Place footer node (initially hidden if showFooter is false).
			footerNode = this.footerNode = put("div.dgrid-footer" +
				(this.showFooter ? "" : ".dgrid-footer-hidden"));
			put(domNode, footerNode);
			
			if(isRTL){
				domNode.className += " dgrid-rtl" + (has("webkit") ? "" : " dgrid-rtl-nonwebkit");
			}
			
			listen(bodyNode, "scroll", function(event){
				if(self.showHeader){
					// keep the header aligned with the body
					headerNode.scrollLeft = event.scrollLeft || bodyNode.scrollLeft;
				}
				// re-fire, since browsers are not consistent about propagation here
				event.stopPropagation();
				listen.emit(domNode, "scroll", {scrollTarget: bodyNode});
			});
			this.configStructure();
			this.renderHeader();
			
			this.contentNode = this.touchNode = put(this.bodyNode, "div.dgrid-content.ui-widget-content");
			// add window resize handler, with reference for later removal if needed
			this._listeners.push(this._resizeHandle = listen(window, "resize",
				miscUtil.throttleDelayed(winResizeHandler, this)));
		},
		
		postCreate: has("touch") ? function(){
			if(this.useTouchScroll){
				this.inherited(arguments);
			}
		} : function(){},
		
		startup: function(){
			// summary:
			//		Called automatically after postCreate if the component is already
			//		visible; otherwise, should be called manually once placed.
			
			if(this._started){ return; } // prevent double-triggering
			this.inherited(arguments);
			this._started = true;
			this.resize();
			// apply sort (and refresh) now that we're ready to render
			this.set("sort", this._sort);
		},
		
		configStructure: function(){
			// does nothing in List, this is more of a hook for the Grid
		},
		resize: function(){
			var
				bodyNode = this.bodyNode,
				headerNode = this.headerNode,
				footerNode = this.footerNode,
				headerHeight = headerNode.offsetHeight,
				footerHeight = this.showFooter ? footerNode.offsetHeight : 0,
				quirks = has("quirks") || has("ie") < 7;
			
			this.headerScrollNode.style.height = bodyNode.style.marginTop = headerHeight + "px";
			bodyNode.style.marginBottom = footerHeight + "px";
			
			if(quirks){
				// in IE6 and quirks mode, the "bottom" CSS property is ignored.
				// We guard against negative values in case of issues with external CSS.
				bodyNode.style.height = ""; // reset first
				bodyNode.style.height =
					Math.max((this.domNode.offsetHeight - headerHeight - footerHeight), 0) + "px";
				if (footerHeight) {
					// Work around additional glitch where IE 6 / quirks fails to update
					// the position of the bottom-aligned footer; this jogs its memory.
					footerNode.style.bottom = '1px';
					setTimeout(function(){ footerNode.style.bottom = ''; }, 0);
				}
			}
			
			if(!scrollbarWidth){
				// Measure the browser's scrollbar width using a DIV we'll delete right away
				scrollbarWidth = has("dom-scrollbar-width");
				scrollbarHeight = has("dom-scrollbar-height");
				
				// Avoid issues with certain widgets inside in IE7, and
				// ColumnSet scroll issues with all supported IE versions
				if(has("ie")){
					scrollbarWidth++;
					scrollbarHeight++;
				}
				
				// add rules that can be used where scrollbar width/height is needed
				miscUtil.addCssRule(".dgrid-scrollbar-width", "width: " + scrollbarWidth + "px");
				miscUtil.addCssRule(".dgrid-scrollbar-height", "height: " + scrollbarHeight + "px");
				
				if(scrollbarWidth != 17 && !quirks){
					// for modern browsers, we can perform a one-time operation which adds
					// a rule to account for scrollbar width in all grid headers.
					miscUtil.addCssRule(".dgrid-header", "right: " + scrollbarWidth + "px");
					// add another for RTL grids
					miscUtil.addCssRule(".dgrid-rtl-nonwebkit .dgrid-header", "left: " + scrollbarWidth + "px");
				}
			}
			
			if(quirks){
				// old IE doesn't support left + right + width:auto; set width directly
				headerNode.style.width = bodyNode.clientWidth + "px";
				setTimeout(function(){
					// sync up (after the browser catches up with the new width)
					headerNode.scrollLeft = bodyNode.scrollLeft;
				}, 0);
			}
		},
		
		addCssRule: function(selector, css){
			// summary:
			//		Version of util/misc.addCssRule which tracks added rules and removes
			//		them when the List is destroyed.
			
			var rule = miscUtil.addCssRule(selector, css);
			if(this.cleanAddedRules){
				// Although this isn't a listener, it shares the same remove contract
				this._listeners.push(rule);
			}
			return rule;
		},
		
		on: function(eventType, listener){
			// delegate events to the domNode
			var signal = listen(this.domNode, eventType, listener);
			if(!has("dom-addeventlistener")){
				this._listeners.push(signal);
			}
			return signal;
		},
		
		cleanup: function(){
			// summary:
			//		Clears out all rows currently in the list.
			
			var observers = this.observers,
				i;
			for(i in this._rowIdToObject){
				if(this._rowIdToObject[i] != this.columns){
					var rowElement = byId(i);
					if(rowElement){
						this.removeRow(rowElement, true);
					}
				}
			}
			// remove any store observers
			for(i = 0;i < observers.length; i++){
				var observer = observers[i];
				observer && observer.cancel();
			}
			this.observers = [];
			this._numObservers = 0;
			this.preload = null;
		},
		destroy: function(){
			// summary:
			//		Destroys this grid
			
			// Remove any event listeners and other such removables
			if(this._listeners){ // Guard against accidental subsequent calls to destroy
				for(var i = this._listeners.length; i--;){
					this._listeners[i].remove();
				}
				delete this._listeners;
			}
			
			this.cleanup();
			// destroy DOM
			put(this.domNode, "!");
			this.inherited(arguments);
		},
		refresh: function(){
			// summary:
			//		refreshes the contents of the grid
			this.cleanup();
			this._rowIdToObject = {};
			this._autoId = 0;
			
			// make sure all the content has been removed so it can be recreated
			this.contentNode.innerHTML = "";
			// Ensure scroll position always resets (especially for TouchScroll).
			this.scrollTo({ x: 0, y: 0 });
		},
		
		newRow: function(object, parentNode, beforeNode, i, options){
			if(parentNode){
				var row = this.insertRow(object, parentNode, beforeNode, i, options);
				put(row, ".ui-state-highlight");
				setTimeout(function(){
					put(row, "!ui-state-highlight");
				}, 250);
				return row;
			}
		},
		adjustRowIndices: function(firstRow){
			// this traverses through rows to maintain odd/even classes on the rows when indexes shift;
			var next = firstRow;
			var rowIndex = next.rowIndex;
			if(rowIndex > -1){ // make sure we have a real number in case this is called on a non-row
				do{
					// Skip non-numeric, non-rows
					if(next.rowIndex > -1){
						if(this.maintainOddEven){
							if((next.className + ' ').indexOf("dgrid-row ") > -1){
								put(next, '.' + (rowIndex % 2 == 1 ? oddClass : evenClass) + '!' + (rowIndex % 2 == 0 ? oddClass : evenClass));
							}
						}
						next.rowIndex = rowIndex++;
					}
				}while((next = next.nextSibling) && next.rowIndex != rowIndex);
			}
		},
		renderArray: function(results, beforeNode, options){
			// summary:
			//		This renders an array or collection of objects as rows in the grid, before the
			//		given node. This will listen for changes in the collection if an observe method
			//		is available (as it should be if it comes from an Observable data store).
			options = options || {};
			var self = this,
				start = options.start || 0,
				observers = this.observers,
				row, rows, container, observerIndex;
			
			if(!beforeNode){
				this._lastCollection = results;
			}
			if(results.observe){
				// observe the results for changes
				self._numObservers++;
				observerIndex = observers.push(results.observe(function(object, from, to){
					var firstRow, nextNode, parentNode;
					// a change in the data took place
					if(from > -1 && rows[from]){
						// remove from old slot
						row = rows.splice(from, 1)[0];
						// check to make the sure the node is still there before we try to remove it, (in case it was moved to a different place in the DOM)
						if(row.parentNode == container){
							firstRow = row.nextSibling;
							if(firstRow){ // it's possible for this to have been already removed if it is in overlapping query results
								if(from != to){ // if from and to are identical, it is an in-place update and we don't want to alter the rowIndex at all
									firstRow.rowIndex--; // adjust the rowIndex so adjustRowIndices has the right starting point
								}
							}
							self.removeRow(row); // now remove
						}
						// the removal of rows could cause us to need to page in more items
						if(self._processScroll){
							self._processScroll();
						}
					}
					if(to > -1){
						// Add to new slot (either before an existing row, or at the end)
						// First determine the DOM node that this should be placed before.
						if(rows.length){
							nextNode = rows[to];
							if(!nextNode){
								nextNode = rows[to - 1];
								if(nextNode){
									// Make sure to skip connected nodes, so we don't accidentally
									// insert a row in between a parent and its children.
									nextNode = (nextNode.connected || nextNode).nextSibling;
								}
							}
						}else{
							// There are no rows.  Allow for subclasses to insert new rows somewhere other than
							// at the end of the parent node.
							nextNode = self._getFirstRowSibling && self._getFirstRowSibling(container);
						}
						parentNode = (beforeNode && beforeNode.parentNode) ||
							(nextNode && nextNode.parentNode) || self.contentNode;
						row = self.newRow(object, parentNode, nextNode, options.start + to, options);
						
						if(row){
							row.observerIndex = observerIndex;
							rows.splice(to, 0, row);
							if(!firstRow || to < from){
								// the inserted row is first, so we update firstRow to point to it
								var previous = row.previousSibling;
								// if we are not in sync with the previous row, roll the firstRow back one so adjustRowIndices can sync everything back up.
								firstRow = !previous || previous.rowIndex + 1 == row.rowIndex || row.rowIndex == 0 ?
									row : previous;
							}
						}
						options.count++;
					}
					from != to && firstRow && self.adjustRowIndices(firstRow);
					self._onNotification(rows, object, from, to);
				}, true)) - 1;
			}
			var rowsFragment = document.createDocumentFragment(),
				lastRow;
			
			function mapEach(object){
				lastRow = self.insertRow(object, rowsFragment, null, start++, options);
				lastRow.observerIndex = observerIndex;
				return lastRow;
			}
			function whenError(error){
				if(typeof observerIndex !== "undefined"){
					observers[observerIndex].cancel();
					observers[observerIndex] = 0;
					self._numObservers--;
				}
				if(error){
					throw error;
				}
			}
			function whenDone(resolvedRows){
				container = beforeNode ? beforeNode.parentNode : self.contentNode;
				if(container && container.parentNode &&
						(container !== self.contentNode || resolvedRows.length)){
					container.insertBefore(rowsFragment, beforeNode || null);
					lastRow = resolvedRows[resolvedRows.length - 1];
					lastRow && self.adjustRowIndices(lastRow);
				}else if(observers[observerIndex]){
					// Remove the observer and don't bother inserting;
					// rows are already out of view or there were none to track
					whenError();
				}
				return (rows = resolvedRows);
			}
			
			// now render the results
			if(results.map){
				rows = results.map(mapEach, console.error);
				if(rows.then){
					return rows.then(whenDone, whenError);
				}
			}else{
				rows = [];
				for(var i = 0, l = results.length; i < l; i++){
					rows[i] = mapEach(results[i]);
				}
			}
			
			return whenDone(rows);
		},

		_onNotification: function(rows, object, from, to){
			// summary:
			//		Protected method called whenever a store notification is observed.
			//		Intended to be extended as necessary by mixins/extensions.
		},

		renderHeader: function(){
			// no-op in a plain list
		},
		
		_autoId: 0,
		insertRow: function(object, parent, beforeNode, i, options){
			// summary:
			//		Creates a single row in the grid.
			
			// Include parentId within row identifier if one was specified in options.
			// (This is used by tree to allow the same object to appear under
			// multiple parents.)
			var parentId = options.parentId,
				id = this.id + "-row-" + (parentId ? parentId + "-" : "") + 
					((this.store && this.store.getIdentity) ? 
						this.store.getIdentity(object) : this._autoId++),
				row = byId(id),
				previousRow = row && row.previousSibling;
		
			if(row){// if it existed elsewhere in the DOM, we will remove it, so we can recreate it
				this.removeRow(row);
			}
			row = this.renderRow(object, options);
			row.className = (row.className || "") + " ui-state-default dgrid-row " + (i % 2 == 1 ? oddClass : evenClass);
			// get the row id for easy retrieval
			this._rowIdToObject[row.id = id] = object;
			parent.insertBefore(row, beforeNode || null);
			if(previousRow){
				// in this case, we are pulling the row from another location in the grid, and we need to readjust the rowIndices from the point it was removed
				this.adjustRowIndices(previousRow);
			}
			row.rowIndex = i;
			return row;
		},
		renderRow: function(value, options){
			// summary:
			//		Responsible for returning the DOM for a single row in the grid.
			
			return put("div", "" + value);
		},
		removeRow: function(rowElement, justCleanup){
			// summary:
			//		Simply deletes the node in a plain List.
			//		Column plugins may aspect this to implement their own cleanup routines.
			// rowElement: Object|DOMNode
			//		Object or element representing the row to be removed.
			// justCleanup: Boolean
			//		If true, the row element will not be removed from the DOM; this can
			//		be used by extensions/plugins in cases where the DOM will be
			//		massively cleaned up at a later point in time.
			
			rowElement = rowElement.element || rowElement;
			delete this._rowIdToObject[rowElement.id];
			if(!justCleanup){
				put(rowElement, "!");
			}
		},
		
		row: function(target){
			// summary:
			//		Get the row object by id, object, node, or event
			var id;
			
			if(target instanceof this._Row){ return target; } // no-op; already a row
			
			if(target.target && target.target.nodeType){
				// event
				target = target.target;
			}
			if(target.nodeType){
				var object;
				do{
					var rowId = target.id;
					if((object = this._rowIdToObject[rowId])){
						return new this._Row(rowId.substring(this.id.length + 5), object, target); 
					}
					target = target.parentNode;
				}while(target && target != this.domNode);
				return;
			}
			if(typeof target == "object"){
				// assume target represents a store item
				id = this.store.getIdentity(target);
			}else{
				// assume target is a row ID
				id = target;
				target = this._rowIdToObject[this.id + "-row-" + id];
			}
			return new this._Row(id, target, byId(this.id + "-row-" + id));
		},
		cell: function(target){
			// this doesn't do much in a plain list
			return {
				row: this.row(target)
			};
		},
		
		_move: function(item, steps, targetClass, visible){
			var nextSibling, current, element;
			// Start at the element indicated by the provided row or cell object.
			element = current = item.element;
			steps = steps || 1;
			
			do{
				// Outer loop: move in the appropriate direction.
				if((nextSibling = current[steps < 0 ? "previousSibling" : "nextSibling"])){
					do{
						// Inner loop: advance, and dig into children if applicable.
						current = nextSibling;
						if(current && (current.className + " ").indexOf(targetClass + " ") > -1){
							// Element with the appropriate class name; count step, stop digging.
							element = current;
							steps += steps < 0 ? 1 : -1;
							break;
						}
						// If the next sibling isn't a match, drill down to search, unless
						// visible is true and children are hidden.
					}while((nextSibling = (!visible || !current.hidden) && current[steps < 0 ? "lastChild" : "firstChild"]));
				}else{
					current = current.parentNode;
					if(current === this.bodyNode || current === this.headerNode){
						// Break out if we step out of the navigation area entirely.
						break;
					}
				}
			}while(steps);
			// Return the final element we arrived at, which might still be the
			// starting element if we couldn't navigate further in that direction.
			return element;
		},
		
		up: function(row, steps, visible){
			// summary:
			//		Returns the row that is the given number of steps (1 by default)
			//		above the row represented by the given object.
			// row:
			//		The row to navigate upward from.
			// steps:
			//		Number of steps to navigate up from the given row; default is 1.
			// visible:
			//		If true, rows that are currently hidden (i.e. children of
			//		collapsed tree rows) will not be counted in the traversal.
			// returns:
			//		A row object representing the appropriate row.  If the top of the
			//		list is reached before the given number of steps, the first row will
			//		be returned.
			if(!row.element){ row = this.row(row); }
			return this.row(this._move(row, -(steps || 1), "dgrid-row", visible));
		},
		down: function(row, steps, visible){
			// summary:
			//		Returns the row that is the given number of steps (1 by default)
			//		below the row represented by the given object.
			// row:
			//		The row to navigate downward from.
			// steps:
			//		Number of steps to navigate down from the given row; default is 1.
			// visible:
			//		If true, rows that are currently hidden (i.e. children of
			//		collapsed tree rows) will not be counted in the traversal.
			// returns:
			//		A row object representing the appropriate row.  If the bottom of the
			//		list is reached before the given number of steps, the last row will
			//		be returned.
			if(!row.element){ row = this.row(row); }
			return this.row(this._move(row, steps || 1, "dgrid-row", visible));
		},
		
		scrollTo: has("touch") ? function(options){
			// If TouchScroll is the superclass, defer to its implementation.
			return this.useTouchScroll ? this.inherited(arguments) :
				desktopScrollTo.call(this, options);
		} : desktopScrollTo,
		
		getScrollPosition: has("touch") ? function(){
			// If TouchScroll is the superclass, defer to its implementation.
			return this.useTouchScroll ? this.inherited(arguments) :
				desktopGetScrollPosition.call(this);
		} : desktopGetScrollPosition,
		
		get: function(/*String*/ name /*, ... */){
			// summary:
			//		Get a property on a List instance.
			//	name:
			//		The property to get.
			//	returns:
			//		The property value on this List instance.
			// description:
			//		Get a named property on a List object. The property may
			//		potentially be retrieved via a getter method in subclasses. In the base class
			//		this just retrieves the object's property.
			
			var fn = "_get" + name.charAt(0).toUpperCase() + name.slice(1);
			
			if(typeof this[fn] === "function"){
				return this[fn].apply(this, [].slice.call(arguments, 1));
			}
			
			// Alert users that try to use Dijit-style getter/setters so they don’t get confused
			// if they try to use them and it does not work
			if(! 1  && typeof this[fn + "Attr"] === "function"){
				console.warn("dgrid: Use " + fn + " instead of " + fn + "Attr for getting " + name);
			}
			
			return this[name];
		},
		
		set: function(/*String*/ name, /*Object*/ value /*, ... */){
			//	summary:
			//		Set a property on a List instance
			//	name:
			//		The property to set.
			//	value:
			//		The value to set in the property.
			//	returns:
			//		The function returns this List instance.
			//	description:
			//		Sets named properties on a List object.
			//		A programmatic setter may be defined in subclasses.
			//
			//		set() may also be called with a hash of name/value pairs, ex:
			//	|	myObj.set({
			//	|		foo: "Howdy",
			//	|		bar: 3
			//	|	})
			//		This is equivalent to calling set(foo, "Howdy") and set(bar, 3)
			
			if(typeof name === "object"){
				for(var k in name){
					this.set(k, name[k]);
				}
			}else{
				var fn = "_set" + name.charAt(0).toUpperCase() + name.slice(1);
				
				if(typeof this[fn] === "function"){
					this[fn].apply(this, [].slice.call(arguments, 1));
				}else{
					// Alert users that try to use Dijit-style getter/setters so they don’t get confused
					// if they try to use them and it does not work
					if(! 1  && typeof this[fn + "Attr"] === "function"){
						console.warn("dgrid: Use " + fn + " instead of " + fn + "Attr for setting " + name);
					}
					
					this[name] = value;
				}
			}
			
			return this;
		},
		
		// Accept both class and className programmatically to set domNode class.
		_getClass: getClass,
		_setClass: setClass,
		_getClassName: getClass,
		_setClassName: setClass,
		
		_setSort: function(property, descending){
			// summary:
			//		Sort the content
			// property: String|Array
			//		String specifying field to sort by, or actual array of objects
			//		with attribute and descending properties
			// descending: boolean
			//		In the case where property is a string, this argument
			//		specifies whether to sort ascending (false) or descending (true)
			
			this._sort = typeof property != "string" ? property :
				[{attribute: property, descending: descending}];
			
			this.refresh();
			
			if(this._lastCollection){
				if(property.length){
					// if an array was passed in, flatten to just first sort attribute
					// for default array sort logic
					if(typeof property != "string"){
						descending = property[0].descending;
						property = property[0].attribute;
					}
					
					this._lastCollection.sort(function(a,b){
						var aVal = a[property], bVal = b[property];
						// fall back undefined values to "" for more consistent behavior
						if(aVal === undefined){ aVal = ""; }
						if(bVal === undefined){ bVal = ""; }
						return aVal == bVal ? 0 : (aVal > bVal == !descending ? 1 : -1);
					});
				}
				this.renderArray(this._lastCollection);
			}
		},
		// TODO: remove the following two (and rename _sort to sort) in 0.4
		sort: function(property, descending){
			kernel.deprecated("sort(...)", 'use set("sort", ...) instead', "dgrid 0.4");
			this.set("sort", property, descending);
		},
		_getSort: function(){
			return this._sort;
		},
		
		_setShowHeader: function(show){
			// this is in List rather than just in Grid, primarily for two reasons:
			// (1) just in case someone *does* want to show a header in a List
			// (2) helps address IE < 8 header display issue in List
			
			var headerNode = this.headerNode;
			
			this.showHeader = show;
			
			// add/remove class which has styles for "hiding" header
			put(headerNode, (show ? "!" : ".") + "dgrid-header-hidden");
			
			this.renderHeader();
			this.resize(); // resize to account for (dis)appearance of header
			
			if(show){
				// Update scroll position of header to make sure it's in sync.
				headerNode.scrollLeft = this.getScrollPosition().x;
			}
		},
		setShowHeader: function(show){
			kernel.deprecated("setShowHeader(...)", 'use set("showHeader", ...) instead', "dgrid 0.4");
			this.set("showHeader", show);
		},
		
		_setShowFooter: function(show){
			this.showFooter = show;
			
			// add/remove class which has styles for hiding footer
			put(this.footerNode, (show ? "!" : ".") + "dgrid-footer-hidden");
			
			this.resize(); // to account for (dis)appearance of footer
		}
	});
});

},
'dgrid/Keyboard':function(){
define([
	"dojo/_base/declare",
	"dojo/aspect",
	"dojo/on",
	"dojo/_base/lang",
	"dojo/has",
	"put-selector/put",
	"dojo/_base/Deferred",
	"dojo/_base/sniff"
], function(declare, aspect, on, lang, has, put, Deferred){

var delegatingInputTypes = {
		checkbox: 1,
		radio: 1,
		button: 1
	},
	hasGridCellClass = /\bdgrid-cell\b/,
	hasGridRowClass = /\bdgrid-row\b/;

has.add("dom-contains", function(global, doc, element){
	return !!element.contains; // not supported by FF < 9
});

function contains(parent, node){
	// summary:
	//		Checks to see if an element is contained by another element.
	
	if(has("dom-contains")){
		return parent.contains(node);
	}else{
		return parent.compareDocumentPosition(node) & 8 /* DOCUMENT_POSITION_CONTAINS */;
	}
}

var Keyboard = declare(null, {
	// summary:
	//		Adds keyboard navigation capability to a list or grid.
	
	// pageSkip: Number
	//		Number of rows to jump by when page up or page down is pressed.
	pageSkip: 10,
	
	tabIndex: 0,
	
	// keyMap: Object
	//		Hash which maps key codes to functions to be executed (in the context
	//		of the instance) for key events within the grid's body.
	keyMap: null,
	
	// headerKeyMap: Object
	//		Hash which maps key codes to functions to be executed (in the context
	//		of the instance) for key events within the grid's header row.
	headerKeyMap: null,
	
	postMixInProperties: function(){
		this.inherited(arguments);
		
		if(!this.keyMap){
			this.keyMap = lang.mixin({}, Keyboard.defaultKeyMap);
		}
		if(!this.headerKeyMap){
			this.headerKeyMap = lang.mixin({}, Keyboard.defaultHeaderKeyMap);
		}
	},
	
	postCreate: function(){
		this.inherited(arguments);
		var grid = this;
		
		function handledEvent(event){
			// text boxes and other inputs that can use direction keys should be ignored and not affect cell/row navigation
			var target = event.target;
			return target.type && (!delegatingInputTypes[target.type] || event.keyCode == 32);
		}
		
		function enableNavigation(areaNode){
			var cellNavigation = grid.cellNavigation,
				isFocusableClass = cellNavigation ? hasGridCellClass : hasGridRowClass,
				isHeader = areaNode === grid.headerNode,
				initialNode = areaNode;
			
			function initHeader(){
				grid._focusedHeaderNode = initialNode =
					cellNavigation ? grid.headerNode.getElementsByTagName("th")[0] : grid.headerNode;
				if(initialNode){ initialNode.tabIndex = grid.tabIndex; }
			}
			
			if(isHeader){
				// Initialize header now (since it's already been rendered),
				// and aspect after future renderHeader calls to reset focus.
				initHeader();
				aspect.after(grid, "renderHeader", initHeader, true);
			}else{
				aspect.after(grid, "renderArray", function(ret){
					// summary:
					//		Ensures the first element of a grid is always keyboard selectable after data has been
					//		retrieved if there is not already a valid focused element.
					
					return Deferred.when(ret, function(ret){
						var focusedNode = grid._focusedNode || initialNode;
						
						// do not update the focused element if we already have a valid one
						if(isFocusableClass.test(focusedNode.className) && contains(areaNode, focusedNode)){
							return ret;
						}
						
						// ensure that the focused element is actually a grid cell, not a
						// dgrid-preload or dgrid-content element, which should not be focusable,
						// even when data is loaded asynchronously
						for(var i = 0, elements = areaNode.getElementsByTagName("*"), element; (element = elements[i]); ++i){
							if(isFocusableClass.test(element.className)){
								focusedNode = grid._focusedNode = element;
								break;
							}
						}
						
						focusedNode.tabIndex = grid.tabIndex;
						return ret;
					});
				});
			}
			
			grid._listeners.push(on(areaNode, "mousedown", function(event){
				if(!handledEvent(event)){
					grid._focusOnNode(event.target, isHeader, event);
				}
			}));
			
			grid._listeners.push(on(areaNode, "keydown", function(event){
				// For now, don't squash browser-specific functionalities by letting
				// ALT and META function as they would natively
				if(event.metaKey || event.altKey) {
					return;
				}
				
				var handler = grid[isHeader ? "headerKeyMap" : "keyMap"][event.keyCode];
				
				// Text boxes and other inputs that can use direction keys should be ignored and not affect cell/row navigation
				if(handler && !handledEvent(event)){
					handler.call(grid, event);
				}
			}));
		}
		
		if(this.tabableHeader){
			enableNavigation(this.headerNode);
			on(this.headerNode, "dgrid-cellfocusin", function(){
				grid.scrollTo({ x: this.scrollLeft });
			});
		}
		enableNavigation(this.contentNode);
	},
	
	addKeyHandler: function(key, callback, isHeader){
		// summary:
		//		Adds a handler to the keyMap on the instance.
		//		Supports binding additional handlers to already-mapped keys.
		// key: Number
		//		Key code representing the key to be handled.
		// callback: Function
		//		Callback to be executed (in instance context) when the key is pressed.
		// isHeader: Boolean
		//		Whether the handler is to be added for the grid body (false, default)
		//		or the header (true).
		
		// Aspects may be about 10% slower than using an array-based appraoch,
		// but there is significantly less code involved (here and above).
		return aspect.after( // Handle
			this[isHeader ? "headerKeyMap" : "keyMap"], key, callback, true);
	},
	
	_focusOnNode: function(element, isHeader, event){
		var focusedNodeProperty = "_focused" + (isHeader ? "Header" : "") + "Node",
			focusedNode = this[focusedNodeProperty],
			cellOrRowType = this.cellNavigation ? "cell" : "row",
			cell = this[cellOrRowType](element),
			inputs,
			input,
			numInputs,
			inputFocused,
			i;
		
		element = cell && cell.element;
		if(!element){ return; }
		
		if(this.cellNavigation){
			inputs = element.getElementsByTagName("input");
			for(i = 0, numInputs = inputs.length; i < numInputs; i++){
				input = inputs[i];
				if((input.tabIndex != -1 || "lastValue" in input) && !input.disabled){
					// Employ workaround for focus rectangle in IE < 8
					if(has("ie") < 8){ input.style.position = "relative"; }
					input.focus();
					if(has("ie") < 8){ input.style.position = ""; }
					inputFocused = true;
					break;
				}
			}
		}
		
		event = lang.mixin({ grid: this }, event);
		if(event.type){
			event.parentType = event.type;
		}
		if(!event.bubbles){
			// IE doesn't always have a bubbles property already true.
			// Opera throws if you try to set it to true if it is already true.
			event.bubbles = true;
		}
		if(focusedNode){
			// Clean up previously-focused element
			// Remove the class name and the tabIndex attribute
			put(focusedNode, "!dgrid-focus[!tabIndex]");
			if(has("ie") < 8){
				// Clean up after workaround below (for non-input cases)
				focusedNode.style.position = "";
			}
			
			// Expose object representing focused cell or row losing focus, via
			// event.cell or event.row; which is set depends on cellNavigation.
			event[cellOrRowType] = this[cellOrRowType](focusedNode);
			on.emit(element, "dgrid-cellfocusout", event);
		}
		focusedNode = this[focusedNodeProperty] = element;
		
		// Expose object representing focused cell or row gaining focus, via
		// event.cell or event.row; which is set depends on cellNavigation.
		// Note that yes, the same event object is being reused; on.emit
		// performs a shallow copy of properties into a new event object.
		event[cellOrRowType] = cell;
		
		if(!inputFocused){
			if(has("ie") < 8){
				// setting the position to relative magically makes the outline
				// work properly for focusing later on with old IE.
				// (can't be done a priori with CSS or screws up the entire table)
				element.style.position = "relative";
			}
			element.tabIndex = this.tabIndex;
			element.focus();
		}
		put(element, ".dgrid-focus");
		on.emit(focusedNode, "dgrid-cellfocusin", event);
	},
	
	focusHeader: function(element){
		this._focusOnNode(element || this._focusedHeaderNode, true);
	},
	
	focus: function(element){
		this._focusOnNode(element || this._focusedNode, false);
	}
});

// Common functions used in default keyMap (called in instance context)

var moveFocusVertical = Keyboard.moveFocusVertical = function(event, steps){
	var cellNavigation = this.cellNavigation,
		target = this[cellNavigation ? "cell" : "row"](event),
		columnId = cellNavigation && target.column.id,
		next = this.down(this._focusedNode, steps, true);
	
	// Navigate within same column if cell navigation is enabled
	if(cellNavigation){ next = this.cell(next, columnId); }
	this._focusOnNode(next, false, event);
	
	event.preventDefault();
};

var moveFocusUp = Keyboard.moveFocusUp = function(event){
	moveFocusVertical.call(this, event, -1);
};

var moveFocusDown = Keyboard.moveFocusDown = function(event){
	moveFocusVertical.call(this, event, 1);
};

var moveFocusPageUp = Keyboard.moveFocusPageUp = function(event){
	moveFocusVertical.call(this, event, -this.pageSkip);
};

var moveFocusPageDown = Keyboard.moveFocusPageDown = function(event){
	moveFocusVertical.call(this, event, this.pageSkip);
};

var moveFocusHorizontal = Keyboard.moveFocusHorizontal = function(event, steps){
	if(!this.cellNavigation){ return; }
	var isHeader = !this.row(event), // header reports row as undefined
		currentNode = this["_focused" + (isHeader ? "Header" : "") + "Node"];
	
	this._focusOnNode(this.right(currentNode, steps), isHeader, event);
	event.preventDefault();
};

var moveFocusLeft = Keyboard.moveFocusLeft = function(event){
	moveFocusHorizontal.call(this, event, -1);
};

var moveFocusRight = Keyboard.moveFocusRight = function(event){
	moveFocusHorizontal.call(this, event, 1);
};

var moveHeaderFocusEnd = Keyboard.moveHeaderFocusEnd = function(event, scrollToBeginning){
	// Header case is always simple, since all rows/cells are present
	var nodes;
	if(this.cellNavigation){
		nodes = this.headerNode.getElementsByTagName("th");
		this._focusOnNode(nodes[scrollToBeginning ? 0 : nodes.length - 1], true, event);
	}
	// In row-navigation mode, there's nothing to do - only one row in header
	
	// Prevent browser from scrolling entire page
	event.preventDefault();
};

var moveHeaderFocusHome = Keyboard.moveHeaderFocusHome = function(event){
	moveHeaderFocusEnd.call(this, event, true);
};

var moveFocusEnd = Keyboard.moveFocusEnd = function(event, scrollToTop){
	// summary:
	//		Handles requests to scroll to the beginning or end of the grid.
	
	// Assume scrolling to top unless event is specifically for End key
	var self = this,
		cellNavigation = this.cellNavigation,
		contentNode = this.contentNode,
		contentPos = scrollToTop ? 0 : contentNode.scrollHeight,
		scrollPos = contentNode.scrollTop + contentPos,
		endChild = contentNode[scrollToTop ? "firstChild" : "lastChild"],
		hasPreload = endChild.className.indexOf("dgrid-preload") > -1,
		endTarget = hasPreload ? endChild[(scrollToTop ? "next" : "previous") + "Sibling"] : endChild,
		endPos = endTarget.offsetTop + (scrollToTop ? 0 : endTarget.offsetHeight),
		handle;
	
	if(hasPreload){
		// Find the nearest dgrid-row to the relevant end of the grid
		while(endTarget && endTarget.className.indexOf("dgrid-row") < 0){
			endTarget = endTarget[(scrollToTop ? "next" : "previous") + "Sibling"];
		}
		// If none is found, there are no rows, and nothing to navigate
		if(!endTarget){ return; }
	}
	
	// Grid content may be lazy-loaded, so check if content needs to be
	// loaded first
	if(!hasPreload || endChild.offsetHeight < 1){
		// End row is loaded; focus the first/last row/cell now
		if(cellNavigation){
			// Preserve column that was currently focused
			endTarget = this.cell(endTarget, this.cell(event).column.id);
		}
		this._focusOnNode(endTarget, false, event);
	}else{
		// In IE < 9, the event member references will become invalid by the time
		// _focusOnNode is called, so make a (shallow) copy up-front
		if(!has("dom-addeventlistener")){
			event = lang.mixin({}, event);
		}
		
		// If the topmost/bottommost row rendered doesn't reach the top/bottom of
		// the contentNode, we are using OnDemandList and need to wait for more
		// data to render, then focus the first/last row in the new content.
		handle = aspect.after(this, "renderArray", function(rows){
			handle.remove();
			return Deferred.when(rows, function(rows){
				var target = rows[scrollToTop ? 0 : rows.length - 1];
				if(cellNavigation){
					// Preserve column that was currently focused
					target = self.cell(target, self.cell(event).column.id);
				}
				self._focusOnNode(target, false, event);
			});
		});
	}
	
	if(scrollPos === endPos){
		// Grid body is already scrolled to end; prevent browser from scrolling
		// entire page instead
		event.preventDefault();
	}
};

var moveFocusHome = Keyboard.moveFocusHome = function(event){
	moveFocusEnd.call(this, event, true);
};

function preventDefault(event){
	event.preventDefault();
}

Keyboard.defaultKeyMap = {
	32: preventDefault, // space
	33: moveFocusPageUp, // page up
	34: moveFocusPageDown, // page down
	35: moveFocusEnd, // end
	36: moveFocusHome, // home
	37: moveFocusLeft, // left
	38: moveFocusUp, // up
	39: moveFocusRight, // right
	40: moveFocusDown // down
};

// Header needs fewer default bindings (no vertical), so bind it separately
Keyboard.defaultHeaderKeyMap = {
	32: preventDefault, // space
	35: moveHeaderFocusEnd, // end
	36: moveHeaderFocusHome, // home
	37: moveFocusLeft, // left
	39: moveFocusRight // right
};

return Keyboard;
});
},
'dojo/Evented':function(){
define(["./aspect", "./on"], function(aspect, on){
	// module:
	//		dojo/Evented

 	"use strict";
 	var after = aspect.after;
	function Evented(){
		// summary:
		//		A class that can be used as a mixin or base class,
		//		to add on() and emit() methods to a class
		//		for listening for events and emitting events:
		//
		//		|	define(["dojo/Evented"], function(Evented){
		//		|		var EventedWidget = dojo.declare([Evented, dijit._Widget], {...});
		//		|		widget = new EventedWidget();
		//		|		widget.on("open", function(event){
		//		|		... do something with event
		//		|	 });
		//		|
		//		|	widget.emit("open", {name:"some event", ...});
	}
	Evented.prototype = {
		on: function(type, listener){
			return on.parse(this, type, listener, function(target, type){
				return after(target, 'on' + type, listener, true);
			});
		},
		emit: function(type, event){
			var args = [this];
			args.push.apply(args, arguments);
			return on.emit.apply(on, args);
		}
	};
	return Evented;
});

},
'dgrid/util/misc':function(){
define(["put-selector/put"], function(put){
	// summary:
	//		This module defines miscellaneous utility methods for purposes of
	//		adding styles, and throttling/debouncing function calls.
	
	// establish an extra stylesheet which addCssRule calls will use,
	// plus an array to track actual indices in stylesheet for removal
	var extraRules = [],
		extraSheet,
		removeMethod,
		rulesProperty,
		invalidCssChars = /([^A-Za-z0-9_\u00A0-\uFFFF-])/g;
	
	function removeRule(index){
		// Function called by the remove method on objects returned by addCssRule.
		var realIndex = extraRules[index],
			i, l;
		if (realIndex === undefined) { return; } // already removed
		
		// remove rule indicated in internal array at index
		extraSheet[removeMethod](realIndex);
		
		// Clear internal array item representing rule that was just deleted.
		// NOTE: we do NOT splice, since the point of this array is specifically
		// to negotiate the splicing that occurs in the stylesheet itself!
		extraRules[index] = undefined;
		
		// Then update array items as necessary to downshift remaining rule indices.
		// Can start at index + 1, since array is sparse but strictly increasing.
		for(i = index + 1, l = extraRules.length; i < l; i++){
			if(extraRules[i] > realIndex){ extraRules[i]--; }
		}
	}
	
	var util = {
		// Throttle/debounce functions
		
		defaultDelay: 15,
		throttle: function(cb, context, delay){
			// summary:
			//		Returns a function which calls the given callback at most once per
			//		delay milliseconds.  (Inspired by plugd)
			var ran = false;
			delay = delay || util.defaultDelay;
			return function(){
				if(ran){ return; }
				ran = true;
				cb.apply(context, arguments);
				setTimeout(function(){ ran = false; }, delay);
			};
		},
		throttleDelayed: function(cb, context, delay){
			// summary:
			//		Like throttle, except that the callback runs after the delay,
			//		rather than before it.
			var ran = false;
			delay = delay || util.defaultDelay;
			return function(){
				if(ran){ return; }
				ran = true;
				var a = arguments;
				setTimeout(function(){
					ran = false;
					cb.apply(context, a);
				}, delay);
			};
		},
		debounce: function(cb, context, delay){
			// summary:
			//		Returns a function which calls the given callback only after a
			//		certain time has passed without successive calls.  (Inspired by plugd)
			var timer;
			delay = delay || util.defaultDelay;
			return function(){
				if(timer){
					clearTimeout(timer);
					timer = null;
				}
				var a = arguments;
				timer = setTimeout(function(){
					cb.apply(context, a);
				}, delay);
			};
		},
		
		// Iterative functions
		
		each: function(arrayOrObject, callback, context){
			// summary:
			//		Given an array or object, iterates through its keys.
			//		Does not use hasOwnProperty (since even Dojo does not
			//		consistently use it), but will iterate using a for or for-in
			//		loop as appropriate.
			
			var i, len;
			
			if(!arrayOrObject){
				return;
			}
			
			if(typeof arrayOrObject.length === "number"){
				for(i = 0, len = arrayOrObject.length; i < len; i++){
					callback.call(context, arrayOrObject[i], i, arrayOrObject);
				}
			}else{
				for(i in arrayOrObject){
					callback.call(context, arrayOrObject[i], i, arrayOrObject);
				}
			}
		},
		
		// CSS-related functions
		
		addCssRule: function(selector, css){
			// summary:
			//		Dynamically adds a style rule to the document.  Returns an object
			//		with a remove method which can be called to later remove the rule.
			
			if(!extraSheet){
				// First time, create an extra stylesheet for adding rules
				extraSheet = put(document.getElementsByTagName("head")[0], "style");
				// Keep reference to actual StyleSheet object (`styleSheet` for IE < 9)
				extraSheet = extraSheet.sheet || extraSheet.styleSheet;
				// Store name of method used to remove rules (`removeRule` for IE < 9)
				removeMethod = extraSheet.deleteRule ? "deleteRule" : "removeRule";
				// Store name of property used to access rules (`rules` for IE < 9)
				rulesProperty = extraSheet.cssRules ? "cssRules" : "rules";
			}
			
			var index = extraRules.length;
			extraRules[index] = (extraSheet.cssRules || extraSheet.rules).length;
			extraSheet.addRule ?
				extraSheet.addRule(selector, css) :
				extraSheet.insertRule(selector + '{' + css + '}', extraRules[index]);
			
			return {
				get: function(prop) {
					return extraSheet[rulesProperty][extraRules[index]].style[prop];
				},
				set: function(prop, value) {
					if (typeof extraRules[index] !== "undefined") {
						extraSheet[rulesProperty][extraRules[index]].style[prop] = value;
					}
				},
				remove: function(){
					removeRule(index);
				}
			};
		},
		
		escapeCssIdentifier: function(id){
			// summary:
			//		Escapes normally-invalid characters in a CSS identifier (such as .);
			//		see http://www.w3.org/TR/CSS2/syndata.html#value-def-identifier
			// id: String
			//		CSS identifier (e.g. tag name, class, or id) to be escaped
			
			return id.replace(invalidCssChars, "\\$1");
		}
	};
	return util;
});
},
'dojo/errors/CancelError':function(){
define(["./create"], function(create){
	// module:
	//		dojo/errors/CancelError

	/*=====
	return function(){
		// summary:
		//		Default error if a promise is canceled without a reason.
	};
	=====*/

	return create("CancelError", null, null, { dojoType: "cancel" });
});

},
'dojo/_base/sniff':function(){
define(["./kernel", "./lang", "../sniff"], function(dojo, lang, has){
	// module:
	//		dojo/_base/sniff

	/*=====
	return {
		// summary:
		//		Deprecated.   New code should use dojo/sniff.
		//		This module populates the dojo browser version sniffing properties like dojo.isIE.
	};
	=====*/

	if(! 1 ){
		return has;
	}

	// no idea what this is for, or if it's used
	dojo._name = "browser";

	lang.mixin(dojo, {
		// isBrowser: Boolean
		//		True if the client is a web-browser
		isBrowser: true,

		// isFF: Number|undefined
		//		Version as a Number if client is FireFox. undefined otherwise. Corresponds to
		//		major detected FireFox version (1.5, 2, 3, etc.)
		isFF: has("ff"),

		// isIE: Number|undefined
		//		Version as a Number if client is MSIE(PC). undefined otherwise. Corresponds to
		//		major detected IE version (6, 7, 8, etc.)
		isIE: has("ie"),

		// isKhtml: Number|undefined
		//		Version as a Number if client is a KHTML browser. undefined otherwise. Corresponds to major
		//		detected version.
		isKhtml: has("khtml"),

		// isWebKit: Number|undefined
		//		Version as a Number if client is a WebKit-derived browser (Konqueror,
		//		Safari, Chrome, etc.). undefined otherwise.
		isWebKit: has("webkit"),

		// isMozilla: Number|undefined
		//		Version as a Number if client is a Mozilla-based browser (Firefox,
		//		SeaMonkey). undefined otherwise. Corresponds to major detected version.
		isMozilla: has("mozilla"),
		// isMoz: Number|undefined
		//		Version as a Number if client is a Mozilla-based browser (Firefox,
		//		SeaMonkey). undefined otherwise. Corresponds to major detected version.
		isMoz: has("mozilla"),

		// isOpera: Number|undefined
		//		Version as a Number if client is Opera. undefined otherwise. Corresponds to
		//		major detected version.
		isOpera: has("opera"),

		// isSafari: Number|undefined
		//		Version as a Number if client is Safari or iPhone. undefined otherwise.
		isSafari: has("safari"),

		// isChrome: Number|undefined
		//		Version as a Number if client is Chrome browser. undefined otherwise.
		isChrome: has("chrome"),

		// isMac: Boolean
		//		True if the client runs on Mac
		isMac: has("mac"),

		// isIos: Number|undefined
		//		Version as a Number if client is iPhone, iPod, or iPad. undefined otherwise.
		isIos: has("ios"),

		// isAndroid: Number|undefined
		//		Version as a Number if client is android browser. undefined otherwise.
		isAndroid: has("android"),

		// isWii: Boolean
		//		True if client is Wii
		isWii: has("wii"),

		// isQuirks: Boolean
		//		Page is in quirks mode.
		isQuirks: has("quirks"),

		// isAir: Boolean
		//		True if client is Adobe Air
		isAir: has("air")
	});

	return has;
});

},
'dojo/Deferred':function(){
define([
	"./has",
	"./_base/lang",
	"./errors/CancelError",
	"./promise/Promise",
	"./promise/instrumentation"
], function(has, lang, CancelError, Promise, instrumentation){
	"use strict";

	// module:
	//		dojo/Deferred

	var PROGRESS = 0,
			RESOLVED = 1,
			REJECTED = 2;
	var FULFILLED_ERROR_MESSAGE = "This deferred has already been fulfilled.";

	var freezeObject = Object.freeze || function(){};

	var signalWaiting = function(waiting, type, result, rejection, deferred){
		if( 1 ){
			if(type === REJECTED && Deferred.instrumentRejected && waiting.length === 0){
				Deferred.instrumentRejected(result, false, rejection, deferred);
			}
		}

		for(var i = 0; i < waiting.length; i++){
			signalListener(waiting[i], type, result, rejection);
		}
	};

	var signalListener = function(listener, type, result, rejection){
		var func = listener[type];
		var deferred = listener.deferred;
		if(func){
			try{
				var newResult = func(result);
				if(type === PROGRESS){
					if(typeof newResult !== "undefined"){
						signalDeferred(deferred, type, newResult);
					}
				}else{
					if(newResult && typeof newResult.then === "function"){
						listener.cancel = newResult.cancel;
						newResult.then(
								// Only make resolvers if they're actually going to be used
								makeDeferredSignaler(deferred, RESOLVED),
								makeDeferredSignaler(deferred, REJECTED),
								makeDeferredSignaler(deferred, PROGRESS));
						return;
					}
					signalDeferred(deferred, RESOLVED, newResult);
				}
			}catch(error){
				signalDeferred(deferred, REJECTED, error);
			}
		}else{
			signalDeferred(deferred, type, result);
		}

		if( 1 ){
			if(type === REJECTED && Deferred.instrumentRejected){
				Deferred.instrumentRejected(result, !!func, rejection, deferred.promise);
			}
		}
	};

	var makeDeferredSignaler = function(deferred, type){
		return function(value){
			signalDeferred(deferred, type, value);
		};
	};

	var signalDeferred = function(deferred, type, result){
		if(!deferred.isCanceled()){
			switch(type){
				case PROGRESS:
					deferred.progress(result);
					break;
				case RESOLVED:
					deferred.resolve(result);
					break;
				case REJECTED:
					deferred.reject(result);
					break;
			}
		}
	};

	var Deferred = function(canceler){
		// summary:
		//		Creates a new deferred. This API is preferred over
		//		`dojo/_base/Deferred`.
		// description:
		//		Creates a new deferred, as an abstraction over (primarily)
		//		asynchronous operations. The deferred is the private interface
		//		that should not be returned to calling code. That's what the
		//		`promise` is for. See `dojo/promise/Promise`.
		// canceler: Function?
		//		Will be invoked if the deferred is canceled. The canceler
		//		receives the reason the deferred was canceled as its argument.
		//		The deferred is rejected with its return value, or a new
		//		`dojo/errors/CancelError` instance.

		// promise: dojo/promise/Promise
		//		The public promise object that clients can add callbacks to. 
		var promise = this.promise = new Promise();

		var deferred = this;
		var fulfilled, result, rejection;
		var canceled = false;
		var waiting = [];

		if( 1  && Error.captureStackTrace){
			Error.captureStackTrace(deferred, Deferred);
			Error.captureStackTrace(promise, Deferred);
		}

		this.isResolved = promise.isResolved = function(){
			// summary:
			//		Checks whether the deferred has been resolved.
			// returns: Boolean

			return fulfilled === RESOLVED;
		};

		this.isRejected = promise.isRejected = function(){
			// summary:
			//		Checks whether the deferred has been rejected.
			// returns: Boolean

			return fulfilled === REJECTED;
		};

		this.isFulfilled = promise.isFulfilled = function(){
			// summary:
			//		Checks whether the deferred has been resolved or rejected.
			// returns: Boolean

			return !!fulfilled;
		};

		this.isCanceled = promise.isCanceled = function(){
			// summary:
			//		Checks whether the deferred has been canceled.
			// returns: Boolean

			return canceled;
		};

		this.progress = function(update, strict){
			// summary:
			//		Emit a progress update on the deferred.
			// description:
			//		Emit a progress update on the deferred. Progress updates
			//		can be used to communicate updates about the asynchronous
			//		operation before it has finished.
			// update: any
			//		The progress update. Passed to progbacks.
			// strict: Boolean?
			//		If strict, will throw an error if the deferred has already
			//		been fulfilled and consequently no progress can be emitted.
			// returns: dojo/promise/Promise
			//		Returns the original promise for the deferred.

			if(!fulfilled){
				signalWaiting(waiting, PROGRESS, update, null, deferred);
				return promise;
			}else if(strict === true){
				throw new Error(FULFILLED_ERROR_MESSAGE);
			}else{
				return promise;
			}
		};

		this.resolve = function(value, strict){
			// summary:
			//		Resolve the deferred.
			// description:
			//		Resolve the deferred, putting it in a success state.
			// value: any
			//		The result of the deferred. Passed to callbacks.
			// strict: Boolean?
			//		If strict, will throw an error if the deferred has already
			//		been fulfilled and consequently cannot be resolved.
			// returns: dojo/promise/Promise
			//		Returns the original promise for the deferred.

			if(!fulfilled){
				// Set fulfilled, store value. After signaling waiting listeners unset
				// waiting.
				signalWaiting(waiting, fulfilled = RESOLVED, result = value, null, deferred);
				waiting = null;
				return promise;
			}else if(strict === true){
				throw new Error(FULFILLED_ERROR_MESSAGE);
			}else{
				return promise;
			}
		};

		var reject = this.reject = function(error, strict){
			// summary:
			//		Reject the deferred.
			// description:
			//		Reject the deferred, putting it in an error state.
			// error: any
			//		The error result of the deferred. Passed to errbacks.
			// strict: Boolean?
			//		If strict, will throw an error if the deferred has already
			//		been fulfilled and consequently cannot be rejected.
			// returns: dojo/promise/Promise
			//		Returns the original promise for the deferred.

			if(!fulfilled){
				if( 1  && Error.captureStackTrace){
					Error.captureStackTrace(rejection = {}, reject);
				}
				signalWaiting(waiting, fulfilled = REJECTED, result = error, rejection, deferred);
				waiting = null;
				return promise;
			}else if(strict === true){
				throw new Error(FULFILLED_ERROR_MESSAGE);
			}else{
				return promise;
			}
		};

		this.then = promise.then = function(callback, errback, progback){
			// summary:
			//		Add new callbacks to the deferred.
			// description:
			//		Add new callbacks to the deferred. Callbacks can be added
			//		before or after the deferred is fulfilled.
			// callback: Function?
			//		Callback to be invoked when the promise is resolved.
			//		Receives the resolution value.
			// errback: Function?
			//		Callback to be invoked when the promise is rejected.
			//		Receives the rejection error.
			// progback: Function?
			//		Callback to be invoked when the promise emits a progress
			//		update. Receives the progress update.
			// returns: dojo/promise/Promise
			//		Returns a new promise for the result of the callback(s).
			//		This can be used for chaining many asynchronous operations.

			var listener = [progback, callback, errback];
			// Ensure we cancel the promise we're waiting for, or if callback/errback
			// have returned a promise, cancel that one.
			listener.cancel = promise.cancel;
			listener.deferred = new Deferred(function(reason){
				// Check whether cancel is really available, returned promises are not
				// required to expose `cancel`
				return listener.cancel && listener.cancel(reason);
			});
			if(fulfilled && !waiting){
				signalListener(listener, fulfilled, result, rejection);
			}else{
				waiting.push(listener);
			}
			return listener.deferred.promise;
		};

		this.cancel = promise.cancel = function(reason, strict){
			// summary:
			//		Inform the deferred it may cancel its asynchronous operation.
			// description:
			//		Inform the deferred it may cancel its asynchronous operation.
			//		The deferred's (optional) canceler is invoked and the
			//		deferred will be left in a rejected state. Can affect other
			//		promises that originate with the same deferred.
			// reason: any
			//		A message that may be sent to the deferred's canceler,
			//		explaining why it's being canceled.
			// strict: Boolean?
			//		If strict, will throw an error if the deferred has already
			//		been fulfilled and consequently cannot be canceled.
			// returns: any
			//		Returns the rejection reason if the deferred was canceled
			//		normally.

			if(!fulfilled){
				// Cancel can be called even after the deferred is fulfilled
				if(canceler){
					var returnedReason = canceler(reason);
					reason = typeof returnedReason === "undefined" ? reason : returnedReason;
				}
				canceled = true;
				if(!fulfilled){
					// Allow canceler to provide its own reason, but fall back to a CancelError
					if(typeof reason === "undefined"){
						reason = new CancelError();
					}
					reject(reason);
					return reason;
				}else if(fulfilled === REJECTED && result === reason){
					return reason;
				}
			}else if(strict === true){
				throw new Error(FULFILLED_ERROR_MESSAGE);
			}
		};

		freezeObject(promise);
	};

	Deferred.prototype.toString = function(){
		// returns: String
		//		Returns `[object Deferred]`.

		return "[object Deferred]";
	};

	if(instrumentation){
		instrumentation(Deferred);
	}

	return Deferred;
});

},
'dojo/aspect':function(){
define([], function(){

	// module:
	//		dojo/aspect

	"use strict";
	var undefined, nextId = 0;
	function advise(dispatcher, type, advice, receiveArguments){
		var previous = dispatcher[type];
		var around = type == "around";
		var signal;
		if(around){
			var advised = advice(function(){
				return previous.advice(this, arguments);
			});
			signal = {
				remove: function(){
					if(advised){
						advised = dispatcher = advice = null;
					}
				},
				advice: function(target, args){
					return advised ?
						advised.apply(target, args) :  // called the advised function
						previous.advice(target, args); // cancelled, skip to next one
				}
			};
		}else{
			// create the remove handler
			signal = {
				remove: function(){
					if(signal.advice){
						var previous = signal.previous;
						var next = signal.next;
						if(!next && !previous){
							delete dispatcher[type];
						}else{
							if(previous){
								previous.next = next;
							}else{
								dispatcher[type] = next;
							}
							if(next){
								next.previous = previous;
							}
						}

						// remove the advice to signal that this signal has been removed
						dispatcher = advice = signal.advice = null;
					}
				},
				id: nextId++,
				advice: advice,
				receiveArguments: receiveArguments
			};
		}
		if(previous && !around){
			if(type == "after"){
				// add the listener to the end of the list
				// note that we had to change this loop a little bit to workaround a bizarre IE10 JIT bug
				while(previous.next && (previous = previous.next)){}
				previous.next = signal;
				signal.previous = previous;
			}else if(type == "before"){
				// add to beginning
				dispatcher[type] = signal;
				signal.next = previous;
				previous.previous = signal;
			}
		}else{
			// around or first one just replaces
			dispatcher[type] = signal;
		}
		return signal;
	}
	function aspect(type){
		return function(target, methodName, advice, receiveArguments){
			var existing = target[methodName], dispatcher;
			if(!existing || existing.target != target){
				// no dispatcher in place
				target[methodName] = dispatcher = function(){
					var executionId = nextId;
					// before advice
					var args = arguments;
					var before = dispatcher.before;
					while(before){
						args = before.advice.apply(this, args) || args;
						before = before.next;
					}
					// around advice
					if(dispatcher.around){
						var results = dispatcher.around.advice(this, args);
					}
					// after advice
					var after = dispatcher.after;
					while(after && after.id < executionId){
						if(after.receiveArguments){
							var newResults = after.advice.apply(this, args);
							// change the return value only if a new value was returned
							results = newResults === undefined ? results : newResults;
						}else{
							results = after.advice.call(this, results, args);
						}
						after = after.next;
					}
					return results;
				};
				if(existing){
					dispatcher.around = {advice: function(target, args){
						return existing.apply(target, args);
					}};
				}
				dispatcher.target = target;
			}
			var results = advise((dispatcher || existing), type, advice, receiveArguments);
			advice = null;
			return results;
		};
	}

	// TODOC: after/before/around return object

	var after = aspect("after");
	/*=====
	after = function(target, methodName, advice, receiveArguments){
		// summary:
		//		The "after" export of the aspect module is a function that can be used to attach
		//		"after" advice to a method. This function will be executed after the original method
		//		is executed. By default the function will be called with a single argument, the return
		//		value of the original method, or the the return value of the last executed advice (if a previous one exists).
		//		The fourth (optional) argument can be set to true to so the function receives the original
		//		arguments (from when the original method was called) rather than the return value.
		//		If there are multiple "after" advisors, they are executed in the order they were registered.
		// target: Object
		//		This is the target object
		// methodName: String
		//		This is the name of the method to attach to.
		// advice: Function
		//		This is function to be called after the original method
		// receiveArguments: Boolean?
		//		If this is set to true, the advice function receives the original arguments (from when the original mehtod
		//		was called) rather than the return value of the original/previous method.
		// returns:
		//		A signal object that can be used to cancel the advice. If remove() is called on this signal object, it will
		//		stop the advice function from being executed.
	};
	=====*/

	var before = aspect("before");
	/*=====
	before = function(target, methodName, advice){
		// summary:
		//		The "before" export of the aspect module is a function that can be used to attach
		//		"before" advice to a method. This function will be executed before the original method
		//		is executed. This function will be called with the arguments used to call the method.
		//		This function may optionally return an array as the new arguments to use to call
		//		the original method (or the previous, next-to-execute before advice, if one exists).
		//		If the before method doesn't return anything (returns undefined) the original arguments
		//		will be preserved.
		//		If there are multiple "before" advisors, they are executed in the reverse order they were registered.
		// target: Object
		//		This is the target object
		// methodName: String
		//		This is the name of the method to attach to.
		// advice: Function
		//		This is function to be called before the original method
	};
	=====*/

	var around = aspect("around");
	/*=====
	 around = function(target, methodName, advice){
		// summary:
		//		The "around" export of the aspect module is a function that can be used to attach
		//		"around" advice to a method. The advisor function is immediately executed when
		//		the around() is called, is passed a single argument that is a function that can be
		//		called to continue execution of the original method (or the next around advisor).
		//		The advisor function should return a function, and this function will be called whenever
		//		the method is called. It will be called with the arguments used to call the method.
		//		Whatever this function returns will be returned as the result of the method call (unless after advise changes it).
		// example:
		//		If there are multiple "around" advisors, the most recent one is executed first,
		//		which can then delegate to the next one and so on. For example:
		//		|	around(obj, "foo", function(originalFoo){
		//		|		return function(){
		//		|			var start = new Date().getTime();
		//		|			var results = originalFoo.apply(this, arguments); // call the original
		//		|			var end = new Date().getTime();
		//		|			console.log("foo execution took " + (end - start) + " ms");
		//		|			return results;
		//		|		};
		//		|	});
		// target: Object
		//		This is the target object
		// methodName: String
		//		This is the name of the method to attach to.
		// advice: Function
		//		This is function to be called around the original method
	};
	=====*/

	return {
		// summary:
		//		provides aspect oriented programming functionality, allowing for
		//		one to add before, around, or after advice on existing methods.
		// example:
		//	|	define(["dojo/aspect"], function(aspect){
		//	|		var signal = aspect.after(targetObject, "methodName", function(someArgument){
		//	|			this will be called when targetObject.methodName() is called, after the original function is called
		//	|		});
		//
		// example:
		//	The returned signal object can be used to cancel the advice.
		//	|	signal.remove(); // this will stop the advice from being executed anymore
		//	|	aspect.before(targetObject, "methodName", function(someArgument){
		//	|		// this will be called when targetObject.methodName() is called, before the original function is called
		//	|	 });

		before: before,
		around: around,
		after: after
	};
});

},
'dojo/when':function(){
define([
	"./Deferred",
	"./promise/Promise"
], function(Deferred, Promise){
	"use strict";

	// module:
	//		dojo/when

	return function when(valueOrPromise, callback, errback, progback){
		// summary:
		//		Transparently applies callbacks to values and/or promises.
		// description:
		//		Accepts promises but also transparently handles non-promises. If no
		//		callbacks are provided returns a promise, regardless of the initial
		//		value. Foreign promises are converted.
		//
		//		If callbacks are provided and the initial value is not a promise,
		//		the callback is executed immediately with no error handling. Returns
		//		a promise if the initial value is a promise, or the result of the
		//		callback otherwise.
		// valueOrPromise:
		//		Either a regular value or an object with a `then()` method that
		//		follows the Promises/A specification.
		// callback: Function?
		//		Callback to be invoked when the promise is resolved, or a non-promise
		//		is received.
		// errback: Function?
		//		Callback to be invoked when the promise is rejected.
		// progback: Function?
		//		Callback to be invoked when the promise emits a progress update.
		// returns: dojo/promise/Promise
		//		Promise, or if a callback is provided, the result of the callback.

		var receivedPromise = valueOrPromise && typeof valueOrPromise.then === "function";
		var nativePromise = receivedPromise && valueOrPromise instanceof Promise;

		if(!receivedPromise){
			if(arguments.length > 1){
				return callback ? callback(valueOrPromise) : valueOrPromise;
			}else{
				return new Deferred().resolve(valueOrPromise);
			}
		}else if(!nativePromise){
			var deferred = new Deferred(valueOrPromise.cancel);
			valueOrPromise.then(deferred.resolve, deferred.reject, deferred.progress);
			valueOrPromise = deferred.promise;
		}

		if(callback || errback || progback){
			return valueOrPromise.then(callback, errback, progback);
		}
		return valueOrPromise;
	};
});

},
'xstyle/css':function(){
define(["require"], function(moduleRequire){
"use strict";
/*
 * AMD css! plugin
 * This plugin will load and wait for css files.  This could be handy when
 * loading css files as part of a layer or as a way to apply a run-time theme. This
 * module checks to see if the CSS is already loaded before incurring the cost
 * of loading the full CSS loader codebase
 */
 	function testElementStyle(tag, id, property){
 		// test an element's style
		var docElement = document.documentElement;
		var testDiv = docElement.insertBefore(document.createElement(tag), docElement.firstChild);
		testDiv.id = id;
		var styleValue = (testDiv.currentStyle || getComputedStyle(testDiv, null))[property];
		docElement.removeChild(testDiv);
 		return styleValue;
 	} 
 	return {
		load: function(resourceDef, require, callback, config) {
			var url = require.toUrl(resourceDef);
			var cachedCss = require.cache && require.cache['url:' + url];
			if(cachedCss){
				// we have CSS cached inline in the build
				if(cachedCss.xCss){
					var parser = cachedCss.parser;
					var xCss =cachedCss.xCss;
					cachedCss = cachedCss.cssText;
				}
				moduleRequire(['./util/createStyleSheet'],function(createStyleSheet){
					createStyleSheet(cachedCss);
				});
				if(xCss){
					//require([parsed], callback);
				}
				return checkForParser();
			}
			function checkForParser(){
				var parser = testElementStyle('x-parse', null, 'content');
				if(parser && parser != 'none'){
					// TODO: wait for parser to load
					require([eval(parser)], callback);
				}else{
					callback();
				}
			}
			
			// if there is an id test available, see if the referenced rule is already loaded,
			// and if so we can completely avoid any dynamic CSS loading. If it is
			// not present, we need to use the dynamic CSS loader.
			var displayStyle = testElementStyle('div', resourceDef.replace(/\//g,'-').replace(/\..*/,'') + "-loaded", 'display');
			if(displayStyle == "none"){
				return checkForParser();
			}
			// use dynamic loader
			moduleRequire(["./core/load-css"], function(load){
				load(url, checkForParser);
			});
		}
	};
});

},
'dojo/on':function(){
define(["./has!dom-addeventlistener?:./aspect", "./_base/kernel", "./sniff"], function(aspect, dojo, has){

	"use strict";
	if( 1 ){ // check to make sure we are in a browser, this module should work anywhere
		var major = window.ScriptEngineMajorVersion;
		has.add("jscript", major && (major() + ScriptEngineMinorVersion() / 10));
		has.add("event-orientationchange", has("touch") && !has("android")); // TODO: how do we detect this?
		has.add("event-stopimmediatepropagation", window.Event && !!window.Event.prototype && !!window.Event.prototype.stopImmediatePropagation);
		has.add("event-focusin", function(global, doc, element){
			// All browsers except firefox support focusin, but too hard to feature test webkit since element.onfocusin
			// is undefined.  Just return true for IE and use fallback path for other browsers.
			return !!element.attachEvent;
		});
	}
	var on = function(target, type, listener, dontFix){
		// summary:
		//		A function that provides core event listening functionality. With this function
		//		you can provide a target, event type, and listener to be notified of
		//		future matching events that are fired.
		// target: Element|Object
		//		This is the target object or DOM element that to receive events from
		// type: String|Function
		//		This is the name of the event to listen for or an extension event type.
		// listener: Function
		//		This is the function that should be called when the event fires.
		// returns: Object
		//		An object with a remove() method that can be used to stop listening for this
		//		event.
		// description:
		//		To listen for "click" events on a button node, we can do:
		//		|	define(["dojo/on"], function(listen){
		//		|		on(button, "click", clickHandler);
		//		|		...
		//		Evented JavaScript objects can also have their own events.
		//		|	var obj = new Evented;
		//		|	on(obj, "foo", fooHandler);
		//		And then we could publish a "foo" event:
		//		|	on.emit(obj, "foo", {key: "value"});
		//		We can use extension events as well. For example, you could listen for a tap gesture:
		//		|	define(["dojo/on", "dojo/gesture/tap", function(listen, tap){
		//		|		on(button, tap, tapHandler);
		//		|		...
		//		which would trigger fooHandler. Note that for a simple object this is equivalent to calling:
		//		|	obj.onfoo({key:"value"});
		//		If you use on.emit on a DOM node, it will use native event dispatching when possible.

		if(typeof target.on == "function" && typeof type != "function" && !target.nodeType){
			// delegate to the target's on() method, so it can handle it's own listening if it wants (unless it 
			// is DOM node and we may be dealing with jQuery or Prototype's incompatible addition to the
			// Element prototype 
			return target.on(type, listener);
		}
		// delegate to main listener code
		return on.parse(target, type, listener, addListener, dontFix, this);
	};
	on.pausable =  function(target, type, listener, dontFix){
		// summary:
		//		This function acts the same as on(), but with pausable functionality. The
		//		returned signal object has pause() and resume() functions. Calling the
		//		pause() method will cause the listener to not be called for future events. Calling the
		//		resume() method will cause the listener to again be called for future events.
		var paused;
		var signal = on(target, type, function(){
			if(!paused){
				return listener.apply(this, arguments);
			}
		}, dontFix);
		signal.pause = function(){
			paused = true;
		};
		signal.resume = function(){
			paused = false;
		};
		return signal;
	};
	on.once = function(target, type, listener, dontFix){
		// summary:
		//		This function acts the same as on(), but will only call the listener once. The 
		//		listener will be called for the first
		//		event that takes place and then listener will automatically be removed.
		var signal = on(target, type, function(){
			// remove this listener
			signal.remove();
			// proceed to call the listener
			return listener.apply(this, arguments);
		});
		return signal;
	};
	on.parse = function(target, type, listener, addListener, dontFix, matchesTarget){
		if(type.call){
			// event handler function
			// on(node, touch.press, touchListener);
			return type.call(matchesTarget, target, listener);
		}

		if(type.indexOf(",") > -1){
			// we allow comma delimited event names, so you can register for multiple events at once
			var events = type.split(/\s*,\s*/);
			var handles = [];
			var i = 0;
			var eventName;
			while(eventName = events[i++]){
				handles.push(addListener(target, eventName, listener, dontFix, matchesTarget));
			}
			handles.remove = function(){
				for(var i = 0; i < handles.length; i++){
					handles[i].remove();
				}
			};
			return handles;
		}
		return addListener(target, type, listener, dontFix, matchesTarget);
	};
	var touchEvents = /^touch/;
	function addListener(target, type, listener, dontFix, matchesTarget){
		// event delegation:
		var selector = type.match(/(.*):(.*)/);
		// if we have a selector:event, the last one is interpreted as an event, and we use event delegation
		if(selector){
			type = selector[2];
			selector = selector[1];
			// create the extension event for selectors and directly call it
			return on.selector(selector, type).call(matchesTarget, target, listener);
		}
		// test to see if it a touch event right now, so we don't have to do it every time it fires
		if(has("touch")){
			if(touchEvents.test(type)){
				// touch event, fix it
				listener = fixTouchListener(listener);
			}
			if(!has("event-orientationchange") && (type == "orientationchange")){
				//"orientationchange" not supported <= Android 2.1, 
				//but works through "resize" on window
				type = "resize"; 
				target = window;
				listener = fixTouchListener(listener);
			} 
		}
		if(addStopImmediate){
			// add stopImmediatePropagation if it doesn't exist
			listener = addStopImmediate(listener);
		}
		// normal path, the target is |this|
		if(target.addEventListener){
			// the target has addEventListener, which should be used if available (might or might not be a node, non-nodes can implement this method as well)
			// check for capture conversions
			var capture = type in captures,
				adjustedType = capture ? captures[type] : type;
			target.addEventListener(adjustedType, listener, capture);
			// create and return the signal
			return {
				remove: function(){
					target.removeEventListener(adjustedType, listener, capture);
				}
			};
		}
		type = "on" + type;
		if(fixAttach && target.attachEvent){
			return fixAttach(target, type, listener);
		}
		throw new Error("Target must be an event emitter");
	}

	on.selector = function(selector, eventType, children){
		// summary:
		//		Creates a new extension event with event delegation. This is based on
		//		the provided event type (can be extension event) that
		//		only calls the listener when the CSS selector matches the target of the event.
		//
		//		The application must require() an appropriate level of dojo/query to handle the selector.
		// selector:
		//		The CSS selector to use for filter events and determine the |this| of the event listener.
		// eventType:
		//		The event to listen for
		// children:
		//		Indicates if children elements of the selector should be allowed. This defaults to 
		//		true
		// example:
		// |	require(["dojo/on", "dojo/mouse", "dojo/query!css2"], function(listen, mouse){
		// |		on(node, on.selector(".my-class", mouse.enter), handlerForMyHover);
		return function(target, listener){
			// if the selector is function, use it to select the node, otherwise use the matches method
			var matchesTarget = typeof selector == "function" ? {matches: selector} : this,
				bubble = eventType.bubble;
			function select(eventTarget){
				// see if we have a valid matchesTarget or default to dojo.query
				matchesTarget = matchesTarget && matchesTarget.matches ? matchesTarget : dojo.query;
				// there is a selector, so make sure it matches
				while(!matchesTarget.matches(eventTarget, selector, target)){
					if(eventTarget == target || children === false || !(eventTarget = eventTarget.parentNode) || eventTarget.nodeType != 1){ // intentional assignment
						return;
					}
				}
				return eventTarget;
			}
			if(bubble){
				// the event type doesn't naturally bubble, but has a bubbling form, use that, and give it the selector so it can perform the select itself
				return on(target, bubble(select), listener);
			}
			// standard event delegation
			return on(target, eventType, function(event){
				// call select to see if we match
				var eventTarget = select(event.target);
				// if it matches we call the listener
				return eventTarget && listener.call(eventTarget, event);
			});
		};
	};

	function syntheticPreventDefault(){
		this.cancelable = false;
		this.defaultPrevented = true;
	}
	function syntheticStopPropagation(){
		this.bubbles = false;
	}
	var slice = [].slice,
		syntheticDispatch = on.emit = function(target, type, event){
		// summary:
		//		Fires an event on the target object.
		// target:
		//		The target object to fire the event on. This can be a DOM element or a plain 
		//		JS object. If the target is a DOM element, native event emitting mechanisms
		//		are used when possible.
		// type:
		//		The event type name. You can emulate standard native events like "click" and 
		//		"mouseover" or create custom events like "open" or "finish".
		// event:
		//		An object that provides the properties for the event. See https://developer.mozilla.org/en/DOM/event.initEvent 
		//		for some of the properties. These properties are copied to the event object.
		//		Of particular importance are the cancelable and bubbles properties. The
		//		cancelable property indicates whether or not the event has a default action
		//		that can be cancelled. The event is cancelled by calling preventDefault() on
		//		the event object. The bubbles property indicates whether or not the
		//		event will bubble up the DOM tree. If bubbles is true, the event will be called
		//		on the target and then each parent successively until the top of the tree
		//		is reached or stopPropagation() is called. Both bubbles and cancelable 
		//		default to false.
		// returns:
		//		If the event is cancelable and the event is not cancelled,
		//		emit will return true. If the event is cancelable and the event is cancelled,
		//		emit will return false.
		// details:
		//		Note that this is designed to emit events for listeners registered through
		//		dojo/on. It should actually work with any event listener except those
		//		added through IE's attachEvent (IE8 and below's non-W3C event emitting
		//		doesn't support custom event types). It should work with all events registered
		//		through dojo/on. Also note that the emit method does do any default
		//		action, it only returns a value to indicate if the default action should take
		//		place. For example, emitting a keypress event would not cause a character
		//		to appear in a textbox.
		// example:
		//		To fire our own click event
		//	|	on.emit(dojo.byId("button"), "click", {
		//	|		cancelable: true,
		//	|		bubbles: true,
		//	|		screenX: 33,
		//	|		screenY: 44
		//	|	});
		//		We can also fire our own custom events:
		//	|	on.emit(dojo.byId("slider"), "slide", {
		//	|		cancelable: true,
		//	|		bubbles: true,
		//	|		direction: "left-to-right"
		//	|	});
		var args = slice.call(arguments, 2);
		var method = "on" + type;
		if("parentNode" in target){
			// node (or node-like), create event controller methods
			var newEvent = args[0] = {};
			for(var i in event){
				newEvent[i] = event[i];
			}
			newEvent.preventDefault = syntheticPreventDefault;
			newEvent.stopPropagation = syntheticStopPropagation;
			newEvent.target = target;
			newEvent.type = type;
			event = newEvent;
		}
		do{
			// call any node which has a handler (note that ideally we would try/catch to simulate normal event propagation but that causes too much pain for debugging)
			target[method] && target[method].apply(target, args);
			// and then continue up the parent node chain if it is still bubbling (if started as bubbles and stopPropagation hasn't been called)
		}while(event && event.bubbles && (target = target.parentNode));
		return event && event.cancelable && event; // if it is still true (was cancelable and was cancelled), return the event to indicate default action should happen
	};
	var captures = has("event-focusin") ? {} : {focusin: "focus", focusout: "blur"};
	if(!has("event-stopimmediatepropagation")){
		var stopImmediatePropagation =function(){
			this.immediatelyStopped = true;
			this.modified = true; // mark it as modified so the event will be cached in IE
		};
		var addStopImmediate = function(listener){
			return function(event){
				if(!event.immediatelyStopped){// check to make sure it hasn't been stopped immediately
					event.stopImmediatePropagation = stopImmediatePropagation;
					return listener.apply(this, arguments);
				}
			};
		}
	} 
	if(has("dom-addeventlistener")){
		// emitter that works with native event handling
		on.emit = function(target, type, event){
			if(target.dispatchEvent && document.createEvent){
				// use the native event emitting mechanism if it is available on the target object
				// create a generic event				
				// we could create branch into the different types of event constructors, but 
				// that would be a lot of extra code, with little benefit that I can see, seems 
				// best to use the generic constructor and copy properties over, making it 
				// easy to have events look like the ones created with specific initializers
				var nativeEvent = target.ownerDocument.createEvent("HTMLEvents");
				nativeEvent.initEvent(type, !!event.bubbles, !!event.cancelable);
				// and copy all our properties over
				for(var i in event){
					if(!(i in nativeEvent)){
						nativeEvent[i] = event[i];
					}
				}
				return target.dispatchEvent(nativeEvent) && nativeEvent;
			}
			return syntheticDispatch.apply(on, arguments); // emit for a non-node
		};
	}else{
		// no addEventListener, basically old IE event normalization
		on._fixEvent = function(evt, sender){
			// summary:
			//		normalizes properties on the event object including event
			//		bubbling methods, keystroke normalization, and x/y positions
			// evt:
			//		native event object
			// sender:
			//		node to treat as "currentTarget"
			if(!evt){
				var w = sender && (sender.ownerDocument || sender.document || sender).parentWindow || window;
				evt = w.event;
			}
			if(!evt){return evt;}
			try{
				if(lastEvent && evt.type == lastEvent.type  && evt.srcElement == lastEvent.target){
					// should be same event, reuse event object (so it can be augmented);
					// accessing evt.srcElement rather than evt.target since evt.target not set on IE until fixup below
					evt = lastEvent;
				}
			}catch(e){
				// will occur on IE on lastEvent.type reference if lastEvent points to a previous event that already
				// finished bubbling, but the setTimeout() to clear lastEvent hasn't fired yet
			}
			if(!evt.target){ // check to see if it has been fixed yet
				evt.target = evt.srcElement;
				evt.currentTarget = (sender || evt.srcElement);
				if(evt.type == "mouseover"){
					evt.relatedTarget = evt.fromElement;
				}
				if(evt.type == "mouseout"){
					evt.relatedTarget = evt.toElement;
				}
				if(!evt.stopPropagation){
					evt.stopPropagation = stopPropagation;
					evt.preventDefault = preventDefault;
				}
				switch(evt.type){
					case "keypress":
						var c = ("charCode" in evt ? evt.charCode : evt.keyCode);
						if (c==10){
							// CTRL-ENTER is CTRL-ASCII(10) on IE, but CTRL-ENTER on Mozilla
							c=0;
							evt.keyCode = 13;
						}else if(c==13||c==27){
							c=0; // Mozilla considers ENTER and ESC non-printable
						}else if(c==3){
							c=99; // Mozilla maps CTRL-BREAK to CTRL-c
						}
						// Mozilla sets keyCode to 0 when there is a charCode
						// but that stops the event on IE.
						evt.charCode = c;
						_setKeyChar(evt);
						break;
				}
			}
			return evt;
		};
		var lastEvent, IESignal = function(handle){
			this.handle = handle;
		};
		IESignal.prototype.remove = function(){
			delete _dojoIEListeners_[this.handle];
		};
		var fixListener = function(listener){
			// this is a minimal function for closing on the previous listener with as few as variables as possible
			return function(evt){
				evt = on._fixEvent(evt, this);
				var result = listener.call(this, evt);
				if(evt.modified){
					// cache the last event and reuse it if we can
					if(!lastEvent){
						setTimeout(function(){
							lastEvent = null;
						});
					}
					lastEvent = evt;
				}
				return result;
			};
		};
		var fixAttach = function(target, type, listener){
			listener = fixListener(listener);
			if(((target.ownerDocument ? target.ownerDocument.parentWindow : target.parentWindow || target.window || window) != top || 
						has("jscript") < 5.8) && 
					!has("config-_allow_leaks")){
				// IE will leak memory on certain handlers in frames (IE8 and earlier) and in unattached DOM nodes for JScript 5.7 and below.
				// Here we use global redirection to solve the memory leaks
				if(typeof _dojoIEListeners_ == "undefined"){
					_dojoIEListeners_ = [];
				}
				var emitter = target[type];
				if(!emitter || !emitter.listeners){
					var oldListener = emitter;
					emitter = Function('event', 'var callee = arguments.callee; for(var i = 0; i<callee.listeners.length; i++){var listener = _dojoIEListeners_[callee.listeners[i]]; if(listener){listener.call(this,event);}}');
					emitter.listeners = [];
					target[type] = emitter;
					emitter.global = this;
					if(oldListener){
						emitter.listeners.push(_dojoIEListeners_.push(oldListener) - 1);
					}
				}
				var handle;
				emitter.listeners.push(handle = (emitter.global._dojoIEListeners_.push(listener) - 1));
				return new IESignal(handle);
			}
			return aspect.after(target, type, listener, true);
		};

		var _setKeyChar = function(evt){
			evt.keyChar = evt.charCode ? String.fromCharCode(evt.charCode) : '';
			evt.charOrCode = evt.keyChar || evt.keyCode;
		};
		// Called in Event scope
		var stopPropagation = function(){
			this.cancelBubble = true;
		};
		var preventDefault = on._preventDefault = function(){
			// Setting keyCode to 0 is the only way to prevent certain keypresses (namely
			// ctrl-combinations that correspond to menu accelerator keys).
			// Otoh, it prevents upstream listeners from getting this information
			// Try to split the difference here by clobbering keyCode only for ctrl
			// combinations. If you still need to access the key upstream, bubbledKeyCode is
			// provided as a workaround.
			this.bubbledKeyCode = this.keyCode;
			if(this.ctrlKey){
				try{
					// squelch errors when keyCode is read-only
					// (e.g. if keyCode is ctrl or shift)
					this.keyCode = 0;
				}catch(e){
				}
			}
			this.defaultPrevented = true;
			this.returnValue = false;
			this.modified = true; // mark it as modified  (for defaultPrevented flag) so the event will be cached in IE
		};
	}
	if(has("touch")){ 
		var Event = function(){};
		var windowOrientation = window.orientation; 
		var fixTouchListener = function(listener){ 
			return function(originalEvent){ 
				//Event normalization(for ontouchxxx and resize): 
				//1.incorrect e.pageX|pageY in iOS 
				//2.there are no "e.rotation", "e.scale" and "onorientationchange" in Android
				//3.More TBD e.g. force | screenX | screenX | clientX | clientY | radiusX | radiusY

				// see if it has already been corrected
				var event = originalEvent.corrected;
				if(!event){
					var type = originalEvent.type;
					try{
						delete originalEvent.type; // on some JS engines (android), deleting properties make them mutable
					}catch(e){} 
					if(originalEvent.type){
						// deleting properties doesn't work (older iOS), have to use delegation
						if(has('mozilla')){
							// Firefox doesn't like delegated properties, so we have to copy
							var event = {};
							for(var name in originalEvent){
								event[name] = originalEvent[name];
							}
						}else{
							// old iOS branch
							Event.prototype = originalEvent;
							var event = new Event;
						}
						// have to delegate methods to make them work
						event.preventDefault = function(){
							originalEvent.preventDefault();
						};
						event.stopPropagation = function(){
							originalEvent.stopPropagation();
						};
					}else{
						// deletion worked, use property as is
						event = originalEvent;
						event.type = type;
					}
					originalEvent.corrected = event;
					if(type == 'resize'){
						if(windowOrientation == window.orientation){ 
							return null;//double tap causes an unexpected 'resize' in Android
						} 
						windowOrientation = window.orientation;
						event.type = "orientationchange"; 
						return listener.call(this, event);
					}
					// We use the original event and augment, rather than doing an expensive mixin operation
					if(!("rotation" in event)){ // test to see if it has rotation
						event.rotation = 0; 
						event.scale = 1;
					}
					//use event.changedTouches[0].pageX|pageY|screenX|screenY|clientX|clientY|target
					var firstChangeTouch = event.changedTouches[0];
					for(var i in firstChangeTouch){ // use for-in, we don't need to have dependency on dojo/_base/lang here
						delete event[i]; // delete it first to make it mutable
						event[i] = firstChangeTouch[i];
					}
				}
				return listener.call(this, event); 
			}; 
		}; 
	}
	return on;
});

}}});
define("dgrid/dgrid", [], 1);
