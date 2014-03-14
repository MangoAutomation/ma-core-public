<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@taglib prefix="jviews" uri="/modules/jspViews/web/jviews.tld" %><%--
--%><%@attribute name="duration" type="java.lang.Integer" required="true"%><%--
--%><%@attribute name="durationType" required="true"%><%--
--%><%@attribute name="width" type="java.lang.Integer" required="true"%><%--
--%><%@attribute name="height" type="java.lang.Integer" required="true"%><%--
--%><sst:list var="chartPointList"/><%--
--%><jsp:doBody/><%--
--%><jviews:chart duration="${duration}" durationType="${durationType}" width="${width}" height="${height}"><%--
  --%><c:forEach items="${chartPointList}" var="chartPoint"><%--
    --%><jviews:chartPoint xid="${chartPoint.xid}" color="${chartPoint.color}"/><%--
  --%></c:forEach><%--
--%></jviews:chart><%--
--%><img id="c${componentId}" src="images/hourglass.png"/><%--
--%><script type="text/javascript">
  mango.view.jsp.functions["c${componentId}"] = function(value) { $("c${componentId}").src = value; }
</script>