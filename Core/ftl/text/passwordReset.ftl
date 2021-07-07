<#ftl strip_whitespace=false><#--
    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
    @author Matthew Lohbihler
--><@fmt key="ftl.automatedEmail"/>

<@fmt message=message/>

<@fmt key="ftl.passwordReset.body"/>

${resetUri}

<@fmt key="ftl.passwordReset.passwordResetToken"/>

${token}

Reset link and token will expire at ${expiration?datetime} ${expiration?string["z"]}.

<@fmt key="ftl.htmlFooter.mango"/>
