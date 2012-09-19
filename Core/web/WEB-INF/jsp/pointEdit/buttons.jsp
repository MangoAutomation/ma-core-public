<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.web.mvc.controller.DataPointEditController"%>
<table>
  <tr>
    <td colspan="2" align="center">
      <input type="submit" value="<fmt:message key="common.save"/>"
              onclick="return doSave('<%= DataPointEditController.SUBMIT_SAVE %>');"/>
      <c:choose>
        <c:when test="${form.enabled}">
          <input type="submit" value="<fmt:message key="pointEdit.buttons.disable"/>"
                  onclick="return doSave('<%= DataPointEditController.SUBMIT_DISABLE %>');"/>
          <input type="submit" value="<fmt:message key="pointEdit.buttons.restart"/>" 
                  onclick="return doSave('<%= DataPointEditController.SUBMIT_RESTART %>');"/>
        </c:when>
        <c:otherwise>
          <input type="submit" value="<fmt:message key="pointEdit.buttons.enable"/>"
                  onclick="return doSave('<%= DataPointEditController.SUBMIT_ENABLE %>');"/>
        </c:otherwise>
      </c:choose>
      
      <input type="button" value="<fmt:message key="common.cancel"/>"
              onclick="window.location='data_point_details.shtm?dpid=${form.id}';"/>
    </td>
    <td></td>
  </tr>
  
  <tr>
    <td colspan="2"><fmt:message key="pointEdit.buttons.note"/></td>
  </tr>
</table>