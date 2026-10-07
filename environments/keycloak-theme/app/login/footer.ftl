<#--
  Crédito de rodapé. No parent `base` esta macro é VAZIA — é o ponto de extensão
  oficial para um rodapé próprio, sem conflito com nada.

  O template.ftl a invoca via <@loginFooter.content/> como último filho do painel
  direito, logo abaixo da faixa MRZ.

  Sem ano no copyright: é legalmente suficiente e nunca desatualiza. Computar o
  ano em FreeMarker (.now?string('yyyy')) adicionaria dependência de timezone
  por zero benefício numa tela de login.
-->
<#macro content>
  <footer class="app-credits">
    <span class="app-credits__owner">&copy; App</span>
    <span class="app-credits__vendor">Desenvolvido por <strong>Gems Solu&ccedil;&otilde;es Tecnol&oacute;gicas</strong></span>
  </footer>
</#macro>
