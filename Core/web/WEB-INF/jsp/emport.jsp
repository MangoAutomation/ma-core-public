<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@page import="com.serotonin.m2m2.module.EmportDefinition"%>
<%@page import="com.serotonin.m2m2.module.ModuleRegistry"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<c:set var="definitions" value="<%= ModuleRegistry.getDefinitions(EmportDefinition.class) %>"/>
<tag:page showHeader="${param.showHeader}" showToolbar="${param.showToolbar}" dwr="EmportDwr" onload="init">
  <script type="text/javascript">
    function init() {
        setDisabled("cancelBtn", true);
        importUpdate();
    }
    
    function doExport() {
        setDisabled("exportBtn", true);
        EmportDwr.createExportData($get("prettyIndent"), $get("exportElement"), function(data) {
            $set("emportData", data);
            setDisabled("exportBtn", false);
        });
    }
    
    function doImport() {
        setDisabled("importBtn", true);
        hideGenericMessages("importMessages");
        $set("alternateMessage", "<fmt:message key="emport.importProgress"/>");
        EmportDwr.importData($get("emportData"), function(response) {
            if (response.data.importStarted)
                importUpdate();
            else if (response.hasMessages) {
                showDwrMessages(response.messages, $("importMessages"));
                $set("alternateMessage");
                setDisabled("importBtn", false);
            }
            else {
                $set("alternateMessage", "<fmt:message key="emport.noMessages"/>");
                setDisabled("importBtn", false);
            }
        });
    }
    
    function importUpdate() {
        EmportDwr.importUpdate(function(response) {
            if (response.data.noImport)
                // no op
                return;
            
            $set("alternateMessage", "<fmt:message key="emport.importProgress"/>");
            setDisabled("importBtn", true);
            setDisabled("cancelBtn", false);
            
            showDwrMessages(response.messages, $("importMessages"));
            
            if (response.data.cancelled || response.data.complete) {
                setDisabled("importBtn", false);
                setDisabled("cancelBtn", true);
                
                if (response.data.cancelled)
                    $set("alternateMessage", "<fmt:message key="emport.importCancelled"/>");
                else
                    $set("alternateMessage", "<fmt:message key="emport.importComplete"/>");
            }
            else
                setTimeout(importUpdate, 1000);
        });
    }
    
    function importCancel() {
        EmportDwr.importCancel();
    }
    
    function selectAll(checked) {
    	var cbs = document.getElementsByName("exportElement");
    	for (var i=0; i<cbs.length; i++)
    	    $set(cbs[i], checked);
    }
  </script>
  
  <div class="borderDiv marR marB" style="float:left;">
    <table width="100%">
      <tr>
        <td colspan="2">
          <span class="smallTitle"><fmt:message key="emport.export"/></span>
          <tag:help id="emport"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel">
          <b><fmt:message key="emport.select"/></b><br/>
          <a href="#" onclick="selectAll(true); return false"><fmt:message key="emport.selectAll"/></a> |
          <a href="#" onclick="selectAll(false); return false"><fmt:message key="emport.unselectAll"/></a>
        </td>
        <td></td>
      </tr>
      <tr>
        <td class="formField" style="padding-left:50px;">
          <input type="checkbox" name="exportElement" id="eventHandlers" value="eventHandlers"/> <label for="eventHandlers"><fmt:message key="header.eventHandlers"/></label><br/>
          <input type="checkbox" name="exportElement" id="dataSources" value="dataSources"/> <label for="dataSources"><fmt:message key="header.dataSources"/></label><br/>
          <input type="checkbox" name="exportElement" id="dataPoints" value="dataPoints"/> <label for="dataPoints"><fmt:message key="emport.dataPoints"/></label><br/>
          <input type="checkbox" name="exportElement" id="users" value="users"/> <label for="users"><fmt:message key="header.users"/></label><br/>
          <input type="checkbox" name="exportElement" id="mailingLists" value="mailingLists"/> <label for="mailingLists"><fmt:message key="header.mailingLists"/></label><br/>
          <input type="checkbox" name="exportElement" id="publishers" value="publishers"/> <label for="publishers"><fmt:message key="header.publishers"/></label><br/>
          <input type="checkbox" name="exportElement" id="pointHierarchy" value="pointHierarchy"/> <label for="pointHierarchy"><fmt:message key="header.pointHierarchy"/></label><br/>
          <input type="checkbox" name="exportElement" id="systemSettings" value="systemSettings"/> <label for="systemSettings"><fmt:message key="header.systemSettings"/></label><br/>
          
        </td>
        <td>
          <c:forEach items="${definitions}" var="def">
            <input type="checkbox" name="exportElement" id="${def.elementId}" value="${def.elementId}"/> <label for="${def.elementId}"><fmt:message key="${def.descriptionKey}"/></label><br/>
          </c:forEach>
        </td>
<!--          <input type="checkbox" id="systemSettings"/> <label for="systemSettings"><fmt:message key="header.systemSettings"/></label><br/>-->
<!--          <input type="checkbox" id="imageSets"/> <label for="imageSets"><fmt:message key="header.imageSets"/></label><br/>-->
<!--          <input type="checkbox" id="dynamicImages"/> <label for="dynamicImages"><fmt:message key="header.dynamicImages"/></label><br/>-->
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="emport.indent"/></td>
        <td><input type="text" id="prettyIndent" value="3" class="formVeryShort"/></td>
      </tr>
      <tr>
        <td colspan="2" align="center">
          <input id="exportBtn" type="button" value="<fmt:message key="emport.export"/>" onclick="doExport()"/>
        </td>
      </tr>
    </table>
  </div>

  <div class="borderDiv marB" style="float:left;">
    <table width="100%">
      <tr><td><span class="smallTitle"><fmt:message key="emport.import"/></span></td></tr>
      <tr>
        <td>
          <fmt:message key="emport.importInstruction"/>
          <input id="importBtn" type="button" value="<fmt:message key="emport.import"/>" onclick="doImport()"/>
          <input id="cancelBtn" type="button" value="<fmt:message key="common.cancel"/>" onclick="importCancel()" disabled="disabled"/>
        </td>
      </tr>
      <tbody id="importMessages"></tbody>
      <tr><td id="alternateMessage"></td></tr>
    </table>
  </div>
  
  <div style="clear:both;">
    <span class="formLabelRequired"><fmt:message key="emport.data"/></span><br/>
    <textarea rows="40" cols="150" id="emportData"></textarea>
  </div>
</tag:page>