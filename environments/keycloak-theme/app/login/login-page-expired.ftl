<#--
  Sessão de autenticação expirada.

  Os dois links são DIFERENTES e fáceis de confundir — manter ambos:
    - url.loginRestartFlowUrl → reinicia o fluxo do zero.
    - url.loginAction         → tenta continuar de onde parou.
-->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("pageExpiredTitle")}
    <#elseif section = "form">
        <p id="instruction1" class="instruction app-instruction">
            ${msg("pageExpiredMsg1")} <a id="loginRestartLink" href="${url.loginRestartFlowUrl}">${msg("doClickHere")}</a>.<br/>
            ${msg("pageExpiredMsg2")} <a id="loginContinueLink" href="${url.loginAction}">${msg("doClickHere")}</a>.
        </p>
    </#if>
</@layout.registrationLayout>
