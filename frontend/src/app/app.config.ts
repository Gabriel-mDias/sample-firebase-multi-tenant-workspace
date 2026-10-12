import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import {
  provideHttpClient,
  withFetch,
  withInterceptors,
  withInterceptorsFromDi,
} from '@angular/common/http';
import { provideGemsHttp, provideGemsTheme } from '@gabriel-mdias/angular-gems-sdk';
import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { IDENTITY_ADAPTER } from './core/auth/identity.adapter';
import { FirebaseWebAdapter } from './core/auth/firebase-web.adapter';
import { firebaseInterceptor, gemsApiRoot } from './core/auth/firebase.interceptor';
registerLocaleData(localePt);
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: LOCALE_ID, useValue: 'pt-BR' },
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(
      withFetch(),
      withInterceptors([firebaseInterceptor]),
      withInterceptorsFromDi(),
    ),
    { provide: IDENTITY_ADAPTER, useExisting: FirebaseWebAdapter },
    provideGemsTheme({
      primary: '#8a6425',
      secondary: '#24231f',
      tertiary: '#0a0a0a',
      background: '#f6f4ef',
      danger: '#a32323',
      success: '#286137',
      warning: '#8a6425',
      info: '#27566a',
    }),
    provideGemsHttp(gemsApiRoot(environment.apiBaseUrl)),
  ],
};
