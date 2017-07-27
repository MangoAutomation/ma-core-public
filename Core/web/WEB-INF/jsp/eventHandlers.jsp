<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@page import="com.serotonin.m2m2.module.EventTypeDefinition"%>
<%@page import="com.serotonin.m2m2.module.ModuleRegistry"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.Common"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.handlers.EmailEventHandlerDefinition"%>
<%@page import="com.serotonin.m2m2.vo.event.EmailEventHandlerVO"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.handlers.SetPointEventHandlerDefinition"%>
<%@page import="com.serotonin.m2m2.vo.event.SetPointEventHandlerVO"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.handlers.ProcessEventHandlerDefinition"%>
<%@page import="com.serotonin.m2m2.vo.event.ProcessEventHandlerVO"%>

<%@page import="com.serotonin.m2m2.DataTypes"%>
<c:set var="NEW_ID"><%= Common.NEW_ID %></c:set>

<tag:page showHeader="${param.showHeader}" showToolbar="${param.showToolbar}" dwr="EventHandlersDwr" js="/resources/emailRecipients.js">
<jsp:attribute name="styles">
  <style>
    html > body .dijitTreeNodeLabelSelected { background-color: inherit; color: inherit; }
    .dijitTreeIcon { display: none; }
  </style>
</jsp:attribute>

