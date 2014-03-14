<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@ taglib prefix="views" tagdir="/WEB-INF/tags/graphicalViews" %><%--
--%><%@tag body-content="empty"%><%--
--%><%@attribute name="view" type="com.serotonin.m2m2.gviews.GraphicalView" required="true" rtexprvalue="true"%><%--
--%><%@attribute name="emptyMessageKey" required="true"%>
<div id="viewContent">
  <c:choose>
    <c:when test="${empty view}"><fmt:message key="${emptyMessageKey}"/></c:when>
    <c:when test="${empty view.backgroundFilename}">
      <img id="viewBackground" src="images/spacer.gif" alt="" width="740" height="500"/>
    </c:when>
    <c:otherwise>
      <img id="viewBackground" src="${view.backgroundFilename}" alt=""/>
    </c:otherwise>
  </c:choose>
  
  <c:forEach items="${view.viewComponents}" var="vc">
    <!-- vc ${vc.id} -->
    <c:choose>
      <c:when test="${!vc.visible}"><!-- vc ${vc.id} not visible --></c:when>
      
      <c:when test="${vc.defName == 'simpleCompound'}">
        <div id="c${vc.id}" style="position:absolute;left:${vc.x}px;top:${vc.y}px;"
                  onmouseover="vcOver('c${vc.id}', 5);" onmouseout="vcOut('c${vc.id}');">
          <views:pointComponent vc="${vc.leadComponent}"/>
          <c:choose>
            <c:when test="${empty vc.backgroundColour}"><c:set var="bkgd"></c:set></c:when>
            <c:otherwise><c:set var="bkgd">background:${vc.backgroundColour};</c:set></c:otherwise>
          </c:choose>
          <div id="c${vc.id}Controls" class="controlContent" style="left:5px;top:5px;${bkgd}">
            <b>${vc.name}</b><br/>
            <c:forEach items="${vc.childComponents}" var="child">
              <c:if test="${child.viewComponent.visible && child.viewComponent.id != vc.leadComponent.id}">
                <views:pointComponent vc="${child.viewComponent}"/>
              </c:if>
            </c:forEach>
          </div>
        </div>
      </c:when>
      
      <c:when test="${vc.defName == 'imageChart'}">
        <div id="c${vc.id}" style="position:absolute;left:${vc.x}px;top:${vc.y}px;"
                  onmouseover="vcOver('c${vc.id}', 10);" onmouseout="vcOut('c${vc.id}');">
          <div id="c${vc.id}Content"><img src="images/icon_comp.png" alt=""/></div>
          <div id="c${vc.id}Controls" class="controlContent">
            <div id="c${vc.id}Info">
              <tag:img png="hourglass" title="common.gettingData"/>
            </div>
          </div>
        </div>
      </c:when>
      
      <c:when test="${vc.compoundComponent}">
        <div id="c${vc.id}" style="position:absolute;left:${vc.x}px;top:${vc.y}px;"
                  onmouseover="vcOver('c${vc.id}', 5);" onmouseout="vcOut('c${vc.id}');">
          ${vc.staticContent}
          <div id="c${vc.id}Controls" class="controlsDiv">
            <table cellpadding="0" cellspacing="1">
              <tr onmouseover="showMenu('c${vc.id}Info', 16, 0);" onmouseout="hideLayer('c${vc.id}Info');"><td>
                <tag:img png="information"/>
                <div id="c${vc.id}Info" onmouseout="hideLayer(this);">
                  <tag:img png="hourglass" title="common.gettingData"/>
                </div>
              </td></tr>
              <c:if test="${vc.displayImageChart}">
                <tr onmouseover="mango.view.showChart('${vc.id}', event, this);" 
                        onmouseout="mango.view.hideChart('${vc.id}', event, this);"><td>
                  <img src="images/icon_chart.png" alt=""/>
                  <div id="c${vc.id}ChartLayer"></div>
                  <textarea style="display:none;" id="c${vc.id}Chart"><tag:img png="hourglass" title="common.gettingData"/></textarea>
                </td></tr>
              </c:if>
            </table>
          </div>
          <c:forEach items="${vc.childComponents}" var="child">
            <c:if test="${child.viewComponent.visible}"><views:pointComponent vc="${child.viewComponent}"/></c:if>
          </c:forEach>
        </div>
      </c:when>
      
      <c:otherwise><views:pointComponent vc="${vc}"/></c:otherwise>
    </c:choose>
  </c:forEach>
</div>