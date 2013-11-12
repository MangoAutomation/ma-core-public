<%--

    Page to test js on Data Point Settings View

    Copyright (C) 2013 Infinite Automation. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<c:set var="dwrClasses">DataPointDwr,DataPointEditDwr</c:set>

<tag:page dwr="${dwrClasses}" onload="initDataPointSettings">
  <jsp:attribute name="styles">
  <style type="text/css">
    .mangoForm ul { margin: 0; padding: 0; }
    .mangoForm ul li { margin-bottom: 5px; list-style: none; }
    .mangoForm label { width: 100px; text-align: right; padding-right: 10px; display: inline-block; }
    .mangoForm label.required { font-weight: bold; }
    
    <link href="${dojoURI}dgrid/css/dgrid.css" type="text/css" rel="stylesheet"/>
    <link href="${dojoURI}dojox/form/resources/UploaderFileList.css" type="text/css" rel="stylesheet"/>
    <link href="${dojoURI}dojox/form/resources/Rating.css" type="text/css" rel="stylesheet"/>
  </style>
  
  <script type="text/javascript"></script>
  </jsp:attribute>
  
  <jsp:body>
       
  <jsp:include page="/WEB-INF/snippet/view/dataPoint/dataPointSettings.jsp"/>
    
  </jsp:body>
</tag:page>