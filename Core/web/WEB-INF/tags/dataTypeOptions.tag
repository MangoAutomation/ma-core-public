<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@tag import="com.serotonin.m2m2.DataTypes"%><%--
--%><%@include file="/WEB-INF/tags/decl.tagf" %><%--
--%><%@tag body-content="empty" %><%--
--%><%@attribute name="id" %><%--
--%><%@attribute name="name" %><%--
--%><%@attribute name="value" %><%--
--%><%@attribute name="onchange" %><%--
--%><%@attribute name="excludeBinary" type="java.lang.Boolean" %><%--
--%><%@attribute name="excludeMultistate" type="java.lang.Boolean" %><%--
--%><%@attribute name="excludeNumeric" type="java.lang.Boolean" %><%--
--%><%@attribute name="excludeAlphanumeric" type="java.lang.Boolean" %><%--
--%><%@attribute name="excludeImage" type="java.lang.Boolean" %><%--
--%><select<%--
--%><c:if test="${!empty id}"> id="${id}"</c:if><%--
--%><c:if test="${!empty name}"> name="${name}"</c:if><%--
--%><c:if test="${!empty value}"> value="${value}"</c:if><%--
--%><c:if test="${!empty onchange}"> onchange="${onchange}"</c:if>>
  <c:if test="${!excludeBinary}"><option value="<%= DataTypes.BINARY %>"><fmt:message key="common.dataTypes.binary"/></option></c:if>
  <c:if test="${!excludeMultistate}"><option value="<%= DataTypes.MULTISTATE %>"><fmt:message key="common.dataTypes.multistate"/></option></c:if>
  <c:if test="${!excludeNumeric}"><option value="<%= DataTypes.NUMERIC %>"><fmt:message key="common.dataTypes.numeric"/></option></c:if>
  <c:if test="${!excludeAlphanumeric}"><option value="<%= DataTypes.ALPHANUMERIC %>"><fmt:message key="common.dataTypes.alphanumeric"/></option></c:if>
  <c:if test="${!excludeImage}"><option value="<%= DataTypes.IMAGE %>"><fmt:message key="common.dataTypes.image"/></option></c:if>
</select>