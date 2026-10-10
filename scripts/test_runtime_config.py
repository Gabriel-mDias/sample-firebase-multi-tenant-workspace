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
            env = {'PATH': os.environ['PATH'], 'SYSTEMROOT': os.environ.get('SYSTEMROOT', ''),
                   'ENV_JS_PATH': str(target), **{k:v for k,v in values.items() if v is not None}}
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
