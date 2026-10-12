// Sem query/hash ou URLs externas; toda navegação empresarial ainda passa pelo guard da rota.
export function safeReturnPath(value: string | null | undefined): string {
  if (
    !value ||
    value.includes('\\') ||
    value.includes('%') ||
    value.includes('?') ||
    value.includes('#')
  )
    return '/';
  if (value === '/' || value === '/tenants') return value;
  if (/^\/demo-registro(?:\/(?:list|form|view)(?:\/[0-9a-f-]{36})?)?$/.test(value)) return value;
  return '/';
}
