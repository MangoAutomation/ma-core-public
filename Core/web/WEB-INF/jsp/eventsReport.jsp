<%--
    Copyright (C) 2013 Infinite Automation. All rights reserved.
    @author Terry Packer
--%>
<%@page import="com.serotonin.m2m2.Common"%>
<%@page import="com.serotonin.m2m2.vo.UserComment"%>
<%@page import="com.serotonin.m2m2.rt.event.type.EventType"%>
<%@page import="com.serotonin.m2m2.module.ModuleRegistry"%>
<%@page import="com.serotonin.m2m2.module.EventTypeDefinition"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<tag:page showHeader="${param.showHeader}" showToolbar="${param.showToolbar}" dwr="EventsDwr,EventInstanceDwr" >
  <%@ include file="/WEB-INF/jsp/include/userComment.jsp" %>
  <style>
    .incrementControl { width: 2em; }

    #eventInstanceTable .dgrid-column-id {text-align: center; width: 12em;}
    #eventInstanceTable .dgrid-column-alarmLevel {width: 18em !important;} /* Important because all cell contents are actually smaller */
    #eventInstanceTable .dgrid-column-activeTimestampString {text-align: center; width: 15em;}
    #eventInstanceTable .dgrid-column-messageString {text-align: left; width: 20em;}
    #eventInstanceTable .dgrid-column-rtnTimestampString {text-align: center; width: 20em;}
    #eventInstanceTable .dgrid-column-alarmLevel {text-align: center; width: 9em;}
    #eventInstanceTable .dgrid-column-totalTimeString {text-align: center; width: 12em;}
    #eventInstanceTable .dgrid-column-acknowledged {text-align: center; width: 19em !important;}/* Important because all cell contents are actually smaller */
    
    
  </style>
  <script type="text/javascript" src="/resources/stores.js"></script>
  <script type="text/javascript" src="/resources/view/eventInstance/eventInstanceView.js"></script>

  <script type="text/javascript">
  var databaseType = '<c:out value="<%= Common.databaseProxy.getType() %>"></c:out>';
  var eventReportUserId = ${sessionUser.id}; //Get the UserId
  
  //For use on page to compare Event Types 2
  var constants_DATA_POINT = '${applicationScope['constants.EventType.EventTypeNames.DATA_POINT']}';
  var constants_DATA_SOURCE = '${applicationScope['constants.EventType.EventTypeNames.DATA_SOURCE']}';
  var constants_SYSTEM = '${applicationScope['constants.EventType.EventTypeNames.SYSTEM']}';
  var constants_TYPE_SET_POINT_HANDLER_FAILURE = '${applicationScope['constants.SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE']}';
  var constants_TYPE_LICENSE_CHECK = '${applicationScope['constants.SystemEventType.TYPE_LICENSE_CHECK']}';
  var constants_TYPE_UPGRADE_CHECK = '${applicationScope['constants.SystemEventType.TYPE_UPGRADE_CHECK']}';
  var constants_PUBLISHER = '${applicationScope['constants.EventType.EventTypeNames.PUBLISHER']}';
  var constants_AUDIT = '${applicationScope['constants.EventType.EventTypeNames.AUDIT']}';
  var constants_AUDIT_TYPE_DATA_SOURCE = '${applicationScope['constants.AuditEventType.TYPE_DATA_SOURCE']}';
  var constants_AUDIT_TYPE_DATA_POINT = '${applicationScope['constants.AuditEventType.TYPE_DATA_POINT']}';
  var constants_AUDIT_TYPE_POINT_EVENT_DETECTOR = '${applicationScope['constants.AuditEventType.TYPE_POINT_EVENT_DETECTOR']}';
  var constants_AUDIT_TYPE_EVENT_HANDLER = '${applicationScope['constants.AuditEventType.TYPE_EVENT_HANDLER']}';
  var constants_USER_COMMENT_TYPE_EVENT = "${applicationScope['constants.UserComment.TYPE_EVENT']}";
  var constants_TYPE_SYSTEM_STARTUP = "${applicationScope['constants.SystemEventType.TYPE_SYSTEM_STARTUP']}";
  var constants_TYPE_SYSTEM_SHUTDOWN = "${applicationScope['constants.SystemEventType.TYPE_SYSTEM_SHUTDOWN']}";
  var constants_TYPE_USER_LOGIN = "${applicationScope['constants.SystemEventType.TYPE_USER_LOGIN']}";
  
  //Get from the URL Parameter
  var alarmLevelUrlParameter = '${param.level}';
  
  require(["dojo/parser","dijit/Calendar","dojo/domReady!"]);

  </script>
  
  <div class="mangoContainer">
  <jsp:include page="/WEB-INF/snippet/view/eventInstance/eventInstanceTable.jsp"/>
  </div>
</tag:page>