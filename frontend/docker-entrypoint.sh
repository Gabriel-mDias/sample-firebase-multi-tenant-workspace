#!/bin/sh
# Only Firebase Web public configuration is published to the browser.
set -eu

python3 - <<'PY'
import json
import os
from pathlib import Path
import re
import sys
from urllib.parse import urlsplit

def reject():
    sys.exit('Invalid public runtime configuration; check required fields and demo opt-in')

def hostname(value):
    return bool(re.fullmatch(r'[A-Za-z0-9](?:[A-Za-z0-9.-]{0,251}[A-Za-z0-9])?', value)) and '..' not in value

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
config = {'production': not local, 'apiBaseUrl': api, 'firebase': firebase}
if local:
    config['authEmulatorUrl'] = emulator
# JSON escaping treats quotes/newlines/backticks as data. Escape HTML delimiters too.
payload = json.dumps(config, ensure_ascii=True, separators=(',', ':')).replace('<', '\\u003c').replace('>', '\\u003e').replace('&', '\\u0026')
target = Path(os.environ.get('ENV_JS_PATH', '/usr/share/nginx/html/env.js'))
temporary = target.with_suffix('.js.tmp')
temporary.write_text('globalThis.__env = ' + payload + ';\n', encoding='utf-8')
temporary.replace(target)
PY

exec nginx -g 'daemon off;'
