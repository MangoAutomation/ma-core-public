<%--
    Edit Data Point View, same as data sources page but loads focused on a data point for editing.
    
    Copyright (C) 2013 Infinite Automation. All rights reserved.
    @author Terry Packer
--%>

<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<jsp:include page="/WEB-INF/jsp/dataSource.jsp"/>

<script type="text/javascript">
<!--
<c:if test="${!empty dataPoint}">
dojo.ready(function(){
    allDataPoints.open(${dataPoint.id});    
});
</c:if>
//-->
</script>