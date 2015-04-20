<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.Common"%>

<tag:page showHeader="${param.showHeader}" showToolbar="${param.showToolbar}" dwr="UsersDwr" onload="init">
  <style type="text/css">
    .permissionStr { display: block; }
  </style>

  <script type="text/javascript">
    dojo.require("dojo.store.Memory");
    dojo.require("dijit.form.FilteringSelect");
  
    var userId = ${sessionUser.id};
    var editingUserId;
//     var dataSources;
    var adminUser;
    var userMemoryStore; //For realtime user manip
    var myTooltipDialog;
    
    function init() {
        UsersDwr.getInitData(function(data) {
            if (data.admin) {
                adminUser = true;
                
                show("userList");
                show("usernameRow");
                show("administrationRow");
                show("disabledRow");
                show("permissionsRow");
                show("deleteImg");
                show("sendTestEmailImg");
                
                //Create the global store
                userMemoryStore = new dojo.store.Memory({data: data.users});
                
                var i, j;
                for (i=0; i<data.users.length; i++) {
                    appendUser(data.users[i].id);
                    updateUser(data.users[i]);
                }
                
                //Create the Filtering Select
                new dijit.form.FilteringSelect({
                    store: userMemoryStore,
                    searchAttr: "username",                  
                    autoComplete: false,
                    style: "width: 100%",
                    highlightMatch: "all",
                    queryExpr: "*\${0}*",
                    required: false
                }, "su_username");
            }
            else {
                // Not an admin user.
                adminUser = false;
                editingUserId = data.user.id;
                showUserCB(data.user);
            }
            
            dwr.util.addOptions("timezone", [{id:'', value:'<sst:i18n key="users.timezone.def" escapeQuotes="true"/>'}], "id", "value");
            dwr.util.addOptions("timezone", data.timezones);
        });
    }
    
    function showUser(userId) {
        if (editingUserId)
            stopImageFader($("u"+ editingUserId +"Img"));
        if (myTooltipDialog)
            myTooltipDialog.destroy();
        editingUserId = userId;
        UsersDwr.getUser(userId, showUserCB);
        startImageFader($("u"+ editingUserId +"Img"));
    }
    
    function showUserCB(user) {
        show($("userDetails"));
        $set("username", user.username);
        $set("password", user.password);
        $set("email", user.email);
        $set("phone", user.phone);
        $set("administrator", user.admin);
        $set("disabled", user.disabled);
        $set("receiveAlarmEmails", user.receiveAlarmEmails);
        $set("receiveOwnAuditEvents", user.receiveOwnAuditEvents);
        $set("timezone", user.timezone);
        $set("permissions", user.permissions);
        
        setUserMessage();
        updateUserImg();
    }
    
    function saveUser() {
        setUserMessage();
        if (adminUser)
            UsersDwr.saveUserAdmin(editingUserId, $get("username"), $get("password"), $get("email"), $get("phone"), 
                    $get("administrator"), $get("disabled"), $get("receiveAlarmEmails"), $get("receiveOwnAuditEvents"),
                    $get("timezone"), $get("permissions"), saveUserCB);
        else
            UsersDwr.saveUser(editingUserId, $get("password"), $get("email"), $get("phone"),
                    $get("receiveAlarmEmails"), $get("receiveOwnAuditEvents"), $get("timezone"), saveUserCB);
    }
    
    function saveUserCB(response) {
        if (response.hasMessages)
            showDwrMessages(response.messages, "genericMessages");
        else if (!adminUser)
            setUserMessage("<fmt:message key="users.dataSaved"/>");
        else {
            if (editingUserId == <c:out value="<%= Common.NEW_ID %>"/>) {
                stopImageFader($("u"+ editingUserId +"Img"));
                editingUserId = response.data.userId;
                appendUser(editingUserId);
                startImageFader($("u"+ editingUserId +"Img"));
                setUserMessage("<fmt:message key="users.added"/>");
                userMemoryStore.put({id: response.data.userId, username: $get("username")});
            }
            else
                setUserMessage("<fmt:message key="users.saved"/>");
            UsersDwr.getUser(editingUserId, updateUser)
        }
    }
    
    function sendTestEmail() {
        UsersDwr.sendTestEmail($get("email"), $get("username"), function(result) {
            stopImageFader($("sendTestEmailImg"));
            if (result.exception)
                setUserMessage(result.exception);
            else
                setUserMessage(result.message);
        });
        startImageFader($("sendTestEmailImg"));
    }
    
    function setUserMessage(message) {
        if (message)
            $set("userMessage", message);
        else {
            $set("userMessage");
            hideContextualMessages("userDetails");
            hideGenericMessages("genericMessages");
        }
    }
    
    function appendUser(userId) {
        createFromTemplate("u_TEMPLATE_", userId, "usersTable");
    }
    
    function updateUser(user) {
        $("u"+ user.id +"Username").innerHTML = user.username;
        setUserImg(user.admin, user.disabled, $("u"+ user.id +"Img"));
    }
    
    function updateUserImg() {
        setUserImg($get("administrator"), $get("disabled"), $("userImg"));
    }
    
    function deleteUser() {
        if (confirm("<m2m2:translate key="users.deleteConfirm" escapeDQuotes="true"/>")) {
            var userId = editingUserId;
            startImageFader("deleteImg");
            UsersDwr.deleteUser(userId, function(response) {
                stopImageFader("deleteImg");
                
                if (response.hasMessages)
                    setUserMessage(response.messages[0].genericMessage);
                else {
                    userMemoryStore.remove(userId);
                    stopImageFader("u"+ userId +"Img");
                    $("usersTable").removeChild($("u"+ userId));
                    hide("userDetails");
                    editingUserId = null;
                }
            });
        }
    }
    
    /*
     * Make a Copy of an existing user
    */
    function copyUser(copyImage){
    	if (editingUserId > 0)
            stopImageFader($("u"+ editingUserId +"Img"));
    	
    	editingUserId = <c:out value="<%= Common.NEW_ID %>"/>;
    	var userId = copyImage.id.substring(6); //Strip off the first 'uImage'
    	UsersDwr.getCopy(userId,function(response){
    		//Load up the new User
    		showUserCB(response.data.vo);
    	});
    }
    
    /*
     * Switch to another user
     */
     function switchUser(){
        var newUser = $get("su_username");
        UsersDwr.su(newUser,function(response){
            if(response.hasMessages)
                showDwrMessages(response.messages);           
            else
	           // Will need to reload the page
	           window.location.reload();
        });
    }
    
    function openPermissionList() {
    	UsersDwr.getAllUserGroups($get("permissions"), function(groups) {
            if (myTooltipDialog)
                myTooltipDialog.destroy();
            
    		var content = "";
    		if (groups.length == 0)
    		    content = "<m2m2:translate key="users.permissions.nothingNew" escapeDQuotes="true"/>";
    		else {
    		    for (var i=0; i<groups.length; i++)
    		        content += "<a id='perm-"+ escapeQuotes(groups[i]) +"' class='ptr permissionStr'>"+ groups[i] +"</a>";
    		}
    		
            require(["dijit/TooltipDialog", "dijit/popup", "dojo/dom" ], function(TooltipDialog, popup, dom) {
                myTooltipDialog = new TooltipDialog({
                    id: 'myTooltipDialog',
                    content: content,
                    onMouseLeave: function() { 
                        popup.close(myTooltipDialog);
                    }
                });
                
                popup.open({
                    popup: myTooltipDialog,
                    around: $("permissions")
                });
            });
            
            require(["dojo/query"], function(query) {
            	query(".permissionStr").forEach(function(e) {
            		// Curious combination of AMD and non.
                    dojo.connect(e, "onclick", addGroups);
            	})
            });
    	});
    }
    
    function addGroups() {
    	var self = this;
    	var groups = $get("permissions");
    	if (groups.length > 0 && groups.substring(groups.length-1) != ",")
    		groups += ",";
    	groups += this.id.substring(5);
    	$set("permissions", groups);
    	openPermissionList();
    }
  </script>
  
  <table>
    <tr>
      <td valign="top" id="userList" style="display:none;">
        <div class="borderDiv">
          <table class="wide">
            <tr>
              <td>
                <span class="smallTitle"><fmt:message key="users.title"/></span>
                <tag:help id="userAdministration"/>
              </td>
              <td align="right"><tag:img png="user_add" onclick="showUser(${applicationScope['constants.Common.NEW_ID']})"
                      title="users.add" id="u${applicationScope['constants.Common.NEW_ID']}Img"/></td>
            </tr>
          </table>
         <c:if test="${sessionUser.admin}">
         <table>
            <tr>
                <td colspan="2" id="suMessage" class="formError"></td>
            </tr>
            <tr>
            <td>Switch to User</td><td><input type="text" id="su_username"/></td>
            <td colspan="2"><button id="su_button" onclick="switchUser()">Switch</button>
            </tr>
         </table>
         </c:if>
          <table id="usersTable">
            <tbody id="u_TEMPLATE_" class="ptr" style="display:none;"><tr>
              <td><tag:img id="u_TEMPLATE_Img" png="user_green" title="users.user"/></td>
              <td class="link" id="u_TEMPLATE_Username" onclick="showUser(getMangoId(this));"></td>
              <td><tag:img id="uImage_TEMPLATE_" png="copy" onclick="copyUser(this);" title="common.copy" /></td>
            </tr></tbody>
          </table>
        </div>
      </td>
      
      <td valign="top" style="display:none;" id="userDetails">
        <div class="borderDiv">
          <table class="wide">
            <tr>
              <td>
                <span class="smallTitle"><tag:img id="userImg" png="user_green" title="users.user"/>
                <fmt:message key="users.details"/></span>
              </td>
              <td align="right">
                <tag:img png="save" onclick="saveUser();" title="common.save"/>
                <tag:img id="deleteImg" png="delete" onclick="deleteUser();" title="common.delete" style="display:none;"/>
                <tag:img id="sendTestEmailImg" png="email_go" onclick="sendTestEmail();" title="common.sendTestEmail"
                        style="display:none;"/>
              </td>
            </tr>
          </table>
          
          <table><tbody id="genericMessages"></tbody></table>
          
          <table>
            <tr>
              <td colspan="2" id="userMessage" class="formError"></td>
            </tr>
            <tr id="usernameRow" style="display:none;">
              <td class="formLabelRequired"><fmt:message key="users.username"/></td>
              <td class="formField"><input id="username" type="text"/></td>
            </tr>
            <tr>
              <td class="formLabelRequired"><fmt:message key="users.newPassword"/></td>
              <td class="formField"><input id="password" type="text"/></td>
            </tr>
            <tr>
              <td class="formLabelRequired"><fmt:message key="users.email"/></td>
              <td class="formField"><input id="email" type="text" class="formLong"/></td>
            </tr>
            <tr>
              <td class="formLabel"><fmt:message key="users.phone"/></td>
              <td class="formField"><input id="phone" type="text"/></td>
            </tr>
            <tr id="administrationRow" style="display:none;">
              <td class="formLabelRequired"><fmt:message key="common.administrator"/></td>
              <td class="formField"><input id="administrator" type="checkbox" onclick="updateUserImg();"/></td>
            </tr>
            <tr id="disabledRow" style="display:none;">
              <td class="formLabelRequired"><fmt:message key="common.disabled"/></td>
              <td class="formField"><input id="disabled" type="checkbox" onclick="updateUserImg();"/></td>
            </tr>
            <tr>
              <td class="formLabelRequired"><fmt:message key="users.receiveAlarmEmails"/></td>
              <td class="formField"><tag:alarmLevelOptions id="receiveAlarmEmails"/></td>
            </tr>
            <tr>
              <td class="formLabelRequired"><fmt:message key="users.receiveOwnAuditEvents"/></td>
              <td class="formField"><input id="receiveOwnAuditEvents" type="checkbox"/></td>
            </tr>
            <tr>
              <td class="formLabelRequired"><fmt:message key="users.timezone"/></td>
              <td class="formField"><select id="timezone"></select></td>
            </tr>
            <tbody id="permissionsRow" style="display:none;">
              <tr>
                <td class="formLabelRequired"><fmt:message key="users.permissions"/></td>
                <td class="formField">
                  <input id="permissions" type="text" class="formLong"/>
                  <tag:img png="bullet_down" title="users.permissions" onclick="openPermissionList()"/>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </td>
    </tr>
  </table>
</tag:page>
