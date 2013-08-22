<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@include file="/WEB-INF/tags/decl.tagf"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.module.ModuleRegistry"%>
<%@page import="com.serotonin.m2m2.module.MenuItemDefinition"%>

<c:choose>
    <c:when test="true">
    <tag:mobile-page dwr="DataSourceListDwr" >

        <div id="icons" dojoType="dojox.mobile.View">
            <div id="iconsMain">
                <ul dojoType="dojox.mobile.IconContainer" defaultIcon="/images/cog.png">
			        <c:if test="${!empty sessionUser}">
			          <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.USER) %>" var="mi">
			            <m2m2:mobile-menuItem def="${mi}"/>
			          </c:forEach>
			                
			          <c:if test="${sessionUser.dataSourcePermission}">
			            <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.DATA_SOURCE) %>" var="mi">
			              <m2m2:mobile-menuItem def="${mi}"/>
			            </c:forEach>
			          </c:if>
			          
			          <m2m2:mobile-menuItem id="usersMi" href="/users.shtm" png="user" key="header.users"/>
			          
			          <c:if test="${sessionUser.admin}">
			            <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ADMINISTRATOR) %>" var="mi">
			              <m2m2:mobile-menuItem def="${mi}"/>
			            </c:forEach>
			          </c:if>
			          
			          <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ANONYMOUS) %>" var="mi">
			            <m2m2:mobile-menuItem def="${mi}"/>
			          </c:forEach>
			        </c:if>
			        <c:if test="${empty sessionUser}">
			          <m2m2:mobile-menuItem id="loginMi" href="/login.htm" png="control_play_blue" key="header.login"/>
			          <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ANONYMOUS) %>" var="mi">
			            <m2m2:mobile-menuItem def="${mi}"/>
			          </c:forEach>
			        </c:if>
              
                
                </ul>
            </div>
        </div>

    <script type="text/javascript">
            require(["dojox/mobile", "dojox/mobile/parser", "dojo/ready", 
                "mango/mobile/DataSource", "dojox/mobile/compat", "dojox/mobile/Button",
                "dojox/mobile/View", "dojox/mobile/IconContainer", "dojox/mobile/ScrollableView",
                "dojox/mobile/IconItem",
                "dojo/domReady!"], function(mobile, parser, ready, DataSource){
                    ready(function(){
                        DataSource.init();
                    });
                });
    </script>
    
    
    </tag:mobile-page>
    
    </c:when>
    <c:when test="false">
