<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@page import="com.serotonin.m2m2.Common"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<tag:page showHeader="${param.showHeader}" showToolbar="${param.showToolbar}" dwr="ModulesDwr">
  <script type="text/javascript">
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
        if (confirm("<m2m2:translate key="modules.restartConfirm" escapeDQuotes="true"/>"))
            ModulesDwr.scheduleRestart(function() {
                alert("<m2m2:translate key="modules.restartScheduled" escapeDQuotes="true"/>");
            });
    }
    
    var versionUpgradeList;
    function versionCheck() {
        ModulesDwr.versionCheck(function(upgrades) {
            if (upgrades.length > 0) {
                versionUpgradeList = upgrades;
                var s = "";
                for (var i=0; i<upgrades.length; i++) {
                    s += ""+ upgrades[i].key +": "+ upgrades[i].value;
                    s += "<span class='infoData' style='padding-left:20px;' id='"+ upgrades[i].key +"downloadResult'></span>";
                    s += "</br>";
                }
                $set("upgradeModules", s);
                show("upgradesDiv");
            }
            else
                alert("<m2m2:translate key="modules.versionCheck.none" escapeDQuotes="true"/>");
        });
    }
    
    function downloadUpgrades() {
        ModulesDwr.startDownloads(versionUpgradeList, function() { downloadMonitor(); });
    }
    
    function downloadMonitor() {
        ModulesDwr.monitorDownloads(function(results) {
            for (var i=0; i<results.data.results.length; i++)
                $set(results.data.results[i].key +"downloadResult", results.data.results[i].value);
                
            if (results.data.finished) {
                hide("upgradeModulesButtons");
                show("upgradeModulesFinished");
            }
            else
                setTimeout(downloadMonitor, 1000);
        });
    }
    
    function storeCheck() {
        if (window.location.href.indexOf('store') != -1) {
            $("goToStore").click();
            setTimeout("history.back()", 2000);
        }
    }
    
    window.onload = function() {
        storeCheck();
    };
  </script>
  
  <div>
    <tag:img png="puzzle" title="modules.modules"/>
    <span class="smallTitle"><fmt:message key="modules.modules"/></span>
  </div>
  
  <div id="guid">
    <form action="<c:out value='<%= Common.envProps.getString("store.url") %>'/>/account/store" method="post" target="_blank">
      <fmt:message key="modules.guid"/> <b>${guid}</b>
      <textarea rows="2" cols="80" style="display:none;" name="orderJson">${json}</textarea>
      <input id="goToStore" type="submit" value="<fmt:message key="modules.update"/>" style="margin-left:20px;"/>
      <input type="button" value="<m2m2:translate key="modules.restart"/>" onclick="restartInstance();"/>
<%--       <input type="button" value="<m2m2:translate key="modules.versionCheck"/>" onclick="versionCheck();"/> --%>
    </form>
  </div>
  
  <div id="upgradesDiv" class="borderDiv" style="display:none; margin: 10px 100px 0px 100px; padding: 10px;">
    <p><b><m2m2:translate key="modules.versionCheck.some"/></b><p>
    <div id="upgradeModules" style="margin:20px;"></div>
    <div id="upgradeModulesButtons">
      <input type="button" value="<fmt:message key="modules.downloadUpgrades"/>" onclick="downloadUpgrades()"/>
      <input type="button" value="<fmt:message key="modules.upgradesClose"/>" onclick="hide('upgradesDiv')"/>
    </div>
    <div id="upgradeModulesFinished" style="display:none;">
      <b><fmt:message key="modules.downloadsFinished"/></b>
      <input type="button" value="<m2m2:translate key="modules.restart"/>" style="margin-left:20px;" onclick="restartInstance();"/>
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
        
        <span class="version">${module.version}</span>
               
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