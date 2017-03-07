<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@page import="com.serotonin.m2m2.Common"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<tag:page showHeader="${param.showHeader}" showToolbar="${param.showToolbar}" dwr="ModulesDwr">
  <c:set var="storeUrl" value='<%= Common.envProps.getString("store.url") %>'/>

  <style type="text/css">
    .modName { width: 200px; display: inline-block; }
    .relNotes { width: 50px; display: inline-block; }
    .relNotesContent { margin: 2px; max-height: 300px; overflow: auto; }
    .relNotesContent .desc { color: #699D2E; }
    .relNotesContent .vendor { font-style: italic; margin-bottom: 5px; }
    .upgradeSection { margin-bottom: 20px; }
    .upgradeSectionTitle { margin-bottom: 5px; }
    .upgradeSectionOptions { margin-left: 20px; }
    .modulesList { margin-top: 5px; }
  </style>
  <script type="text/javascript">
    dojo.require("dijit.TooltipDialog");
  
    function toggleDeletion(name) {
        var id = "module-"+name;
        if (!dojo.hasClass(id, "marked")) {
            if (!confirm("<m2m2:translate key="modules.module.deleteConfirm" escapeDQuotes="true"/>"))
                return;
        }
        
        ModulesDwr.toggleDeletion(name, function(marked) {
            if (marked)
                dojo.addClass(id, "marked");
            else
                dojo.removeClass(id, "marked");
        });
    }
    
    function restartInstance() {
        if (confirm("<m2m2:translate key='modules.restartConfirm' escapeDQuotes='true'/>"))
            ModulesDwr.scheduleRestart(function(response) {
                alert("<m2m2:translate key='modules.restartScheduled' escapeDQuotes='true'/>");
                window.location.href = response.data.shutdownUri;
            });
    }
    
    function shutdownInstance(){
        if (confirm("<m2m2:translate key='modules.shutdownConfirm' escapeDQuotes='true'/>"))
            ModulesDwr.scheduleShutdown(function(response) {
                alert("<m2m2:translate key='modules.shutdownScheduled' escapeDQuotes='true'/>");
                //Redirect to shutdown page
                window.location.href = response.data.shutdownUri;
            });
    }
    
    var versionCheckData = null;
    var allModuleList;
    function versionCheck() {
        disableButton("versionCheckBtn");
        
        ModulesDwr.versionCheck(function(result) {
            enableButton("versionCheckBtn");
        	
            if (result.data.unknownHost) {
            	alert("<m2m2:translate key="modules.versionCheck.unknownHost" escapeDQuotes="true"/> " + result.data.unknownHost);
            	return;
            }
            if (result.data.error) {
                alert("<m2m2:translate key="modules.versionCheck.error" escapeDQuotes="true"/> "+ result.data.error);
                return;
            }
            
            versionCheckData = result.data;
            allModuleList = [];
            var upgradeList = versionCheckData.upgrades;
            var newInstallList = versionCheckData.newInstalls;
            
            // Reset the state of all of the upgrade stuff in case this is a second run.
            if("updates" in versionCheckData) {
            	show("installUpgrades")
            	delete $("isInstallUpgrades").checked;
            	$("isInstallUpgrades").disabled = false;
            } else {
            	hide("installUpgrades");
            	if($("isInstallUpgrades").checked)
            		delete $("isInstallUpgrades").checked;
            }
            
            //call the appropriate drawLists
            toggleInstallUpgrades();
            
            show("upgradeModulesButtons");
            hide("upgradeModulesThrobber");
            hide("upgradeModulesFinished");
            hide("upgradeModulesError");
            show("upgradesDiv");
            $("masterUpgradeCB").disabled = false;
            $("masterNewInstallCB").disabled = false;
            toggler($("newInstallToggler"), true);
            toggler($("advancedOptionsToggler"), false);
            $set("upgradeStage");
        });
    }
    
    function toggleInstallUpgrades() {
    	allModuleMap = {};
    	$set($("masterUpgradeCB"), true);
    	$set($("masterNewInstallCB"), false);
    	if(versionCheckData == null)
    		versionCheck();
    	else if($get("isInstallUpgrades") || !("updates" in versionCheckData))
    		drawLists(versionCheckData.upgrades, versionCheckData.newInstalls)
    	else
    		drawLists(versionCheckData.updates, versionCheckData["newInstalls-oldCore"]);
    }
    
    function drawLists(upgradeList, newInstallList) {
    	var notes = "<m2m2:translate key="modules.versionCheck.notes" escapeDQuotes="true"/>";
        // Draw the upgrade list.
        if (upgradeList.length > 0) {
            var s = "";
            for (var i=0; i<upgradeList.length; i++) {
                allModuleList.push(upgradeList[i]);
                var name = upgradeList[i].name;
                s += "<div>";
                s += "<input type='checkbox' id='"+ name +"Check' checked='checked' class='modCB upgradeCB'>";
                s += "<div class='modName'><label for='"+ name +"Check'>&nbsp;" + name +"-"+ upgradeList[i].version +"</label></div>";
                s += "&nbsp;<div id='"+ name +"relNotes' class='relNotes'>"+ notes +"</div>";
                s += "<span class='infoData' style='padding-left:20px;' id='"+ name +"downloadResult'></span>";
                s += "</div>";
            }
            $set("upgradeModulesList", s);
            show("upgradeModulesOptions");
            hide("upgradeModulesNone");
        }
        else {
            hide("upgradeModulesOptions");
            show("upgradeModulesNone");
        }
        
     // Draw the new install list.
        if (newInstallList.length > 0) {
            s = "";
            for (var i=0; i<newInstallList.length; i++) {
                allModuleList.push(newInstallList[i]);
                var name = newInstallList[i].name;
                s += "<div>";
                s += "<input type='checkbox' id='"+ name +"Check' class='modCB newInstallCB'>";
                s += "<div class='modName'><label for='"+ name +"Check'>&nbsp;" + name +"-"+ newInstallList[i].version +"</label></div>";
                s += "&nbsp;<div id='"+ name +"relNotes' class='relNotes'>"+ notes +"</div>";
                s += "<span class='infoData' style='padding-left:20px;' id='"+ name +"downloadResult'></span>";
                s += "</div>";
            }
            $set("newInstallModulesList", s);
            show("newInstallModulesOptions");
            hide("newInstallModulesNone");
        }
        else {
            hide("newInstallModulesOptions");
            show("newInstallModulesNone");
        }
        
        // Create the rollovers for the release notes.
        var notes = dojo.query(".relNotes");
        for (var i=0; i<notes.length; i++) {
            dojo.connect(notes[i], "onmouseover", showReleaseNotes);
            dojo.connect(notes[i], "onmouseout", cancelReleaseNotes);
        }
    }
    
    var myTooltipDialog;
    var relNotesTimeout;
    function showReleaseNotes() {
        var thisNode = this;
        
        // Get the release notes content. Get the module name by clipping 'relNotes' from the end of the id.
        var modName = this.id.substring(0, this.id.length - 8);
        var mod = getElement(allModuleList, modName, "name");
        
        var content = "<div class='relNotesContent'>";
        content += "<div class='desc'>"+ mod.shortDescription +"</div>";
        if (mod.vendorName)
            content += "<div class='vendor'>"+ mod.vendorName +"</div>";
        content += "<div class='notes'>"+ mod.releaseNotes +"</div>";
        content += "</div>";
        
        // Start a timeout to display the content instead of displaying immediately. This is so that a mouse that just 
        // happens to hover over the element - without the intention of viewing the notes - does not actually open
        // the dialog, since the mouseout event will cancel the timeout. This is important because, to close an open
        // dialog, the user must mouseout of the *dialog*, not the 'notes' element.
        relNotesTimeout = setTimeout(function() {
        	if (myTooltipDialog)
                myTooltipDialog.destroy();
        	
            require(["dijit/TooltipDialog", "dijit/popup", "dojo/dom" ], function(TooltipDialog, popup, dom) {
                myTooltipDialog = new TooltipDialog({
                    id: 'myTooltipDialog',
                    style: "width: 500px;",
                    content: content,
                    onMouseLeave: function() { 
                        popup.close(myTooltipDialog);
                    }
                });
                
                popup.open({
                    popup: myTooltipDialog,
                    around: thisNode
                });
            });
        }, 200);
    }
    
    function cancelReleaseNotes() {
    	clearTimeout(relNotesTimeout);
    }
    
    function startDownloads() {
    	if($("isInstallUpgrades").checked)
    		if(!confirm("<m2m2:translate key="modules.download.coreUpgradeConfirm" escapeDQuotes="true"/>"))
    			return;
        disableButton("downloadUpgradesBtn");
        disableButton("isInstallUpgrades");
        show("upgradeModulesThrobber");
        
        // Create a list of the checked modules.
        var checkedModules = [];
        var cbs = dojo.query(".modCB");
        for (var i=0; i<cbs.length; i++) {
            if (cbs[i].checked) {
                var name = cbs[i].id;
                // Remove the 'Check' at the end.
                name = name.substring(0, name.length - 5);
                checkedModules.push({"key": name, "value": getElement(allModuleList, name, 'name').version});
            }
        }
        
        ModulesDwr.startDownloads(checkedModules, $get("backupCheck"), $get("restartCheck"), function(error) {
            // Check if there was an error with the selected modules.
            if (error)
                alert("<m2m2:translate key="modules.consistencyCheck" escapeDQuotes="true"/>");
            else {
                // Disable all of the checkboxes
                var cbs = dojo.query(".modCB");
                for (var i=0; i<cbs.length; i++)
                    cbs[i].disabled = true;
                $("masterUpgradeCB").disabled = true;
                $("masterNewInstallCB").disabled = true;
                
                downloadMonitor();
            }
        });
    }
    
    function cancelUpgrade() {
    	hide('upgradesDiv');
    	ModulesDwr.tryCancelDownloads(function(response) {
    		showDwrMessages(response);
    	});
    }
    
    function downloadMonitor() {
        ModulesDwr.monitorDownloads(function(results) {
        	$set("upgradeStage", results.data.stage);
            for (var i=0; i<results.data.results.length; i++)
                $set(results.data.results[i].key +"downloadResult", results.data.results[i].value);
                
            if (results.data.finished) {
                enableButton("downloadUpgradesBtn");
                hide("upgradeModulesButtons");
                hide("upgradeModulesThrobber");
                
                if (results.data.cancelled) {
                	//Nothing to do
            	} else if (results.data.error) {
                    $set("upgradeModulesErrorMessage", results.data.error);
                    show("upgradeModulesError");
                }
                else if (results.data.restart)
                    // Forward to the shutdown page
                    window.location = "/shutdown.htm";
                else
                    show("upgradeModulesFinished");
            }
            else
                setTimeout(downloadMonitor, 1000);
        });
    }
    
    function updateCBs(checked, clazz) {
        var cbs = dojo.query("."+ clazz);
        for (var i=0; i<cbs.length; i++)
            cbs[i].checked = checked;
    }
    
    function toggler(a, showing) {
    	if (typeof(showing) == "undefined")
    		showing = a.showing;
    	
        if (showing) {
            delete a.showing;
            $set(a, "<fmt:message key="common.show"/>");
            if (a.id == "newInstallToggler")
                hide("newInstallModulesOptions")
            else
                hide("advancedOptionsList")
        }
        else {
            a.showing = true;
            $set(a, "<fmt:message key="common.hide"/>");
            if (a.id == "newInstallToggler")
                show("newInstallModulesOptions")
            else
                show("advancedOptionsList")
        }
    }
    
    function downloadLicense() {
        var r = bareUri();
        var g = "${guid}";
        var d = "${distributor}";
        window.location = "${storeUrl}/account/servlet/getDownloadToken?r="+ r +"&g="+ g +"&d="+ d;
    }
    
    function storeCheck() {
        if (window.location.href.indexOf('store') != -1) {
            $("goToStore").click();
            setTimeout("history.back()", 2000);
        }
    }
    
    dojo.ready(function() {
        <c:if test="${licenseDownloaded}">
          alert("<m2m2:translate key="modules.licenseDownloaded" escapeDQuotes="true"/>");
        </c:if>
    	
        storeCheck();
        $set("redirectURI", bareUri());
    });
    
    function bareUri() {
        var s = ""+ window.location;
        var pos = s.indexOf("?");
        if (pos != -1)
            s = s.substring(0, pos);
        return s;
    }
  </script>
  
  <div>
    <tag:img png="puzzle" title="modules.modules"/>
    <span class="smallTitle"><fmt:message key="modules.modules"/></span>
  </div>
  
  <div id="guid">
    <form action="${storeUrl}/account/store" method="post" target="mangoStore">
      <fmt:message key="modules.guid"/> <b>${guid}</b>
      <textarea rows="2" cols="80" style="display:none;" name="orderJson">${json}</textarea>
      <input type="hidden" id="redirectURI" name="redirect" value=""/>
      <input id="goToStore" type="submit" value="<fmt:message key="modules.update"/>" style="margin-left:20px;"/>
      <input id="downloadLicenseBtn" type="button" value="<fmt:message key="modules.downloadLicense"/>" onclick="downloadLicense();"/>
      <input type="button" value="<m2m2:translate key='modules.restart'/>" onclick="restartInstance();"/>
<%--       <input type="button" value="<m2m2:translate key='modules.shutdown'/>" onclick="shutdownInstance();"/> --%>
      <input id="versionCheckBtn" type="button" value="<m2m2:translate key="modules.versionCheck"/>" onclick="versionCheck();"/>
    </form>
  </div>
  
  <div id="upgradesDiv" class="borderDiv" style="display:none; margin: 10px 100px 0px 100px; padding: 10px;">
    <div class="upgradeSection">
      <div class="upgradeSectionTitle">
        <b><m2m2:translate key="modules.versionCheck.some"/></b>
      </div>
      <div id="upgradeModulesNone" class="upgradeSectionOptions">
        <m2m2:translate key="modules.versionCheck.none"/>
      </div>
      <div id="upgradeModulesOptions" class="upgradeSectionOptions">
        <input type="checkbox" id="masterUpgradeCB" checked="checked" onclick="updateCBs(this.checked, 'upgradeCB')"/>
        <label for="masterUpgradeCB"><m2m2:translate key="common.all"/></label>
        <div id="upgradeModulesList" class="modulesList"></div>
      </div>
    </div>
    
    <div class="upgradeSection">
      <div class="upgradeSectionTitle">
        <b><m2m2:translate key="modules.versionCheck.new"/></b>
        (<a id="newInstallToggler" class="ptr" onclick="toggler(this)"><fmt:message key="common.show"/></a>)
      </div>
      <div id="newInstallModulesNone" class="upgradeSectionOptions">
        <m2m2:translate key="modules.versionCheck.noneToInstall"/>
      </div>
      <div id="newInstallModulesOptions" class="upgradeSectionOptions">
        <input type="checkbox" id="masterNewInstallCB" onclick="updateCBs(this.checked, 'newInstallCB')"/>
        <label for="masterNewInstallCB"><m2m2:translate key="common.all"/></label>
        <div id="newInstallModulesList" class="modulesList"></div>
      </div>
    </div>
    
    <div class="upgradeSection">
      <div class="upgradeSectionTitle">
        <b><m2m2:translate key="modules.versionCheck.advanced"/></b>
        (<a id="advancedOptionsToggler" class="ptr" onclick="toggler(this)"><fmt:message key="common.show"/></a>)
      </div>
      <div id="advancedOptionsList" class="upgradeSectionOptions">
        <div><input type='checkbox' id='backupCheck' checked="checked"><label for='backupCheck'>&nbsp;<m2m2:translate key="modules.versionCheck.advanced.backup"/></label></div>
        <div><input type='checkbox' id='restartCheck' checked="checked"><label for='restartCheck'>&nbsp;<m2m2:translate key="modules.versionCheck.advanced.restart"/></label></div>
      </div>
    </div>
    
    <div id="upgradeModulesButtons">
      <div id="installUpgrades">
      	<input id="isInstallUpgrades" type="checkbox" onclick="toggleInstallUpgrades();"/>
      	<fmt:message key="modules.installUpgrades"/>
      </div>
      <input id="downloadUpgradesBtn" type="button" value="<fmt:message key="modules.downloadUpgrades"/>" onclick="startDownloads();"/>
      <input id="upgradesCloseBtn" type="button" value="<fmt:message key="modules.upgradesClose"/>" onclick="cancelUpgrade();"/>
      <tag:help id="performUpgrades" />&nbsp;
      <img id="upgradeModulesThrobber" src="/images/throbber.gif" style="vertical-align: bottom;"/>&nbsp;
      <span id="upgradeStage"></span>
    </div>
    <div id="upgradeModulesFinished" style="display:none;">
      <b><fmt:message key="modules.downloadsFinished"/></b>
      <input type="button" value="<m2m2:translate key="modules.restart"/>" style="margin-left:20px;" onclick="restartInstance();"/>
    </div>
    <div id="upgradeModulesError" style="display:none;" class="infoData">
      <b><fmt:message key="modules.downloadsError"/> <span id="upgradeModulesErrorMessage"></span></b>
    </div>
  </div>
  
  <div id="moduleList">
    <c:forEach items="${modules}" var="module">
      <div id="module-${module.name}" class="module <c:if test="${module.markedForDeletion}">marked</c:if>">
        <c:choose>
          <c:when test="${module.name == 'core'}">
            <a class="name" href="<c:out value='<%= Common.envProps.getString("store.url") %>'/>/core">${module.name}</a>
          </c:when>
          <c:otherwise>
            <div class="deleteMark">
              <tag:img png="delete" title="modules.module.delete" onclick="toggleDeletion('${module.name}')"/>
            </div>
            <a class="name" href="<c:out value='<%= Common.envProps.getString("store.url") %>'/>/module/${module.name}">${module.name}</a>
          </c:otherwise>
        </c:choose>
        
        <span class="version">
          ${module.version} -
          <c:choose>
            <c:when test="${empty module.licenseType}">*** unlicensed ***</c:when>
            <c:otherwise>${module.licenseType}</c:otherwise>
          </c:choose> 
        </span>
               
        <div class="vendor">
          <c:choose>
            <c:when test="${empty module.vendor}"></c:when>
            <c:when test="${empty module.vendorUrl}">${module.vendor}</c:when>
            <c:otherwise><a href="${module.vendorUrl}" target="_blank">${module.vendor}</a></c:otherwise>
          </c:choose>
        </div>
        
        <c:if test="${!empty module.description}"><div class="description"><m2m2:translate message="${module.description}"/></div></c:if>
        <c:set var="licErrors" value="${module.licenseErrors}"/>
        <c:set var="licWarnings" value="${module.licenseWarnings}"/>
        
        <c:if test="${!empty licErrors || !empty licWarnings}">
          <c:if test="${!empty licErrors}">
            <ul class="errors">
              <c:forEach items="${licErrors}" var="licError"><li><m2m2:translate message="${licError}"/></li></c:forEach>
            </ul>
          </c:if>
          <c:if test="${!empty licWarnings}">
            <ul class="warnings">
              <c:forEach items="${licWarnings}" var="licWarning"><li><m2m2:translate message="${licWarning}"/></li></c:forEach>
            </ul>
          </c:if>
        </c:if>
      </div>
    </c:forEach>
  </div>
</tag:page>