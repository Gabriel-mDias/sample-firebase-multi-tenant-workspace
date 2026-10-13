"""Exercise the actual entrypoint serializer, including executable JavaScript boundaries."""
import json
import importlib.util
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class RuntimeConfigTest(unittest.TestCase):
    def write_config(self, overrides=None):
        source = (ROOT / 'frontend/docker-entrypoint.sh').read_text(encoding='utf-8')
        match = re.search(r"python3 - <<'PY'\n(.*?)\nPY", source, re.S)
        self.assertIsNotNone(match, 'Entrypoint must serialize environment as data')
        values = {
            'API_BASE_URL': '/api',
            'FIREBASE_API_KEY': 'public-web-config',
            'FIREBASE_AUTH_DOMAIN': 'sample.firebaseapp.com',
            'FIREBASE_PROJECT_ID': 'sample-product',
            'FIREBASE_APP_ID': 'public-app-id',
        }
        values.update(overrides or {})
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / 'env.js'
            resolv = Path(folder) / 'resolv.conf'
            resolv.write_text('nameserver 192.0.2.53\n', encoding='utf-8')
            env = {'PATH': os.environ['PATH'], 'SYSTEMROOT': os.environ.get('SYSTEMROOT', ''),
                   'ENV_JS_PATH': str(target),
                   'SECURITY_HEADERS_PATH': str(Path(folder) / 'security-headers.conf'),
                   'API_UPSTREAM_CONF_PATH': str(Path(folder) / 'api-upstream.conf'),
                   'RESOLV_CONF_PATH': str(resolv),
                   **{k:v for k,v in values.items() if v is not None}}
            result = subprocess.run([sys.executable, '-c', match[1]], env=env,
                                    capture_output=True, text=True, timeout=20)
            return result, target.read_text(encoding='utf-8') if target.exists() else None

    def test_production_public_fields(self):
        result, source = self.write_config()
        self.assertEqual(result.returncode, 0, result.stderr)
        config = json.loads(source.removeprefix('globalThis.__env = ').strip().removesuffix(';'))
        self.assertEqual(config['apiBaseUrl'], '/api')
        self.assertTrue(config['production'])
        self.assertEqual(set(config['firebase']), {'apiKey', 'authDomain', 'projectId', 'appId'})
        self.assertNotIn('authEmulatorUrl', config)

    def test_quote_newline_backtick_and_script_injection_remain_data(self):
        payload = '\"; globalThis.pwned=true; //\n` ${1+1} </script>\u2028'
        result, source = self.write_config({'FIREBASE_API_KEY': payload, 'FIREBASE_APP_ID': payload})
        self.assertEqual(result.returncode, 0, result.stderr)
        execution = subprocess.run(['node', '-e', source + '\nprocess.stdout.write(JSON.stringify({config:globalThis.__env,pwned:!!globalThis.pwned}));'],
                                   capture_output=True, text=True, encoding='utf-8', timeout=20)
        self.assertEqual(execution.returncode, 0, execution.stderr)
        output = json.loads(execution.stdout)
        self.assertFalse(output['pwned'])
        self.assertEqual(output['config']['firebase']['apiKey'], payload)
        self.assertEqual(output['config']['firebase']['appId'], payload)
        self.assertNotIn('</script>', source)

    def test_missing_or_empty_required_fields_fail_without_output(self):
        for field in ['FIREBASE_API_KEY', 'FIREBASE_AUTH_DOMAIN', 'FIREBASE_PROJECT_ID', 'FIREBASE_APP_ID']:
            for value in [None, '', '  ']:
                with self.subTest(field=field, value=value):
                    result, source = self.write_config({field:value})
                    self.assertNotEqual(result.returncode, 0)
                    self.assertIsNone(source)

    def test_demo_requires_explicit_profile_and_loopback_browser(self):
        for profile in ['local', 'test', 'ci', 'demo']:
            result, source = self.write_config({'APP_RUNTIME_PROFILE':profile, 'FIREBASE_PROJECT_ID':'demo-gems',
                                               'FIREBASE_AUTH_DOMAIN':'localhost', 'AUTH_EMULATOR_URL':'http://localhost:9099'})
            self.assertEqual(result.returncode, 0, result.stderr)
            config = json.loads(source.removeprefix('globalThis.__env = ').strip().removesuffix(';'))
            self.assertFalse(config['production'])
            self.assertEqual(config['authEmulatorUrl'], 'http://localhost:9099')

    def test_emulator_real_project_profile_and_hostname_guards(self):
        valid = {'APP_RUNTIME_PROFILE':'demo', 'FIREBASE_PROJECT_ID':'demo-gems', 'AUTH_EMULATOR_URL':'http://localhost:9099'}
        cases = [{'APP_RUNTIME_PROFILE':p} for p in ['', 'production', 'demo,production', 'local ']]
        cases += [{'FIREBASE_PROJECT_ID':'sample-product'}, {'AUTH_EMULATOR_URL':None}]
        cases += [{'AUTH_EMULATOR_URL':url} for url in [
            'http://auth-emulator:9099', 'http://localhost.evil:9099', 'https://localhost:9099',
            'http://localhost:9099/path', 'http://localhost:9099?x=1', 'http://localhost:9099#x',
            'http://user@localhost:9099', 'http://127.0.0.1:0', 'http://127.0.0.1:65536',
            'http://localhost:9099\n', 'http://localhost:9099/']]
        for case in cases:
            with self.subTest(case=case):
                result, source = self.write_config({**valid, **case})
                self.assertNotEqual(result.returncode, 0)
                self.assertIsNone(source)

    def test_emulator_is_rejected_without_profile_opt_in(self):
        result, source = self.write_config({'AUTH_EMULATOR_URL':'http://localhost:9099', 'FIREBASE_PROJECT_ID':'demo-gems'})
        self.assertNotEqual(result.returncode, 0)
        self.assertIsNone(source)

    def test_api_and_firebase_hostname_guards(self):
        for url in ['//evil/api', 'javascript:alert(1)', 'https://user@api.example/api', '/api\n',
                    'https://api.example/api?secret=1', 'http://api.example:65536/api']:
            result, source = self.write_config({'API_BASE_URL':url})
            self.assertNotEqual(result.returncode, 0)
            self.assertIsNone(source)
        for domain in ['localhost/path', 'user@host', 'host\n', 'https://sample.firebaseapp.com']:
            result, source = self.write_config({'FIREBASE_AUTH_DOMAIN':domain})
            self.assertNotEqual(result.returncode, 0)
            self.assertIsNone(source)

    def test_container_secret_is_never_browser_configuration(self):
        result, source = self.write_config({'GOOGLE_APPLICATION_CREDENTIALS':'private-admin-file', 'DB_PASSWORD':'private-db-value'})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn('private-', source)


