<%--
    Copyright (C) 2015 Infinite Automation. All rights reserved.
    @author Terry Packer
    This page exists to hold any tags that are only used in modules
    and thus wouldn't not get pre-compiled during a build
    
--%><%@ include file="/WEB-INF/jsp/include/tech.jsp" %><%--
--%><tag:html5>

<jsp:attribute name="styles">
    <link rel="stylesheet" href="/modules/dataPointDetailsView/web/css/dataPointDetails.css">
</jsp:attribute>

<jsp:attribute name="scripts">
    <script type="text/javascript" src="/modules/dataPointDetailsView/web/js/dataPointDetails.js"></script>
</jsp:attribute>
<jsp:body>
  <tag:scriptPermissions></tag:scriptPermissions>
</jsp:body>
</tag:html5>