<jsp:body>
  <script>
    dojo.require("dijit.Tree");
    dojo.require("dijit.tree.TreeStoreModel");
    dojo.require("dojo.data.ItemFileWriteStore");
    dojo.require("dojo.store.Memory");
    dojo.require("dijit.form.FilteringSelect");
    dojo.require("dijit.form.ComboBox");
    
    var allPoints;
    var defaultHandlerId;
    var emailRecipients;
    var escalRecipients;
    var inactiveRecipients;
    var store;
    var targetPointSelector,activePointSelector,inactivePointSelector;
    
    var contextArray = new Array(); 
    
    // Define a convenience function for unwrapping values in the store.
    function $$(item, attr, value) {
        if (typeof(value) == "undefined")
            // Get
            return store.getValue(item, attr);
        // Set
        item[attr][0] = value;
    };
    var tree;
    
    dojo.ready(function() {
        EventHandlersDwr.getInitData(function(data) {
            <c:if test="${!empty param.ehid}">
              defaultHandlerId = ${param.ehid};
            </c:if>
            
            var i, j, k;
            var dp, ds, p, et;
            var pointNode, dataSourceNode, publisherNode, etNode, wid;
            
            allPoints = data.allPoints;
            
            dojo.forEach(allPoints, function(item) { item.fancyName = item.name; });
            
            //Create the filtering Selects for the points
            targetPointSelector = new dijit.form.FilteringSelect({
                store: null,
                searchAttr: "name",                  
                autoComplete: false,
                style: "width: 100%",
                highlightMatch: "all",
                queryExpr: "*\${0}*",
                onChange: function(point) {
                    if (this.item) {
                    	targetPointSelectChanged();
                    }
                },
                required: true
            }, "targetPointSelect");
            activePointSelector = new dijit.form.FilteringSelect({
                store: null,
                searchAttr: "name",                  
                autoComplete: false,
                style: "width: 100%",
                highlightMatch: "all",
                queryExpr: "*\${0}*",
                required: true
            }, "activePointId");
            inactivePointSelector = new dijit.form.FilteringSelect({
                store: null,
                searchAttr: "name",                  
                autoComplete: false,
                style: "width: 100%",
                highlightMatch: "all",
                queryExpr: "*\${0}*",
                required: true
            }, "inactivePointId");
            emailAdditionalContextSelector = new dijit.form.ComboBox({
                store: new dojo.store.Memory({data: allPoints}),
                labelType: "html",
                labelAttr: "fancyName",
                searchAttr: "name",
                autoComplete: false,
                style: "width: 254px;",
                queryExpr: "*\${0}*",
                highlightMatch: "all",
                required: false,
                dropDownPosition: ["above"],
                onChange: function(point) {
                    if (this.item) {
                        selectPoint(this.item.id);
                        this.loadAndOpenDropDown();
                        this.set('displayedValue',pointLookupText);
                        if(typeof(this._startSearch) == 'function')
                            this._startSearch(pointLookupText); //Dangerous because could change, but works!
                   }
                },
                onKeyUp: function(event){
                    pointLookupText = this.get('displayedValue');
                }
            }, "emailAdditionalContextSelector");
            setpointAdditionalContextSelector = new dijit.form.ComboBox({
                store: new dojo.store.Memory({data: allPoints}),
                labelType: "html",
                labelAttr: "fancyName",
                searchAttr: "name",
                autoComplete: false,
                style: "width: 254px;",
                queryExpr: "*\${0}*",
                highlightMatch: "all",
                required: false,
                dropDownPosition: ["above"],
                onChange: function(point) {
                    if (this.item) {
                        selectPoint(this.item.id);
                        this.loadAndOpenDropDown();
                        this.set('displayedValue',pointLookupText);
                        if(typeof(this._startSearch) == 'function')
                            this._startSearch(pointLookupText); //Dangerous because could change, but works!
                   }
                },
                onKeyUp: function(event){
                    pointLookupText = this.get('displayedValue');
                }
            }, "setpointAdditionalContextSelector");
            
            emailRecipients = new mango.erecip.EmailRecipients("emailRecipients",
                    "<m2m2:translate key="eventHandlers.recipTestEmailMessage" escapeDQuotes="true"/>",
                    data.mailingLists, data.users);
            emailRecipients.write("emailRecipients", "emailRecipients", null,
                    "<m2m2:translate key="eventHandlers.emailRecipients" escapeDQuotes="true"/>");
            
            escalRecipients = new mango.erecip.EmailRecipients("escalRecipients",
                    "<m2m2:translate key="eventHandlers.escalTestEmailMessage" escapeDQuotes="true"/>",
                    data.mailingLists, data.users);
            escalRecipients.write("escalRecipients", "escalRecipients", "escalationAddresses2",
                    "<m2m2:translate key="eventHandlers.escalRecipients" escapeDQuotes="true"/>");
            
            inactiveRecipients = new mango.erecip.EmailRecipients("inactiveRecipients",
                    "<m2m2:translate key="eventHandlers.inactiveTestEmailMessage" escapeDQuotes="true"/>",
                    data.mailingLists, data.users);
            inactiveRecipients.write("inactiveRecipients", "inactiveRecipients", "inactiveAddresses2",
                    "<m2m2:translate key="eventHandlers.inactiveRecipients" escapeDQuotes="true"/>");
            
            var storeItems = [];
            
            //
            // Point event detectors
            var pedRoot = {
                    name: '<img src="images/bell.png"/> <fmt:message key="eventHandlers.pointEventDetector"/>',
                    children: []
            };
            storeItems.push(pedRoot);
            
            for (i=0; i<data.dataPoints.length; i++) {
                dp = makeNonTreeItem(data.dataPoints[i]);
                var pointNode = { name: "<img src='images/icon_comp.png'/> "+ dp.name, object: dp };
                pedRoot.children.push(pointNode);
                
                for (j=0; j<dp.eventTypes.length; j++) {
                    et = dp.eventTypes[j];
                    createEventTypeNode("ped"+ et.typeRef2, et, pointNode);
                }
            }
            
            //
            // Data source events
            var dataSourceRoot = {
                    name: '<fmt:message key="eventHandlers.dataSourceEvents"/>',
                    children: []
            };
            storeItems.push(dataSourceRoot);
            
            for (i=0; i<data.dataSources.length; i++) {
                ds = makeNonTreeItem(data.dataSources[i]);
                var dataSourceNode = { name: "<img src='images/icon_ds.png'/> "+ ds.name, object: ds };
                dataSourceRoot.children.push(dataSourceNode);
                
                for (j=0; j<ds.eventTypes.length; j++) {
                    et = ds.eventTypes[j];
                    createEventTypeNode("dse"+ et.typeRef1 +"/"+ et.typeRef2, et, dataSourceNode);
                }
            }
            
            //
            // User-accessible module-defined events.
            if (data.userEventTypes) {
                for (type in data.userEventTypes) {
                    var info = data.userEventTypes[type];
                    
                    var name = info.description;
                    if (info.iconPath)
                        name = '<img src="'+ info.iconPath +'"/> '+ name;
                    
                    var etRoot = { name: name, children: [] };
                    storeItems.push(etRoot);
                    
                    for (i=0; i<info.vos.length; i++) {
                        et = info.vos[i];
                        createEventTypeNode(type +"/"+ et.subtype +"/"+ et.typeRef1 +"/"+ et.typeRef2, et, etRoot);
                    }
                }
            }
            
            //
            // Publisher events
            if (data.publishers) {
                var publisherRoot = {
                        name: '<fmt:message key="eventHandlers.publisherEvents"/>',
                        children: []
                };
                storeItems.push(publisherRoot);
                
                for (i=0; i<data.publishers.length; i++) {
                    p = makeNonTreeItem(data.publishers[i]);
                    var publisherNode = { name: "<img src='images/transmit.png'/> "+ p.name, object: p  };
                    publisherRoot.children.push(publisherNode);
                    
                    for (j=0; j<p.eventTypes.length; j++) {
                        et = p.eventTypes[j];
                        createEventTypeNode("pube"+ et.typeRef1 +"/"+ et.typeRef2, et, publisherNode);
                    }
                }
            }
            
            //
            // System events
            if (data.systemEvents) {
                var systemRoot = {
                        name: '<fmt:message key="eventHandlers.systemEvents"/>',
                        children: []
                };
                storeItems.push(systemRoot);
                
                for (i=0; i<data.systemEvents.length; i++) {
                    et = data.systemEvents[i];
                    //createEventTypeNode("sys"+ et.typeRef1, et, systemRoot);
                    createEventTypeNode("sys"+ et.subtype, et, systemRoot);
                }
            }
            
            //
            // Audit events
            if (data.auditEvents) {
                var auditRoot = {
                        name: '<fmt:message key="eventHandlers.auditEvents"/>',
                        children: []
                };
                storeItems.push(auditRoot);
                
                for (i=0; i<data.auditEvents.length; i++) {
                    et = data.auditEvents[i];
                    //createEventTypeNode("aud"+ et.typeRef1, et, auditRoot);
                    createEventTypeNode("aud"+ et.subtype, et, auditRoot);
                }
            }
            
            //
            // Admin-accessible module-defined events.
            if (data.adminEventTypes) {
                for (type in data.adminEventTypes) {
                    var info = data.adminEventTypes[type];
                    
                    var name = info.description;
                    if (info.iconPath)
                        name = '<img src="'+ info.iconPath +'"/> '+ name;
                    
                    var etRoot = { name: name, children: [] };
                    storeItems.push(etRoot);
                    
                    for (i=0; i<info.vos.length; i++) {
                        et = info.vos[i];
                        createEventTypeNode(type +"/"+ et.subtype +"/"+ et.typeRef1 +"/"+ et.typeRef2, et, etRoot);
                    }
                }
            }
            
            function createEventTypeNode(widgetId, eventType, parent) {
                makeNonTreeItem(eventType);
                var node = {
                        name: "<img id='"+ widgetId +"Img'/> "+ eventType.description,
                        rawNode: true,
                        eventTypeNode: true,
                        widgetId: widgetId,
                        object: eventType
                };
                if (!parent.children)
                    parent.children = [];
                parent.children.push(node);
                addHandlerItems(eventType.handlers, node);
            }
            
            function addHandlerItems(handlers, parent) {
                for (var i=0; i<handlers.length; i++) {
                    if (!parent.children)
                        parent.children = [];
                    parent.children.push(createHandlerItem(handlers[i]));
                }
            }
            
            // Create the item store
            store = new dojo.data.ItemFileWriteStore({data: { label: 'name', items: storeItems } });
            
            // Create the tree.
            tree = new dijit.Tree({
                model: new dijit.tree.ForestStoreModel({ store: store }),
                showRoot: false,
                persist: false,
                onClick: function(item, widget) {
                    if (item.eventTypeNode) {
                        selectedEventTypeNode = widget;
                        selectedHandlerNode = null;
                        showHandlerEdit();
                    }
                    else if (item.handlerNode) {
                        selectedHandlerNode = widget;
                        selectedEventTypeNode = widget.getParent();
                        showHandlerEdit();
                    }
                    else
                        hide("handlerEditDiv");
                },
                _createTreeNode: function(args){
                    var tnode = new dijit._TreeNode(args);
                    tnode.labelNode.innerHTML = args.label;
                    return tnode;
                },
                onOpen: function(item, node) {
                    if (item.children) {
                        for (var i=0; i<item.children.length; i++) {
                            var child = item.children[i];
                            if ($$(child, "rawNode")) {
                                setAlarmLevelImg($$(child, "object").alarmLevel, $($$(child, "widgetId") +"Img"));
                                delete child.rawNode;
                            }
                        }
                    }
                }
            }, "eventTypeTree");
            
            hide("loadingImg");
            show("tree");
            
            tree._expandNode(tree.getNodesByItem(pedRoot)[0]);

            // Default the selection if the parameter was provided.
            if (defaultHandlerId) {
                var path = [];
                function findHandler(arr) {
                    for (var i=0; i<arr.length; i++) {
                        var item = arr[i];
                        var wid = $$(item, "widgetId");
                        if (wid && wid.startsWith("handler")) {
                            var id = $$(item, "object").id;
                            if (id == defaultHandlerId) {
                                path.push(item);
                                return true;
                            }
                        }
                        
                        if (item.children) {
                            if (findHandler(item.children)) {
                                path.push(item);
                                return true;
                            }
                        }
                    }
                }
                findHandler(storeItems);
                
                // Path is in reverse order.
                for (var i=path.length-1; i>0; i--)
                    tree._expandNode(tree.getNodesByItem(path[i])[0]);
                
                selectedHandlerNode = tree.getNodesByItem(path[0])[0];
                tree._setSelectedNodesAttr([selectedHandlerNode]);
                tree.onClick(selectedHandlerNode.item, selectedHandlerNode);
            }
            defaultHandler = null;
        });
    });
    
    function createHandlerItem(handler) {
        makeNonTreeItem(handler);
        var img = "images/cog_wrench.png";
        if (handler.handlerType == '<c:out value="<%= EmailEventHandlerDefinition.TYPE_NAME %>"/>')
            img = "images/cog_email.png";
        else if (handler.handlerType == '<c:out value="<%= ProcessEventHandlerDefinition.TYPE_NAME %>"/>')
            img = "images/cog_process.png";
        var item = {
                name: "<img src='"+ img +"'/> <span id='"+ handler.id +"Msg'>"+ handler.message +"</span>",
                widgetId: "handler"+ handler.id,
                object: handler,
                handlerNode: true
        };
        return item;
    }
    
    var selectedEventTypeNode;
    var selectedHandlerNode;
    
    function showHandlerEdit() {
        show("handlerEditDiv");
        setUserMessage("");
        
        // Set the target points.
        targetPointSelector.store = new dojo.store.Memory();
        for (var i=0; i<allPoints.length; i++) {
            dp = allPoints[i];
            dp.fancyName = dp.name
            if (dp.settable)
               targetPointSelector.store.put(dp);
        }        
//         //Default to first in list
//         if(targetPointSelector.store.data.length > 0){
//             //Set selection to first in list
//             targetPointSelector.set('value',targetPointSelector.store[0].id);
//             targetPointSelector.set('displayedValue',targetPointSelector.store[0].name);
//         }
        
        
        if (selectedHandlerNode) {
            $("saveImg").src = "images/save.png";
            show("deleteImg");
            
            // Put values from the handler object into the input controls.
            var handler = $$(selectedHandlerNode.item, "object");
            $set("handlerTypeSelect", handler.handlerType);
            $("handlerTypeSelect").disabled = true;
            $set("xid", handler.xid);
            $set("alias", handler.alias);
            $set("disabled", handler.disabled);
            if (handler.handlerType == '<c:out value="<%= SetPointEventHandlerDefinition.TYPE_NAME %>"/>') {
                targetPointSelector.set('value',handler.targetPointId);
                $set("activeAction", handler.activeAction);
                $set("inactiveAction", handler.inactiveAction);
                activeEditor.setValue(handler.activeScript);
                inactiveEditor.setValue(handler.inactiveScript);
                setScriptPermissions(handler.scriptPermissions);
                contextArray = new Array();
                for(var k = 0; k < handler.additionalContext.length; k+=1)
                	addToContextArray(handler.additionalContext[k].key, handler.additionalContext[k].value)
                writeContextArray("setpointContextTable");
            }
            else if (handler.handlerType == '<c:out value="<%= EmailEventHandlerDefinition.TYPE_NAME %>"/>') {
                emailRecipients.updateRecipientList(handler.activeRecipients);
                $set("sendEscalation", handler.sendEscalation);
                $set("escalationDelayType", handler.escalationDelayType);
                $set("escalationDelay", handler.escalationDelay);
                escalRecipients.updateRecipientList(handler.escalationRecipients);
                $set("sendInactive", handler.sendInactive);
                $set("inactiveOverride", handler.inactiveOverride);
                $set("includeSystemInfo", handler.includeSystemInfo);
                $set("includeLogfile", handler.includeLogfile);
                $set("includePointValueCount", handler.includePointValueCount);
                inactiveRecipients.updateRecipientList(handler.inactiveRecipients);
                $set("customTemplate", handler.customTemplate);
                contextArray = new Array();
                for(var k = 0; k < handler.additionalContext.length; k+=1)
                	addToContextArray(handler.additionalContext[k].key, handler.additionalContext[k].value)
                writeContextArray("emailContextTable");
            }
            else if (handler.handlerType == '<c:out value="<%= ProcessEventHandlerDefinition.TYPE_NAME %>"/>') {
                $set("activeProcessCommand", handler.activeProcessCommand);
                $set("activeProcessTimeout", handler.activeProcessTimeout);
                $set("inactiveProcessCommand", handler.inactiveProcessCommand);
                $set("inactiveProcessTimeout", handler.inactiveProcessTimeout);
            }
        }
        else {
            $("saveImg").src = "images/save_add.png";
            hide("deleteImg");
            $("handlerTypeSelect").disabled = false;
            
            // Clear values that may be left over from another handler.
            $set("xid", "");
            $set("alias", "");
            $set("disabled", false);
            $set("activeAction", <c:out value="<%= SetPointEventHandlerVO.SET_ACTION_NONE %>"/>);
            $set("inactiveAction", <c:out value="<%= SetPointEventHandlerVO.SET_ACTION_NONE %>"/>);
            $set("sendEscalation", false);
            $set("escalationDelayType", <c:out value="<%= Common.TimePeriods.HOURS %>"/>);
            $set("escalationDelay", 1);
            $set("sendInactive", false);
            $set("inactiveOverride", false);
            $set("activeProcessCommand", "");
            $set("activeProcessTimeout", 15);
            $set("inactiveProcessCommand", "");
            $set("inactiveProcessTimeout", 15);
            $set("includePointValueCount", 10);
            $set("customTemplate", "");
            
            // Clear the recipient lists.
            emailRecipients.updateRecipientList();
            escalRecipients.updateRecipientList();
            inactiveRecipients.updateRecipientList();
            contextArray = new Array();
            writeContextArray("emailContextTable");
            writeContextArray("setpointContextTable");
            
            // Clear the script permissions.
            setScriptPermissions({"scriptDataSourcePermission":"", "scriptDataPointReadPermission":"", "scriptDataPointSetPermission":""});
        }
        
        // Set the use source value checkbox.
        handlerTypeChanged();
        activeActionChanged();
        inactiveActionChanged();
        targetPointSelectChanged();
        sendEscalationChanged();
        sendInactiveChanged();
    }
    
    var currentHandlerEditor;
    function handlerTypeChanged() {
        setUserMessage();
        var handlerId = $get("handlerTypeSelect");
        if (currentHandlerEditor) {
            hide(currentHandlerEditor);
            hide($(currentHandlerEditor.id +"Img"));
        }
        currentHandlerEditor = $("handler"+ handlerId);
        show(currentHandlerEditor);
        show($(currentHandlerEditor.id +"Img"));
    }
    
    function targetPointSelectChanged() {
        
        // Make sure there are points in the list.
        if (targetPointSelector.store.data.length === 0)
            return;
        
        // Get the content for the value to set section.
        var targetPointId = targetPointSelector.value;
		if(targetPointId === ''){
			return; //Don't care
		}
        
        var activeValueStr = "";
        var inactiveValueStr = "";
        if (selectedHandlerNode) {
            var handler = $$(selectedHandlerNode.item, "object");
            activeValueStr = handler.activeValueToSet;
            inactiveValueStr = handler.inactiveValueToSet;
        }
        EventHandlersDwr.createSetValueContent(targetPointId, activeValueStr, "Active",
                function(content) { $("activeValueToSetContent").innerHTML = content; });
        EventHandlersDwr.createSetValueContent(targetPointId, inactiveValueStr, "Inactive",
                function(content) { $("inactiveValueToSetContent").innerHTML = content; });
        
        // Update the source point lists.
        var targetDataTypeId = getPoint(targetPointId).dataType;
        //Clear out the active and inactive stores
        activePointSelector.store = new dojo.store.Memory();
        activePointSelector.reset();
		inactivePointSelector.store = new dojo.store.Memory();  
		inactivePointSelector.reset();
		//Add the necessary points
        for (var i=0; i<allPoints.length; i++) {
            dp = allPoints[i];
            if (dp.id != targetPointId && dp.dataType == targetDataTypeId) {
                activePointSelector.store.put(dp);
                inactivePointSelector.store.put(dp);
            }
        }
		//Set the values to the currently selected handler
        if (selectedHandlerNode) {
            var handler = $$(selectedHandlerNode.item, "object");
            activePointSelector.set('value',handler.activePointId);
            inactivePointSelector.set('value',handler.inactivePointId);
        }
    }
    
    function activeActionChanged() {
        var action = $get("activeAction");
        if (action == <c:out value="<%= SetPointEventHandlerVO.SET_ACTION_POINT_VALUE %>"/>) {
            show("activePointIdRow");
            hide("activeValueToSetRow");
            hide("activeScriptRow");
        }
        else if (action == <c:out value="<%= SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE %>"/>) {
            hide("activePointIdRow");
            show("activeValueToSetRow");
            hide("activeScriptRow");
        }
        else if (action == <c:out value="<%= SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE %>"/>) {
            hide("activePointIdRow");
            hide("activeValueToSetRow");
            show("activeScriptRow");
        }
        else {
            hide("activePointIdRow");
            hide("activeValueToSetRow");
            hide("activeScriptRow");
        }
    }
    
    function inactiveActionChanged() {
        var action = $get("inactiveAction");
        if (action == <c:out value="<%= SetPointEventHandlerVO.SET_ACTION_POINT_VALUE %>"/>) {
            show("inactivePointIdRow");
            hide("inactiveValueToSetRow");
            hide("inactiveScriptRow");
        }
        else if (action == <c:out value="<%= SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE %>"/>) {
            hide("inactivePointIdRow");
            show("inactiveValueToSetRow");
            hide("inactiveScriptRow");
        }
        else if (action == <c:out value="<%= SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE %>"/>) {
            hide("inactivePointIdRow");
            hide("inactiveValueToSetRow");
            show("inactiveScriptRow");
        }
        else {
            hide("inactivePointIdRow");
            hide("inactiveValueToSetRow");
            hide("inactiveScriptRow");
        }
    }
    
    function sendEscalationChanged() {
        if ($get("sendEscalation")) {
            show("escalationAddresses1");
            show("escalationAddresses2");
        }
        else {
            hide("escalationAddresses1");
            hide("escalationAddresses2");
        }
    }
    
    function getPoint(id) {
        return getElement(allPoints, id);
    }
    
    function saveHandler() {
        setUserMessage();
        hideGenericMessages("genericMessages")
        
        var handlerId = ${NEW_ID};
        if (selectedHandlerNode)
            handlerId = $$(selectedHandlerNode.item, "object").id;
        
        // Do some validation.
        var handlerType = $get("handlerTypeSelect");
        var xid = $get("xid");
        var alias = $get("alias");
        var disabled = $get("disabled");
        var eventType = $$(selectedEventTypeNode.item, "object");
        if (handlerType == '<c:out value="<%= EmailEventHandlerDefinition.TYPE_NAME %>"/>') {
            var emailList = emailRecipients.createRecipientArray();
            var escalList = escalRecipients.createRecipientArray();
            var inactiveList = inactiveRecipients.createRecipientArray();
            var context = createContextArray();
            EventHandlersDwr.saveEmailEventHandler(eventType.type, eventType.subtype, eventType.typeRef1,
                    eventType.typeRef2, handlerId, xid, alias, disabled, emailList, $get("customTemplate"), $get("sendEscalation"),
                    $get("escalationDelayType"), $get("escalationDelay"), escalList, $get("sendInactive"),
                    $get("inactiveOverride"), inactiveList, $get("includeSystemInfo"), $get("includePointValueCount"), $get("includeLogfile"), context, saveEventHandlerCB);
        }
        else if (handlerType == '<c:out value="<%= SetPointEventHandlerDefinition.TYPE_NAME %>"/>') {
        	var context = createContextArray();
            EventHandlersDwr.saveSetPointEventHandler(eventType.type, eventType.subtype, eventType.typeRef1,
                    eventType.typeRef2, handlerId, xid, alias, disabled, targetPointSelector.value,
                    $get("activeAction"), $get("setPointValueActive"), activePointSelector.value, activeEditor.getValue(), $get("inactiveAction"), 
                    $get("setPointValueInactive"), inactivePointSelector.value, inactiveEditor.getValue(), context, getScriptPermissions(), saveEventHandlerCB);
        }
        else if ('handlerType == <c:out value="<%= ProcessEventHandlerDefinition.TYPE_NAME %>"/>') {
            EventHandlersDwr.saveProcessEventHandler(eventType.type, eventType.subtype, eventType.typeRef1,
                    eventType.typeRef2, handlerId, xid, alias, disabled, $get("activeProcessCommand"),
                    $get("activeProcessTimeout"), $get("inactiveProcessCommand"), $get("inactiveProcessTimeout"), 
                    saveEventHandlerCB);
        }
    }
    
    function saveEventHandlerCB(response) {
        if (response.hasMessages)
            showDwrMessages(response.messages, $("genericMessages"));
        else {
            var handler = response.data.handler;
            if (!selectedHandlerNode) {
                var storeItem = createHandlerItem(handler);
                var item = store.newItem(storeItem, {parent: selectedEventTypeNode.item, attribute: "children"});
                tree._expandNode(selectedEventTypeNode);
                selectedHandlerNode = tree.getNodesByItem(item)[0];
                tree._setSelectedNodesAttr([selectedHandlerNode]);
                tree.onClick(selectedHandlerNode.item, selectedHandlerNode);
            }
            else
                $set(handler.id +"Msg", handler.message);
            
            $$(selectedHandlerNode.item, "object", makeNonTreeItem(handler));
            setUserMessage("<fmt:message key="eventHandlers.saved"/>");
        }
    }
    
    function deleteHandler() {
        EventHandlersDwr.deleteEventHandler($$(selectedHandlerNode.item, "object").id);
        store.deleteItem(selectedHandlerNode.item);
        hide("handlerEditDiv");
    }
    
    function validateScript(editor, active) {
    	var script = editor.getValue();
   		EventHandlersDwr.validateScript(editor.getValue(), targetPointSelector.value, active, 
   				createContextArray(), getScriptPermissions(), function(response) {
   			if(active) {
   				showDwrMessages(response.messages);
   	            hide("activeScriptValidationOutput");
   	            if (response.data.out){
   	                output = response.data.out;
   	                $set("activeScriptValidationOutput", output);
   	                show("activeScriptValidationOutput");
   	            }
   			} else {
   				showDwrMessages(response.messages);
   	            hide("inactiveScriptValidationOutput");
   	            if (response.data.out){
   	                output = response.data.out;
   	                $set("inactiveScriptValidationOutput", output);
   	                show("inactiveScriptValidationOutput");
   	            }
   			}
   				
   		})
    }
    
    function setUserMessage(msg) {
        showMessage("userMessage", msg);
    }
    
    function testProcessCommand(active) {
        var comm = active ? $get("activeProcessCommand") : $get("inactiveProcessCommand")
        var to = active ? $get("activeProcessTimeout") : $get("inactiveProcessTimeout")
        EventHandlersDwr.testProcessCommand(comm, to, function(msg) {
            if (msg)
                alert(msg);
        });
    }
    
    function sendInactiveChanged() {
        if ($get("sendInactive")) {
            show("inactiveAddresses1");
            inactiveOverrideChanged();
        }
        else {
            hide("inactiveAddresses1");
            hide("inactiveAddresses2");
        }
    }
    
    function inactiveOverrideChanged() {
        if ($get("inactiveOverride"))
            show("inactiveAddresses2");
        else
            hide("inactiveAddresses2");
    }
    
    //Controls for the extra context table in email handlers
    function selectPoint(pointId){
      if(!alreadyAddedToContextArray(pointId)){
          addToContextArray(pointId, "p"+pointId);
          var handlerType = $get(handlerTypeSelect);
          if(handlerType == '<c:out value="<%= EmailEventHandlerDefinition.TYPE_NAME %>"/>')
          	writeContextArray("emailContextTable");
          else if(handlerType == '<c:out value="<%= SetPointEventHandlerDefinition.TYPE_NAME %>"/>')
        	  writeContextArray("setpointContextTable");
      }
  }
  
  function addToContextArray(pointId, templateKey) {
      var data = getElement(allPoints, pointId);
      if (data) {
          
          //Were we already added
          
          // Missing names imply that the point was deleted, so ignore.
          contextArray[contextArray.length] = {
              pointId : pointId,
              pointName : data.name,
              pointType : data.dataTypeMessage,
              templateKey : templateKey
          };
          //Disable in list
          data.fancyName = "<span class='disabled'>"+ data.name +"</span>";
      }
  }
  
  function removeFromContextArray(pointId) {
      for (var i=contextArray.length-1; i>=0; i--) {
          if (contextArray[i].pointId == pointId)
              contextArray.splice(i, 1);
      }
      writeContextArray("emailContextTable");
      var data = getElement(allPoints, pointId);
      if(data)
          data.fancyName = data.name;
  }
    
    function writeContextArray(contextTable) {
      dwr.util.removeAllRows(contextTable);
      if (contextArray.length == 0) {
          show($(contextTable + "Empty"));
          hide($(contextTable + "Headers"));
      }
      else {
          hide($(contextTable + "Empty"));
          show($(contextTable + "Headers"));
          dwr.util.addRows(contextTable, contextArray,
              [
                  function(data) { return data.pointName; },
                  function(data) { return data.pointType; },
                  function(data) {
                          return "<input type='text' value='"+ data.templateKey +"' class='formLong' "+
                                  "onblur='updateTemplateKey("+ data.pointId +", this.value)'/>";
                  },
                  function(data) { 
                          return "<img src='images/bullet_delete.png' class='ptr' "+
                                  "onclick='removeFromContextArray("+ data.pointId +")'/>";
                  }
              ],
              {
                  rowCreator:function(options) {
                      var tr = document.createElement("tr");
                      tr.className = "smRow"+ (options.rowIndex % 2 == 0 ? "" : "Alt");
                      return tr;
                  },
                  cellCreator:function(options) {
                      var td = document.createElement("td");
                      if (options.cellNum == 3){
                          td.align = "center";
                          td.id = "updateContextTd_" + options.rowData.pointId;
                      }
                      return td;
                  }
              });
      }
  }
    
  /**
   * Does the context array already have this item
   */
  function alreadyAddedToContextArray(id){
	  for(var i=0; i<contextArray.length; i++){
	   if(contextArray[i].pointId == id)
		   return true;
	  }
	  
	  return false;
  }
  
  function createContextArray() {
      //Array of ScriptContextVariables {dataPointId, templateKey}
      var context = new Array();
      for (var i=0; i<contextArray.length; i++) {
          var ctxVar = {
                  key : contextArray[i].pointId,
                  value : contextArray[i].templateKey,
          };
          context[context.length] = ctxVar;
      }
      return context;
  }
  
  function updateTemplateKey(pointId, templateKey) {
      for (var i=contextArray.length-1; i>=0; i--) {
          if (contextArray[i].pointId == pointId)
              contextArray[i].templateKey = templateKey;
      }
  }
  </script>
  <table class="borderDiv marB"><tr><td>
    <tag:img png="cog" title="eventHandlers.eventHandlers"/>
    <span class="smallTitle"><fmt:message key="eventHandlers.eventHandlers"/></span>
    <tag:help id="eventHandlers"/>
  </td></tr></table>
  
  <table cellpadding="0" cellspacing="0">
    <tr>
      <td valign="top">
        <div class="borderDivPadded marR">
          <span class="smallTitle"><fmt:message key="eventHandlers.types"/></span>
          <img src="images/hourglass.png" id="loadingImg"/>
          <div id="tree" style="display:none;"><div id="eventTypeTree"></div></div>
        </div>
      </td>
      
      <td valign="top">
        <div id="handlerEditDiv" class="borderDivPadded" style="display:none;">
          <table width="100%">
            <tr>
              <td class="smallTitle"><fmt:message key="eventHandlers.eventHandler"/></td>
              <td align="right">
                <tag:img id="deleteImg" png="delete" title="common.delete" onclick="deleteHandler();"/>
                <tag:img id="saveImg" png="save" title="common.save" onclick="saveHandler();"/>
              </td>
            </tr>
            <tr><td class="formError" id="userMessage"></td></tr>
          </table>
          
          <table width="100%">
            <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.type"/></td>
              <td class="formField">
                <select id="handlerTypeSelect" onchange="handlerTypeChanged()">
                  <option value="<c:out value="<%= EmailEventHandlerDefinition.TYPE_NAME %>"/>"><fmt:message key="eventHandlers.type.email"/></option>
                  <option value="<c:out value="<%= SetPointEventHandlerDefinition.TYPE_NAME %>"/>"><fmt:message key="eventHandlers.type.setPoint"/></option>
                  <option value="<c:out value="<%= ProcessEventHandlerDefinition.TYPE_NAME %>"/>"><fmt:message key="eventHandlers.type.process"/></option>
                </select>
                <%-- hardcoded the TYPE_NAMEs --%>
                <tag:img id="handlerSET_POINTImg" png="cog_wrench" title="eventHandlers.type.setPointHandler" style="display:none;"/>
                <tag:img id="handlerEMAILImg" png="cog_email" title="eventHandlers.type.emailHandler" style="display:none;"/>
                <tag:img id="handlerPROCESSImg" png="cog_process" title="eventHandlers.type.processHandler" style="display:none;"/>
              </td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
              <td class="formField"><input type="text" id="xid"/></td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.alias"/></td>
              <td class="formField"><input id="alias" type="text"/></td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="common.disabled"/></td>
              <td class="formField"><input type="checkbox" id="disabled"/></td>
            </tr>
            
            <tr><td class="horzSeparator" colspan="2"></td></tr>
          </table>
          
          <table id="handler<c:out value="<%= SetPointEventHandlerDefinition.TYPE_NAME %>"/>" style="display:none; width: 100%">
            <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.target"/></td>
              <td class="formField">
                <select id="targetPointSelect"></select>
              </td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.activeAction"/></td>
              <td class="formField">
                <select id="activeAction" onchange="activeActionChanged()">
                  <option value="<c:out value="<%= SetPointEventHandlerVO.SET_ACTION_NONE %>"/>"><fmt:message key="eventHandlers.action.none"/></option>
                  <option value="<c:out value="<%= SetPointEventHandlerVO.SET_ACTION_POINT_VALUE %>"/>"><fmt:message key="eventHandlers.action.point"/></option>
                  <option value="<c:out value="<%= SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE %>"/>"><fmt:message key="eventHandlers.action.static"/></option>
                  <option value="<c:out value="<%= SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE %>"/>"><fmt:message key="eventHandlers.action.script"/></option>
                </select>
              </td>
            </tr>
          
            <tr id="activePointIdRow">
              <td class="formLabel"><fmt:message key="eventHandlers.sourcePoint"/></td>
              <td class="formField"><select id="activePointId"></select></td>
            </tr>
          
            <tr id="activeValueToSetRow">
              <td class="formLabel"><fmt:message key="eventHandlers.valueToSet"/></td>
              <td class="formField" id="activeValueToSetContent"></td>
            </tr>
            
            <tr id="activeScriptRow">
              <td class="formLabelRequired">
                <fmt:message key="eventHandlers.script"/>
                <tag:img png="accept" onclick="validateScript(activeEditor, true);" title="common.validate"/>
              </td>
              <td class="formField">
                <div id="activeScript" style="font-family: Courier New !important; position: relative; height:400px; width:700px"></div>
                <div id="activeScriptValidationOutput" style="display:none;color:green; width: 100%px; overflow: auto"></div>
			  </td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.inactiveAction"/></td>
              <td class="formField">
                <select id="inactiveAction" onchange="inactiveActionChanged()">
                  <option value="<c:out value="<%= SetPointEventHandlerVO.SET_ACTION_NONE %>"/>"><fmt:message key="eventHandlers.action.none"/></option>
                  <option value="<c:out value="<%= SetPointEventHandlerVO.SET_ACTION_POINT_VALUE %>"/>"><fmt:message key="eventHandlers.action.point"/></option>
                  <option value="<c:out value="<%= SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE %>"/>"><fmt:message key="eventHandlers.action.static"/></option>
                  <option value="<c:out value="<%= SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE %>"/>"><fmt:message key="eventHandlers.action.script"/></option>
                </select>
              </td>
            </tr>
          
            <tr id="inactivePointIdRow">
              <td class="formLabel"><fmt:message key="eventHandlers.sourcePoint"/></td>
              <td class="formField"><select id="inactivePointId"></select></td>
            </tr>
          
            <tr id="inactiveValueToSetRow">
              <td class="formLabel"><fmt:message key="eventHandlers.valueToSet"/></td>
              <td class="formField" id="inactiveValueToSetContent"></td>
            </tr>
            
            <tr id="inactiveScriptRow">
              <td class="formLabelRequired">
                <fmt:message key="eventHandlers.script"/>
                <tag:img png="accept" onclick="validateScript(inactiveEditor, false);" title="common.validate"/>
              </td>
              <td class="formField">
                <div id="inactiveScript" style="font-family: Courier New !important; position: relative; height:400px; width:700px"></div>
                <div id="inactiveScriptValidationOutput" style="display:none;color:green; width: 100%px; overflow: auto"></div>
			  </td>
            </tr>
            <tr><td class="horzSeparator" colspan="2"></td></tr>
            <tag:scriptPermissions></tag:scriptPermissions>
            <tr><td class="formLabel"><fmt:message key="eventHandlers.additionalContext"/></td>
              <td><select id="setpointAdditionalContextSelector"></select></td>
            </tr>
            <tr><td colspan="2">
              <table cellspacing="1" id="contextContainer" width="100%">
		        <tbody id="setpointContextTableEmpty" style="display:none;">
		          <tr><td style='text-align:center;'><fmt:message key="eventHandlers.additionalContextEmpty"/></td></tr>
		        </tbody>
		        <tbody id="setpointContextTableHeaders" style="display:none;">
		          <tr class="smRowHeader">
		            <td><fmt:message key="common.pointName"/></td>
		            <td><fmt:message key="dsEdit.pointDataType"/></td>
		            <td><fmt:message key="pointEdit.text.key"/></td>
		            <td></td>
		          </tr>
		        </tbody>
		        <tbody id="setpointContextTable"></tbody>
		      </table></td>
		    </tr>
          </table>
            
          <table id="handler<c:out value="<%= EmailEventHandlerDefinition.TYPE_NAME %>"/>" style="display:none; width: 100%">
            <tbody id="emailRecipients"></tbody>
            
            <tr><td class="horzSeparator" colspan="2"></td></tr>
            <tr><td class="formLabel"><fmt:message key="eventHandlers.additionalContext"/></td>
              <td><select id="emailAdditionalContextSelector"></select></td>
            </tr>
            <tr><td colspan="2">
              <table cellspacing="1" id="contextContainer" width="100%">
		        <tbody id="emailContextTableEmpty" style="display:none;">
		          <tr><td style='text-align:center;'><fmt:message key="eventHandlers.additionalContextEmpty"/></td></tr>
		        </tbody>
		        <tbody id="emailContextTableHeaders" style="display:none;">
		          <tr class="smRowHeader">
		            <td><fmt:message key="common.pointName"/></td>
		            <td><fmt:message key="dsEdit.pointDataType"/></td>
		            <td><fmt:message key="pointEdit.text.key"/></td>
		            <td></td>
		          </tr>
		        </tbody>
		        <tbody id="emailContextTable"></tbody>
		      </table></td>
		    </tr>
            <tr><td class="formLabel"><fmt:message key="eventHandlers.customTemplate"/></td>
              <td class="formField"><textarea id="customTemplate"></textarea></td></tr>
            <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.includeSystemInfo"/></td>
              <td class="formField"><input id="includeSystemInfo" type="checkbox" /></td>
            </tr>  
            <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.includeLogfile"/></td>
              <td class="formField"><input id="includeLogfile" type="checkbox"/></td>
            </tr>            
           <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.includePointValueCount"/></td>
              <td class="formField"><input id="includePointValueCount" type="number" class="formShort"/></td>
            </tr>  
            <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.escal"/></td>
              <td class="formField"><input id="sendEscalation" type="checkbox" onclick="sendEscalationChanged()"/></td>
            </tr>
            
            <tr id="escalationAddresses1">
              <td class="formLabelRequired"><fmt:message key="eventHandlers.escalPeriod"/></td>
              <td class="formField">
                <input id="escalationDelay" type="text" class="formShort"/>
                <tag:timePeriods id="escalationDelayType" min="true" h="true" d="true"/>
              </td>
            </tr>
              
            <tbody id="escalRecipients"></tbody>
            
            <tr><td class="horzSeparator" colspan="2"></td></tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.inactiveNotif"/></td>
              <td class="formField"><input id="sendInactive" type="checkbox" onclick="sendInactiveChanged()"/></td>
            </tr>
            
            <tr id="inactiveAddresses1">
              <td class="formLabelRequired"><fmt:message key="eventHandlers.inactiveOverride"/></td>
              <td class="formField"><input id="inactiveOverride" type="checkbox" onclick="inactiveOverrideChanged()"/></td>
            </tr>
              
            <tbody id="inactiveRecipients"></tbody>
            
          </table>
          
          <table id="handler<c:out value="<%= ProcessEventHandlerDefinition.TYPE_NAME %>"/>" style="display:none; width: 100%">
            <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.activeCommand"/></td>
              <td class="formField">
                <input type="text" id="activeProcessCommand" class="formLong"/>
                <tag:img png="cog_go" onclick="testProcessCommand(true)" title="eventHandlers.commandTest.title"/>
              </td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.activeTimeout"/></td>
              <td class="formField"><input type="text" id="activeProcessTimeout" class="formShort"/></td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.inactiveCommand"/></td>
              <td class="formField">
                <input type="text" id="inactiveProcessCommand" class="formLong"/>
                <tag:img png="cog_go" onclick="testProcessCommand(false)" title="eventHandlers.commandTest.title"/>
              </td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="eventHandlers.inactiveTimeout"/></td>
              <td class="formField"><input type="text" id="inactiveProcessTimeout" class="formShort"/></td>
            </tr>
          </table>
          
          <table>
            <tbody id="genericMessages"></tbody>
          </table>
        </div>
      </td>
    </tr>
  </table>
<script src="/resources/ace/ace.js" type="text/javascript" charset="utf-8"></script>
<script src="/resources/ace/theme-tomorrow_night_bright.js" type="text/javascript" charset="utf-8"></script>
<script src="/resources/ace/mode-javascript.js" type="text/javascript" charset="utf-8"></script>
<script src="/resources/ace/worker-javascript.js" type="text/javascript" charset="utf-8"></script>
<script>
  ace.config.set("basePath", "/resources/ace");
    var activeEditor = ace.edit("activeScript");
    var inactiveEditor = ace.edit("inactiveScript");
    activeEditor.setTheme("ace/theme/tomorrow_night_bright");
    inactiveEditor.setTheme("ace/theme/tomorrow_night_bright");
    var JavaScriptMode = ace.require("ace/mode/javascript").Mode;
    activeEditor.getSession().setMode(new JavaScriptMode());
    inactiveEditor.getSession().setMode(new JavaScriptMode());
</script>
</jsp:body>
</tag:page>