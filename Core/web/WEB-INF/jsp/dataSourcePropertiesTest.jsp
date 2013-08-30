<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<c:set var="dwrClasses">DataSourceDwr,DataPointDwr</c:set>
<c:if test="${!empty dataSource.definition.dwrClass}">
  <c:set var="dwrClasses">${dwrClasses},${dataSource.definition.dwrClass.simpleName}</c:set>
</c:if>
<tag:page dwr="${dwrClasses}" onload="init">
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
  
  <script type="text/javascript">

//       //Collect the table js interface
//       var dojoConfig = {packages:[{name: "deltamation", location: "/resources/deltamation"}]};
      
//       //Load up our ds
//       function init(){
//     	    initProperties(${dataSource.id},${dataSource.enabled});
//       }
      
      
     </script>
  </jsp:attribute>
  
  <jsp:body>
       
    <jsp:include page="/WEB-INF/jsp/dataSourceProperties.jsp"/>
    
  </jsp:body>
</tag:page>