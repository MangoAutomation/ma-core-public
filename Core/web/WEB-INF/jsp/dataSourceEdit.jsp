<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@page import="com.serotonin.m2m2.Constants"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.Common"%>

<c:set var="dwrClasses">DataSourceEditDwr</c:set>
<c:if test="${!empty dataSource.definition.dwrClass}">
  <c:set var="dwrClasses">${dwrClasses},${dataSource.definition.dwrClass.simpleName}</c:set>
</c:if>

<tag:page dwr="${dwrClasses}" onload="init">
  <script type="text/javascript">
    dojo.require("dijit.Dialog");
    dojo.require("dijit.form.Form");
    dojo.require("dijit.form.Button");
    
    var currentPoint;
    var pointListColumnFunctions = [];
    var pointListOptions;
    
    function init() {
        changePurgeOverride();
        
        var pointListColumnHeaders = new Array();
        
        pointListColumnHeaders.push("<fmt:message key="dsEdit.name"/>");
        pointListColumnFunctions.push(function(p) { return "<b>"+ p.name +"</b>"; });
        
        pointListColumnHeaders.push("<fmt:message key="dsEdit.pointDataType"/>");
        pointListColumnFunctions.push(function(p) { return p.dataTypeMessage; });
        
        pointListColumnHeaders.push("<fmt:message key="dsEdit.status"/>");
        pointListColumnFunctions.push(function(p) {
                var id = "toggleImg"+ p.id;
                var onclick = "togglePoint("+ p.id +")";
                if (p.enabled)
                    return writeImage(id, null, "brick_go", "<fmt:message key="common.enabledToggle"/>", onclick);
                return writeImage(id, null, "brick_stop", "<fmt:message key="common.disabledToggle"/>", onclick);
        });
        
        if (typeof appendPointListColumnFunctions == 'function')
            appendPointListColumnFunctions(pointListColumnHeaders, pointListColumnFunctions);
        
        pointListColumnHeaders.push("");
        pointListColumnFunctions.push(function(p) {
                return writeImage("editImg"+ p.id, null, "icon_comp_edit", "<fmt:message key="common.edit"/>", "editPoint("+ p.id +")");
        });
        
        var headers = $("pointListHeaders");
        var td;
        for (var i=0; i<pointListColumnHeaders.length; i++) {
            td = document.createElement("td");
            if (typeof(pointListColumnHeaders[i]) == "string")
                td.innerHTML = pointListColumnHeaders[i];
            else
                pointListColumnHeaders[i](td);
            headers.appendChild(td);
        }
        
        pointListOptions = {
                rowCreator: function(options) {
                    var tr = document.createElement("tr");
                    tr.mangoId = "p"+ options.rowData.id;
                    tr.className = "row"+ (options.rowIndex % 2 == 0 ? "" : "Alt");
                    return tr;
                },
                cellCreator: function(options) {
                    var td = document.createElement("td");
                    if (options.cellNum == 2)
                        td.align = "center";
                    return td;
                }
        };
        
        var dsStatus = $("dsStatusImg");
        setDataSourceStatusImg(${dataSource.enabled}, dsStatus);
        hide(dsStatus);
        
        if (typeof initImpl == 'function') initImpl();
        
        DataSourceEditDwr.editInit(function(response) {
            writePointList(response.data.points);
            writeAlarms(response.data.alarms);
            
            <c:if test="${!empty param.pid}">
              // Default the selection if the parameter was provided.
              editPoint(${param.pid});
            </c:if>
        });
        showMessage("dataSourceMessage");
        showMessage("pointMessage");
        
        getStatusMessages();
    }
    
    function saveDataSource() {
        startImageFader("dsSaveImg", true);
        hideContextualMessages($("dataSourceProperties"));
        saveDataSourceImpl({
            name: $get("dataSourceName"),
            xid: $get("dataSourceXid"),
            purgeOverride: $get("dataSourcePurgeOverride"),
            purgePeriod: $get("dataSourcePurgePeriod"),
            purgeType: $get("dataSourcePurgeType")
        });
    }
    
    function saveDataSourceCB(response) {
        stopImageFader("dsSaveImg");
        if (response.hasMessages)
            showDwrMessages(response.messages, "dataSourceGenericMessages");
        else {
            showMessage("dataSourceMessage", "<fmt:message key="dsEdit.saved"/>");
            DataSourceEditDwr.getPoints(writePointList);
        }
        getAlarms();
    }
    
    function toggleDataSource() {
        if (typeof toggleDataSourceImpl == 'function') toggleDataSourceImpl();
        
        var imgNode = $("dsStatusImg");
        if (!hasImageFader(imgNode)) {
            DataSourceEditDwr.toggleEditDataSource(function(result) {
                var imgNode = $("dsStatusImg");
                stopImageFader(imgNode);
                setDataSourceStatusImg(result.enabled, imgNode);
                getAlarms();
            });
            startImageFader(imgNode);
        }
    }
    
    function togglePoint(pointId) {
        startImageFader("toggleImg"+ pointId, true);
        DataSourceEditDwr.togglePoint(pointId, function(response) {
            stopImageFader("toggleImg"+ response.data.id);
            writePointList(response.data.points);
        });
    }
    
    function deletePoint() {
        if (confirm("<fmt:message key="dsEdit.deleteConfirm"/>")) {
            DataSourceEditDwr.deletePoint(currentPoint.id, function(points) {
                stopImageFader("pointDeleteImg");
                hide("pointDetails");
                currentPoint = null;
                writePointList(points);
            });
            startImageFader("pointDeleteImg", true);
        }
    }
    
    function writePointList(points) {
        if (typeof writePointListImpl == 'function') writePointListImpl(points);
        
        if (!points)
            return;
        show("pointProperties");
        show("alarmsTable");
        show("dsStatusImg");
    
        if (currentPoint)
            stopImageFader("editImg"+ currentPoint.id);
        dwr.util.removeAllRows("pointsList");
        dwr.util.addRows("pointsList", points, pointListColumnFunctions, pointListOptions);
    }
    
    function addPoint(ref) {
        if (!isShowing("pointProperties")) {
            alert("<fmt:message key="dsEdit.saveWarning"/>");
            return;
        }
        
        if (currentPoint)
            stopImageFader("editImg"+ currentPoint.id);
        
        startImageFader("editImg"+ <c:out value="<%= Common.NEW_ID %>"/>);
        hideContextualMessages("pointProperties");
        
        addPointImpl(ref);
    }
    
    function editPoint(pointId) {
        if (currentPoint)
            stopImageFader("editImg"+ currentPoint.id);
        hideContextualMessages("pointProperties");
        DataSourceEditDwr.getPoint(pointId, editPointCB);
    }
    
    // This method can be used by implementations to add a new point from e.g. a tool. See Modbus for an example.
    function editPointCB(point) {
        currentPoint = point;
        display("pointDeleteImg", point.id != <c:out value="<%= Common.NEW_ID %>"/>);
        var locator = currentPoint.pointLocator;
        
        $set("name", currentPoint.name);
        $set("xid", currentPoint.xid);
        var cancel;
        if (typeof editPointCBImpl == 'function') 
        	cancel = editPointCBImpl(locator);
        if (!cancel) {
            var img = "editImg"+ point.id;
           	startImageFader(img);
           	show("pointDetails");
           	
            require(["dojo/_base/html", "dojo/dom-style"], function(html, domStyle){
                var position = html.position(img, true);
                domStyle.set("pointDetails", "top", position.y +"px");
           	});
        }
    }
    
    function cancelEditPoint() {
        if (currentPoint) {
            stopImageFader("editImg"+ currentPoint.id);
            currentPoint = null;
            hide("pointDetails");
        }
    }
    
    function savePoint() {
        startImageFader("pointSaveImg", true);
        hideContextualMessages("pointProperties");
        var locator = currentPoint.pointLocator;
        
        // Prevents DWR warnings. These properties are read-only. If sent back to the server
        // DWR will say as much. Deleting the properties saves a bit of logging.
        delete locator.configurationDescription;
        delete locator.dataTypeMessage;
        
        savePointImpl(locator);
    }
    
    function savePointCB(response) {
        stopImageFader("pointSaveImg");
        if (response.hasMessages)
            showDwrMessages(response.messages);
        else {
            writePointList(response.data.points);
            editPoint(response.data.id);
            showMessage("pointMessage", "<fmt:message key="dsEdit.pointSaved"/>");
        }
    }
    
    function closePoint() {
        if (currentPoint)
            stopImageFader("editImg"+ currentPoint.id);
        hideContextualMessages("pointProperties");
        hide("pointDetails");
        currentPoint = null;
    }
    
    function getAlarms() {
        DataSourceEditDwr.getAlarms(writeAlarms);
    }
    
    function writeAlarms(alarms) {
        dwr.util.removeAllRows("alarmsList");
        if (alarms.length == 0) {
            show("noAlarmsMsg");
            hide("alarmsList");
        }
        else {
            hide("noAlarmsMsg");
            show("alarmsList");
            dwr.util.addRows("alarmsList", alarms, [
                    function(alarm) {
                        var div = document.createElement("div");
                        var img = document.createElement("img");
                        setAlarmLevelImg(alarm.alarmLevel, img);
                        div.appendChild(img);
                        
                        var span = document.createElement("span");
                        span.innerHTML = alarm.prettyActiveTimestamp +": "+ alarm.message;
                        div.appendChild(span);
                        
                        return div; 
                    }],
                    {
                        cellCreator: function(options) {
                            var td = document.createElement("td");
                            td.className = "formError";
                            return td;
                        }
                    });
        }
    }
    
    function alarmLevelChanged(eventId) {
        var alarmLevel = $get("alarmLevel"+ eventId);
        DataSourceEditDwr.updateEventAlarmLevel(eventId, alarmLevel);
        setAlarmLevelImg(alarmLevel, "alarmLevelImg"+ eventId);
    }
    
    function exportDataSource() {
        DataSourceEditDwr.exportDataSource(function(json) {
            $set("exportData", json);
            exportDialog.show();
        });
    }
    
    function exportDataPoint() {
        DataSourceEditDwr.exportDataPoint(currentPoint.id, function(json) {
            $set("exportData", json);
            exportDialog.show();
        });
    }
    
    function changePurgeOverride() {
        var override = $get("dataSourcePurgeOverride");
        $("dataSourcePurgePeriod").disabled = !override;
        $("dataSourcePurgeType").disabled = !override;
    }
    
    function getStatusMessages() {
        DataSourceEditDwr.getGeneralStatusMessages(function(result) {
            dwr.util.removeAllOptions("generalStatusMessages");
            dwr.util.addOptions("generalStatusMessages", result.data.messages);
            if (typeof getStatusMessagesImpl == 'function') getStatusMessagesImpl();
        });
    }
  </script>
  
  <tag:labelledSection labelKey="dsEdit.currentAlarms" id="alarmsTable" closed="true">
    <div style="float: right"><tag:img png="control_repeat_blue" title="common.refresh" onclick="getAlarms()"/></div>
    <table>
      <tr id="noAlarmsMsg"><td><b><fmt:message key="dsEdit.noAlarms"/></b></td></tr>
      <tbody id="alarmsList"></tbody>
    </table>
  </tag:labelledSection>
  
  <tag:labelledSection labelKey="dsEdit.rtStatus" closed="true">
    <div style="float: right"><tag:img png="control_repeat_blue" title="common.refresh" onclick="getStatusMessages()"/></div>
    <ul id="generalStatusMessages"></ul>
    <c:if test="${!empty dataSource.definition.statusPagePath}">
      Module-defined status.
      <c:set var="statpage">/<c:out value="<%= Constants.DIR_MODULES %>"/>/${dataSource.definition.module.name}/${dataSource.definition.statusPagePath}</c:set>
      <jsp:include page="${statpage}"/>
    </c:if>
  </tag:labelledSection>
  
  <c:set var="incpage">/<c:out value="<%= Constants.DIR_MODULES %>"/>/${dataSource.definition.module.name}/${dataSource.definition.editPagePath}</c:set>
  <jsp:include page="${incpage}"/>
  
  <div data-dojo-type="dijit.Dialog" data-dojo-id="exportDialog" title="<fmt:message key="emport.export"/>" style="display: none">
    <form data-dojo-type="dijit.form.Form">
      <div class="dijitDialogPaneContentArea">
        <textarea rows="40" cols="100" id="exportData"></textarea>
      </div>
      <div class="dijitDialogPaneActionBar">
        <button data-dojo-type="dijit.form.Button" type="button" data-dojo-props="onClick:function() {exportDialog.hide();}"><fmt:message key="common.close"/></button>
      </div>
    </form>
  </div>
  
<%--  
  <textarea id="pointsToDelete"></textarea>
  <input type="button" onclick="massDelete()" value="del"/>
  
  <script type="text/javascript">
    function massDelete() {
        var txt = $get("pointsToDelete");
        var list = txt.split(",");
        var eh = dwr.engine._errorHandler;
        dwr.engine._errorHandler = function(e) {
            //console.log("error");
        }
        
        for (var i=0; i<list.length; i++)
            delImpl(list[i]);
        
        dwr.engine._errorHandler = eh;
    }
    
    function delImpl(id) {
        DataSourceEditDwr.deletePoint(id, function(points) {
            console.log("deleted "+ id);
        });
    }
  </script> --%>
</tag:page>