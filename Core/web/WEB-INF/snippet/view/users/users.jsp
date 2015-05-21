<%--
    Copyright (C) 2013 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<div id="userErrors"></div>
<div class="clearfix"></div>

<%-- For admins place a SU div --%>
<c:if test="${sessionUser.admin}">
<div>
  <span class="smallTitle"><fmt:message key="users.su" /></span>
  <div class="clearfix"></div>
  <div id="switchUserPicker"></div>
</div>
<hr>
</c:if>

<div>
  <c:if test="${sessionUser.admin}">
  <span class="smallTitle"><fmt:message key="users.editUser" /></span>
  <div class="clearfix"></div>
  <div id="userPicker"></div>
  <img id="newUser" class="ptr"
    src="/images/user_add.png" 
    title="<fmt:message key="users.add" />" 
    alt="<fmt:message key="users.add" />"
  />
  <hr>
  </c:if>

  <div id="userEditView" style="float:left; display:none">
      <table class="wide">
        <tr>
          <td>
            <span class="smallTitle">
              <img id="userImg"
                src="/images/user_green.png" 
                title="<fmt:message key="users.user" />" 
                alt="<fmt:message key="users.user" />"
              />
              <fmt:message key="users.details" />
            </span>
            <img id="usersHelp" class="ptr"
              src="/images/help.png"
              title="<fmt:message key="users.user" />" 
              alt="<fmt:message key="users.user" />"
            />
          </td>
          <td align="right">
            <img id="saveUser" class="ptr"
              src="/images/save.png" 
              title="<fmt:message key="common.save" />" 
              alt="<fmt:message key="common.save" />"
            />
            <img id="deleteUser" class="ptr"
              src="/images/delete.png"
              title="<fmt:message key="common.delete" />" 
              alt="<fmt:message key="common.delete" />"
            />             
            <img id="sendTestEmail" class="ptr"
              src="/images/email_go.png"
              title="<fmt:message key="common.sendTestEmail" />" 
              alt="<fmt:message key="common.sendTestEmail" />"     
            />          
        </tr>
        <tr>
          <td colspan="2" id="userMessage" class="formError"></td>
        </tr>
        <tr>
          <td class="formLabelRequired"><fmt:message
              key="users.username" /></td>
          <td class="formField"><input id="username" type="text" /></td>
        </tr>
        <tr>
          <td class="formLabelRequired"><fmt:message
              key="users.newPassword" /></td>
          <td class="formField"><input id="password" type="text" /></td>
        </tr>
        <tr>
          <td class="formLabelRequired"><fmt:message
              key="users.email" /></td>
          <td class="formField"><input id="email" type="text"
            class="formLong" /></td>
        </tr>
        <tr>
          <td class="formLabel"><fmt:message key="users.phone" /></td>
          <td class="formField"><input id="phone" type="text" /></td>
        </tr>
        <tr id="disabledRow" style="display: none;">
          <td class="formLabelRequired"><fmt:message
              key="common.disabled" /></td>
          <td class="formField"><input id="disabled"
            type="checkbox" /></td>
        </tr>
        <tr>
          <td class="formLabelRequired"><fmt:message
              key="users.receiveAlarmEmails" /></td>
          <td class="formField"><html5:alarmLevelSelect id="receiveAlarmEmails" required="true"/></td>
        </tr>
        <tr>
          <td class="formLabelRequired"><fmt:message
              key="users.receiveOwnAuditEvents" /></td>
          <td class="formField"><input id="receiveOwnAuditEvents"
            type="checkbox" /></td>
        </tr>
        <tr>
          <td class="formLabelRequired"><fmt:message
              key="users.timezone" /></td>
          <td class="formField"><div id="timezone"></div></td>
        </tr>
        <tbody id="permissionsRow" style="display: none;">
          <tr>
            <td class="formLabelRequired"><fmt:message
                key="users.permissions" /></td>
            <td class="formField">
            <input id="permissions" type="text" class="formLong" /> 
              <img id="permissionsViewer" class="ptr" 
                src="/images/bullet_down.png" 
                title="<fmt:message key="users.permissions" />" 
                alt="<fmt:message key="users.permissions" />"
              />
            </td>
          </tr>
        </tbody>
      </table>
  </div>
</div>