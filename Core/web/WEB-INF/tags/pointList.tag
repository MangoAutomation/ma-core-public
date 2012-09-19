<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@tag import="com.serotonin.m2m2.Common"%>
<%@include file="/WEB-INF/tags/decl.tagf"%>
<%@attribute name="pointHelpId" %>

<table cellpadding="0" cellspacing="0" id="pointProperties" style="display:none;">
  <tr>
    <td valign="top">
      <div class="borderDiv marR marB">
        <table width="100%">
          <tr>
            <td class="smallTitle"><fmt:message key="dsEdit.points.points"/></td>
            <td align="right">
              <tag:img id="editImg${applicationScope['constants.Common.NEW_ID']}" png="icon_comp_add"
                      onclick="editPoint(${applicationScope['constants.Common.NEW_ID']})" />
            </td>
          </tr>
        </table>
        <table cellspacing="1">
          <tr class="rowHeader" id="pointListHeaders"></tr>
          <tbody id="pointsList"></tbody>
        </table>
      </div>
    </td>

    <td valign="top">
      <div id="pointDetails" class="borderDiv marB" style="display: none;">
        <table width="100%">
          <tr>
            <td>
              <span class="smallTitle"><fmt:message key="dsEdit.points.details"/></span>
              <c:if test="${!empty pointHelpId}"><tag:help id="${pointHelpId}"/></c:if>
            </td>
            <td align="right">
              <tag:img id="pointSaveImg" png="save" onclick="savePoint()" title="common.save"/>
              <tag:img id="pointDeleteImg" png="delete" onclick="deletePoint()" title="common.delete" />
              <tag:img png="emport" title="emport.export" onclick="exportDataPoint()"/>
            </td>
          </tr>
        </table>
        <div id="pointMessage" class="ctxmsg formError"></div>
        
        <table>
          <tr>
            <td class="formLabelRequired"><fmt:message key="dsEdit.points.name"/></td>
            <td class="formField"><input type="text" id="name"/></td>
          </tr>
          <tr>
            <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
            <td class="formField"><input type="text" id="xid"/></td>
          </tr>
          
          <jsp:doBody/>
          
        </table>
      </div>
    </td>
  </tr>
</table>