import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withFetch, withInterceptorsFromDi } from '@angular/common/http';
import { provideGemsHttp, provideGemsTheme } from '@gabriel-mdias/angular-gems-sdk';
import { provideGemsKeycloak } from '@gabriel-mdias/angular-gems-sdk/auth';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { keycloakOptions } from './core/auth/keycloak.init';

// Sem isto os pipes `currency`/`date`/`number` formatam em en-US
// (ex.: "R$1,200.00" e "08/04/2026" em vez de "R$ 1.200,00" e "04/08/2026").
registerLocaleData(localePt);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: LOCALE_ID, useValue: 'pt-BR' },
    provideRouter(routes),
    // Necessário para os componentes da SDK que usam a API de animações clássica (ex.: o
    // [@expandCollapse] do gems-summary-card). provideAnimationsAsync está deprecado, mas é
    // requisito enquanto a SDK depender dessa API. Carrega o módulo de animações sob demanda,
    // mantendo o bundle inicial menor.
    provideAnimationsAsync(),
    provideHttpClient(withFetch(), withInterceptorsFromDi()),
    provideGemsKeycloak(keycloakOptions()),
    // ESTE É O ÚNICO LUGAR DE COR DO PROJETO (Princípio VI). Nunca sobrescreva --gems-* em CSS de
    // componente, e nunca escreva hex direto numa folha de estilo de tela: a paleta muda aqui, e
    // uma cor cravada em componente sobrevive à troca de tema sem que ninguém perceba.
    //
    // A paleta abaixo é neutra de propósito — troque-a pela do projeto real na primeira rodada.
    provideGemsTheme({
      primary: '#2563EB', // Azul — cor primária (destaque principal)
      secondary: '#475569', // Cinza-ardósia — cor secundária
      tertiary: '#1A2B3C', // Azul-marinho escuro — texto principal
      background: '#F8FAFC', // Off-white — fundo da aplicação
      danger: '#DC2626',
      success: '#16A34A',
      warning: '#D97706',
      info: '#0284C7',
    }),
    provideGemsHttp(environment.apiBaseUrl),
  ],
};
