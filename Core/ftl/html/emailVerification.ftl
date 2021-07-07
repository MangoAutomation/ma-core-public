<#--
    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
    @author Terry Packer
-->
<#include "include/header.ftl">

<div><span class="copySmall"><@fmt key="ftl.automatedEmail"/></span></div>

<p><@fmt key="ftl.emailVerification.body"/></p>

<p><a href="${verificationUri}"><@fmt key="ftl.emailVerification.confirmYourEmail"/></a></p>

<p>
<span><@fmt key="ftl.emailVerification.verificationToken"/></span>
<pre>${token}</pre>
</p>

<p>Confirmation link and token will expire at ${expiration?datetime} ${expiration?string["z"]}.</p>

<#include "include/footer.ftl">
