<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@page import="com.serotonin.m2m2.Common"%>
<%@page import="com.serotonin.m2m2.vo.UserComment"%>
<%@page import="com.serotonin.m2m2.rt.event.type.EventType"%>
<%@page import="com.serotonin.m2m2.web.dwr.EventsDwr"%>
<%@page import="com.serotonin.m2m2.module.ModuleRegistry"%>
<%@page import="com.serotonin.m2m2.module.EventTypeDefinition"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<tag:page dwr="EventsDwr">
  <%@ include file="/WEB-INF/jsp/include/userComment.jsp" %>
  <style>
    .incrementControl { width: 2em; }
  </style>
  <script type="text/javascript">
    dojo.require("dijit.Calendar");
    
    // Tell the log poll that we're interested in monitoring pending alarms.
    mango.longPoll.pollRequest.pendingAlarms = true;
  
    dojo.ready(function() {
        // Wrapping setDateRange in a function appears to prevent the calendar from staying exposed.
        EventsDwr.getDateRangeDefaults(<c:out value="<%= Common.TimePeriods.DAYS %>"/>, 1, function(data) { setDateRange(data); });
        
        var x = dijit.byId("datePicker");
        x.goToToday();
        x.onChange = jumpToDateClicked;
        hide("datePickerDiv");
    });
    
    function updatePendingAlarmsContent(content) {
        hide("hourglass");
        
        $set("pendingAlarms", content);
        if (content) {
            show("ackAllDiv");
            hide("noAlarms");
        }
        else {
            $set("pendingAlarms", "");
            hide("ackAllDiv");
            show("noAlarms");
        }
    }
    
    function doSearch(page, date) {
        setDisabled("searchBtn", true);
        $set("searchMessage", "<fmt:message key="events.search.searching"/>");
        EventsDwr.search($get("eventId"), $get("eventType"), $get("eventStatus"), $get("alarmLevel"),
                $get("keywords"), $get("dateRangeType"), $get("relativeType"), $get("prevPeriodCount"), 
                $get("prevPeriodType"), $get("pastPeriodCount"), $get("pastPeriodType"), $get("fromNone"), 
                $get("fromYear"), $get("fromMonth"), $get("fromDay"), $get("fromHour"), $get("fromMinute"), 
                $get("fromSecond"), $get("toNone"), $get("toYear"), $get("toMonth"), $get("toDay"), $get("toHour"), 
                $get("toMinute"), $get("toSecond"), page, date, function(results) {
            $set("searchResults", results.data.content);
            setDisabled("searchBtn", false);
            $set("searchMessage", results.data.resultCount);
        });
    }

    function jumpToDate(parent) {
        var div = $("datePickerDiv");
        show(div);
        var bounds = getAbsoluteNodeBounds(parent);
        div.style.top = bounds.y +"px";
        div.style.left = bounds.x +"px";
    }

    var dptimeout = null;
    function expireDatePicker() {
        dptimeout = setTimeout(function() { hide("datePickerDiv"); }, 500);
    }

    function cancelDatePickerExpiry() {
        if (dptimeout) {
            clearTimeout(dptimeout);
            dptimeout = null;
        }
    }

    function jumpToDateClicked(date) {
        var div = $("datePickerDiv");
        if (isShowing(div)) {
            hide(div);
            doSearch(0, date);
        }
    }

    function newSearch() {
        var x = dijit.byId("datePicker");
        x.goToToday();
        doSearch(0);
    }
    
    function silenceAll() {
        MiscDwr.silenceAll(function(result) {
            var silenced = result.data.silenced;
            for (var i=0; i<silenced.length; i++)
                setSilenced(silenced[i], true);
        });
    }
    
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
    
    function exportEvents() {
        startImageFader($("exportEventsImg"));
        EventsDwr.exportEvents($get("eventId"), $get("eventSourceType"), $get("eventStatus"), $get("alarmLevel"),
                $get("keywords"), $get("dateRangeType"), $get("relativeType"), $get("prevPeriodCount"), 
                $get("prevPeriodType"), $get("pastPeriodCount"), $get("pastPeriodType"), $get("fromNone"), 
                $get("fromYear"), $get("fromMonth"), $get("fromDay"), $get("fromHour"), $get("fromMinute"), 
                $get("fromSecond"), $get("toNone"), $get("toYear"), $get("toMonth"), $get("toDay"), $get("toHour"), 
                $get("toMinute"), $get("toSecond"), function(data) {
            stopImageFader($("exportEventsImg"));
            window.location = "eventExport/eventData.csv";
        });
    }
  </script>
  
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