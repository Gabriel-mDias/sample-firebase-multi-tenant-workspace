<#--
  Layout base do tema App — um documento aberto em duas páginas.

  Estrutura: um único cartão (.app-doc) dividido em dois painéis.
    .app-doc__aside — 42%, navy, imagem sob overlay, logo branca, guilhoché,
                        selo carimbado e brasão em marca d'água.
    .app-doc__main  — 58%, off-white, cabeçalho + formulário + faixa de código
                        + rodapé.

  A ILUSTRAÇÃO E AS CORES SÃO DO MOLDE. Troque-as pela identidade do seu
  projeto; a estrutura de painéis não depende delas.

  Este arquivo é cópia adaptada de base/login/template.ftl. A assinatura da macro
  `registrationLayout` e os nomes das seções `<#nested>` foram preservados
  EXATAMENTE, porque as páginas que NÃO sobrescrevemos (login-otp, terms,
  logout-confirm, ...) também invocam esta macro. Alterar o contrato aqui
  quebraria essas páginas herdadas.
-->
<#import "footer.ftl" as loginFooter>
<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}"<#if realm.internationalizationEnabled> lang="${locale.currentLanguageTag}" dir="${(locale.rtl)?then('rtl','ltr')}"</#if>>

<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="theme-color" content="#008C45">

    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <#-- O parent `base` não possui resources/img/, mas referencia este caminho.
         Sem o favicon.ico copiado para resources/img/, toda requisição dá 404. -->
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <script type="importmap">
        {
            "imports": {
                "rfc4648": "${url.resourcesCommonPath}/vendor/rfc4648/rfc4648.js"
            }
        }
    </script>
    <#-- Os JS abaixo vivem em base/login/resources/js/ e resolvem pela cadeia de
         parents — NÃO precisam ser copiados, mas as tags devem permanecer.
         Remover o authChecker.js quebra silenciosamente a detecção de SSO
         entre abas. -->
    <script src="${url.resourcesPath}/js/menu-button-links.js" type="module"></script>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <script type="module">
        import { startSessionPolling } from "${url.resourcesPath}/js/authChecker.js";

        startSessionPolling(
          "${url.ssoLoginInOtherTabsUrl?no_esc}"
        );
    </script>
</head>

