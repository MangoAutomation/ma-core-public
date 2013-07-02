<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.Common"%>

<tag:page dwr="UsersDwr" onload="init">
  <script type="text/javascript">
    var userId = ${sessionUser.id};
    var editingUserId;
    var dataSources;
    var adminUser;
    
    function init() {
        UsersDwr.getInitData(function(data) {
            if (data.admin) {
                adminUser = true;
                
                show("userList");
                show("usernameRow");
                show("administrationRow");
                show("disabledRow");
                show("dataSources");
                show("deleteImg");
                show("sendTestEmailImg");
                
                var i, j;
                for (i=0; i<data.users.length; i++) {
                    appendUser(data.users[i].id);
                    updateUser(data.users[i]);
                }
                
//                 //alert(data.allowed);
//                 var ss;
//                 if (data.allowed == 1)
//                 	ss = ' '+ data.allowed + ' user allowed ';
//                 else	
//                 	ss = ' '+ data.allowed + ' users allowed ';
//                 ss += data.users.length+' defined';
//                 if (data.allowed < +data.users.length)
//                 	ss += '<br>some users will be disabled'; 
//                 ss += '<br>';	
//                 document.getElementById('allowed').innerHTML = ss;

                var dshtml = "", id, dp;
                dataSources = data.dataSources;
                for (i=0; i<dataSources.length; i++) {
                    id = "ds"+ dataSources[i].id;
                    dshtml += "<img id='tgup" + dataSources[i].id + "' src='images/icon_toggle_plus.png' onclick='expandSource(this);'/><img id='tgdn" + dataSources[i].id + "' src='images/icon_toggle_minus.png' onclick='collapseSource(this);' style='display:none'/>";
                    dshtml += '<input type="checkbox" id="'+ id +'" onclick="dataSourceChange(this)">';
                    dshtml += '<label for="'+ id +'"> '+ dataSources[i].name +"</label>";
                    dshtml += " <a onclick=\"bulkSetPermissions('none',"+dataSources[i].id+");\"><fmt:message key='common.access.none'/></a> ";
                	dshtml += "<a onclick=\"bulkSetPermissions('read',"+dataSources[i].id+");\"><fmt:message key='common.access.read'/></a> ";
                	dshtml += "<a onclick=\"bulkSetPermissions('set',"+dataSources[i].id+");\"><fmt:message key='common.access.set'/></a><br></br>";
                    dshtml += '<div style="margin-left:25px; margin-top: -10px" id="dsps'+ dataSources[i].id +'">';
                    if (dataSources[i].points.length > 0) {
                        dshtml +=   '<table cellspacing="0" cellpadding="1">';
                        for (j=0; j<dataSources[i].points.length; j++) {
                            dp = dataSources[i].points[j];
                            dshtml += '<tr>';
                            dshtml +=   '<td class="formLabelRequired">'+ dp.name +'</td>';
                            dshtml +=   '<td>';
                            dshtml +=     '<input type="radio" name="dp'+ dp.id +'" id="dp'+ dp.id +'/0" value="0">';
                            dshtml +=             '<label for="dp'+ dp.id +'/0"><fmt:message key="common.access.none"/></label> ';
                            dshtml +=     '<input type="radio" name="dp'+ dp.id +'" id="dp'+ dp.id +'/1" value="1">';
                            dshtml +=             '<label for="dp'+ dp.id +'/1"><fmt:message key="common.access.read"/></label> ';
                            if (dp.settable) {
                                dshtml +=     '<input type="radio" name="dp'+ dp.id +'" id="dp'+ dp.id +'/2" value="2">';
                                dshtml +=             '<label for="dp'+ dp.id +'/2"><fmt:message key="common.access.set"/></label>';
                            }
                            dshtml +=   '</td>';
                            dshtml += '</tr>';
                        }
                        dshtml +=   '</table>';
                    }else{
                    	dshtml += "<fmt:message key='users.dataSources.noPoints'/>";
                    }
                    dshtml += '</div>';
                }
                $("dataSourceList").innerHTML = dshtml;
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
        
        if (adminUser) {
            // Turn off all data sources and set all data points to 'none'.
            var i, j, dscb, dp,tgup;
            for (i=0; i<dataSources.length; i++) {
                dscb = $("ds"+ dataSources[i].id);
                dscb.checked = false;
                dataSourceChange(dscb);
                
                tgup = $("tgup" + dataSources[i].id);
                collapseSource(tgup); //Collapse the source
                for (j=0; j<dataSources[i].points.length; j++)
                    $set("dp"+ dataSources[i].points[j].id, "0");
            }
            
            // Turn on the data sources to which the user has permission.
            for (i=0; i<user.dataSourcePermissions.length; i++) {
                dscb = $("ds"+ user.dataSourcePermissions[i]);
                dscb.checked = true;
                //Just Don't expand dataSourceChange(dscb);
            }
            
            //TODO Collapse ALL HERE
            
            // Update the data point permissions.
            for (i=0; i<user.dataPointPermissions.length; i++)
                $set("dp"+ user.dataPointPermissions[i].dataPointId, user.dataPointPermissions[i].permission);
        }
        
        setUserMessage();
        updateUserImg();
    }
    
    function saveUser() {
        setUserMessage();
        if (adminUser) {
            // Create the list of allowed data sources and data point permissions.
            var i, j;
            var dsPermis = new Array();
            var dpPermis = new Array();
            var dpval;
            for (i=0; i<dataSources.length; i++) {
                if ($("ds"+ dataSources[i].id).checked)
                    dsPermis[dsPermis.length] = dataSources[i].id;
                else {
                    for (j=0; j<dataSources[i].points.length; j++) {
                        dpval = $get("dp"+ dataSources[i].points[j].id);
                        if (dpval == "1" || dpval == "2")
                            dpPermis[dpPermis.length] = {dataPointId: dataSources[i].points[j].id, permission: dpval};
                    }
                }
            }
            
            UsersDwr.saveUserAdmin(editingUserId, $get("username"), $get("password"), $get("email"), $get("phone"), 
                    $get("administrator"), $get("disabled"), $get("receiveAlarmEmails"), $get("receiveOwnAuditEvents"),
                    $get("timezone"), dsPermis, dpPermis, saveUserCB);
        }
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
            }
            else
                setUserMessage("<fmt:message key="users.saved"/>");
            UsersDwr.getUser(editingUserId, updateUser)
        }
    }
    
//     function saveUserCB(response) {
//         if (response.hasMessages)
//             showDwrMessages(response.messages, "genericMessages");
//         else {
//         	if (!adminUser)
// 	            setUserMessage("<fmt:message key="users.dataSaved"/>");
// 	        else {
<%-- 	            if (editingUserId == <c:out value="<%= Common.NEW_ID %>"/>) { --%>
// 	                stopImageFader($("u"+ editingUserId +"Img"));
// 	                editingUserId = response.data.userId;
// 	                appendUser(editingUserId);
// 	                startImageFader($("u"+ editingUserId +"Img"));
// 	                setUserMessage("<fmt:message key="users.added"/>");
// 	            }
// 	            else
// 	                setUserMessage("<fmt:message key="users.saved"/>");
// 	            UsersDwr.getUser(editingUserId, updateUser)
// 	        }
//         	//shouldn't have to call this twice, but I do
//             FadeOut($("userDetails"));
//             stopImageFader($("u"+ editingUserId +"Img"));
//             FadeOut($("userDetails"));
//         }
//     }
    
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
//     	if (user.defaultUser)
//     		$("u"+ user.id +"Username").innerHTML = user.username+" (default)";
//     	else
//     		if (user.disabled)
//     			$("u"+ user.id +"Username").innerHTML = user.username+'  disabled';
//     		else
//         		$("u"+ user.id +"Username").innerHTML = user.username;
        $("u"+ user.id +"Username").innerHTML = user.username;
        setUserImg(user.admin, user.disabled, $("u"+ user.id +"Img"));
    }
    
    function updateUserImg() {
        var admin = $get("administrator");
        if (adminUser) {
            if (admin)
                hide("dataSources");
            else
                show("dataSources");
        }
        setUserImg(admin, $get("disabled"), $("userImg"));
    }
    
    function dataSourceChange(dscb) {
    	if(dscb.checked){
    		//Close the Points
    		var dsId = dscb.id.substring(2);
    		var tgup = dojo.byId("tgup" + dsId);
    		collapseSource(tgup)
    	}
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
                    stopImageFader("u"+ userId +"Img");
                    $("usersTable").removeChild($("u"+ userId));
                    hide("userDetails");
                    editingUserId = null;
                }
            });
        }
    }
    
    /**
     * tgup = "xxxxID" where ID is the id of the datasource to expand
    */
    function expandSource(tgdn){
    	//Get the DS ID
    	var dsId = tgdn.id.substring(4);
    	
    	//Check to see if the checkbox is selected, if it is we won't open this
    	var dscb = dojo.byId("ds" + dsId);
    	if(!dscb.checked){
	    	//Hide the expand image
	    	hide("tgup"+dsId);
	    	//Show the collapse image
	    	show("tgdn"+dsId);
	    	display("dsps"+ dsId, true);
    	}
    }
    
    /**
     * tgup = "xxxxID" where ID is the id of the datasource to collapse
    */
    function collapseSource(tgup){
    	var dsId = tgup.id.substring(4);
    	//Hide the collapse image
    	hide("tgdn"+dsId);
    	//Show the expand image
    	show("tgup"+dsId);
    	//Show the Data
    	display("dsps"+ dsId, false);
    }
    
    /**
     * Bulk Set All Permissions
    **/
    function bulkSetPermissions(type,dsId){
    	
    	var permission = "0";
    	if(type === 'none')
    		permission = "0";
    	else if (type === 'read')
    		permission = "1";
    	else if (type == 'set')
    		permission = "2";
    	
    	if(dsId){
            var i, j;
            for (i=0; i<dataSources.length; i++) {
	            if(dataSources[i].id === dsId){
	                for (j=0; j<dataSources[i].points.length; j++){
	                    	$set("dp"+ dataSources[i].points[j].id, permission);
	                    
	                }
	                break; //Drop out after updating the DS We want
            	}
            }
    	}else{
	    	setAllPermissions(permission);
    	}
    }

    /**
     * 0 for none
     * 1 for read
     * 2 for set
    **/
    function setAllPermissions(permission){
        var i, j, dscb;
        for (i=0; i<dataSources.length; i++) {
            dscb = $("ds"+ dataSources[i].id);
            dscb.checked = false;
            dataSourceChange(dscb);
            for (j=0; j<dataSources[i].points.length; j++)
                $set("dp"+ dataSources[i].points[j].id, permission);
        }
     
    }
    
    
  </script>
  
  <table>
    <tr>
      <td valign="top" id="userList" style="display:none;">
        <div class="borderDiv">
          <table width="100%">
            <tr>
              <td>
                <span class="smallTitle"><fmt:message key="users.title"/></span>
                <tag:help id="userAdministration"/>
              </td>
              <td align="right"><tag:img png="user_add" onclick="showUser(${applicationScope['constants.Common.NEW_ID']})"
                      title="users.add" id="u${applicationScope['constants.Common.NEW_ID']}Img"/></td>
            </tr>
          </table>
          <table id="usersTable">
            <tbody id="u_TEMPLATE_" onclick="showUser(getMangoId(this))" class="ptr" style="display:none;"><tr>
              <td><tag:img id="u_TEMPLATE_Img" png="user_green" title="users.user"/></td>
              <td class="link" id="u_TEMPLATE_Username"></td>
            </tr></tbody>
          </table>
        </div>
      </td>
      
      <td valign="top" style="display:none;" id="userDetails">
        <div class="borderDiv">
          <table width="100%">
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
            <tbody id="dataSources" style="display:none;">
              <tr><td class="horzSeparator" colspan="2"></td></tr>
              <tr>
              	<td class="formLabelRequired"><fmt:message key="users.dataSources.bulkSet"/></td>
                <td class="formField">
                	<a onclick="bulkSetPermissions('none');"><fmt:message key="common.access.none"/></a>
                	<a onclick="bulkSetPermissions('read');"><fmt:message key="common.access.read"/></a>
                	<a onclick="bulkSetPermissions('set');"><fmt:message key="common.access.set"/></a>
                </td>
              </tr>
              <tr id="dataSources">
                <td class="formLabelRequired"><fmt:message key="users.dataSources"/></td>
                <td class="formField" id="dataSourceList"></td>
              </tr>
            </tbody>
          </table>
        </div>
      </td>
    </tr>
  </table>
</tag:page>