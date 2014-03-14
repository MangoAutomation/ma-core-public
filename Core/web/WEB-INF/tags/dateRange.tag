<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@tag body-content="empty"%>
<table>
  <tr>
    <td><fmt:message key="common.dateRangeFrom"/></td>
    <td><input type="text" id="fromYear" class="formVeryShort" value="${fromYear}"/></td>
    <td><tag:monthOptions id="fromMonth" value="${fromMonth}"/></td>
    <td><tag:dayOptions id="fromDay" value="${fromDay}"/></td>
    <td>,</td>
    <td><tag:hourOptions id="fromHour" value="${fromHour}"/></td>
    <td>:</td>
    <td><tag:minuteOptions id="fromMinute" value="${fromMinute}"/></td>
    <td>:</td>
    <td><tag:secondOptions id="fromSecond" value="${fromSecond}"/></td>
    <td><input type="checkbox" name="fromNone" id="fromNone" onclick="updateDateRange()"/><label
            for="fromNone"><fmt:message key="common.inception"/></label></td>
  </tr>
  <tr>
    <td><fmt:message key="common.dateRangeTo"/></td>
    <td><input type="text" id="toYear" class="formVeryShort" value="${toYear}"/></td>
    <td><tag:monthOptions id="toMonth" value="${toMonth}"/></td>
    <td><tag:dayOptions id="toDay" value="${toDay}"/></td>
    <td>,</td>
    <td><tag:hourOptions id="toHour" value="${toHour}"/></td>
    <td>:</td>
    <td><tag:minuteOptions id="toMinute" value="${toMinute}"/></td>
    <td>:</td>
    <td><tag:secondOptions id="toSecond" value="${toSecond}"/></td>
    <td><input type="checkbox" name="toNone" id="toNone" checked="checked" onclick="updateDateRange()"/><label
            for="toNone"><fmt:message key="common.latest"/></label></td>
  </tr>
</table>