<tag:page dwr="DataSourceListDwr" onload="init">
  <style type="text/css">
    .mangoForm ul { margin: 0; padding: 0; }
    .mangoForm ul li { margin-bottom: 5px; list-style: none; }
    .mangoForm label { width: 100px; text-align: right; padding-right: 10px; display: inline-block; }
    .mangoForm label.required { font-weight: bold; }
  </style>
  <script type="text/javascript">
    dojo.require("dijit.Dialog");
    dojo.require("dijit.form.Form");
    dojo.require("dijit.form.ValidationTextBox");
    dojo.require("dijit.form.Button");
    
    function init() {
        DataSourceListDwr.init(function(response) {
        	hide("hourglass");
        	
        	var dss = response.data.dataSources;
            dwr.util.removeAllRows("dataSourceList");
            if (dss.length == 0)
                show("noDataSources");
            else {
            	hide("noDataSources");
            	dwr.util.addRows("dataSourceList", dss,
                    [
                        function(ds) { return "<b>"+ ds.name +"</b>"; },
                        function(ds) { return ds.typeDescription; },
                        function(ds) { return ds.connectionDescription; },
                        function(ds) {
                        	if (ds.enabled) {
                        		return writeImage("dsImg"+ ds.id, null, "database_go", 
                        				"<m2m2:translate key="common.enabledToggle" escapeDQuotes="true"/>", 
                        				"toggleDataSource("+ ds.id +")");
                        	}
                            return writeImage("dsImg"+ ds.id, null, "database_stop", 
                                    "<m2m2:translate key="common.disabledToggle" escapeDQuotes="true"/>", 
                                    "toggleDataSource("+ ds.id +")");
                        },
                        function(ds) {
                        	var s = "";
                        	s += '<a href="data_source_edit.shtm?dsid='+ ds.id +'">';
                        	s += writeImage(null, null, "icon_ds_edit",
                        			"<m2m2:translate key="common.edit" escapeDQuotes="true"/>", null);
                        	s += "</a>";
                        	
                            s += writeImage("deleteDataSourceImg"+ ds.id, null, "icon_ds_delete", 
                                    "<m2m2:translate key="common.delete" escapeDQuotes="true"/>", 
                                    "deleteDataSource("+ ds.id +")");
                            
                            s += writeImage("copyDataSourceImg"+ ds.id, null, "icon_ds_add", 
                                    "<m2m2:translate key="common.copy" escapeDQuotes="true"/>", 
                                    "copyDataSource("+ ds.id +")");
                            
                            s += writeImage("exportDataSourceImg"+ ds.id, null, "emport", 
                                    "<m2m2:translate key="emport.export" escapeDQuotes="true"/>", 
                                    "exportDataSource("+ ds.id +")");
                        
                        	return s;
                      	}
                    ],
                    {
                        rowCreator: function(options) {
                            var tr = document.createElement("tr");
                            tr.id = "dataSourceRow"+ options.rowData.id;
                            tr.className = "row"+ (options.rowIndex % 2 == 0 ? "" : "Alt");
                            return tr;
                        },
                        cellCreator: function(options) {
                            var td = document.createElement("td");
                            if (options.cellNum == 3)
                                td.align = "center";
                            return td;
                        }
                    }
            	);
            }
            
            if (response.data.types) {
                dwr.util.addOptions("dataSourceTypes", response.data.types, "key", "value");
                show("dataSourceTypesContent");
            }
        });
        
        copyDataSourceDialog.onHide = cancelCopyDataSource;
    }
    
    function toggleDataSource(dataSourceId) {
        var imgNode = $("dsImg"+ dataSourceId);
        if (!hasImageFader(imgNode)) {
        	startImageFader(imgNode);
            DataSourceListDwr.toggleDataSource(dataSourceId, function(result) {
                updateStatusImg($("dsImg"+ result.id), result.enabled);
            });
        }
    }
    
    function updateStatusImg(imgNode, enabled) {
        stopImageFader(imgNode);
        setDataSourceStatusImg(enabled, imgNode);
    }
    
    function deleteDataSource(dataSourceId) {
        if (confirm("<fmt:message key="dsList.dsDeleteConfirm"/>")) {
            startImageFader("deleteDataSourceImg"+ dataSourceId);
            DataSourceListDwr.deleteDataSource(dataSourceId, function(dataSourceId) {
                stopImageFader("deleteDataSourceImg"+ dataSourceId);
                // Delete the data source row
                var row = $("dataSourceRow"+ dataSourceId);
                row.parentNode.removeChild(row);
            });
        }
    }
    
    function copyDataSource(fromDataSourceId) {
        startImageFader("copyDataSourceImg"+ fromDataSourceId);
        DataSourceListDwr.dataSourceInfo(fromDataSourceId, function(response) {
            $set("copyId", fromDataSourceId);
            $set("copyName", response.data.name);
            $set("copyXid", response.data.xid);
            $set("copyDeviceName", response.data.deviceName);
            copyDataSourceDialog.show();
        });
    }
    
    function sumbitCopyDataSource() {
        DataSourceListDwr.copyDataSource($get("copyId"), $get("copyName"), $get("copyXid"), $get("copyDeviceName"),
                function(response) {
                    if (response.hasMessages) {
                        for (var i=0; i<response.messages.length; i++) {
                            var m = response.messages[i];
                            var x;
                            if (m.contextKey == "dataSourceName")
                                x = dijit.byId("copyName");
                            else if (m.contextKey == "xid")
                                x = dijit.byId("copyXid");
                            if (x) {
                                x.focus();
                                x.displayMessage(m.contextualMessage);
                                break;
                            }
                        }
                    }
                    else
                        window.location = "data_source_edit.shtm?dsid="+ response.data.newId;
                }
        );
    }
    
    function cancelCopyDataSource() {
        stopImageFader("copyDataSourceImg"+ $get("copyId"));
    }
    
    function addDataSource() {
        window.location = "data_source_edit.shtm?typeId="+ $get("dataSourceTypes");
    }
    
    function exportDataSource(dataSourceId) {
        DataSourceListDwr.exportDataSourceAndPoints(dataSourceId, function(json) {
            $set("exportData", json);
            dataSourceExportDialog.show();
        });
    }
  </script>
  
  <table>
    <tr>
      <td>
        <tag:img png="icon_ds" title="dsList.dataSources"/>
        <span class="smallTitle"><fmt:message key="dsList.dataSources"/></span>
        <tag:help id="dataSourceList"/>
      </td>
      <td align="right" id="dataSourceTypesContent" style="display:none">
        <select id="dataSourceTypes"></select>
        <tag:img png="icon_ds_add" title="common.add" onclick="addDataSource()"/>
      </td>
    </tr>

    <tr>
      <td colspan="2">
        <table>
          <tr class="rowHeader">
            <td><fmt:message key="dsList.name"/></td>
            <td><fmt:message key="dsList.type"/></td>
            <td><fmt:message key="dsList.connection"/></td>
            <td><fmt:message key="dsList.status"/></td>
            <td></td>
          </tr>
          
          <tr id="hourglass" class="row"><td colspan="5" align="center"><tag:img png="hourglass" title="common.loading"/></td></tr>
          <tr id="noDataSources" class="row" style="display:none;"><td colspan="5"><fmt:message key="dsList.emptyList"/></td></tr>
          <tbody id="dataSourceList"></tbody>
        </table>
      </td>
    </tr>
  </table>
  
  <div data-dojo-type="dijit.Dialog" data-dojo-id="copyDataSourceDialog" title="<fmt:message key="dsList.copy"/>" style="display: none">
    <form data-dojo-type="dijit.form.Form">
      <div class="dijitDialogPaneContentArea mangoForm">
        <ul>
          <li>
            <label for='copyXid' class="required"><fmt:message key="common.xid"/></label>
            <div id="copyXid" data-dojo-type="dijit.form.ValidationTextBox" data-dojo-id="copyXid"></div>
          </li>
          <li>
            <label for='copyName' class="required"><fmt:message key="dsEdit.name"/></label>
            <div id="copyName" data-dojo-type="dijit.form.ValidationTextBox" data-dojo-id="copyName"></div>
          </li>
          <li>
            <label for='copyDeviceName'><fmt:message key="pointEdit.props.deviceName"/></label>
            <div id="copyDeviceName" data-dojo-type="dijit.form.ValidationTextBox"></div>
          </li>
        </ul>
        <input type="hidden" id="copyId"/>
      </div>
      <div class="dijitDialogPaneActionBar">
        <button data-dojo-type="dijit.form.Button" type="button" data-dojo-props="onClick:sumbitCopyDataSource"><fmt:message key="common.copy"/></button>
        <button data-dojo-type="dijit.form.Button" type="button" data-dojo-props="onClick:function() {copyDataSourceDialog.hide();}"><fmt:message key="common.cancel"/></button>
      </div>
    </form>
  </div>
  
  <div data-dojo-type="dijit.Dialog" data-dojo-id="dataSourceExportDialog" title="<fmt:message key="dsList.export"/>" style="display: none">
    <form data-dojo-type="dijit.form.Form">
      <div class="dijitDialogPaneContentArea mangoForm">
        <textarea rows="40" cols="100" id="exportData"></textarea>
      </div>
      <div class="dijitDialogPaneActionBar">
        <button data-dojo-type="dijit.form.Button" type="button" data-dojo-props="onClick:function() {dataSourceExportDialog.hide();}"><fmt:message key="common.close"/></button>
      </div>
    </form>
  </div>
</tag:page>
    </c:when>
</c:choose>