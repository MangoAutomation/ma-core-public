<%--
    Copyright (C) 2013 Infinite Automation. All rights reserved.
    @author Terry Packer
--%>
<%@page import="com.serotonin.m2m2.Common"%>
<%@page import="com.serotonin.m2m2.vo.UserComment"%>
<%@page import="com.serotonin.m2m2.rt.event.type.EventType"%>
<%@page import="com.serotonin.m2m2.web.dwr.EventsDwr"%>
<%@page import="com.serotonin.m2m2.module.ModuleRegistry"%>
<%@page import="com.serotonin.m2m2.module.EventTypeDefinition"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<tag:page showHeader="${param.showHeader}" showToolbar="${param.showToolbar}" dwr="EventsDwr,EventInstanceDwr" onload="init">
  <%@ include file="/WEB-INF/jsp/include/userComment.jsp" %>
  <style>
    .incrementControl { width: 2em; }

    #eventInstanceTable .dgrid-column-id {text-align: center; width: 12em;}
    #eventInstanceTable .dgrid-column-alarmLevel {width: 18em !important;} /* Important because all cell contents are actually smaller */
    #eventInstanceTable .dgrid-column-activeTimestampString {text-align: center; width: 15em;}
    #eventInstanceTable .dgrid-column-messageString {text-align: left;}
    #eventInstanceTable .dgrid-column-rtnTimestampString {text-align: center; width: 20em;}
    #eventInstanceTable .dgrid-column-alarmLevel {text-align: center; width: 9em;}
    #eventInstanceTable .dgrid-column-totalTimeString {text-align: center; width: 12em;}
    #eventInstanceTable .dgrid-column-acknowledged {text-align: center; width: 19em !important;}/* Important because all cell contents are actually smaller */
    
    
  </style>
  <script type="text/javascript" src="/resources/stores.js"></script>
  <script type="text/javascript" src="resources/events.js"></script>
  <script type="text/javascript" src="/resources/view/eventInstance/eventInstanceView.js"></script>
  
  <script type="text/javascript">
  var databaseType = '<c:out value="<%= Common.databaseProxy.getType() %>"></c:out>';

  //For use on page to compare Event Types2
  var constants_DATA_POINT = '${applicationScope['constants.EventType.EventTypeNames.DATA_POINT']}';
  var constants_DATA_SOURCE = '${applicationScope['constants.EventType.EventTypeNames.DATA_SOURCE']}';
  var constants_SYSTEM = '${applicationScope['constants.EventType.EventTypeNames.SYSTEM']}';
  var constants_TYPE_SET_POINT_HANDLER_FAILURE = '${applicationScope['constants.SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE']}';
  var constants_TYPE_LICENSE_CHECK = '${applicationScope['constants.SystemEventType.TYPE_LICENSE_CHECK']}';
  var constants_PUBLISHER = '${applicationScope['constants.EventType.EventTypeNames.PUBLISHER']}';
  var constants_AUDIT = '${applicationScope['constants.EventType.EventTypeNames.AUDIT']}';
  var constants_AUDIT_TYPE_DATA_SOURCE = '${applicationScope['constants.AuditEventType.TYPE_DATA_SOURCE']}';
  var constants_AUDIT_TYPE_DATA_POINT = '${applicationScope['constants.AuditEventType.TYPE_DATA_POINT']}';
  var constants_AUDIT_TYPE_POINT_EVENT_DETECTOR = '${applicationScope['constants.AuditEventType.TYPE_POINT_EVENT_DETECTOR']}';
  var constants_AUDIT_TYPE_EVENT_HANDLER = '${applicationScope['constants.AuditEventType.TYPE_EVENT_HANDLER']}';
  var constants_USER_COMMENT_TYPE_EVENT = "${applicationScope['constants.UserComment.TYPE_EVENT']}";
  
  require(["dojo/parser","dijit/Calendar","dojo/domReady!"]);

     function init(){
          // Wrapping setDateRange in a function appears to prevent the calendar from staying exposed.
          EventsDwr.getDateRangeDefaults(<c:out value="<%= Common.TimePeriods.DAYS %>"/>, 1, function(data) { setDateRange(data); });
          
          var x = dijit.byId("datePicker");
          x.goToToday();
          x.onChange = jumpToDateClicked;
          hide("datePickerDiv");

          };
  
  
  function updateDateRangeFields() {
      var dateRangeType = $get("dateRangeType");
      if (dateRangeType == <c:out value="<%= EventsDwr.DATE_RANGE_TYPE_RELATIVE %>"/>) {
          show("dateRangeRelative");
          hide("dateRangeSpecific");
          
          var relativeType = $get("relativeType");
          if (relativeType == 1) {
              setDisabled("prevPeriodCount", false);
              setDisabled("prevPeriodType", false);
              setDisabled("pastPeriodCount", true);
              setDisabled("pastPeriodType", true);
          }
          else {
              setDisabled("prevPeriodCount", true);
              setDisabled("prevPeriodType", true);
              setDisabled("pastPeriodCount", false);
              setDisabled("pastPeriodType", false);
          }
      }
      else if (dateRangeType == <c:out value="<%= EventsDwr.DATE_RANGE_TYPE_SPECIFIC %>"/>) {
          hide("dateRangeRelative");
          show("dateRangeSpecific");
          updateDateRange();
      }
      else {
          hide("dateRangeRelative");
          hide("dateRangeSpecific");
      }
  }
  </script>
  
  <div class="mangoContainer">
  <jsp:include page="/WEB-INF/snippet/view/eventInstance/eventInstanceTable.jsp"/>
  </div>
  
  <div class="borderDiv marB" style="float:left;">
    <div class="smallTitle titlePadding" style="float:left;">
      <tag:img png="flag_white" title="events.alarms"/>
      <fmt:message key="events.pending"/>
    </div>
    <div id="ackAllDiv" class="titlePadding" style="display:none;float:right;">
      <fmt:message key="events.acknowledgeAll"/>
      <tag:img png="tick" onclick="MiscDwr.acknowledgeAllPendingEvents()" title="events.acknowledgeAll"/>&nbsp;
      <fmt:message key="events.silenceAll"/>
      <tag:img png="sound_mute" onclick="silenceAll()" title="events.silenceAll"/><br/>
    </div>
    <div id="pendingAlarms" style="clear:both;"></div>
    <div id="noAlarms" style="display:none;padding:6px;text-align:center;">
      <b><fmt:message key="events.emptyList"/></b>
    </div>
    <div id="hourglass" style="padding:6px;text-align:center;"><tag:img png="hourglass"/></div>
  </div>
  
  <div class="borderDiv" style="clear:left;float:left;">
    <div class="smallTitle titlePadding"><fmt:message key="events.search"/></div>
    <div>
      <table>
        <tr>
          <td class="formLabel"><fmt:message key="events.id"/></td>
          <td class="formField"><input id="eventId" type="text"></td>
        </tr>
        <tr>
          <td class="formLabel"><fmt:message key="events.search.type"/></td>
          <td class="formField">
            <select id="eventType">
              <option value=""><fmt:message key="common.all"/></option>
              <option value="<c:out value="<%= EventType.EventTypeNames.DATA_POINT %>"/>"><fmt:message key="eventHandlers.pointEventDetector"/></option>
              <option value="<c:out value="<%= EventType.EventTypeNames.DATA_SOURCE %>"/>"><fmt:message key="eventHandlers.dataSourceEvents"/></option>
              <option value="<c:out value="<%= EventType.EventTypeNames.PUBLISHER %>"/>"><fmt:message key="eventHandlers.publisherEvents"/></option>
              <option value="<c:out value="<%= EventType.EventTypeNames.SYSTEM %>"/>"><fmt:message key="eventHandlers.systemEvents"/></option>
              <option value="<c:out value="<%= EventType.EventTypeNames.AUDIT %>"/>"><fmt:message key="eventHandlers.auditEvents"/></option>
              
              <c:forEach items="<%= ModuleRegistry.getDefinitions(EventTypeDefinition.class) %>" var="eventTypeDef">
                <option value="${eventTypeDef.typeName}"><fmt:message key="${eventTypeDef.descriptionKey}"/></option>
              </c:forEach>
            </select>
          </td>
        </tr>
        <tr>
          <td class="formLabel"><fmt:message key="common.status"/></td>
          <td class="formField">
            <select id="eventStatus">
              <option value="<c:out value="<%= EventsDwr.STATUS_ALL %>"/>"><fmt:message key="common.all"/></option>
              <option value="<c:out value="<%= EventsDwr.STATUS_ACTIVE %>"/>"><fmt:message key="common.active"/></option>
              <option value="<c:out value="<%= EventsDwr.STATUS_RTN %>"/>"><fmt:message key="event.rtn.rtn"/></option>
              <option value="<c:out value="<%= EventsDwr.STATUS_NORTN %>"/>"><fmt:message key="common.nortn"/></option>
            </select>
          </td>
        </tr>
        <tr>
          <td class="formLabel"><fmt:message key="common.alarmLevel"/></td>
          <td class="formField"><tag:alarmLevelOptions id="alarmLevel" allOption="true"/></td>
        </tr>
        <tr>
          <td class="formLabel"><fmt:message key="events.search.keywords"/></td>
          <td class="formField"><input id="keywords" type="text"/></td>
        </tr>
        
        <tr>
          <td class="formLabel"><fmt:message key="events.search.dateRange"/></td>
          <td class="formField">
            <table>
              <tr><td>
                 <select id="dateRangeType" onchange="updateDateRangeFields()">
                   <option value="<c:out value="<%= EventsDwr.DATE_RANGE_TYPE_NONE %>"/>"><fmt:message key="events.search.dateRange.none"/></option>
                   <option value="<c:out value="<%= EventsDwr.DATE_RANGE_TYPE_RELATIVE %>"/>"><fmt:message key="events.search.dateRange.relative"/></option>
                   <option value="<c:out value="<%= EventsDwr.DATE_RANGE_TYPE_SPECIFIC %>"/>"><fmt:message key="events.search.dateRange.specific"/></option>
                 </select>
              </td></tr>
              <tr>
                <td style="padding-left:40px;">
                  <table id="dateRangeRelative" style="display: none;">
                    <tr>
                      <td valign="top"><input type="radio" name="relativeType" onchange="updateDateRangeFields()"
                              id="relprev" value="<c:out value="<%= EventsDwr.RELATIVE_DATE_TYPE_PREVIOUS %>"/>" 
                              checked="checked"/><label for="relprev"><fmt:message key="events.search.previous"/></label></td>
                      <td valign="top">
                        <input type="text" id="prevPeriodCount" class="formVeryShort"/>
                        <tag:timePeriods id="prevPeriodType" min="true" h="true" d="true" w="true" mon="true" y="true"/><br/>
                        <span class="formError" id="previousPeriodCountError"></span>
                      </td>
                    </tr>
                    <tr>
                      <td valign="top"><input type="radio" name="relativeType" onchange="updateDateRangeFields()"
                              id="relpast" value="<c:out value="<%= EventsDwr.RELATIVE_DATE_TYPE_PAST %>"/>"/><label 
                              for="relpast"><fmt:message key="events.search.past"/></label></td>
                      <td valign="top">
                        <input type="text" id="pastPeriodCount" class="formVeryShort"/>
                        <tag:timePeriods id="pastPeriodType" min="true" h="true" d="true" w="true" mon="true" y="true"/><br/>
                        <span class="formError" id="pastPeriodCountError"></span>
                      </td>
                    </tr>
                  </table>
                  
                  <div id="dateRangeSpecific" style="display: none;"><tag:dateRange/></div>
                </td>
              </tr>
            </table>
          </td>
        </tr>
        
        <tr>
          <td colspan="2" align="center">
            <input id="searchBtn" type="button" value="<fmt:message key="events.search.search"/>" onclick="newSearch()"/>
            <span id="searchMessage" class="formError"></span>
          </td>
        </tr>
      </table>
    </div>
    <div id="searchResults"></div>
  </div>
  <div id="datePickerDiv" style="position:absolute; top:0px; left:0px;" onmouseover="cancelDatePickerExpiry()" onmouseout="expireDatePicker()">
    <div id="datePicker" dojoType="dijit.Calendar" dayWidth="narrow" lang="${lang}"></div>
  </div>
</tag:page>