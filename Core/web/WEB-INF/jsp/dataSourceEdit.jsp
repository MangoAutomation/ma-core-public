<%--
    Edit Data Source View - Same as data sources except this loads up the editing source.

    Copyright (C) 2013 Infinite Automation. All rights reserved.
    @author Terry Packer
--%>

<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<jsp:include page="/WEB-INF/jsp/dataSource.jsp"/>

<script type="text/javascript">
<!--
<c:if test="${!empty dataSource}">
dojo.ready(function(){
    dataSources.open(${dataSource.id});    
});
</c:if>
//-->
</script>