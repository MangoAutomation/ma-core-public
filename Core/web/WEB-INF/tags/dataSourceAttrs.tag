<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf" %><%--
--%><%@attribute name="descriptionKey" required="true" rtexprvalue="true" %><%--
--%><%@attribute name="helpId" rtexprvalue="true" %><%--
--%><%@attribute name="extraPanels" fragment="true" %><%--
--%><table cellpadding="0" cellspacing="0">
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
          <tr>
            <td class="formLabelRequired"><fmt:message key="dsEdit.head.name"/></td>
            <td class="formField"><input type="text" id="dataSourceName" value="${dataSource.name}"/></td>
          </tr>
          <tr>
            <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
            <td class="formField"><input type="text" id="dataSourceXid" value="${dataSource.xid}"/></td>
          </tr>
          <tr>
            <td class="formLabel"><fmt:message key="dsEdit.logging.purge"/></td>
            <td class="formField">
              <div>
                <sst:checkbox id="dataSourcePurgeOverride" selectedValue="${dataSource.purgeOverride}" onclick="changePurgeOverride()"/>
                <label for="dataSourcePurgeOverride"><fmt:message key="dsEdit.logging.purgeOverride"/></label>
              </div>
              <div>
                <fmt:message key="pointEdit.logging.after"/>
                <input type="text" id="dataSourcePurgePeriod" value="${dataSource.purgePeriod}" class="formShort"/>
                <tag:timePeriods id="dataSourcePurgeType" value="${dataSource.purgeType}" d="true" w="true" mon="true" y="true"/>
              </div>
            </td>
          </tr>          
          <jsp:doBody/>
          
        </table>
        
        <tag:dsEvents/>
      </div>
    </td>
    
    <c:if test="${!empty extraPanels}">
      <jsp:invoke fragment="extraPanels"/>
    </c:if>
  </tr>
</table>