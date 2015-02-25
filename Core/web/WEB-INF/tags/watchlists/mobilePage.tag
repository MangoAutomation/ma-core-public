<!DOCTYPE html PUBLIC "-//WAPFORUM//DTD XHTML Mobile 1.2//EN" "http://www.openmobilealliance.org/tech/DTD/xhtml-mobile12.dtd">
<%@tag import="com.serotonin.m2m2.module.ModuleRegistry"%>
<%@tag import="com.serotonin.m2m2.Common"%>
<%@include file="/WEB-INF/tags/decl.tagf"%>
<%@attribute name="styles" fragment="true" %>
<%@attribute name="dwr" rtexprvalue="true" %>
<%@attribute name="js" %>
<%@attribute name="onload" %>

<html>
<head>
  <title><fmt:message key="header.title"/></title>
  <meta http-equiv="content-type" content="application/xhtml+xml;charset=utf-8"/>
  <link href="${modulePath}/web/mobile/mobile.css" type="text/css" rel="stylesheet"/>
  
    <c:if test="${empty dojoURI}">
    <c:set var="dojoURI">http://ajax.googleapis.com/ajax/libs/dojo/1.9.1/</c:set>
  </c:if>
  
  <!-- Scripts -->
  <script type="text/javascript" src="${dojoURI}dojo/dojo.js" data-dojo-config="async: false, parseOnLoad: true, isDebug:true, extraLocale: ['${lang}']"></script>
  <tag:versionedJavascript  src="/dwr/engine.js" />
  <tag:versionedJavascript  src="/dwr/util.js" />
  <tag:versionedJavascript  src="/dwr/interface/MiscDwr.js" />
  <tag:versionedJavascript  src="/resources/common.js" />
  <tag:versionedJavascript  src="${modulePath}/web/mobile/mobile-header.js" />
    
  <c:forEach items="${dwr}" var="dwrname"><tag:versionedJavascript  src="/dwr/interface/${dwrname}.js" />
  </c:forEach>

  <c:forEach items="${js}" var="jspath"><tag:versionedJavascript  src="${jspath}" />
  </c:forEach>

  <script type="text/javascript">mango.i18n = <sst:convert obj="${clientSideMessages}"/>;
  </script>
  
  <c:forEach items="<%= Common.moduleScripts %>" var="modScript"><tag:versionedJavascript  src="/${modScript}" />
  </c:forEach>
  
  <!-- Setup the Header and Start the Longpoll -->
  <script type="text/javascript">dojo.ready(mango.header.onLoad);</script>
  
</head>

<body>
<!-- Header Area, must exist for long poll to update alarms -->
 <div style="width:100%;background-color:white;text-align:center;margin:20px 0 60px;">
 <img style="width:150px;" src="/images/logo.png">
  <a href="/events.shtm">
    <span id="__header__alarmLevelDiv" style="display:none;">
      <img id="__header__alarmLevelImg" src="/images/spacer.gif" alt="" title=""/>
      <span id="__header__alarmLevelText"></span>
    </span>
  </a>
 </div>
<!-- End Header area -->

<jsp:doBody/>

<!-- Footer Area -->
<table width="100%" cellspacing="0" cellpadding="0" border="0">
  <tr>
    <td class="footer" align="center">&copy;2013 Infinite Automation <fmt:message key="footer.rightsReserved"/></td>
  </tr>
</table>
  <c:if test="${!empty onload}">
  	<script type="text/javascript">dojo.ready(${onload});</script>
  </c:if>

<!-- End Footer Area --> 
</body>
</html>