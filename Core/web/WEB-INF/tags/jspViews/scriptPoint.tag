<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@taglib prefix="jviews" uri="/modules/jspViews/web/jviews.tld" %><%--
--%><%@attribute name="xid" required="true"%><%--
--%><%@attribute name="raw" type="java.lang.Boolean"%><%--
--%><%@attribute name="disabledValue"%><%--
--%><%@attribute name="time" type="java.lang.Boolean"%><%--
--%><jviews:simplePoint xid="${xid}" raw="${raw}" disabledValue="${disabledValue}" time="${time}"/>
<script type="text/javascript">
  mango.view.jsp.functions["c${componentId}"] = function(value, time) {
    <jsp:doBody/>
  }
</script>