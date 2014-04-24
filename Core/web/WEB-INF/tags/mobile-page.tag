<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<%@tag import="com.serotonin.m2m2.module.ModuleRegistry"%>
<%@tag import="com.serotonin.m2m2.Common"%>
<%@include file="/WEB-INF/tags/decl.tagf"%>
<%@ taglib prefix="page" tagdir="/WEB-INF/tags/page" %>
<%@attribute name="styles" fragment="true" %>
<%@attribute name="dwr" rtexprvalue="true" %>
<%@attribute name="js" %>
<%@attribute name="onload" %>

<c:set var="theme">claro</c:set>
<%-- <c:set var="theme">nihilo</c:set> --%>
<%-- <c:set var="theme">soria</c:set> --%>
<%-- <c:set var="theme">tundra</c:set> --%>

<html>
<head>
  <title><c:choose>
    <c:when test="${!empty instanceDescription}">${instanceDescription}</c:when>
    <c:otherwise><fmt:message key="header.title"/></c:otherwise>
  </c:choose></title>
  
  <!-- Meta -->
  <meta http-equiv="content-type" content="application/xhtml+xml;charset=utf-8"/>
  <meta http-equiv="Content-Style-Type" content="text/css" />
  <meta name="Copyright" content="&copy;2006-2011 Serotonin Software Technologies Inc."/>
  <meta name="DESCRIPTION" content="Mango Automation from Infinite Automation Systems"/>
  <meta name="KEYWORDS" content="Mango Automation from Infinite Automation Systems"/>
  <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,minimum-scale=1,user-scalable=no"/>
  <meta name="apple-mobile-web-app-capable" content="yes" />
  
  
  <c:if test="${empty dojoURI}">
	<c:set var="dojoURI">/resources/</c:set>
  </c:if>
  
  <!-- Style -->
  <link rel="icon" href="<%= Common.applicationFavicon %>"/>
  <link rel="shortcut icon" href="<%= Common.applicationFavicon %>"/>
 
  <style type="text/css">
    @import "${dojoURI}/dojox/mobile/themes/iphone/iphone.css" rel="stylesheet";
    html, body{
        height: 100%;
        overflow: hidden;
    }
  </style>  
  <link href="/resources/common.css" type="text/css" rel="stylesheet"/>
  
  <c:forEach items="<%= Common.moduleStyles %>" var="modStyle">
    <link href="/${modStyle}" type="text/css" rel="stylesheet"/></c:forEach>
  <jsp:invoke fragment="styles"/>
  
  <!-- Scripts -->
  <script type="text/javascript" src="${dojoURI}dojo/dojo.js" data-dojo-config="async: false, parseOnLoad: true, isDebug:true, extraLocale: ['${lang}']"></script>
  <tag:versionedJavascript  src="/dwr/engine.js" />
  <tag:versionedJavascript  src="/dwr/util.js" />
  <tag:versionedJavascript  src="/dwr/interface/MiscDwr.js" />
  <tag:versionedJavascript  src="/resources/soundmanager2-nodebug-jsmin.js" />
  <tag:versionedJavascript  src="/resources/common.js" />

  <c:forEach items="${dwr}" var="dwrname">
    <tag:versionedJavascript  src="/dwr/interface/${dwrname}.js" /></c:forEach>
  <c:forEach items="${js}" var="jspath">
    <tag:versionedJavascript  src="${jspath}" /></c:forEach>
  <script type="text/javascript">
    mango.i18n = <sst:convert obj="${clientSideMessages}"/>;
  </script>

   <tag:versionedJavascript  src="/resources/header.js" />
   <script type="text/javascript">
     
     //Load in the Mobile Libraries
	// Load the widget parser and mobile base
	require(["dojox/mobile/parser", "dojox/mobile/deviceTheme", "dojox/mobile/compat", "dojox/mobile"]);
	     
     dwr.util.setEscapeHtml(false);
     <c:if test="${!empty sessionUser}">
       dojo.ready(mango.header.onLoad);
       dojo.ready(function() { setUserMuted(${sessionUser.muted}); });
     </c:if>
     
     function setLocale(locale) {
         MiscDwr.setLocale(locale, function() { window.location = window.location });
     }
     
     function goHomeUrl() {
         MiscDwr.getHomeUrl(function(loc) { window.location = loc; });
     }
     
     function setHomeUrl() {
         MiscDwr.setHomeUrl(window.location.href, function() { alert("<fmt:message key="header.homeUrlSaved"/>"); });
     }
     
     function deleteHomeUrl() {
         MiscDwr.deleteHomeUrl(function() { alert("<fmt:message key="header.homeUrlDeleted"/>"); });
     }
   </script>

  <c:forEach items="<%= Common.moduleScripts %>" var="modScript">
    <script type="text/javascript" src="/${modScript}"></script></c:forEach>
</head>

<body>
<div dojoType="dojox.mobile.Container">
                
      <page:mobile-header/>
      <page:mobile-toolbar/>
      <jsp:doBody/>
      <table width="100%" cellspacing="0" cellpadding="0" border="0">
        <tr><td colspan="2">&nbsp;</td></tr>
        <tr>
          <td colspan="2" class="footer" align="center">&copy;2006-2012 Serotonin Software Technologies Inc., <fmt:message key="footer.rightsReserved"/></td>
        </tr>
        <tr>
          <td colspan="2" align="center"><a href="http://infiniteautomation.com/" ><b></b>Distributed by Infinite Automation Systems Inc.</a></td>
        </tr>
      </table>
</div>

<jsp:include page="/WEB-INF/snippet/errorBox.jsp"/>

<c:if test="${!empty onload}">
  <script type="text/javascript">dojo.ready(${onload});</script>
</c:if>

<c:forEach items="<%= Common.moduleJspfs %>" var="modJspf">
  <jsp:include page="${modJspf}" /></c:forEach>

<!-- Messaging and Error/Export/Copy Views -->
  <jsp:include page="/WEB-INF/snippet/message.jsp"/>

</body>
</html>
