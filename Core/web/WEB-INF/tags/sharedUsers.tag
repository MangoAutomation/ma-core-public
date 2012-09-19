<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf" %><%--
--%><%@tag body-content="empty" %><%--
--%><%@attribute name="doxId" required="true" %><%--
--%><%@attribute name="noUsersKey" required="true" %><%--
--%><%@attribute name="closeFunction" %>
<table>
  <tr>
    <td>
      <tag:img png="user" title="share.sharing"/>
      <span class="smallTitle"><fmt:message key="share.sharing"/></span>
      <tag:help id="${doxId}"/>
    </td>
    <td align="right">
      <c:if test="${!empty closeFunction}">
        <tag:img png="cross" onclick="${closeFunction}" title="common.close" style="display:inline;"/>
      </c:if>
    </td>
  </tr>
  
  <tr>
    <td colspan="2">
      <select id="allShareUsersList"></select>
      <tag:img png="add" onclick="mango.share.addUserToShared();" title="common.add"/>
    </td>
  </tr>
  
  <tr>
    <td colspan="2">
      <table cellspacing="1">
        <tbody id="sharedUsersTableEmpty" style="display:none;">
          <tr><th colspan="3"><fmt:message key="${noUsersKey}"/></th></tr>
        </tbody>
        <tbody id="sharedUsersTableHeaders" style="display:none;">
          <tr class="smRowHeader">
            <td><fmt:message key="share.userName"/></td>
            <td><fmt:message key="share.accessType"/></td>
            <td></td>
          </tr>
        </tbody>
        <tbody id="sharedUsersTable"></tbody>
      </table>
    </td>
  </tr>
</table>