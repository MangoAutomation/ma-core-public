<%--
    Copyright (C) 2015 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<script type="text/javascript">
	//Include a modified version of the code from dataSourceProperties.js
</script>

<tag:labelledSection labelKey="systemSettings.dataPointTemplateManagement" closed="true">
  
  <%--These will get filled by a template drop down with the option to update or create new template --%>
  <hr class="styled-hr"></hr>
  <jsp:include page="/WEB-INF/snippet/view/dataPoint/pointProperties.jsp" />
  <hr class="styled-hr"></hr>
  <jsp:include page="/WEB-INF/snippet/view/dataPoint/loggingProperties.jsp" />
  <hr class="styled-hr"></hr>
  <jsp:include page="/WEB-INF/snippet/view/dataPoint/valuePurge.jsp" />
  <hr class="styled-hr"></hr>
  <jsp:include page="/WEB-INF/snippet/view/dataPoint/textRenderer.jsp" />
  <hr class="styled-hr"></hr>
  <jsp:include page="/WEB-INF/snippet/view/dataPoint/chartRenderer.jsp" />
</tag:labelledSection>
