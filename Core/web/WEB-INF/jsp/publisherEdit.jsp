<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.Main"%>

<c:set var="dwrClasses">PublisherEditDwr</c:set>
<c:if test="${!empty publisher.definition.dwrClass}">
  <c:set var="dwrClasses">${dwrClasses},${publisher.definition.dwrClass.simpleName}</c:set>
</c:if>

<tag:page dwr="${dwrClasses}" onload="initPublisher">
  <script type="text/javascript">
    function savePublisher() {
        hide("nameMsg");
        hide("cacheWarningSizeMsg");
        hide("cacheDiscardSizeMsg");
        hide("message");
        setDisabled("saveBtn", true);
        
        savePublisherImpl($get("name"), $get("xid"), $get("enabled"), $get("cacheWarningSize"),
                $get("cacheDiscardSize"), $get("changesOnly") == "true", $get("sendSnapshot"),
                $get("snapshotSendPeriods"), $get("snapshotSendPeriodType"));
    }
    
    function savePublisherCB(response) {
        setDisabled("saveBtn", false);
        if (response.hasMessages) {
            for (var i=0; i<response.messages.length; i++) {
                var msg = response.messages[i];
                if (msg.genericMessage)
                    alert(msg.genericMessage);
                else
                    showMessage(msg.contextKey +"Msg", msg.contextualMessage);
            }
        }
        else
            showMessage("message", "<fmt:message key="publisherEdit.saved"/>");
    }
    
    function sendSnapshotChanged() {
        if ($get("sendSnapshot")) {
            setDisabled("snapshotSendPeriods", false);
            setDisabled("snapshotSendPeriodType", false);
        }
        else {
            setDisabled("snapshotSendPeriods", true);
            setDisabled("snapshotSendPeriodType", true);
        }
    }
    
    function initPublisher() {
        sendSnapshotChanged();
    }
  </script>
  
  <table>
    <tr>
      <td>
        <c:if test="${!empty publisherEvents}">
          <table class="borderDiv marB">
            <tr><td class="smallTitle"><fmt:message key="publisherEdit.currentAlarms"/></td></tr>
            <c:forEach items="${publisherEvents}" var="event">
              <tr><td class="formError">
                <tag:eventIcon eventBean="${event}"/>
                ${event.prettyActiveTimestamp}:
                ${event.message}
              </td></tr>
            </c:forEach>
          </table>
        </c:if>
        
        <div id="message" class="formError" style="display:none;"></div>
        
        <div class="borderDiv marR marB">
          <table>
            <tr>
              <td colspan="2" class="smallTitle">
                <tag:img png="transmit" title="common.edit"/>
                <fmt:message key="publisherEdit.generalProperties"/> <tag:help id="generalPublisherProperties"/>
              </td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="publisherEdit.name"/></td>
              <td class="formField">
                <input type="text" id="name" value="${publisher.name}"/>
                <div id="nameMsg" class="formError" style="display:none;"></div>
              </td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
              <td class="formField">
                <input type="text" id="xid" value="${publisher.xid}"/>
                <div id="xidMsg" class="formError" style="display:none;"></div>
              </td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="common.enabled"/></td>
              <td class="formField"><sst:checkbox id="enabled" selectedValue="${publisher.enabled}"/></td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="publisherEdit.cacheWarning"/></td>
              <td class="formField">
                <input type="text" id="cacheWarningSize" value="${publisher.cacheWarningSize}" class="formShort"/>
                <div id="cacheWarningSizeMsg" class="formError" style="display:none;"></div>
              </td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="publisherEdit.cacheDiscard"/></td>
              <td class="formField">
                <input type="text" id="cacheDiscardSize" value="${publisher.cacheDiscardSize}" class="formShort"/>
                <div id="cacheDiscardSizeMsg" class="formError" style="display:none;"></div>
              </td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="publisherEdit.updateEvent"/></td>
              <td class="formField">
                <sst:select id="changesOnly" value="${publisher.changesOnly}">
                  <sst:option value="false"><fmt:message key="publisherEdit.updateEvent.all"/></sst:option>
                  <sst:option value="true"><fmt:message key="publisherEdit.updateEvent.changes"/></sst:option>
                </sst:select>
              </td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="publisherEdit.snapshot"/></td>
              <td class="formField"><sst:checkbox id="sendSnapshot" onclick="sendSnapshotChanged()"
                      selectedValue="${publisher.sendSnapshot}"/></td>
            </tr>
            
            <tr>
              <td class="formLabelRequired"><fmt:message key="publisherEdit.snapshotPeriod"/></td>
              <td class="formField">
                <input type="text" id="snapshotSendPeriods" value="${publisher.snapshotSendPeriods}" class="formShort"/>
                <tag:timePeriods id="snapshotSendPeriodType" value="${publisher.snapshotSendPeriodType}" s="true" min="true" h="true"/>
                <div id="snapshotSendPeriodsMsg" class="formError" style="display:none;"></div>
              </td>
            </tr>
          </table>
        </div>
        
        <div>
          <c:set var="incpage">/<c:out value="<%= Main.Constants.DIR_MODULES %>"/>/${publisher.definition.module.name}/${publisher.definition.editPagePath}</c:set>
          <jsp:include page="${incpage}"/>
        </div>
      </td>
    </tr>
    
    <tr><td>&nbsp;</td></tr>
    
    <tr>
      <td align="center">
        <input id="saveBtn" type="button" value="<fmt:message key="common.save"/>" onclick="savePublisher()"/>
        <input type="button" value="<fmt:message key="common.cancel"/>" onclick="window.location='publishers.shtm'"/>
      </td>
    </tr>
  </table>
</tag:page>