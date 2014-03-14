<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@tag body-content="empty"%><%--
--%><%@attribute name="xid" required="true"%><%--
--%><%@attribute name="color"%><%--
--%><sst:map var="pointMap"><%--
  --%><sst:mapEntry key="xid" value="${xid}"/><%--
  --%><sst:mapEntry key="color" value="${color}"/><%--
--%></sst:map><%--
--%><sst:listEntry listVar="chartPointList" value="${pointMap}"/>