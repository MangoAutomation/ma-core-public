<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf" %><%--
--%><%@attribute name="descriptionKey" required="true" rtexprvalue="true" %><%--
--%><%@attribute name="helpId" rtexprvalue="true" %><%--
--%><%@attribute name="extraPanels" fragment="true" %><%--
--%>
<!-- <div id="dataSourcePropertiesTab" data-dojo-type="dijit/layout/ContentPane" title="Data Source" data-dojo-props="selected:true" style="overflow-y:auto" > -->
<div id="dataSourcePropertiesTab">
<input type="hidden" id="dataSource.enabled" value="${dataSource.enabled}"/>
<table cellpadding="0" cellspacing="0">
  <tr>
    <td valign="top">
      <div class="borderDiv marB marR" id="dataSourceProperties">
        <table width="100%">
          <tr>
            <td class="smallTitle">
              <tag:img png="icon_ds" title="common.edit"/>
              <fmt:message key="${descriptionKey}"/>
              <c:if test="${!empty helpId}"><tag:help id="${helpId}"/></c:if>
            </td>
            <td align="right">
              <tag:img png="icon_ds" onclick="toggleDataSource()" id="dsStatusImg" style="display:none"/>
              <tag:img id="dsSaveImg" png="save" onclick="saveDataSource()" title="common.save"/>
              <tag:img png="emport" title="emport.export" onclick="exportDataSource()"/>
            </td>
          </tr>
        </table>
        <div id="dataSourceMessage" class="ctxmsg formError"></div>
        <table>
          <c:if test="${copy}">
          <tr id="copyDeviceName">
            <td class="formLabelRequired"><fmt:message key="dsEdit.deviceName"/></td>
            <td class="formField"><input type="text" id="dataSource.deviceName" value="${dataSource.name}"/></td>
          </tr>
          </c:if>
          <tr>
            <td class="formLabelRequired"><fmt:message key="dsEdit.head.name"/></td>
            <td class="formField"><input type="text" id="dataSource.name" value="${dataSource.name}"/></td>
          </tr>
          <tr>
            <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
            <td class="formField"><input type="text" id="dataSource.xid" value="${dataSource.xid}"/></td>
          </tr>
          <tr>
            <td class="formLabel"><fmt:message key="dsEdit.logging.purge"/></td>
            <td class="formField">
              <div>
                <sst:checkbox id="dataSource.purgeOverride" selectedValue="${dataSource.purgeOverride}" onclick="changePurgeOverride()"/>
                <label for="dataSource.purgeOverride"><fmt:message key="dsEdit.logging.purgeOverride"/></label>
              </div>
              <div>
                <fmt:message key="pointEdit.logging.after"/>
                <input type="text" id="dataSource.purgePeriod" value="${dataSource.purgePeriod}" class="formShort"/>
                <tag:timePeriods id="dataSource.purgeType" value="${dataSource.purgeType}" d="true" w="true" mon="true" y="true"/>
              </div>
            </td>
          </tr>          
          <jsp:doBody/>
          
        </table>
        <tag:dsEvents/>
        <tag:purge dwr="DataSourceEditDwr"/>
      </div>
    </td>
    
    <c:if test="${!empty extraPanels}">
      <jsp:invoke fragment="extraPanels"/>
    </c:if>
  </tr>
</table>
</div>