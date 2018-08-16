<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%--
!!!!
!!!! J.W. Note: This file must result in a 0 byte response or the redirect in index.jsp will result in a org.eclipse.jetty.io.EofException
!!!! being thrown which closes the TCP connection. The JSP comments at the start and end of each line prevent CR/LF being written to the response.
!!!!
--%><%@ page contentType="text/html;charset=UTF-8" language="java" %><%--
--%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%--
--%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><%--
--%><%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><%--
--%><%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %><%--
--%><%@ taglib prefix="m2m2" uri="/WEB-INF/m2m2.tld" %><%--
--%><%@ taglib prefix="sst" uri="http://www.serotoninsoftware.com/tags" %><%--
--%><%@ taglib prefix="tag" tagdir="/WEB-INF/tags" %><%--
--%><%@ taglib prefix="html5" tagdir="/WEB-INF/tags/html5" %>