class RuntimeNginxConfigTest(unittest.TestCase):
    def write_nginx_config(self, overrides=None, resolv_conf='nameserver 192.0.2.53\n',
                           headers_name='security-headers.conf', upstream_name='api-upstream.conf'):
        source = (ROOT / 'frontend/docker-entrypoint.sh').read_text(encoding='utf-8')
        match = re.search(r"python3 - <<'PY'\n(.*?)\nPY", source, re.S)
        self.assertIsNotNone(match, 'Entrypoint must serialize environment as data')
        values = {
            'API_BASE_URL': '/api',
            'FIREBASE_API_KEY': 'public-web-config',
            'FIREBASE_AUTH_DOMAIN': 'sample.firebaseapp.com',
            'FIREBASE_PROJECT_ID': 'sample-product',
            'FIREBASE_APP_ID': 'public-app-id',
        }
        values.update(overrides or {})
        with tempfile.TemporaryDirectory() as folder:
            paths = {
                'ENV_JS_PATH': Path(folder) / 'env.js',
                'SECURITY_HEADERS_PATH': Path(folder) / headers_name,
                'API_UPSTREAM_CONF_PATH': Path(folder) / upstream_name,
                'RESOLV_CONF_PATH': Path(folder) / 'fixture-resolv.conf',
            }
            for path in paths.values():
                path.parent.mkdir(parents=True, exist_ok=True)
            paths['RESOLV_CONF_PATH'].write_text(resolv_conf, encoding='utf-8')
            env = {'PATH': os.environ['PATH'], 'SYSTEMROOT': os.environ.get('SYSTEMROOT', ''),
                   **{key:str(path) for key, path in paths.items()},
                   **{key:value for key, value in values.items() if value is not None}}
            result = subprocess.run([sys.executable, '-c', match[1]], env=env,
                                    capture_output=True, text=True, timeout=20)
            outputs = {key:path.read_text(encoding='utf-8') if path.exists() else None
                       for key, path in paths.items() if key != 'RESOLV_CONF_PATH'}
            return result, outputs

    def read_headers(self, result, outputs):
        self.assertEqual(result.returncode, 0, result.stderr)
        source = outputs['SECURITY_HEADERS_PATH']
        self.assertIsNotNone(source, 'Entrypoint must generate SECURITY_HEADERS_PATH')
        pattern = r'''add_header\s+([\w-]+)\s+(?:"([^"\n]*)"|'([^'\n]*)'|([^;\s]+))\s+(always)\s*;'''
        headers = {}
        for match in re.finditer(pattern, source):
            name = match[1]
            self.assertNotIn(name, headers, 'Security headers must not be duplicated')
            headers[name] = next(value for value in match.groups()[1:4] if value is not None)
        self.assertEqual(len(headers), len(re.findall(r'\badd_header\b', source)),
                         'Every security header must have a valid value and always')
        return headers

    def assert_csp(self, result, outputs, auth_domain='sample.firebaseapp.com', extra_connections=()):
        headers = self.read_headers(result, outputs)
        self.assertIn('Content-Security-Policy', headers)
        directives = {}
        for directive in headers['Content-Security-Policy'].split(';'):
            tokens = directive.split()
            if tokens:
                self.assertNotIn(tokens[0], directives, 'CSP directives must not be duplicated')
                self.assertEqual(len(tokens[1:]), len(set(tokens[1:])))
                directives[tokens[0]] = set(tokens[1:])
        # research.md R1, lines 18-24: exact allowlist, independent of directive order.
        self.assertEqual(directives, {
            'default-src': {"'self'"},
            'object-src': {"'none'"},
            'base-uri': {"'self'"},
            'form-action': {"'self'"},
            'frame-ancestors': {"'none'"},
            'script-src': {"'self'", 'https://apis.google.com'},
            'style-src': {"'self'", "'unsafe-inline'"},
            'img-src': {"'self'", 'data:'},
            'font-src': {"'self'"},
            'connect-src': {"'self'", 'https://identitytoolkit.googleapis.com',
                            'https://securetoken.googleapis.com', *extra_connections},
            'frame-src': {'https://' + auth_domain},
        })

    def assert_upstream(self, result, outputs, upstream):
        self.assertEqual(result.returncode, 0, result.stderr)
        source = outputs['API_UPSTREAM_CONF_PATH']
        self.assertIsNotNone(source, 'Entrypoint must generate API_UPSTREAM_CONF_PATH')
        # research.md R5, lines 69-72: variable proxy_pass preserves the original URI.
        self.assertEqual([statement.strip() for statement in source.split(';') if statement.strip()], [
            'resolver 192.0.2.53 valid=30s',
            'set $api_upstream ' + upstream,
            'proxy_pass $api_upstream',
        ])

    def test_csp_frame_src_uses_configured_auth_domain(self):
        result, outputs = self.write_nginx_config({'FIREBASE_AUTH_DOMAIN':'tenant-login.firebaseapp.com'})
        self.assert_csp(result, outputs, auth_domain='tenant-login.firebaseapp.com')

    def test_csp_connect_src_adds_only_absolute_api_origin(self):
        for api, origin in [('https://api.example:8443/api/v1', 'https://api.example:8443'),
                            ('http://api.example/api', 'http://api.example')]:
            with self.subTest(api=api):
                result, outputs = self.write_nginx_config({'API_BASE_URL':api})
                self.assert_csp(result, outputs, extra_connections=(origin,))

    def test_csp_connect_src_has_no_extra_origin_for_relative_api(self):
        for api in ['/api', '/api/v1']:
            with self.subTest(api=api):
                result, outputs = self.write_nginx_config({'API_BASE_URL':api})
                self.assert_csp(result, outputs)

    def test_csp_emulator_is_allowed_only_in_local_profiles(self):
        with self.subTest(profile='production'):
            result, outputs = self.write_nginx_config()
            self.assert_csp(result, outputs)
        # The existing runtime contract treats local/test/ci/demo as local profiles.
        for profile in ['local', 'test', 'ci', 'demo']:
            with self.subTest(profile=profile):
                result, outputs = self.write_nginx_config({
                    'APP_RUNTIME_PROFILE':profile, 'FIREBASE_PROJECT_ID':'demo-gems',
                    'FIREBASE_AUTH_DOMAIN':'localhost', 'AUTH_EMULATOR_URL':'http://localhost:9099',
                })
                self.assert_csp(result, outputs, auth_domain='localhost',
                                extra_connections=('http://localhost:9099',))

    def test_all_seven_security_headers_have_expected_values_and_always(self):
        result, outputs = self.write_nginx_config()
        headers = self.read_headers(result, outputs)
        # research.md R2, lines 36-42: exactly seven headers, all with always.
        self.assertEqual(set(headers), {
            'Content-Security-Policy', 'X-Frame-Options', 'X-Content-Type-Options',
            'Referrer-Policy', 'Permissions-Policy', 'Cross-Origin-Opener-Policy',
            'X-Permitted-Cross-Domain-Policies',
        })
        self.assertEqual({key:value for key, value in headers.items() if key != 'Content-Security-Policy'}, {
            'X-Frame-Options': 'DENY',
            'X-Content-Type-Options': 'nosniff',
            'Referrer-Policy': 'strict-origin-when-cross-origin',
            'Permissions-Policy': 'camera=(), microphone=(), geolocation=()',
            'Cross-Origin-Opener-Policy': 'same-origin-allow-popups',
            'X-Permitted-Cross-Domain-Policies': 'none',
        })
        self.assert_csp(result, outputs)

    def test_security_headers_output_path_is_injectable(self):
        result, outputs = self.write_nginx_config(headers_name='custom headers/policy.inc')
        self.assertIsNotNone(outputs['ENV_JS_PATH'])
        self.assert_csp(result, outputs)

    def test_api_upstream_default_and_output_path_are_injectable(self):
        result, outputs = self.write_nginx_config(upstream_name='custom upstream/backend.inc')
        self.assert_upstream(result, outputs, 'http://backend:8080')

    def test_api_upstream_accepts_valid_http_and_https_origins(self):
        for upstream in ['http://service.internal:8081', 'https://api.example:8443',
                         'https://api.example', 'http://127.0.0.1:8080']:
            with self.subTest(upstream=upstream):
                result, outputs = self.write_nginx_config({'API_UPSTREAM':upstream})
                self.assert_upstream(result, outputs, upstream)

    def test_api_upstream_rejects_invalid_values_without_echo(self):
        cases = {
            'scheme': ['ftp://upstream-secret.example', 'javascript:upstream-secret', '//upstream-secret.example'],
            'path': ['http://upstream-secret.example/api', 'http://upstream-secret.example/'],
            'query': ['http://upstream-secret.example?token=private-query', 'http://upstream-secret.example?'],
            'credential': ['http://private-user:private-password@upstream-secret.example',
                           'http://private-user@upstream-secret.example'],
            'hostname': ['http://upstream_secret.example', 'http://upstream-secret..example',
                         'http://-upstream-secret.example', 'http://upstream-secret.example;injected',
                         'http://upstream-secret.example\n'],
            'fragment': ['http://upstream-secret.example#private-fragment', 'http://upstream-secret.example#'],
            'port': ['http://upstream-secret.example:0', 'http://upstream-secret.example:65536'],
        }
        diagnostic = None
        for category, values in cases.items():
            for upstream in values:
                with self.subTest(category=category, upstream=upstream):
                    result, outputs = self.write_nginx_config({'API_UPSTREAM':upstream})
                    output = result.stdout + result.stderr
                    self.assertNotIn(upstream, output)
                    for private_value in ['upstream-secret', 'upstream_secret', 'private-user',
                                          'private-password', 'private-query', 'private-fragment']:
                        self.assertNotIn(private_value, output)
                    self.assertNotEqual(result.returncode, 0, 'Invalid API_UPSTREAM must refuse startup')
                    self.assertIsNone(outputs['API_UPSTREAM_CONF_PATH'])
                    self.assertTrue(output.strip(), 'Rejection must have a fixed diagnostic')
                    if diagnostic is None:
                        diagnostic = output
                    self.assertEqual(output, diagnostic, 'Diagnostic must not depend on the invalid value')

    def test_resolv_conf_without_nameserver_is_rejected(self):
        diagnostic = None
        for resolv_conf in ['', 'search example.test\noptions ndots:0\n',
                            '# nameserver 192.0.2.53\nsearch example.test\n']:
            with self.subTest(resolv_conf=resolv_conf):
                result, outputs = self.write_nginx_config(resolv_conf=resolv_conf)
                self.assertNotEqual(result.returncode, 0, 'RESOLV_CONF_PATH must contain a nameserver')
                self.assertIsNone(outputs['API_UPSTREAM_CONF_PATH'])
                output = result.stdout + result.stderr
                self.assertTrue(output.strip(), 'Rejection must have a fixed diagnostic')
                self.assertNotIn('example.test', output)
                if diagnostic is None:
                    diagnostic = output
                self.assertEqual(output, diagnostic)


