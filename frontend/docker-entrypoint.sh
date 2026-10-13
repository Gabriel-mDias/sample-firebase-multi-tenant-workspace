#!/bin/sh
# Only Firebase Web public configuration is published to the browser.
set -eu

python3 - <<'PY'
import json
import ipaddress
import os
from pathlib import Path
import re
import sys
from urllib.parse import urlsplit

def reject():
    sys.exit('Invalid public runtime configuration; check required fields and demo opt-in')

def hostname(value):
    return len(value) <= 253 and all(
        re.fullmatch(r'[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?', label)
        for label in value.split('.'))

def api_url(value):
    if any(ord(char) < 33 for char in value) or '\\' in value:
        return False
    if value.startswith('/'):
        return not value.startswith('//') and '?' not in value and '#' not in value
    try:
        url = urlsplit(value)
        return (url.scheme in {'http', 'https'} and hostname(url.hostname or '')
                and not url.username and not url.password and not url.query and not url.fragment
                and (url.port is None or 1 <= url.port <= 65535))
    except ValueError:
        return False

firebase = {key: os.environ.get(variable, '') for key, variable in {
    'apiKey': 'FIREBASE_API_KEY', 'authDomain': 'FIREBASE_AUTH_DOMAIN',
    'projectId': 'FIREBASE_PROJECT_ID', 'appId': 'FIREBASE_APP_ID',
}.items()}
if any(not value.strip() for value in firebase.values()):
    reject()
if not hostname(firebase['authDomain']) or not re.fullmatch(r'[a-z][a-z0-9-]{3,62}', firebase['projectId']):
    reject()
api = os.environ.get('API_BASE_URL', '/api')
if not api_url(api):
    reject()
profile = os.environ.get('APP_RUNTIME_PROFILE', 'production')
emulator = os.environ.get('AUTH_EMULATOR_URL', '')
local = profile in {'local', 'test', 'ci', 'demo'}
if profile != 'production' and not local:
    reject()
if local:
    match = re.fullmatch(r'http://(localhost|127\.0\.0\.1):([0-9]{1,5})', emulator)
    if not firebase['projectId'].startswith('demo-') or not match or not 1 <= int(match[2]) <= 65535:
        reject()
elif emulator or firebase['projectId'].startswith('demo-'):
    reject()

# Validate every nginx input before publishing any runtime file.
upstream = os.environ.get('API_UPSTREAM', 'http://backend:8080')
match = re.fullmatch(r'https?://([A-Za-z0-9.-]+)(?::([0-9]{1,5}))?', upstream)
if (not match or not hostname(match[1])
        or (match[2] is not None and not 1 <= int(match[2]) <= 65535)):
    sys.exit('Invalid API_UPSTREAM; expected an HTTP or HTTPS origin')
try:
    resolv = Path(os.environ.get('RESOLV_CONF_PATH', '/etc/resolv.conf')).read_text(encoding='utf-8')
    resolvers = []
    for line in resolv.splitlines():
        tokens = line.split('#', 1)[0].split()
        if tokens and tokens[0] == 'nameserver':
            if len(tokens) != 2 or '%' in tokens[1]:
                raise ValueError()
            address = ipaddress.ip_address(tokens[1])
            resolver = '[' + str(address) + ']' if address.version == 6 else str(address)
            if resolver not in resolvers:
                resolvers.append(resolver)
    if not resolvers:
        raise ValueError()
except (OSError, UnicodeError, ValueError):
    sys.exit('Invalid resolver configuration; expected a nameserver IP address')

connections = ["'self'", 'https://identitytoolkit.googleapis.com', 'https://securetoken.googleapis.com']
if not api.startswith('/'):
    url = urlsplit(api)
    origin = url.scheme + '://' + url.hostname
    if url.port is not None:
        origin += ':' + str(url.port)
    if origin not in connections:
        connections.append(origin)
if local and emulator not in connections:
    connections.append(emulator)
csp = '; '.join([
    "default-src 'self'", "object-src 'none'", "base-uri 'self'",
    "form-action 'self'", "frame-ancestors 'none'",
    "script-src 'self' https://apis.google.com", "style-src 'self' 'unsafe-inline'",
    "img-src 'self' data:", "font-src 'self'",
    'connect-src ' + ' '.join(connections), 'frame-src https://' + firebase['authDomain'],
])
headers = {
    'Content-Security-Policy': csp,
    'X-Frame-Options': 'DENY',
    'X-Content-Type-Options': 'nosniff',
    'Referrer-Policy': 'strict-origin-when-cross-origin',
    'Permissions-Policy': 'camera=(), microphone=(), geolocation=()',
    'Cross-Origin-Opener-Policy': 'same-origin-allow-popups',
    'X-Permitted-Cross-Domain-Policies': 'none',
}
security_config = ''.join(f'add_header {name} "{value}" always;\n' for name, value in headers.items())
upstream_config = ('resolver ' + ' '.join(resolvers) + ' valid=30s;\n'
                   'set $api_upstream ' + upstream + ';\n'
                   'proxy_pass $api_upstream;\n')

def atomic_write(target, content):
    temporary = target.with_name(target.name + '.tmp')
    with temporary.open('w', encoding='utf-8', newline='\n') as output:
        output.write(content)
    temporary.replace(target)

config = {'production': not local, 'apiBaseUrl': api, 'firebase': firebase}
if local:
    config['authEmulatorUrl'] = emulator
# JSON escaping treats quotes/newlines/backticks as data. Escape HTML delimiters too.
payload = json.dumps(config, ensure_ascii=True, separators=(',', ':')).replace('<', '\\u003c').replace('>', '\\u003e').replace('&', '\\u0026')
target = Path(os.environ.get('ENV_JS_PATH', '/usr/share/nginx/html/env.js'))
atomic_write(target, 'globalThis.__env = ' + payload + ';\n')
atomic_write(Path(os.environ.get('SECURITY_HEADERS_PATH', '/etc/nginx/gems/security-headers.conf')), security_config)
atomic_write(Path(os.environ.get('API_UPSTREAM_CONF_PATH', '/etc/nginx/gems/api-upstream.conf')), upstream_config)
PY

exec nginx -g 'daemon off;'