<body class="${properties.kcBodyClass!} ${bodyClass}">
<div class="${properties.kcLoginClass!}">
  <div class="${properties.kcFormCardClass!}">

    <#-- ======================= PAINEL ESQUERDO (retrato) ======================= -->
    <#-- Puramente decorativo: aria-hidden para o leitor de tela ir direto ao
         formulário. A logo colorida do painel direito carrega o alt text. -->
    <aside class="app-doc__aside" aria-hidden="true">
      <div class="app-doc__aside-media"></div>
      <#-- O molde nao carrega marca: ponha aqui o emblema e a logo do projeto, em
           resources/img/, e a faixa de identificacao abaixo. -->
      <div class="app-stamp">
        <span class="app-stamp__line1">App</span>
      </div>
    </aside>

    <#-- ========================= PAINEL DIREITO (dados) ======================= -->
    <main class="app-doc__main">
      <div class="app-doc__body">

        <#-- Logo colorida do projeto: só aparece no mobile, quando o painel lateral é ocultado.
             Acrescente-a em resources/img/ e reative a linha abaixo. -->
        <#-- <img class="app-doc__logo-color" src="${url.resourcesPath}/img/logo.svg" alt="App" /> -->

        <header class="${properties.kcFormHeaderClass!}">
            <#if realm.internationalizationEnabled  && locale.supported?size gt 1>
                <div class="${properties.kcLocaleMainClass!}" id="kc-locale">
                    <div id="kc-locale-wrapper" class="${properties.kcLocaleWrapperClass!}">
                        <div id="kc-locale-dropdown" class="menu-button-links ${properties.kcLocaleDropDownClass!}">
                            <button tabindex="1" id="kc-current-locale-link" aria-label="${msg("languages")}" aria-haspopup="true" aria-expanded="false" aria-controls="language-switch1">${locale.current}</button>
                            <ul role="menu" tabindex="-1" aria-labelledby="kc-current-locale-link" aria-activedescendant="" id="language-switch1" class="${properties.kcLocaleListClass!}">
                                <#assign i = 1>
                                <#list locale.supported as l>
                                    <li class="${properties.kcLocaleListItemClass!}" role="none">
                                        <a role="menuitem" id="language-${i}" class="${properties.kcLocaleItemClass!}" href="${l.url}">${l.label}</a>
                                    </li>
                                    <#assign i++>
                                </#list>
                            </ul>
                        </div>
                    </div>
                </div>
            </#if>

            <span class="app-eyebrow">Acesso restrito</span>

        <#if !(auth?has_content && auth.showUsername() && !auth.showResetCredentials())>
            <#if displayRequiredFields>
                <div class="${properties.kcContentWrapperClass!}">
                    <div class="${properties.kcLabelWrapperClass!} subtitle">
                        <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
                    </div>
                    <div class="app-doc__title-col">
                        <h1 id="kc-page-title"><#nested "header"></h1>
                    </div>
                </div>
            <#else>
                <h1 id="kc-page-title"><#nested "header"></h1>
            </#if>
        <#else>
            <#if displayRequiredFields>
                <div class="${properties.kcContentWrapperClass!}">
                    <div class="${properties.kcLabelWrapperClass!} subtitle">
                        <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
                    </div>
                    <div class="app-doc__title-col">
                        <#nested "show-username">
                        <div id="kc-username" class="${properties.kcFormGroupClass!}">
                            <label id="kc-attempted-username">${auth.attemptedUsername}</label>
                            <a id="reset-login" href="${url.loginRestartFlowUrl}" aria-label="${msg("restartLoginTooltip")}">
                                <div class="kc-login-tooltip">
                                    <i class="${properties.kcResetFlowIcon!}"></i>
                                    <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
                                </div>
                            </a>
                        </div>
                    </div>
                </div>
            <#else>
                <#nested "show-username">
                <div id="kc-username" class="${properties.kcFormGroupClass!}">
                    <label id="kc-attempted-username">${auth.attemptedUsername}</label>
                    <a id="reset-login" href="${url.loginRestartFlowUrl}" aria-label="${msg("restartLoginTooltip")}">
                        <div class="kc-login-tooltip">
                            <i class="${properties.kcResetFlowIcon!}"></i>
                            <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
                        </div>
                    </a>
                </div>
            </#if>
        </#if>
        </header>

        <div id="kc-content">
          <div id="kc-content-wrapper">

            <#-- Ações iniciadas pela aplicação não devem exibir o aviso de que a
                 ação precisa ser concluída durante o login. -->
            <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="alert-${message.type} ${properties.kcAlertClass!} app-alert--${message.type}">
                    <div class="app-alert__icon">
                        <#if message.type = 'success'><span class="${properties.kcFeedbackSuccessIcon!}"></span></#if>
                        <#if message.type = 'warning'><span class="${properties.kcFeedbackWarningIcon!}"></span></#if>
                        <#if message.type = 'error'><span class="${properties.kcFeedbackErrorIcon!}"></span></#if>
                        <#if message.type = 'info'><span class="${properties.kcFeedbackInfoIcon!}"></span></#if>
                    </div>
                    <span class="${properties.kcAlertTitleClass!}">${kcSanitize(message.summary)?no_esc}</span>
                </div>
            </#if>

            <#nested "form">

            <#if auth?has_content && auth.showTryAnotherWayLink()>
                <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post">
                    <div class="${properties.kcFormGroupClass!}">
                        <input type="hidden" name="tryAnotherWay" value="on"/>
                        <a href="#" id="try-another-way"
                           onclick="document.forms['kc-select-try-another-way-form'].requestSubmit();return false;">${msg("doTryAnotherWay")}</a>
                    </div>
                </form>
            </#if>

            <#nested "socialProviders">

            <#if displayInfo>
                <div id="kc-info" class="${properties.kcSignUpClass!}">
                    <div id="kc-info-wrapper" class="${properties.kcInfoAreaWrapperClass!}">
                        <#nested "info">
                    </div>
                </div>
            </#if>
          </div>
        </div>

      </div>

      <#-- Zona de leitura mecânica (MRZ). Decoração pura: `aria-hidden` é
           OBRIGATÓRIO, senão o leitor de tela recita a sequência como ruído. -->
      <div class="app-mrz" aria-hidden="true">
        <span>P&lt;BRAAPP&lt;&lt;ACESSO&lt;AO&lt;SISTEMA&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;</span>
        <span>App2026BRA&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;8</span>
      </div>

      <@loginFooter.content/>
    </main>

  </div>
</div>
</body>
</html>
</#macro>
