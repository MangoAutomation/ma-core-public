<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@tag import="com.serotonin.m2m2.Common"%>
<%@include file="/WEB-INF/tags/decl.tagf"%>
<%@attribute name="pointHelpId" %>
    <div id="pointDetails" class="borderDiv marB" style="display:none" >
    <div id="pointProperties" style="display:none"></div> <!-- For tricking the legacy modules to believe this is still in use, it will be "shown" when a data source is saved or viewed -->    
        <table class="wide">
          <tr>
            <td>
              <span class="smallTitle"><fmt:message key="dsEdit.points.details"/></span>
              <c:if test="${!empty pointHelpId}"><tag:help id="${pointHelpId}"/></c:if>
            </td>
            <td align="right">
              <tag:img id="toggleDataPoint" png="icon_ds" onclick="togglePoint()" style="display:none" />
              <tag:img id="pointSaveImg" png="save" onclick="savePoint()" title="common.save"/>
              <tag:img id="pointDeleteImg" png="delete" onclick="deletePoint()" title="common.delete" />
              <tag:img png="emport" title="emport.export" onclick="exportDataPoint()"/>
              <tag:img png="cross" title="common.close" onclick="closePoint()"/>
            </td>
          </tr>
        </table>
        <div id="pointMessage" class="ctxmsg formError"></div>
        
        <table>
          <tr>
            <td class="formLabelRequired"><fmt:message key="dsEdit.deviceName"/></td>
            <td class="formField"><input id="deviceName" /></td>
          </tr>
          <tr>
            <td class="formLabelRequired"><fmt:message key="dsEdit.points.name"/></td>
            <td class="formField"><input type="text" id="name" /></td>
          </tr>
          <tr>
            <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
            <td class="formField"><input type="text" id="xid" /></td>
          </tr>
          <tr>
            <td class="formLabel"><fmt:message key="pointEdit.props.permission.read"/></td>
            <td class="formField">
              <input type="text" id="readPermission" class="formLong"/>
              <tag:img png="bullet_down" onclick="permissionUI.viewPermissions('readPermission')"/>
            </td>
          </tr>
          <tr>
            <td class="formLabel"><fmt:message key="pointEdit.props.permission.set"/></td>
            <td class="formField">
              <input type="text" id="setPermission" class="formLong"/>
              <tag:img png="bullet_down" onclick="permissionUI.viewPermissions('setPermission')"/>
            </td>
          </tr>
          <jsp:doBody/>
        </table>
        <div id="extraPointSettings">
        <hr class="styled-hr"></hr>
        <jsp:include page="/WEB-INF/snippet/view/dataPoint/dataPointTemplate.jsp"/>
        <hr class="styled-hr"></hr>
        <jsp:include page="/WEB-INF/snippet/view/dataPoint/pointProperties.jsp" />
        <hr class="styled-hr"></hr>
        <jsp:include page="/WEB-INF/snippet/view/dataPoint/loggingProperties.jsp" />
        <hr class="styled-hr"></hr>
        <jsp:include page="/WEB-INF/snippet/view/dataPoint/valuePurge.jsp" />
        <hr class="styled-hr"></hr>
        <jsp:include page="/WEB-INF/snippet/view/dataPoint/textRenderer.jsp" />
        <hr class="styled-hr"></hr>
        <jsp:include page="/WEB-INF/snippet/view/dataPoint/chartRenderer.jsp" />
        <hr class="styled-hr"></hr>
        <jsp:include page="/WEB-INF/snippet/view/dataPoint/eventDetectors.jsp" />
        </div>
      </div>