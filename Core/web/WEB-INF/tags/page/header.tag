<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%>
<%@tag import="com.serotonin.m2m2.Common"%>

<table width="100%" cellspacing="0" cellpadding="0" border="0" id="mainHeader">
  <tr>
    <td><img src="/<%= Common.applicationLogo %>" alt="Logo"/></td>
    <c:if test="${!simple}">
      <td align="center" width="99%">
        <a href="events.shtm">
          <span id="__header__alarmLevelDiv" style="display:none;">
            <img id="__header__alarmLevelImg" src="/images/spacer.gif" alt="" title=""/>
            <span id="__header__alarmLevelText"></span>
          </span>
        </a>
      </td>
    </c:if>
    <c:if test="${!empty instanceDescription}">
      <td align="right" valign="bottom" class="smallTitle" style="padding:5px; white-space: nowrap;">${instanceDescription}</td>
    </c:if>
  </tr>
</table>