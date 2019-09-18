<#ftl strip_whitespace=false><#--
    Copyright (C) 2019 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
--><@fmt key="ftl.automatedEmail"/>

<@fmt key="ftl.emailVerification.body"/>

${verificationUri}

<@fmt key="ftl.emailVerification.verificationToken"/>

${token}

Confirmation link and token will expire at ${expiration?datetime} ${expiration?string["z"]}.

<@fmt key="ftl.htmlFooter.mango"/>