class OperationalLegacyCheckerTest(unittest.TestCase):
    def test_runtime_reports_later_paths_and_full_strict_fails(self):
        source = ROOT / 'scripts/verify-operational-legacy.py'
        with tempfile.TemporaryDirectory() as folder:
            fixture = Path(folder)
            scripts = fixture / 'scripts'
            scripts.mkdir()
            (scripts / source.name).write_bytes(source.read_bytes())
            (scripts / 'inicializar-projeto.ps1').write_text('# ' + 'key' + 'cloak\n')
            runtime = subprocess.run([sys.executable, str(scripts / source.name), '--stage', 'runtime', '--strict'],
                                     capture_output=True, text=True, timeout=20)
            self.assertEqual(runtime.returncode, 0)
            report = json.loads(runtime.stdout)
            self.assertEqual(report['pending_paths'], ['scripts/inicializar-projeto.ps1'])
            self.assertEqual(report['full_gate_status'], 'NOT_RUN')
            full = subprocess.run([sys.executable, str(scripts / source.name), '--strict'],
                                  capture_output=True, text=True, timeout=20)
            self.assertEqual(full.returncode, 1)
            self.assertEqual(json.loads(full.stdout)['status'], 'FAIL')

    def test_runtime_catches_nested_retired_provider_and_protects_real_env(self):
        source = ROOT / 'scripts/verify-operational-legacy.py'
        spec = importlib.util.spec_from_file_location('legacy_checker', source)
        checker = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(checker)
        with tempfile.TemporaryDirectory() as folder:
            checker.ROOT = Path(folder)
            target = checker.ROOT / 'environments/shared/provider'
            target.mkdir(parents=True)
            (target / 'config.yml').write_text('provider: ' + 'key' + 'cloak\n')
            (target / 'real.env').write_text('private-configuration-must-not-be-opened\n')
            findings = checker.scan(checker.RUNTIME, True)
            self.assertEqual(len(findings), 1)
            self.assertEqual(findings[0]['path'], 'environments/shared/provider/config.yml')
            self.assertNotIn(target / 'real.env', checker.files(checker.RUNTIME, True))


if __name__ == '__main__':
    unittest.main()
