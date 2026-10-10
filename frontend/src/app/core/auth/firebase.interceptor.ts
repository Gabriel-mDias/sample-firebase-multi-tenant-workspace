import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, defer, EMPTY, from, switchMap, takeUntil, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

interface ApiBase {
  /** Origem exata que pode receber credenciais. */
  origin: string;
  /** Path de fronteira: `[/prefixo]/api`, sem barras finais. */
  path: string;
  /** Raiz entregue a `provideGemsHttp`; as stores acrescentam `api/{recurso}`. */
  root: string;
}

// Única normalização da base da API, compartilhada por stores e interceptor. O formato
// aceito é <origem>[/prefixo]/api; qualquer outro falha de forma fechada, em vez de gerar
// URLs que a store e o classificador interpretam de modos diferentes.
function parseApiBase(apiBaseUrl: string): ApiBase {
  const invalid = () =>
    new Error('API_BASE_URL inválida: use <origem>[/prefixo]/api, sem consulta ou fragmento.');
  const trimmed = apiBaseUrl.replace(/\/+$/, '');
  if (!trimmed.endsWith('/api') || /[?#\\\s]/.test(trimmed)) throw invalid();
  let url: URL;
  try {
    url = new URL(trimmed, window.location.origin);
  } catch {
    throw invalid();
  }
  if (!url.pathname.endsWith('/api')) throw invalid();
  return { origin: url.origin, path: url.pathname, root: trimmed.slice(0, -'/api'.length) };
}

export function isApiDestination(
  url: string,
  apiBaseUrl: string = environment.apiBaseUrl,
): boolean {
  try {
    const base = parseApiBase(apiBaseUrl);
    const target = new URL(url, window.location.origin);
    return (
      target.origin === base.origin &&
      (target.pathname === base.path || target.pathname.startsWith(base.path + '/'))
    );
  } catch {
    return false;
  }
}

// Raiz que `provideGemsHttp` recebe: as stores usam `super('api/{recurso}')`.
export function gemsApiRoot(apiBaseUrl: string = environment.apiBaseUrl): string {
  return parseApiBase(apiBaseUrl).root;
}

export const firebaseInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  // Retira headers sensíveis fornecidos pelo chamador em qualquer destino não controlado.
  const sanitized = request.clone({
    headers: request.headers.delete('Authorization').delete('X-Tenant-Alias'),
  });
  if (!isApiDestination(request.url)) return next(sanitized);
  // Captura o corpo e o contexto quando o consumidor assina; nunca troca o alias depois do await.
  return defer(() => {
    const snapshot = auth.capture();
    const body = request.body == null ? request.body : structuredClone(request.body);
    const base = parseApiBase(environment.apiBaseUrl).path;
    const path = new URL(request.url, window.location.origin).pathname;
    const isMe = path === base + '/auth/me';
    const isContext = path === base + '/auth/context';
    // Demonstrações contratadas: pública não leva credencial; global leva só o Bearer, sem alias.
    // A permissão efetiva continua sendo decidida pelo servidor.
    if (path === base + '/demo/public') return next(sanitized);
    const isGlobal = path.startsWith(base + '/admin/central/') || path === base + '/demo/global';
    if (
      !snapshot.uid ||
      (!isMe && !isContext && !isGlobal && !snapshot.ready) ||
      (isContext && !snapshot.alias)
    )
      return throwError(() => new Error('context-required'));
    return from(auth.idToken(snapshot)).pipe(
      switchMap((token) => {
        if (!auth.isCurrent(snapshot)) return EMPTY;
        const headers = sanitized.headers.set('Authorization', 'Bearer ' + token);
        return next(
          sanitized.clone({
            body,
            headers:
              snapshot.alias && !isMe && !isGlobal
                ? headers.set('X-Tenant-Alias', snapshot.alias)
                : headers,
          }),
        );
      }),
      catchError((error) => {
        if (!auth.isCurrent(snapshot)) return EMPTY;
        if (!(error instanceof HttpErrorResponse) || error.status === 401) auth.expire(snapshot);
        return throwError(() => error);
      }),
      takeUntil(auth.invalidated),
    );
  });
};
