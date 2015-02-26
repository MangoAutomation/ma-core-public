<%--
    Copyright (C) 2013 Infinite Automation. All rights reserved.
    
    This is a fragment page to be loaded via servlet call to
    DataPointEditController
    
    
    
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>


<%@ include file="/WEB-INF/snippet/view/dataPoint/dataPointTemplate.jsp" %>
<%@ include file="/WEB-INF/snippet/view/dataPoint/pointProperties.jsp" %>
<%@ include file="/WEB-INF/snippet/view/dataPoint/loggingProperties.jsp" %>
<%@ include file="/WEB-INF/snippet/view/dataPoint/valuePurge.jsp" %>
<%@ include file="/WEB-INF/snippet/view/dataPoint/textRenderer.jsp" %>
<%@ include file="/WEB-INF/snippet/view/dataPoint/chartRenderer.jsp" %>
<%@ include file="/WEB-INF/snippet/view/dataPoint/eventDetectors.jsp" %>


<script type="text/javascript">
    function initDataPointSettings(){
        initPointProperties();    	
    };

</script>