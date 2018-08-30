<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@page import="com.serotonin.m2m2.db.DatabaseProxy"%>
<%@page import="com.serotonin.m2m2.rt.maint.DataPurge"%>
<%@page import="com.serotonin.m2m2.module.SystemSettingsDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition" %>
<%@page import="com.serotonin.m2m2.module.ModuleRegistry"%>
<%@page import="com.serotonin.m2m2.db.dao.SystemSettingsDao"%>
<%@page import="com.serotonin.m2m2.Common"%>
<%@page import="com.serotonin.m2m2.rt.event.AlarmLevels"%>
<%@page import="com.serotonin.m2m2.rt.event.type.EventType"%>
<%@page import="com.serotonin.m2m2.email.MangoEmailContent"%>
<%@page import="java.util.TimeZone"%>
<%@page import="com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfig" %>
<%@page import="com.serotonin.m2m2.UpgradeVersionState" %>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<tag:page showHeader="${param.showHeader}" showToolbar="${param.showToolbar}" dwr="SystemSettingsDwr" onload="init">
  <script type="text/javascript">
    var systemEventAlarmLevels = new Array();
    var auditEventAlarmLevels = new Array();
    
    function init() {
        SystemSettingsDwr.getSettings(function(settings) {
            $set("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_HOST %>"/>", settings.<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_HOST %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_PORT %>"/>", settings.<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_PORT %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.EMAIL_FROM_ADDRESS %>"/>", settings.<c:out value="<%= SystemSettingsDao.EMAIL_FROM_ADDRESS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.EMAIL_FROM_NAME %>"/>", settings.<c:out value="<%= SystemSettingsDao.EMAIL_FROM_NAME %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.EMAIL_AUTHORIZATION %>"/>", settings.<c:out value="<%= SystemSettingsDao.EMAIL_AUTHORIZATION %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_USERNAME %>"/>", settings.<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_USERNAME %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_PASSWORD %>"/>", settings.<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_PASSWORD %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.EMAIL_TLS %>"/>", settings.<c:out value="<%= SystemSettingsDao.EMAIL_TLS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.EMAIL_CONTENT_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.EMAIL_CONTENT_TYPE %>"/>);
            smtpAuthChange();
            
            var alarmFunctions = [
                function(et) { return et.description; },
                function(et) {
                    var etid = et.type +"-"+ et.subtype;
                    var content = "<select id='alarmLevel"+ etid +"' ";
                    content += "onchange='updateAlarmLevel(\""+ et.type +"\", \""+ et.subtype +"\", this.value)'>";
                    content += "<option value='<c:out value="<%= AlarmLevels.NONE %>"/>'><fmt:message key="<%= AlarmLevels.NONE_DESCRIPTION %>"/></option>";
                    content += "<option value='<c:out value="<%= AlarmLevels.INFORMATION %>"/>'><fmt:message key="<%= AlarmLevels.INFORMATION_DESCRIPTION %>"/></option>";
                    content += "<option value='<c:out value="<%= AlarmLevels.IMPORTANT %>"/>'><fmt:message key="<%= AlarmLevels.IMPORTANT_DESCRIPTION %>"/></option>";
                    content += "<option value='<c:out value="<%= AlarmLevels.WARNING %>"/>'><fmt:message key="<%= AlarmLevels.WARNING_DESCRIPTION %>"/></option>";
                    content += "<option value='<c:out value="<%= AlarmLevels.URGENT %>"/>'><fmt:message key="<%= AlarmLevels.URGENT_DESCRIPTION %>"/></option>";
                    content += "<option value='<c:out value="<%= AlarmLevels.CRITICAL %>"/>'><fmt:message key="<%= AlarmLevels.CRITICAL_DESCRIPTION %>"/></option>";
                    content += "<option value='<c:out value="<%= AlarmLevels.LIFE_SAFETY %>"/>'><fmt:message key="<%= AlarmLevels.LIFE_SAFETY_DESCRIPTION %>"/></option>";
                    content += "<option value='<c:out value="<%= AlarmLevels.DO_NOT_LOG %>"/>'><fmt:message key="<%= AlarmLevels.DO_NOT_LOG_DESCRIPTION %>"/></option>";
                    content += "<option value='<c:out value="<%= AlarmLevels.IGNORE %>"/>'><fmt:message key="<%= AlarmLevels.IGNORE_DESCRIPTION %>"/></option>";
                    
                    content += "</select> ";
                    content += "<img id='alarmLevelImg"+ etid +"' src='images/flag_grey.png' style='display:none'>";
                    return content;
                }
            ];
            var alarmOptions = {
                cellCreator: function(options) {
                    var td = document.createElement("td");
                    td.className = (options.cellNum == 0 ? "formLabelRequired" : "formField");
                    return td;
                }
            };
            setEventTypeData("systemEventAlarmLevelsList", settings.systemEventTypes, alarmFunctions, alarmOptions,
                    systemEventAlarmLevels);
            setEventTypeData("auditEventAlarmLevelsList", settings.auditEventTypes, alarmFunctions, alarmOptions,
                    auditEventAlarmLevels);
            
            $set("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_USE_PROXY %>"/>", settings.<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_USE_PROXY %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER %>"/>", settings.<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_PORT %>"/>", settings.<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_PORT %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME %>"/>", settings.<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD %>"/>", settings.<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD %>"/>);
            httpUseProxyChange();
            
            $set("<c:out value="<%= SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.POINT_DATA_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.POINT_DATA_PURGE_PERIODS %>"/>);
            
            $set("<c:out value="<%= SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIODS %>"/>);
            
            $set("<c:out value="<%= DataPurge.ENABLE_POINT_DATA_PURGE %>"/>", settings.<c:out value="<%= DataPurge.ENABLE_POINT_DATA_PURGE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.POINT_DATA_PURGE_COUNT %>"/>", settings.<c:out value="<%= SystemSettingsDao.POINT_DATA_PURGE_COUNT %>"/>);
                       
            $set("<c:out value="<%= SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIODS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIODS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIODS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.AUDIT_EVENT_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.AUDIT_EVENT_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.AUDIT_EVENT_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.AUDIT_EVENT_PURGE_PERIODS %>"/>);

            $set("<c:out value="<%= SystemSettingsDao.NONE_ALARM_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.NONE_ALARM_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.NONE_ALARM_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.NONE_ALARM_PURGE_PERIODS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIODS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIODS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.WARNING_ALARM_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.WARNING_ALARM_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.WARNING_ALARM_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.WARNING_ALARM_PURGE_PERIODS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.URGENT_ALARM_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.URGENT_ALARM_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.URGENT_ALARM_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.URGENT_ALARM_PURGE_PERIODS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIODS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIODS %>"/>);            
            
            $set("<c:out value="<%= SystemSettingsDao.EVENT_PURGE_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.EVENT_PURGE_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.EVENT_PURGE_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.EVENT_PURGE_PERIODS %>"/>);

            $set("<c:out value="<%= SystemSettingsDao.UI_PERFORMANCE %>"/>", settings.<c:out value="<%= SystemSettingsDao.UI_PERFORMANCE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.FUTURE_DATE_LIMIT_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.FUTURE_DATE_LIMIT_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.FUTURE_DATE_LIMIT_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.FUTURE_DATE_LIMIT_PERIODS %>"/>);
            
            $set("<c:out value="<%= SystemSettingsDao.INSTANCE_DESCRIPTION %>"/>", settings.<c:out value="<%= SystemSettingsDao.INSTANCE_DESCRIPTION %>"/>);
            
            <c:if test="${!empty availableLanguages}">
              var sel = $("<c:out value="<%= SystemSettingsDao.LANGUAGE %>"/>");
              <c:forEach items="${availableLanguages}" var="lang">
                sel.options[sel.options.length] = new Option("${lang.value}", "${lang.key}");
              </c:forEach>
              $set(sel, settings.<c:out value="<%= SystemSettingsDao.LANGUAGE %>"/>);
            </c:if>
            
            $set("<c:out value="<%= SystemSettingsDao.CHART_BACKGROUND_COLOUR %>"/>", settings.<c:out value="<%= SystemSettingsDao.CHART_BACKGROUND_COLOUR %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.PLOT_BACKGROUND_COLOUR %>"/>", settings.<c:out value="<%= SystemSettingsDao.PLOT_BACKGROUND_COLOUR %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.PLOT_GRIDLINE_COLOUR %>"/>", settings.<c:out value="<%= SystemSettingsDao.PLOT_GRIDLINE_COLOUR %>"/>);

            $set("<c:out value="<%= SystemSettingsDao.BACKUP_FILE_LOCATION %>"/>", settings.<c:out value="<%= SystemSettingsDao.BACKUP_FILE_LOCATION %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.BACKUP_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.BACKUP_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.BACKUP_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.BACKUP_PERIODS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.BACKUP_LAST_RUN_SUCCESS %>"/>", settings.<c:out value="<%= SystemSettingsDao.BACKUP_LAST_RUN_SUCCESS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.BACKUP_HOUR %>"/>", settings.<c:out value="<%= SystemSettingsDao.BACKUP_HOUR %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.BACKUP_MINUTE %>"/>", settings.<c:out value="<%= SystemSettingsDao.BACKUP_MINUTE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.BACKUP_FILE_COUNT %>"/>", settings.<c:out value="<%= SystemSettingsDao.BACKUP_FILE_COUNT %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.BACKUP_ENABLED %>"/>", settings.<c:out value="<%= SystemSettingsDao.BACKUP_ENABLED %>"/>);                

            $set("<c:out value="<%= SystemSettingsDao.ALLOW_ANONYMOUS_CHART_VIEW %>"/>", settings.<c:out value="<%= SystemSettingsDao.ALLOW_ANONYMOUS_CHART_VIEW %>"/>);
            $set("<c:out value="<%=SystemSettingsDao.JFREE_CHART_FONT%>"/>", settings.<c:out value="<%= SystemSettingsDao.JFREE_CHART_FONT %>"/>);

            $set("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION %>"/>", settings.<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_PERIOD_TYPE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_PERIODS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_LAST_RUN_SUCCESS %>"/>", settings.<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_LAST_RUN_SUCCESS %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_HOUR %>"/>", settings.<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_HOUR %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_MINUTE %>"/>", settings.<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_MINUTE %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_FILE_COUNT %>"/>", settings.<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_FILE_COUNT %>"/>);
            $set("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_ENABLED %>"/>", settings.<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_ENABLED %>"/>);                

            //Thread Pools
           $set("<c:out value="<%= SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE %>"/>", settings.<c:out value="<%= SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE %>"/>);                
           $set("<c:out value="<%= SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE %>"/>", settings.<c:out value="<%= SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE %>"/>);                
           $set("<c:out value="<%= SystemSettingsDao.MED_PRI_CORE_POOL_SIZE %>"/>", settings.<c:out value="<%= SystemSettingsDao.MED_PRI_CORE_POOL_SIZE %>"/>);                
           $set("<c:out value="<%= SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE %>"/>", settings.<c:out value="<%= SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE %>"/>);                

           //Site Analytics
           $set("<c:out value="<%= SystemSettingsDao.SITE_ANALYTICS_HEAD %>"/>", settings.<c:out value="<%= SystemSettingsDao.SITE_ANALYTICS_HEAD %>"/>);                
           $set("<c:out value="<%= SystemSettingsDao.SITE_ANALYTICS_BODY %>"/>", settings.<c:out value="<%= SystemSettingsDao.SITE_ANALYTICS_BODY %>"/>);                
           
           //Upgrade version state
           $set("<c:out value="<%= SystemSettingsDao.UPGRADE_VERSION_STATE %>"/>", settings.<c:out value="<%= SystemSettingsDao.UPGRADE_VERSION_STATE %>"/>);
           
           //Point Hierarchy
           $set("<c:out value="<%= SystemSettingsDao.EXPORT_HIERARCHY_PATH %>"/>", settings.<c:out value="<%= SystemSettingsDao.EXPORT_HIERARCHY_PATH %>"/>);
           $set("<c:out value="<%= SystemSettingsDao.HIERARCHY_PATH_SEPARATOR %>"/>", settings.<c:out value="<%= SystemSettingsDao.HIERARCHY_PATH_SEPARATOR %>"/>);
           
           //Http Session timeouts
           $set("<c:out value="<%= SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE %>"/>", settings.<c:out value="<%= SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE %>"/>);
           $set("<c:out value="<%= SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS %>"/>", settings.<c:out value="<%= SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS %>"/>);

           
           displayVirtualSerialPorts(settings.virtualSerialPorts)
            
            //1
            // Permissions
            $set("<c:out value="<%= SystemSettingsDao.PERMISSION_DATASOURCE %>"/>", settings.<c:out value="<%= SystemSettingsDao.PERMISSION_DATASOURCE %>"/>);
            dwr.util.addRows("modulePermissions", settings.modulePermissions, [
                    function(p) { return p.label },
                    function(p) {
                        var s = '<input id="'+ escapeDQuotes(p.name) +'" type="text" class="formLong modulePermission"/>';
                        s += ' <img id="permView-'+ escapeDQuotes(p.name) +'" class="ptr permissionBtn" src="/images/bullet_down.png">';                        
                        return s;
                      }
                ],
                {
                    cellCreator: function(options) {
                        var td = document.createElement("td");
                        if (options.cellNum == 0)
                            td.className = "formLabel";
                        else if (options.cellNum == 1)
                            td.className = "formField";
                        return td;
                    }
                }
            );
            // Set the values in the text boxes
            for (var i=0; i<settings.modulePermissions.length; i++)
                $set(settings.modulePermissions[i].name, settings.modulePermissions[i].value);
            // Add onclick to the bullets
            require(["dojo/query"], function(query) {
                query(".permissionBtn").forEach(function(e) {
                    // Curious combination of AMD and non.
                    dojo.connect(e, "onclick", viewPermissions);
                })
            });
        });
        
        <c:if test="${!empty param.def}">
          // There is a section to open by default. Close all open sections.
          require(["dojo/query"], function(query) {
              query("div.labelled-section:not(.closed)").forEach(function(node) { mango.closeLabelledSection(node) });
          });
          // Open the default section.
          var label = $("labelled-section-${param.def}");
          mango.toggleLabelledSection(label);
          // After a short timeout, scroll the section into view.
          setTimeout(function() {
              require(["dojo/window"], function(win) {
                  win.scrollIntoView(label.parentNode);
              });
          }, 500);
        </c:if>
    }
    
    function setEventTypeData(listId, eventTypes, alarmFunctions, alarmOptions, alarmLevelsList) {
        dwr.util.addRows(listId, eventTypes, alarmFunctions, alarmOptions);
        
        var eventType, etid;
        for (var i=0; i<eventTypes.length; i++) {
            eventType = eventTypes[i];
            etid = eventType.type +"-"+ eventType.subtype;
            $set("alarmLevel"+ etid, eventType.alarmLevel);
            setAlarmLevelImg(eventType.alarmLevel, "alarmLevelImg"+ etid);
            alarmLevelsList[alarmLevelsList.length] = { string: eventType.subtype, int: eventType.alarmLevel };
        }
    }
    
    function clearDbSize() {
    	$set("databaseSize", "");
        $set("filedataSize", "-");
        $set("totalSize", "-");
        $set("historyCount", "-");
        $set("topPoints", "-");
        $set("eventCount", "-");
        show("refreshImg");
    }
    
    function dbSizeUpdate() {
    	clearDbSize();
        $set("databaseSize", "<fmt:message key="systemSettings.retrieving"/>");
        hide("refreshImg");
        SystemSettingsDwr.getDatabaseSize(function(data) {
            $set("databaseSize", data.databaseSize);
            if(data.noSqlDatabaseSize != null){
                $set("noSqlDatabaseSize", data.noSqlDatabaseSize);
                show("noSqlDatabaseSizeRow");
            }
            
            $set("filedataSize", data.filedataSize +" ("+ data.filedataCount +" <fmt:message key="systemSettings.files"/>)");
            $set("totalSize", data.totalSize);
            $set("historyCount", data.historyCount);
            
            var cnt = "";
            for (var i=0; i<data.topPoints.length; i++) {
                cnt += "<a href='data_point_details.shtm?dpid="+ data.topPoints[i].pointId +"'>"+
                        data.topPoints[i].pointName +"</a> "+ data.topPoints[i].count +"<br/>";
                if (i == 3)
                    break;
            }
            $set("topPoints", cnt);
            $set("eventCount", data.eventCount);
        });
    }
    
    function saveSystemPermissions() {
        hideContextualMessages("systemPermissionsTab");
        // Gather the module permissions
        var modulePermissions = [];
        require(["dojo/query"], function(query) {
            query(".modulePermission").forEach(function(e) {
                modulePermissions.push({ key: e.id, value: e.value });
            });
        });

        SystemSettingsDwr.saveSystemPermissions(
                $get("<c:out value="<%= SystemSettingsDao.PERMISSION_DATASOURCE %>"/>"),
                modulePermissions,
                function(response) {
                    setDisabled("systemPermissionsBtn", false);
                    if(response.hasMessages)
                        showDwrMessages(response.messages);
                    else
                    	setUserMessage("systemPermissionsMessage", "<fmt:message key="systemSettings.systemPermissionsSaved"/>");
                });
        setUserMessage("systemPermissionsMessage");
        setDisabled("systemPermissionsBtn", true);
    }
    
    function saveHierarchySettings() {
    	setUserMessage("hierarchyMessage");
    	setDisabled("saveHierarchySettingsBtn", true);
    	hideContextualMessages("hierarchySettingsTab");
    	SystemSettingsDwr.saveHierarchySettings(
    		$get("<c:out value="<%= SystemSettingsDao.EXPORT_HIERARCHY_PATH %>"/>"),
    		$get("<c:out value="<%= SystemSettingsDao.HIERARCHY_PATH_SEPARATOR %>"/>"),
    	function(response) {
    			setDisabled("saveHierarchySettingsBtn", false);
            	if(response.hasMessages){
            		showDwrMessages(response.messages);
            	}else{
            		setUserMessage("hierarchyMessage", "<fmt:message key="systemSettings.hierarchySettingsSaved"/>");
            	}
    	});
    }
    
    function saveEmailSettings() {
        setUserMessage("emailMessage");
        setDisabled("saveEmailSettingsBtn", true);
        hideContextualMessages("emailSettingsTab")
        SystemSettingsDwr.saveEmailSettings(
            $get("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_HOST %>"/>"),
            $get("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_PORT %>"/>"),
            $get("<c:out value="<%= SystemSettingsDao.EMAIL_FROM_ADDRESS %>"/>"),
            $get("<c:out value="<%= SystemSettingsDao.EMAIL_FROM_NAME %>"/>"),
            $get("<c:out value="<%= SystemSettingsDao.EMAIL_AUTHORIZATION %>"/>"),
            $get("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_USERNAME %>"/>"),
            $get("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_PASSWORD %>"/>"),
            $get("<c:out value="<%= SystemSettingsDao.EMAIL_TLS %>"/>"),
            $get("<c:out value="<%= SystemSettingsDao.EMAIL_CONTENT_TYPE %>"/>"),
            function(response) {
            	setDisabled("saveEmailSettingsBtn", false);
            	if(response.hasMessages){
            		showDwrMessages(response.messages);
            	}else{
            		setUserMessage("emailMessage", "<fmt:message key="systemSettings.emailSettingsSaved"/>");
            	}
            });

    }
    
    function sendTestEmail() {
        SystemSettingsDwr.sendTestEmail(
                $get("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_HOST %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_PORT %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.EMAIL_FROM_ADDRESS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.EMAIL_FROM_NAME %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.EMAIL_AUTHORIZATION %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_USERNAME %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_PASSWORD %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.EMAIL_TLS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.EMAIL_CONTENT_TYPE %>"/>"),
                function(result) {
                    stopImageFader("sendTestEmailImg");
                    if (result.exception)
                        setUserMessage("emailMessage", result.exception);
                    else
                        setUserMessage("emailMessage", result.message);
                });
        setUserMessage("emailMessage");
        startImageFader("sendTestEmailImg");
    }
    
    function saveThreadPoolSettings() {
        SystemSettingsDwr.saveThreadPoolSettings(
            $get("<c:out value="<%= SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE %>"/>"),
            $get("<c:out value="<%= SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE %>"/>"),
            $get("<c:out value="<%= SystemSettingsDao.MED_PRI_CORE_POOL_SIZE %>"/>"),
            $get("<c:out value="<%= SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE %>"/>"),
            function(response) {
                setDisabled("saveThreadPoolSettingsBtn", false);
                if (response.hasMessages)
                    showDwrMessages(response.messages);
                else
                	setUserMessage("threadPoolMessage", "<fmt:message key="systemSettings.threadPools.settingsSaved"/>");
            });
        setUserMessage("threadPoolMessage");
        setDisabled("saveThreadPoolSettingsBtn", true);
    }
    
    function saveSiteAnalytics() {
        setUserMessage("siteAnalyticsMessages");
        setDisabled("siteAnalyticsBtn", true);
        hideContextualMessages("siteAnalyticsTab")
        SystemSettingsDwr.saveSiteAnalytics(
            $get("<c:out value="<%= SystemSettingsDao.SITE_ANALYTICS_HEAD %>"/>"),
            $get("<c:out value="<%= SystemSettingsDao.SITE_ANALYTICS_BODY %>"/>"),
            function(response) {
            	setDisabled("siteAnalyticsBtn", false);
            	if(response.hasMessages){
            		showDwrMessages(response.messages);
            	}else{
            		setUserMessage("siteAnalyticsMessages", "<fmt:message key="systemSettings.siteAnalytics.saved"/>");
            	}
            });

    }
    
    function updateAlarmLevel(eventType, eventSubtype, alarmLevel) {
        setAlarmLevelImg(alarmLevel, "alarmLevelImg"+ eventType +"-"+ eventSubtype);
        var list;
        if (eventType == "<c:out value="<%= EventType.EventTypeNames.SYSTEM %>"/>")
            list = systemEventAlarmLevels;
        else
            list = auditEventAlarmLevels;
        getElement(list, eventSubtype, "string")["int"] = alarmLevel;
    }
    
    function saveSystemEventAlarmLevels() {
        SystemSettingsDwr.saveSystemEventAlarmLevels(systemEventAlarmLevels, function() {
            setDisabled("systemEventAlarmLevelsBtn", false);
            setUserMessage("systemEventAlarmLevelsMessage", "<fmt:message key="systemSettings.systemAlarmLevelsSaved"/>");
        });
        setUserMessage("systemEventAlarmLevelsMessage");
        setDisabled("systemEventAlarmLevelsBtn", true);
    }
    
    function saveAuditEventAlarmLevels() {
        SystemSettingsDwr.saveAuditEventAlarmLevels(auditEventAlarmLevels, function() {
                setDisabled("auditEventAlarmLevelsBtn", false);
                setUserMessage("auditEventAlarmLevelsMessage", "<fmt:message key="systemSettings.auditAlarmLevelsSaved"/>");
        });
        setUserMessage("auditEventAlarmLevelsMessage");
        setDisabled("auditEventAlarmLevelsBtn", true);
    }
    
    function smtpAuthChange() {
        var auth = $("<c:out value="<%= SystemSettingsDao.EMAIL_AUTHORIZATION %>"/>").checked;
        setDisabled($("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_USERNAME %>"/>"), !auth);
        setDisabled($("<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_PASSWORD %>"/>"), !auth);
    }
    
    function saveHttpSettings() {
        SystemSettingsDwr.saveHttpSettings(
                $get("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_USE_PROXY %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_PORT %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD %>"/>"),
                function() {
                    setDisabled("saveHttpSettingsBtn", false);
                    setUserMessage("httpMessage", "<fmt:message key="systemSettings.httpSaved"/>");
                });
        setUserMessage("httpMessage");
        setDisabled("saveHttpSettingsBtn", true);
    }
    
    function saveHttpServerSettings() {
        SystemSettingsDwr.saveHttpServerSettings(
                $get("<c:out value="<%= SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE %>"/>"),
                function() {
                    setDisabled("saveHttpServerSettingsBtn", false);
                    setUserMessage("httpServerMessage", "<fmt:message key="systemSettings.httpSaved"/>");
                });
        setUserMessage("httpServerMessage");
        setDisabled("saveHttpServerSettingsBtn", true);
    }
    
    function httpUseProxyChange() {
        var proxy = $("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_USE_PROXY %>"/>").checked;
        setDisabled($("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER %>"/>"), !proxy);
        setDisabled($("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_PORT %>"/>"), !proxy);
        setDisabled($("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME %>"/>"), !proxy);
        setDisabled($("<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD %>"/>"), !proxy);
    }
    
    function saveMiscSettings() {
        SystemSettingsDwr.saveMiscSettings(
                $get("<c:out value="<%= SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.POINT_DATA_PURGE_PERIODS %>"/>"),
                
                $get("<c:out value="<%= SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIODS %>"/>"),
                
                $get("<c:out value="<%= DataPurge.ENABLE_POINT_DATA_PURGE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.POINT_DATA_PURGE_COUNT %>"/>"),
                
                $get("<c:out value="<%= SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.AUDIT_EVENT_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.AUDIT_EVENT_PURGE_PERIODS %>"/>"),
                
                $get("<c:out value="<%= SystemSettingsDao.NONE_ALARM_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.NONE_ALARM_PURGE_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.WARNING_ALARM_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.WARNING_ALARM_PURGE_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.URGENT_ALARM_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.URGENT_ALARM_PURGE_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIODS %>"/>"),

                $get("<c:out value="<%= SystemSettingsDao.EVENT_PURGE_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.EVENT_PURGE_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.UI_PERFORMANCE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.FUTURE_DATE_LIMIT_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.FUTURE_DATE_LIMIT_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS %>"/>"),
                function() {
                    setDisabled("saveMiscSettingsBtn", false);
                    setUserMessage("miscMessage", "<fmt:message key="systemSettings.miscSaved"/>");
                });
        setUserMessage("miscMessage");
        setDisabled("saveMiscSettingsBtn", true);
    }
    
    function saveColourSettings() {
        setUserMessage("colourMessage");
        hideContextualMessages("colourSettingsTab")
        setDisabled("saveColourSettingsBtn", true);
        SystemSettingsDwr.saveColourSettings(
                $get("<c:out value="<%= SystemSettingsDao.CHART_BACKGROUND_COLOUR %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.PLOT_BACKGROUND_COLOUR %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.PLOT_GRIDLINE_COLOUR %>"/>"),
                function(response) {
                    setDisabled("saveColourSettingsBtn", false);
                    if (response.hasMessages)
                        showDwrMessages(response.messages);
                    else
                        setUserMessage("colourMessage", "<fmt:message key="systemSettings.coloursSaved"/>");
                }
        );
    }
    
    function setUserMessage(type, msg) {
        if (msg)
            $set(type, msg);
        else
            $set(type, "");
    }
    
    function saveInfoSettings() {
        SystemSettingsDwr.saveInfoSettings(
                $get("<c:out value="<%= SystemSettingsDao.INSTANCE_DESCRIPTION %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.UPGRADE_VERSION_STATE %>"/>"),
                function() {
                    setDisabled("infoBtn", false);
                    setUserMessage("infoMessage", "<fmt:message key="systemSettings.infoSaved"/>");
                });
        setUserMessage("infoMessage");
        setDisabled("infoBtn", true);
    }
    

   
    
    function saveLangSettings() {
        SystemSettingsDwr.saveLanguageSettings($get("<c:out value="<%= SystemSettingsDao.LANGUAGE %>"/>"), function() {
            setDisabled("saveLangSettingsBtn", false);
            setUserMessage("langMessage", "<fmt:message key="systemSettings.langSaved"/>");
        });
        setUserMessage("langMessage");
        setDisabled("saveLangSettingsBtn", true);
    }
    
    function checkPurgeNow() {
        if (confirm("<fmt:message key='systemSettings.purgeDataWithPurgeSettingsConfirm'/>")) {
            SystemSettingsDwr.purgeNow(function(msg) {
                stopImageFader("purgeNowImg");
                clearDbSize();
            });
            startImageFader("purgeNowImg");
        }
    }
    
    function checkPurgeAllPointValuesNow() {
        if (confirm("<fmt:message key="systemSettings.purgeDataConfirm"/>")) {
            setUserMessage("miscMessage", "<fmt:message key="systemSettings.purgeDataInProgress"/>");
            SystemSettingsDwr.purgeAllData(function(msg) {
                setUserMessage("miscMessage", msg);
                clearDbSize();
            });
        }
    }
    
    function checkPurgeAllEventsNow(){
        if (confirm("<fmt:message key='systemSettings.purgeAllEventsConfirm'/>")) {
            setUserMessage("miscMessage", "<fmt:message key='systemSettings.purgeAllEventsInProgress'/>");
            SystemSettingsDwr.purgeAllEvents(function(msg) {
                setUserMessage("miscMessage", msg);
                clearDbSize();
            });
        }
    }
    
    /**
     * Save the Backup Settings
     */
    function saveBackupSettings() {
        hideContextualMessages("backupSettingsTab"); //Clear out any existing msgs
        SystemSettingsDwr.saveBackupSettings(
                $get("<c:out value="<%= SystemSettingsDao.BACKUP_FILE_LOCATION %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.BACKUP_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.BACKUP_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.BACKUP_HOUR %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.BACKUP_MINUTE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.BACKUP_FILE_COUNT %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.BACKUP_ENABLED %>"/>"),
               function(response) {
                setDisabled("saveBackupSettingsBtn", false);
                if (response.hasMessages)
                    showDwrMessages(response.messages);
                else
                    setUserMessage("backupSettingsMessage", "<fmt:message key="systemSettings.systemBackupSettingsSaved"/>");
            });
        setUserMessage("backupSettingsMessage");
        setDisabled("saveBackupSettingsBtn", true);
    }
    
    function backupNow(){
        SystemSettingsDwr.queueBackup()
        setUserMessage("backupSettingsMessage", "<fmt:message key="systemSettings.backupQueued"/>");
    }
    
    /**
     * Save the Chart Settings
     */
    function saveChartSettings() {
        hideContextualMessages("chartSettingsTab"); //Clear out any existing msgs
        SystemSettingsDwr.saveChartSettings(
                $get("<c:out value="<%= SystemSettingsDao.ALLOW_ANONYMOUS_CHART_VIEW %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.JFREE_CHART_FONT %>"/>"),
            function(response) {
                setDisabled("saveChartSettingsBtn", false);
                if (response.hasMessages)
                    showDwrMessages(response.messages);
                else
                    setUserMessage("chartSettingsMessage", "<fmt:message key='systemSettings.systemChartSettingsSaved'/>");
            });
        setUserMessage("chartSettingsMessage");
        setDisabled("saveChartSettingsBtn", true);
    }
    
    function setChartFont() {
    	$set("<c:out value="<%=SystemSettingsDao.JFREE_CHART_FONT%>"/>", $get("font-selector"));
    }
    
    /**
     * Save the Backup Settings
     */
    function saveDatabaseBackupSettings() {
        hideContextualMessages("databaseBackupSettingsTab"); //Clear out any existing msgs
        SystemSettingsDwr.saveDatabaseBackupSettings(
                $get("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_PERIOD_TYPE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_PERIODS %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_HOUR %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_MINUTE %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_FILE_COUNT %>"/>"),
                $get("<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_ENABLED %>"/>"),
               function(response) {
                setDisabled("saveDatabaseBackupSettingsBtn", false);
                if (response.hasMessages)
                    showDwrMessages(response.messages);
                else
                    setUserMessage("databaseBackupSettingsMessage", "<fmt:message key='systemSettings.databaseBackupSettingsSaved'/>");
            });
        setUserMessage("databaseBackupSettingsMessage");
        setDisabled("saveDatabaseBackupSettingsBtn", true);
    }
    
    function backupDatabaseNow(){
        SystemSettingsDwr.queueDatabaseBackup()
        setUserMessage("databaseBackupSettingsMessage", "<fmt:message key='systemSettings.backupQueued'/>");
    }
   /*
    * Save the Backup Settings
    */
    function getDatabaseBackupFiles() {
         hideContextualMessages("databaseBackupSettingsTab"); //Clear out any existing msgs
         SystemSettingsDwr.getDatabaseBackupFiles(function(response){
        	 if(response.hasMessages) {
        		showDwrMessages(response.messages);
        		return;
        	 }
        	 
             var backupFileSelect = document.getElementById("databaseFileToRestore");
             while(backupFileSelect.length > 0)
                 backupFileSelect.remove(0);
             for(var x=0; x<response.data.filenames.length; x++){
                 var option = document.createElement("option");
                 option.text = response.data.filenames[x];
                 option.value = response.data.filenames[x];
                 backupFileSelect.add(option, x);
             }
         });
    
    }
   
   /**
    * Restore the database
    */
   function restoreDatabaseFromBackup(){
       if(!confirm("<fmt:message key='systemSettings.confirmRestoreDatabase'/>"))
           return;
       
       var restoreMessages = document.getElementById("databaseRestoreMessages");
       restoreMessages.innerHTML = ""; //Clear out messages
       var backupFileSelect = document.getElementById("databaseFileToRestore");
       var selectedFile = backupFileSelect.options[backupFileSelect.selectedIndex];
       if(typeof selectedFile == 'undefined'){
           alert("<fmt:message key='systemSettings.noBackupSelected'/>");
           return;
       }
           
       
       SystemSettingsDwr.restoreDatabaseFromBackup(selectedFile.value, function(response){
           if (response.hasMessages)
               showDwrMessages(response.messages, $("databaseRestoreMessages"));
           else
               setUserMessage("databaseBackupSettingsMessage", "<fmt:message key='systemSettings.databaseRestored'/>");

       });
   }
   
    //
    // Permission management
    //
    var permissionUI = new PermissionUI(SystemSettingsDwr);
    function viewPermissions(e, id, permId) {
        if (!permId) {
            if (!id)
                id = this.id;
            // Remove the 'permView-' prefix
            permId = id.substring(9);
        }
        permissionUI.viewPermissions(permId);
    }
    
    //Virtual Serial Ports
    var virtualPorts;
    var editingVirtualSerialPortXid = -1;
    
    function displayVirtualSerialPorts(ports){
    	//Set the global reference
    	virtualPorts = {};
    	for(var i = 0; i<ports.length; i++){
    		var port = ports[i];
    		virtualPorts[port.xid] = port;
    	}
    	
        dwr.util.removeAllRows("virtualCommPortsTable");
        dwr.util.addRows("virtualCommPortsTable", ports, [
                function(p) { return "<img id='dv"+ p.xid +"Img' src='/images/item.png'/>"; },
                function(p) { 
                		var endpoint;
                		if(p.type == <c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SOCKET_BRIDGE %>"/>) {
                			endpoint = p.address + ":" + p.port + " (Client)";
                    	} else if(p.type == <c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SERVER_SOCKET_BRIDGE %>"/>) {
                    		endpoint = "localhost:" + p.port + " (Server)";
                 		}
                		return "<a class='link ptr virtual-comm' id='"+ p.xid +"'>"+ p.portName + ' --> ' + endpoint + "</a>"; 
                	}
        ]);
        
        require(["dojo/query", "dojo/on"], function(query, on) {
            query(".virtual-comm").forEach(function(e) {
                on(e, "click", function() { showVirtualCommPort(e.id); });        	
            });
        });
    	
    }
    
    /**
     * Add a port 
     */
    function saveVirtualSerialPort(){
    	hide("virtualSerialPortGenericMessages");
    	hide("virtualSerialPortValidationMessages");

    	var port = {
    		xid: $get('virtualSerialPortXid'),
    		type: $get('virtualSerialPortType'),
    		portName: $get('virtualSerialPortName'),
    		port: $get('virtualSerialPortPort')
    	};
    	
    	if(port.type == <c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SOCKET_BRIDGE %>"/>) {
 			port.address = $get('virtualSerialPortAddress');
    		port.timeout = $get('virtualSerialPortTimeout');
    	} else if(port.type == <c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SERVER_SOCKET_BRIDGE %>"/>) {
 			port.bufferSize = $get("virtualSerialPortBufferSize");
 			port.timeout = $get('virtualSerialPortTimeout');
 		}
    	
    	var saveCallback = function(result){
    		stopImageFader("saveVirtualSerialPortImg");
    		if(editingVirtualSerialPortXid !== -1)
        		stopImageFader("dv"+ editingVirtualSerialPortXid +"Img");
    		if (result.hasMessages)
                showDwrMessages(result.messages, "virtualSerialPortValidationMessages");
            else {
                displayVirtualSerialPorts(result.data.ports);
                
                showDwrMessages([{
               	 genericMessage: "<fmt:message key="systemSettings.comm.virtual.serialPortSaved"/>",
               	 level: 'error'	 
                }], 'virtualSerialPortGenericMessages');
                
                show("virtualSerialPortGenericMessages");
                
                displayVirtualSerialPorts(result.data.ports);
                hide('virtualSerialPortConfigDiv');
                
            	editingVirtualSerialPortXid = -1;
            }
 		};
    	
    	startImageFader("saveVirtualSerialPortImg", true); 		
    	if(port.type == <c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SOCKET_BRIDGE %>"/>)
    		SystemSettingsDwr.saveSerialSocketBridge(port, saveCallback);
    	else if(port.type == <c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SERVER_SOCKET_BRIDGE %>"/>)
    		SystemSettingsDwr.saveSerialServerSocketBridge(port, saveCallback);
    	else {
    		alert("Error: invalid virtual port type, cannot save.");
    		stopImageFader("saveVirtualSerialPortImg");
    	}
    }
    
    function removeVirtualSerialPort(){
    	hide("virtualSerialPortGenericMessages");
    	hide("virtualSerialPortValidationMessages");
    	
    	var port = virtualPorts[editingVirtualSerialPortXid];
    	var callback = function(result){
    		
    		if (result.hasMessages)
                showDwrMessages(result.messages, "virtualSerialPortValidationMessages");
            else {
            	if(editingVirtualSerialPortXid !== -1)
            		stopImageFader("dv"+ editingVirtualSerialPortXid +"Img");
                 hide("virtualSerialPortConfigDiv");
                 stopImageFader("removeVirtualSerialPortImg");
                 showDwrMessages([{
                	 genericMessage: "<fmt:message key="systemSettings.comm.virtual.serialPortRemoved"/>",
                	 level: 'error'	 
                 }], 'virtualSerialPortGenericMessages');

                 show("virtualSerialPortGenericMessages");
                 displayVirtualSerialPorts(result.data.ports);
                 editingVirtualSerialPortXid = -1;
             }
    	};
    	startImageFader("removeVirtualSerialPortImg", true);
    	if(port.type == <c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SOCKET_BRIDGE %>"/>)
    		SystemSettingsDwr.removeSerialSocketBridge(port, callback);
    	else if(port.type == <c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SERVER_SOCKET_BRIDGE %>"/>)
    		SystemSettingsDwr.removeSerialServerSocketBridge(port, callback);
    	else {
    		alert("Error: invalid virtual port type, cannot save.");
    		stopImageFader("removeVirtualSerialPortImg");
    	}
    }
    
    function showVirtualCommPort(xid){
    	hide("virtualSerialPortGenericMessages");
    	if(editingVirtualSerialPortXid !== -1)
    		stopImageFader($("dv"+ editingVirtualSerialPortXid +"Img"));
    	
    	editingVirtualSerialPortXid = xid;
    	if(xid === -1){
    		//Clear out inputs
    		$set('virtualSerialPortXid');
    		$set('virtualSerialPortName');
    		//Not yet set('virtualSerialPortType');
    		$set('virtualSerialPortAddress', 'localhost');
    		$set('virtualSerialPortPort', 9000);
    		$set('virtualSerialPortTimeout', 0);
    		$set('virtualSerialPortBufferSize', 1024);
    		show('virtualSerialPortConfigDiv');
    		hide('removeVirtualSerialPortImg');
    		show('virtualSerialConfig-address-row');
    		hide('virtualSerialConfig-bufferSize-row');
    	}else{
    		stopImageFader($("dv"+ xid +"Img"));
    		var port = virtualPorts[xid];
    		$set('virtualSerialPortXid', port.xid);
    		$set('virtualSerialPortName', port.portName);
    		$set('virtualSerialPortType', port.type);
    		$set('virtualSerialPortAddress', port.address);
    		$set('virtualSerialPortPort', port.port);
    		$set('virtualSerialPortTimeout', port.timeout);
    		$set('virtualSerialPortBufferSize', port.bufferSize);
    		startImageFader($("dv"+ xid +"Img"));
    		show('virtualSerialPortConfigDiv');
    		show('removeVirtualSerialPortImg');
    		if(port.type == <c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SOCKET_BRIDGE %>"/>) {
    			show('virtualSerialConfig-socketLabel');
    			show('virtualSerialConfig-address-row');
    			hide('virtualSerialConfig-serverLabel');
    			hide('virtualSerialConfig-bufferSize-row');
    		} else if(port.type == <c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SERVER_SOCKET_BRIDGE %>"/>) {
    			hide('virtualSerialConfig-socketLabel');
    			hide('virtualSerialConfig-address-row');
    			show('virtualSerialConfig-serverLabel');
        		show('virtualSerialConfig-bufferSize-row');
    		}
    	}
    }
    
    function updateVirtualSerialPortOptions() {
    	var type = $get('virtualSerialPortType');
    	if(type == <c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SOCKET_BRIDGE %>"/>) {
			show('virtualSerialConfig-address-row');
			hide('virtualSerialConfig-bufferSize-row');
			show('virtualSerialConfig-socketLabel');
			hide('virtualSerialConfig-serverLabel');
		} else if(type == <c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SERVER_SOCKET_BRIDGE %>"/>) {
    		hide('virtualSerialConfig-address-row');
    		show('virtualSerialConfig-bufferSize-row');
			hide('virtualSerialConfig-socketLabel');
			show('virtualSerialConfig-serverLabel');
		}
    }
    
  </script>

  <tag:labelledSection labelKey="systemSettings.systemInformation">
    <table>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.instanceDescription"/></td>
        <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.INSTANCE_DESCRIPTION %>"/>" type="text"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.timezone"/></td>
        <td class="formField"><c:out value="<%= TimeZone.getDefault().getID() %>"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.databaseType"/></td>
        <td class="formField"><c:out value="<%= Common.databaseProxy.getType().name() %>"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.databaseSize"/></td>
        <td class="formField">
          <span id="databaseSize"></span>
          <tag:img id="refreshImg" png="control_repeat_blue" onclick="dbSizeUpdate();" title="common.refresh"/>
          <tag:img id="purgeNowImg" png="bin" onclick="checkPurgeNow()" title="systemSettings.purgeNow"/>
        </td>
      </tr>
      <tr id="noSqlDatabaseSizeRow"  style="display:none">
        <td class="formLabel"><fmt:message key="systemSettings.noSqlDatabaseSize"/></td>
        <td class="formField" id="noSqlDatabaseSize"></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.filedataSize"/></td>
        <td class="formField" id="filedataSize"></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.totalSize"/></td>
        <td class="formField" id="totalSize"></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.historyCount"/></td>
        <td class="formField" id="historyCount"></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.topPoints"/></td>
        <td class="formField" id="topPoints"></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.eventCount"/></td>
        <td class="formField" id="eventCount"></td>
      </tr>
      <tr>
      	<td class="formLabel"><fmt:message key="systemSettings.upgradeVersionState"/></td>
      	<td class="formField">
      	  <select id="<c:out value="<%= SystemSettingsDao.UPGRADE_VERSION_STATE %>"/>">
      	    <option value="<c:out value="<%= UpgradeVersionState.DEVELOPMENT %>"/>">
      	      <fmt:message key="systemSettings.upgradeState.development"/>
      	    </option>
      	    <option value="<c:out value="<%= UpgradeVersionState.ALPHA %>"/>">
      	      <fmt:message key="systemSettings.upgradeState.alpha"/>
      	    </option>
      	    <option value="<c:out value="<%= UpgradeVersionState.BETA %>"/>">
      	      <fmt:message key="systemSettings.upgradeState.beta"/>
      	    </option>
      	    <option value="<c:out value="<%= UpgradeVersionState.RELEASE_CANDIDATE %>"/>">
      	      <fmt:message key="systemSettings.upgradeState.releaseCandidate"/>
      	    </option>
      	    <option value="<c:out value="<%= UpgradeVersionState.PRODUCTION %>"/>">
      	      <fmt:message key="systemSettings.upgradeState.production"/>
      	    </option>
      	  </select>
      	</td>
      </tr>
      <tr>
        <td colspan="2" align="center">
          <input id="infoBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveInfoSettings()"/>
          <tag:help id="systemInformation"/>
        </td>
      </tr>
      <tr><td colspan="2" id="infoMessage" class="formError"></td></tr>
    </table>
  </tag:labelledSection>
  
  <tag:labelledSection labelKey="systemSettings.siteAnalytics" closed="true">
    <table id="siteAnalyticsTab">
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.siteAnalytics.headTag"/></td>
        <td class="formField">
          <textarea id="<c:out value="<%= SystemSettingsDao.SITE_ANALYTICS_HEAD %>"/>" cols="100" rows="5">
          </textarea>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.siteAnalytics.bodyTag"/></td>
        <td class="formField">
          <textarea id="<c:out value="<%= SystemSettingsDao.SITE_ANALYTICS_BODY %>"/>" cols="100" rows="5">
          </textarea>
        </td>
      </tr>
      <tr>
        <td colspan="2" align="center">
          <input id="siteAnalyticsTestBtn" type="button" value="<fmt:message key='common.test'/>" onclick="window.open('/data_point_details.shtm');"/>
          <input id="siteAnalyticsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveSiteAnalytics()"/>
          <tag:help id="siteAnalyticsSettings"/>
        </td>
      </tr>
      <tr><td colspan="2" id="siteAnalyticsMessages" class="formError"></td></tr>
    </table>
  </tag:labelledSection>
  
  <tag:labelledSection labelKey="systemSettings.comm.virtual.serialPorts" closed="true">
  <table><tbody id="virtualSerialPortGenericMessages"></tbody></table>
  <div class="borderDiv marR" style="float: left;">
    <table>
      <tr>
        <td>
          <span class="smallTitle"><fmt:message key="systemSettings.comm.virtual.serialPorts.serialSocketBridges"/></span>
          <tag:help id="serialSocketBridge"/>
        </td>
        <td align="right"><tag:img png="add" onclick="showVirtualCommPort(-1)" title="common.add"/></td>
      </tr>
    </table>
    <table><tbody id="virtualCommPortsTable"></tbody></table>
  </div>
  <div id="virtualSerialPortConfigDiv" class="borderDiv" style="float:left; display:none;">
    <table>
      <tr>
        <td>
          <span id="virtualSerialConfig-socketLabel" class="smallTitle"><fmt:message key="systemSettings.comm.virtual.serialClientSocketBridgeSettings"/></span>
          <span id="virtualSerialConfig-serverLabel" class="smallTitle" style="display:none"><fmt:message key="systemSettings.comm.virtual.serialServerSocketBridgeSettings"/></span>
        </td>
        <td align="right">
          <tag:img id="saveVirtualSerialPortImg" png="save" onclick="saveVirtualSerialPort();" title="common.save"/>
          <tag:img id="removeVirtualSerialPortImg" png="delete" onclick="removeVirtualSerialPort();" title="common.delete" style="display:none;"/>
        </td>
      </tr>
    </table>
            
    <table><tbody id="virtualSerialPortValidationMessages"></tbody></table>
          
    <table id="virtualSerialPortConfigProps">
      <tr>
        <td colspan="2" id="virtualSerialPortMessage" class="formError"></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="virtualSerialPortXid" type="text" disabled/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.comm.virtual.serialClientSocketBridgeSettings.commPortId"/></td>
        <td class="formField"><input id="virtualSerialPortName" type="text"/></td>
<!--         <td class="formField"> -->
<!-- 	        <input id="virtualSerialPortName" type="text"/><br/>  -->
<%-- 	        <sst:select id="commPortIds"> --%>
<%-- 	          <c:forEach items="${commPorts}" var="port"> --%>
<%-- 	            <sst:option value="${port.name}">${port.name}</sst:option> --%>
<%-- 	          </c:forEach> --%>
<%-- 	      </sst:select> --%>
<%-- 	      <tag:img id='commPortsLoadingImg' src="/images/hourglass.png" style='display: none;'/> --%>
<%-- 	      <tag:img src="/images/arrow-turn-090-left.png" onclick="$set('virtualSerialPortName', $get('commPortIds'))"/> --%>
<%-- 	      <tag:img id='commPortRefreshButton' src="/images/arrow_refresh.png" onclick="reloadCommPorts('commPortIds', 'commPortsLoadingImg')" title='common.refreshCommPorts'/> --%>
<!-- 	    </td> -->
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.comm.virtual.serialPorts.types"/></td>
        <td class="formField">
          <select id="virtualSerialPortType" onchange="updateVirtualSerialPortOptions()">
            <option value="<c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SOCKET_BRIDGE %>"/>" selected>
              <fmt:message key="systemSettings.comm.virtual.serialPorts.types.serialClientSocketBridge"/>
            </option>
            <option value="<c:out value="<%= VirtualSerialPortConfig.SerialPortTypes.SERIAL_SERVER_SOCKET_BRIDGE %>"/>">
              <fmt:message key="systemSettings.comm.virtual.serialPorts.types.serialServerSocketBridge"/>
            </option>
          </select>
        </td>
      </tr>
      <tr id="virtualSerialConfig-bufferSize-row">
        <td class="formLabelRequired"><fmt:message key="systemSettings.comm.virtual.serialServerSocketBridgeSettings.bufferSize"/></td>
        <td class="formField"><input id="virtualSerialPortBufferSize" type="text" value="1024"/></td>
      </tr>     
      <tr id="virtualSerialConfig-address-row">
        <td class="formLabelRequired"><fmt:message key="systemSettings.comm.virtual.serialClientSocketBridgeSettings.address"/></td>
        <td class="formField"><input id="virtualSerialPortAddress" type="text" value="localhost"/></td>
      </tr>     
       <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.comm.virtual.serialClientSocketBridgeSettings.port"/></td>
        <td class="formField"><input id="virtualSerialPortPort" type="text" value="9000"/></td>
      </tr>     
       <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.comm.virtual.serialClientSocketBridgeSettings.timeout"/></td>
        <td class="formField"><input id="virtualSerialPortTimeout" type="number" value="0"/></td>
      </tr>     
   </table>
  </div>   
  <div class="clearfix"></div>
  </tag:labelledSection>
  
  <tag:labelledSection labelKey="systemSettings.systemPermissions" closed="true">
    <table id="systemPermissionsTab">
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.permissions.superadmin"/></td>
        <td class="formField"><input type="text" value="<c:out value="<%= SuperadminPermissionDefinition.GROUP_NAME %>"/>" disabled="disabled"></input></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.permissions.datasourceManagement"/></td>
        <td class="formField">
          <c:set var="dsPermId"><c:out value="<%= SystemSettingsDao.PERMISSION_DATASOURCE %>"/></c:set>
          <input id="${dsPermId}" type="text" class="formLong"/>
          <c:set var="dsPermId">permView-${dsPermId}</c:set>
          <tag:img id="${dsPermId}" png="bullet_down" onclick="viewPermissions(null, this.id)"/>
        </td>
      </tr>
      <tbody id="modulePermissions"></tbody>
      <tr>
        <td colspan="2" align="center">
          <input id="systemPermissionsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveSystemPermissions()"/>
          <tag:help id="systemPermissions"/>
        </td>
      </tr>
      <tr><td colspan="2" id="systemPermissionsMessage" class="formError"></td></tr>
    </table>
  </tag:labelledSection>
  
  <tag:labelledSection labelKey="systemSettings.systemAlarmLevels" closed="true">
    <table>
      <tbody id="systemEventAlarmLevelsList"></tbody>
      <tr>
        <td colspan="2" align="center">
          <input id="systemEventAlarmLevelsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveSystemEventAlarmLevels()"/>
          <tag:help id="systemAlarmLevels"/>
        </td>
      </tr>
      <tr><td colspan="2" id="systemEventAlarmLevelsMessage" class="formError"></td></tr>
    </table>
  </tag:labelledSection>
  
  <tag:labelledSection labelKey="systemSettings.auditAlarmLevels" closed="true">
    <table>
      <tbody id="auditEventAlarmLevelsList"></tbody>
      <tr>
        <td colspan="2" align="center">
          <input id="auditEventAlarmLevelsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveAuditEventAlarmLevels()"/>
          <tag:help id="auditAlarmLevels"/>
        </td>
      </tr>
      <tr><td colspan="2" id="auditEventAlarmLevelsMessage" class="formError"></td></tr>
    </table>
  </tag:labelledSection>
  
  <c:if test="${!empty availableLanguages}">
    <tag:labelledSection labelKey="systemSettings.languageSettings" closed="true">
      <table>
        <tr>
          <td class="formLabelRequired"><fmt:message key="systemSettings.systemLanguage"/></td>
          <td class="formField">
            <select id="<c:out value="<%= SystemSettingsDao.LANGUAGE %>"/>"></select>
          </td>
        </tr>
        <tr>
          <td colspan="2" align="center">
            <input id="saveLangSettingsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveLangSettings()"/>
            <tag:help id="languageSettings"/>
          </td>
        </tr>
        <tr><td colspan="2" id="langMessage" class="formError"></td></tr>
      </table>
    </tag:labelledSection>
  </c:if>
  
  <tag:labelledSection labelKey="systemSettings.pointHierarchySettings" closed="true">
	<table id="hierarchySettingsTab">
	  <tr>
	    <td class="formLabelRequired"><fmt:message key="systemSettings.exportDataPointPath"/></td>
	    <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.EXPORT_HIERARCHY_PATH %>"/>" type="checkbox"/></td>
	  </tr>
	  <tr>
	    <td class="formLabelRequired"><fmt:message key="systemSettings.exportPathSeparator"/></td>
	    <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.HIERARCHY_PATH_SEPARATOR %>"/>" type="text"/></td>
	  </tr>
	  <tr>
	    <td colspan="2" align="center">
          <input id="saveHierarchySettingsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveHierarchySettings()"/>
          <tag:help id="hierarchySettings"/>
        </td>
	  </tr>
	  <tr><td colspan="2" id="hierarchyMessage" class="formError"></td></tr>
	</table>  
  </tag:labelledSection>
  
  <tag:labelledSection labelKey="systemSettings.emailSettings" closed="true">
    <table id="emailSettingsTab">
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.smtpHost"/></td>
        <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_HOST %>"/>" type="text"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.smtpPort"/></td>
        <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_PORT %>"/>" type="text"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.fromAddress"/></td>
        <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.EMAIL_FROM_ADDRESS %>"/>" type="text"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.fromName"/></td>
        <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.EMAIL_FROM_NAME %>"/>" type="text"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.auth"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.EMAIL_AUTHORIZATION %>"/>" type="checkbox" onclick="smtpAuthChange()"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.smtpUsername"/></td>
        <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_USERNAME %>"/>" type="text"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.smtpPassword"/></td>
        <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.EMAIL_SMTP_PASSWORD %>"/>" type="password"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.tls"/></td>
        <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.EMAIL_TLS %>"/>" type="checkbox"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.contentType"/></td>
        <td class="formField">
          <select id="<c:out value="<%= SystemSettingsDao.EMAIL_CONTENT_TYPE %>"/>">
            <option value="<c:out value="<%= MangoEmailContent.CONTENT_TYPE_BOTH %>"/>"><fmt:message key="systemSettings.contentType.both"/></option>
            <option value="<c:out value="<%= MangoEmailContent.CONTENT_TYPE_HTML %>"/>"><fmt:message key="systemSettings.contentType.html"/></option>
            <option value="<c:out value="<%= MangoEmailContent.CONTENT_TYPE_TEXT %>"/>"><fmt:message key="systemSettings.contentType.text"/></option>
          </select>
        </td>
      </tr>
      
      <tr>
        <td colspan="2" align="center">
          <input id="saveEmailSettingsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveEmailSettings()"/>
          <tag:help id="emailSettings"/>
          <tag:img id="sendTestEmailImg" png="email_go" onclick="sendTestEmail();" title="common.sendTestEmail"/>
        </td>
      </tr>
      
      <tr><td colspan="2" id="emailMessage" class="formError"></td></tr>
    </table>
  </tag:labelledSection>
  
  <tag:labelledSection labelKey="systemSettings.httpSettings" closed="true">
    <table>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.useProxy"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_USE_PROXY %>"/>" type="checkbox"
                  onclick="httpUseProxyChange()"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.proxyHost"/></td>
        <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER %>"/>" type="text"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.proxyPort"/></td>
        <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_PORT %>"/>" type="text"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.proxyUsername"/></td>
        <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME %>"/>" type="text"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.proxyPassword"/></td>
        <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD %>"/>" type="password"/></td>
      </tr>
      <tr>
        <td colspan="2" align="center">
          <input id="saveHttpSettingsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveHttpSettings()"/>
          <tag:help id="httpSettings"/>
        </td>
      </tr>
      <tr><td colspan="2" id="httpMessage" class="formError"></td></tr>
    </table>
  </tag:labelledSection>

  <tag:labelledSection labelKey="systemSettings.httpServerSettings" closed="true">
    <table>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.httpSessionTimeout"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="httpSessionPeriodType"><c:out value="<%= SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${httpSessionPeriodType}" s="true" min="true" h="true" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td colspan="2" align="center">
          <input id="saveHttpServerSettingsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveHttpServerSettings()"/>
          <tag:help id="httpServerSettings"/>
        </td>
      </tr>
      <tr><td colspan="2" id="httpServerMessage" class="formError"></td></tr>
    </table>
  </tag:labelledSection>

  <tag:labelledSection labelKey="systemSettings.threadPools" closed="true">
  <table>
    <tr>
      <td class="formLabelRequired"><fmt:message key="systemSettings.threadPools.highPriorityCorePoolSize"/></td>
      <td class="formField">
        <input id="<c:out value="<%= SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE %>"/>" type="number"/>
      </td>
    </tr>
    <tr>
      <td class="formLabelRequired"><fmt:message key="systemSettings.threadPools.highPriorityMaximumPoolSize"/></td>
      <td class="formField">
        <input id="<c:out value="<%= SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE %>"/>" type="number"/>
      </td>
    </tr>
    <tr>
      <td class="formLabelRequired"><fmt:message key="systemSettings.threadPools.mediumPriorityCorePoolSize"/></td>
      <td class="formField">
        <input id="<c:out value="<%= SystemSettingsDao.MED_PRI_CORE_POOL_SIZE %>"/>" type="number"/>
      </td>
    </tr>
    <tr>
      <td class="formLabelRequired"><fmt:message key="systemSettings.threadPools.lowPriorityCorePoolSize"/></td>
      <td class="formField">
        <input id="<c:out value="<%= SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE %>"/>" type="number"/>
      </td>
    </tr>
    <tr>
      <td colspan="2" align="center">
        <input id="saveThreadPoolSettingsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveThreadPoolSettings()"/>
        <tag:help id="threadPoolSettings"/>
      </td>
    </tr>
    <tr><td colspan="2" id="threadPoolMessage" class="formError"></td></tr>
    </table>
  </tag:labelledSection>
  
  <tag:labelledSection labelKey="systemSettings.uiPerformance" closed="true">
      <table>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.uiPerformance"/>
        <tag:help id="uiPerformance"/></td>
        <td class="formField">
          <select id="<c:out value="<%= SystemSettingsDao.UI_PERFORMANCE %>"/>">
            <option value="2000"><fmt:message key="systemSettings.uiPerformance.high"/></option>
            <option value="5000"><fmt:message key="systemSettings.uiPerformance.med"/></option>
            <option value="10000"><fmt:message key="systemSettings.uiPerformance.low"/></option>
          </select>
        </td>
      </tr>
      </table>
  </tag:labelledSection>
  
  <tag:labelledSection labelKey="systemSettings.purgeSettings" closed="true">
    <table>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgePointData"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.POINT_DATA_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.enablePurgePointData"/></td>
        <td class="formField"><input id="<c:out value="<%= DataPurge.ENABLE_POINT_DATA_PURGE %>"/>" type="checkbox"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.countPurgedPointData"/></td>
        <td class="formField"><input id="<c:out value="<%= SystemSettingsDao.POINT_DATA_PURGE_COUNT %>"/>" type="checkbox"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgeDataPointEvents"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgeDataSourceEvents"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgeSystemEvents"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgePublisherEvents"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgeAuditEvents"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.AUDIT_EVENT_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.AUDIT_EVENT_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgeNoneAlarm"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.NONE_ALARM_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.NONE_ALARM_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgeInformationAlarm"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgeImportantAlarm"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgeWarningAlarm"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.WARNING_ALARM_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.WARNING_ALARM_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgeUrgentAlarm"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.URGENT_ALARM_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.URGENT_ALARM_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgeCriticalAlarm"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgeLifeSafetyAlarm"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.purgeEvents"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.EVENT_PURGE_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.EVENT_PURGE_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.futureDateLimit"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.FUTURE_DATE_LIMIT_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.FUTURE_DATE_LIMIT_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" min="true" h="true"/>
        </td>
      </tr>
      
      <tr>
        <td colspan="2" align="center">
          <input id="saveMiscSettingsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveMiscSettings()"/>
          <input type="button" value="<fmt:message key="systemSettings.purgeData"/>" onclick="checkPurgeAllPointValuesNow()"/>
          <input type="button" value='<fmt:message key="systemSettings.purgeAllEvents"/>' onclick="checkPurgeAllEventsNow();"/>
          <input type="button" value='<fmt:message key="systemSettings.purgeNow"/>' onclick="checkPurgeNow();"/>
          <tag:help id="otherSettings"/>
        </td>
      </tr>
      
      <tr><td colspan="2" id="miscMessage" class="formError"></td></tr>
    </table>
  </tag:labelledSection>

  <c:forEach items="<%= ModuleRegistry.getDefinitions(SystemSettingsDefinition.class) %>" var="def">
    <tag:labelledSection labelKey="${def.descriptionKey}" closed="true" sectionId="${def.module.name}">
      <c:set var="incpage">${def.module.webPath}/${def.sectionJspPath}</c:set>
      <jsp:include page="${incpage}"/>
    </tag:labelledSection>
  </c:forEach>
  
  <tag:labelledSection labelKey="systemSettings.colourSettings" closed="true">
    <table id="colourSettingsTab">
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.chartBackgroundColour"/></td>
        <td class="formField"><input type="text" id="<c:out value="<%= SystemSettingsDao.CHART_BACKGROUND_COLOUR %>"/>"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.plotBackgroundColour"/></td>
        <td class="formField"><input type="text" id="<c:out value="<%= SystemSettingsDao.PLOT_BACKGROUND_COLOUR %>"/>"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.plotGridlinesColour"/></td>
        <td class="formField"><input type="text" id="<c:out value="<%= SystemSettingsDao.PLOT_GRIDLINE_COLOUR %>"/>"/></td>
      </tr>
    
      <tr>
        <td colspan="2" align="center">
          <input id="saveColourSettingsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveColourSettings()"/>
          <tag:help id="colourSettings"/>
        </td>
      </tr>
      
      <tr><td colspan="2" id="colourMessage" class="formError"></td></tr>
    </table>
  </tag:labelledSection>
  
  <tag:labelledSection labelKey="systemSettings.backupSettings" closed="true">
    <table id="backupSettingsTab">
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.backupEnable"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.BACKUP_ENABLED %>"/>" type="checkbox" />
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.backupLastSuccessfulRun"/></td>
        <td class="formField"><span id="<c:out value="<%= SystemSettingsDao.BACKUP_LAST_RUN_SUCCESS %>"/>"></span></td>
      </tr>
    
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.backupLocation"/></td>
        <td class="formField"><input type="text" id="<c:out value="<%= SystemSettingsDao.BACKUP_FILE_LOCATION %>"/>"/> </td>
      </tr>

      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.backupFrequency"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.BACKUP_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="tpid"><c:out value="<%= SystemSettingsDao.BACKUP_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${tpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.backupTime"/></td>
        <td class="formField">
          <fmt:message key="systemSettings.backupHour"/><input id="<c:out value="<%= SystemSettingsDao.BACKUP_HOUR %>"/>" type="text" class="formShort"/>:
          <fmt:message key="systemSettings.backupMinute"/><input id="<c:out value="<%= SystemSettingsDao.BACKUP_MINUTE %>"/>" type="text" class="formShort"/>
         </td>
     </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.backupFileCount"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.BACKUP_FILE_COUNT %>"/>" type="text" class="formShort"/>
         </td>
     </tr>
      <tr>
        <td colspan="2" align="center">
          <input id="executeBackupNowBtn" type="button" value="<fmt:message key="systemSettings.backupNow"/>" onclick="backupNow()"/>
          <input id="saveBackupSettingsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveBackupSettings()"/>
          <tag:help id="backupSettings"/>
        </td>
      </tr>
      
      <tr><td colspan="2" id="backupSettingsMessage" class="formError"></td></tr>
    </table>
  </tag:labelledSection>
  
    <tag:labelledSection labelKey="systemSettings.H2DatabaseBackupSettings" closed="true">
    <table id="databaseBackupSettingsTab">
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.backupEnable"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_ENABLED %>"/>" type="checkbox" />
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="systemSettings.backupLastSuccessfulRun"/></td>
        <td class="formField"><span id="<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_LAST_RUN_SUCCESS %>"/>"></span></td>
      </tr>
    
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.backupLocation"/></td>
        <td class="formField"><input type="text" id="<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION %>"/>"/> </td>
      </tr>

      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.backupFrequency"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_PERIODS %>"/>" type="text" class="formShort"/>
          <c:set var="dbTpid"><c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_PERIOD_TYPE %>"/></c:set>
          <tag:timePeriods id="${dbTpid}" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.backupTime"/></td>
        <td class="formField">
          <fmt:message key="systemSettings.backupHour"/><input id="<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_HOUR %>"/>" type="text" class="formShort"/>:
          <fmt:message key="systemSettings.backupMinute"/><input id="<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_MINUTE %>"/>" type="text" class="formShort"/>
         </td>
     </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.backupFileCount"/></td>
        <td class="formField">
          <input id="<c:out value="<%= SystemSettingsDao.DATABASE_BACKUP_FILE_COUNT %>"/>" type="text" class="formShort"/>
         </td>
     </tr>
     <tr>
        <td class="formLabel">
          <select id="databaseFileToRestore"></select>
        </td>
        <td class="formField">
          <input type="button" value="<fmt:message key="systemSettings.getBackupFiles"/>" onclick="getDatabaseBackupFiles()"/>
          <input id="restoreDatabaseBtn" type="button" value="<fmt:message key="systemSettings.restoreDatabase"/>" onclick="restoreDatabaseFromBackup()"/>
         </td>
     </tr>
      <tr>
        <td colspan="2" align="center">
          <input id="executeDatabaseBackupNowBtn" type="button" value="<fmt:message key="systemSettings.backupNow"/>" onclick="backupDatabaseNow()"/>
          <input id="saveDatabaseBackupSettingsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveDatabaseBackupSettings()"/>
          <tag:help id="databaseBackupSettings"/>
        </td>
      </tr>
      <tr><td colspan="2" id="databaseBackupSettingsMessage" class="formError"></td></tr>
     <tr>
         <td colspan="2">
          <table>
              <tbody id="databaseRestoreMessages"></tbody>
          </table>
       </td>
     </tr>

    </table>
  </tag:labelledSection>
  
  <tag:labelledSection labelKey="systemSettings.chartSettings" closed="true">
    <table id="chartApiSettingsTab">
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.allowAnonymousChartView"/></td>
        <td class="formField">
          <input id="<c:out value="<%=SystemSettingsDao.ALLOW_ANONYMOUS_CHART_VIEW%>"/>" type="checkbox" />
        </td>
      </tr>
      
      <tr>
        <td class="formLabelRequired"><fmt:message key="systemSettings.chartFont"/></td>
        <td class="formField">
          <input id="<c:out value="<%=SystemSettingsDao.JFREE_CHART_FONT%>"/>" type="text" />
          <tag:img png="bullet_go_left" onclick='setChartFont()'/>
          <select id="font-selector">
            <option value=""><fmt:message key="systemSettings.chartFont.default"/></option>
            <option value="Open Sans, Lucida Sans"><fmt:message key="systemSettings.chartFont.openSans"/></option>
          </select>
        </td>
      </tr>

      <tr>
        <td colspan="2" align="center">
          <input id="saveChartSettingsBtn" type="button" value="<fmt:message key="common.save"/>" onclick="saveChartSettings()"/>
          <tag:help id="chartSettings"/>
        </td>
      </tr>
      
      <tr><td colspan="2" id="chartSettingsMessage" class="formError"></td></tr>
    </table>
  </tag:labelledSection>
  <%-- Include the Data Point Template Configuration When ready
  <jsp:include page="/WEB-INF/jsp/dataPointTemplateManagement.jsp"/>
  --%>
  
  
</tag:page>