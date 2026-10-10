"""Adversarial initializer checks in disposable, locally committed fixtures."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import unittest
import uuid

SOURCE = Path(__file__).resolve().parents[1]
OUTPUT = Path(os.environ.get('SAMPLE_INITIALIZER_OUTPUT', str(
    SOURCE.parent / '.tooling/astra-sample-development-20261009/t028-initializer/fixtures')))
CLI = Path(os.environ.get('SAMPLE_INITIALIZER_CLI', str(SOURCE / 'scripts/sample.py')))
MODULES = ['security', 'model', 'events', 'core', 'deploy']
NS = 'http://maven.apache.org/POM/4.0.0'


def hashes(root):
    return {p.relative_to(root).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in root.rglob('*') if p.is_file() and '.git' not in p.parts}


class InitializerSafety(unittest.TestCase):
    def setUp(self):
        self.root = OUTPUT / ('f-' + uuid.uuid4().hex[:12])
        self.root.mkdir(parents=True)
        self.write('.gitignore', '.env\nnode_modules/\ntarget/\n')
        self.write('.gitattributes', '* text eol=lf\n')
        self.write('AGENTS.md', 'Product governance\n<!-- GEMS-AI:BEGIN -->\npinned\n<!-- GEMS-AI:END -->\n')
        self.write('.gems-ai/rules/base.safe-change.md', 'Managed bytes br.com.gems.app app-deploy\n')
        self.write('gems-ai.lock.json', '{"pin":"untouched"}\n')
        self.write('backend/pom.xml', '<project xmlns="' + NS + '"><modelVersion>4.0.0</modelVersion>'
                   '<parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId></parent>'
                   '<groupId>br.com.gems</groupId><artifactId>app</artifactId><name>App</name>'
                   '<description>Original</description><modules>' + ''.join('<module>app-' + m + '</module>' for m in MODULES)
                   + '</modules><properties><gems.version>3.4.0</gems.version></properties>'
                   '<dependencyManagement><dependencies><dependency><groupId>br.com.gems</groupId>'
                   '<artifactId>gems-firebase-auth</artifactId><version>${gems.version}</version></dependency>'
                   '</dependencies></dependencyManagement></project>\n')
        for m in MODULES:
            self.write('backend/app-' + m + '/pom.xml', '<project xmlns="' + NS + '"><modelVersion>4.0.0</modelVersion>'
                       '<parent><groupId>br.com.gems</groupId><artifactId>app</artifactId></parent>'
                       '<artifactId>app-' + m + '</artifactId><dependencies><dependency><groupId>br.com.gems</groupId>'
                       '<artifactId>gems-utils</artifactId></dependency><dependency><groupId>br.com.gems</groupId>'
                       '<artifactId>app-model</artifactId></dependency></dependencies></project>\n')
        self.write('backend/app-deploy/src/main/java/br/com/gems/app/AppApplication.java',
                   'package br.com.gems.app;\nimport br.com.gems.firebase.auth.IdTokenVerifier;\n'
                   'public class AppApplication {}\n')
        self.write('backend/app-core/src/test/java/br/com/gems/app/BoundaryTest.java',
                   'package br.com.gems.app;\nimport br.com.gems.security.authorization.AuthorizationCatalog;\n'
                   'class BoundaryTest { String job = "app.jobs.mode"; }\n')
        self.write('backend/app-deploy/src/main/java/br/com/gems/app/config/JobsConfiguration.java',
                   'package br.com.gems.app.config;\nclass JobsConfiguration {\n'
                   '    public BootstrapOperator bootstrapOperator( @Value("${app.jobs.operator-uid}") String uid, IdTokenVerifier verifier,\n'
                   '            FirebaseAdminGateway admin, FirebaseAuthProperties properties ) {}\n}\n')
        self.write('backend/app-deploy/src/main/resources/application.yml',
                   'spring:\n  application:\n    name: gems-firebase-multi-tenant\ngems:\n  jpa:\n    base-packages: br.com.gems.app\n'
                   'runtime: ${APP_RUNTIME_PROFILE:local}\nidentity: ${FIREBASE_PROJECT_ID}\n')
        self.write('backend/app-deploy/src/main/resources/authorization.actions.json', '{"global":[],"tenant":[]}\n')
        self.write('frontend/angular.json', json.dumps({'projects': {'app': {'prefix': 'app', 'architect': {'serve': {'buildTarget': 'app:build:development'}}}}}))
        self.write('frontend/package.json', json.dumps({'name': 'app', 'dependencies': {'@gabriel-mdias/angular-gems-sdk': '3.0.0', 'firebase': '12.19.0'}}))
        self.write('frontend/package-lock.json', json.dumps({'name': 'app', 'lockfileVersion': 3, 'packages': {'': {'name': 'app'}, 'node_modules/firebase': {'version': '12.19.0'}}}))
        self.write('frontend/scripts/verify-authorization-actions.mjs', "const backend = '../backend/app-deploy/src/main/resources/authorization.actions.json';\n")
        self.write('environments/local/docker-compose.yml', 'name: app_containers\nservices:\n  postgres:\n    container_name: app-postgres\n    image: postgres:16\n  auth-emulator:\n    container_name: app-auth-emulator\n    environment:\n      APP_RUNTIME_PROFILE: local\n      FIREBASE_PROJECT_ID: demo-gems\nvolumes:\n  pg:\n    name: app_postgres_data\nnetworks:\n  local:\n    name: app_network\n')
        manifest = {'version': 1, 'files': [], 'metadata': ['backend/pom.xml'], 'metadata_entries': []}
        self.write('scripts/demo-manifest.json', json.dumps(manifest, indent=2) + '\n')
        source = CLI.read_text('utf-8')
        import re
        source = re.sub(r"MANIFEST_SHA256 = '[a-f0-9]+'", "MANIFEST_SHA256 = '" + hashlib.sha256((self.root / 'scripts/demo-manifest.json').read_bytes()).hexdigest() + "'", source)
        self.write('scripts/sample.py', source)
        self.write('scripts/inicializar-projeto.ps1', (SOURCE / 'scripts/inicializar-projeto.ps1').read_text('utf-8'))
        self.write('.env', 'PRIVATE SENTINEL NEVER READ\n')
        self.git('init', '-q')
        self.git('add', '.')
        self.commit()

    def write(self, relative, text):
        p = self.root / relative
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(text, encoding='utf-8', newline='\n')

    def git(self, *args):
        return subprocess.run(['git', '-C', str(self.root), *args], check=True, capture_output=True, timeout=30)

    def commit(self):
        self.git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.test', 'commit', '-qm', 'Fixture only')

    def invoke(self, *args):
        return subprocess.run([sys.executable, '-B', str(self.root / 'scripts/sample.py'), 'initialize',
                               '--root', str(self.root), '--name', 'vendas', *args],
                              capture_output=True, text=True, timeout=45)

    def refuse(self, *args):
        before = hashes(self.root)
        result = self.invoke(*args)
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertNotIn('validated_plan', result.stderr, 'Refusal must precede the first mutation')
        self.assertEqual(hashes(self.root), before)
        return result

    def test_default_and_custom_group_preserve_sdk_and_managed_bytes(self):
        # Each group gets its own fixture and observed apply; no skipped branch.
        for group in ['br.com.gems', 'com.acme']:
            if group == 'com.acme':
                self.setUp()
            before = hashes(self.root)
            dry = self.invoke('--group', group, '--dry-run', '--description', 'Vendas & <pedidos>')
            self.assertEqual(dry.returncode, 0, dry.stderr)
            self.assertEqual(hashes(self.root), before)
            applied = self.invoke('--group', group, '--apply', '--description', 'Vendas & <pedidos>')
            self.assertEqual(applied.returncode, 0, applied.stderr)
            import xml.etree.ElementTree as ET
            p = ET.fromstring((self.root / 'backend/pom.xml').read_bytes())
            ns = {'m': NS}
            self.assertEqual(p.findtext('m:groupId', namespaces=ns), group)
            self.assertEqual(p.findtext('m:artifactId', namespaces=ns), 'vendas')
            self.assertEqual(p.findtext('m:description', namespaces=ns), 'Vendas & <pedidos>')
            sdk = p.find('m:dependencyManagement/m:dependencies/m:dependency', ns)
            self.assertEqual(sdk.findtext('m:groupId', namespaces=ns), 'br.com.gems')
            for m in MODULES:
                mp = ET.fromstring((self.root / ('backend/vendas-' + m + '/pom.xml')).read_bytes())
                self.assertEqual(mp.findtext('m:parent/m:groupId', namespaces=ns), group)
                self.assertEqual(mp.findtext('m:dependencies/m:dependency/m:groupId', namespaces=ns), 'br.com.gems')
                self.assertEqual(mp.findall('m:dependencies/m:dependency', ns)[1].findtext('m:groupId', namespaces=ns), group)
            java = self.root / ('backend/vendas-deploy/src/main/java/' + group.replace('.', '/') + '/vendas/VendasApplication.java')
            self.assertTrue(java.is_file())
            self.assertEqual((self.root / 'backend/vendas-deploy/src/main/resources/authorization.actions.json').read_text(), '{"global":[],"tenant":[]}\n')
            self.assertIn('package ' + group + '.vendas;', java.read_text())
            self.assertIn('import br.com.gems.firebase.auth.IdTokenVerifier;', java.read_text())
            jobs = self.root / ('backend/vendas-deploy/src/main/java/' + group.replace('.', '/')
                                + '/vendas/config/JobsConfiguration.java')
            self.assertIn('bootstrapOperator( @Value("${vendas.jobs.operator-uid}") String uid,\n'
                          '            IdTokenVerifier verifier,\n'
                          '            FirebaseAdminGateway admin, FirebaseAuthProperties properties )',
                          jobs.read_text('utf-8'))
            self.assertTrue((self.root / ('backend/vendas-core/src/test/java/' + group.replace('.', '/') + '/vendas/BoundaryTest.java')).is_file())
            for relative in ['AGENTS.md', '.gems-ai/rules/base.safe-change.md', 'gems-ai.lock.json', '.env']:
                self.assertEqual(hashes(self.root)[relative], before[relative])
            angular = json.loads((self.root / 'frontend/angular.json').read_text())
            self.assertEqual(angular['projects']['vendas']['prefix'], 'app')
            self.assertEqual(angular['projects']['vendas']['architect']['serve']['buildTarget'], 'vendas:build:development')
            config = (self.root / 'backend/vendas-deploy/src/main/resources/application.yml').read_text()
            self.assertIn('APP_RUNTIME_PROFILE', config)
            self.assertIn('FIREBASE_PROJECT_ID', config)
            compose = (self.root / 'environments/local/docker-compose.yml').read_text()
            self.assertIn('vendas-auth-emulator', compose)
            self.assertIn('vendas_postgres_data', compose)
            self.assertIn('APP_RUNTIME_PROFILE', compose)
            self.assertEqual(json.loads(dry.stdout)['changes'], json.loads(applied.stdout)['changes'])

    def test_flags_and_identifiers_are_validated_before_writes(self):
        self.refuse('--dry-run', '--apply')
        for group in ['com..acme', 'com.class', 'com.9bad', 'com/acme', 'com.acme\n']:
            self.refuse('--group', group, '--apply')
        self.refuse('--description', 'two\nlines', '--apply')
        self.refuse('--name', 'class', '--apply')
        self.refuse('--name', 'app', '--apply')
        self.refuse('--name', 'con', '--apply')
        self.refuse('--group', 'com.aux', '--apply')

    def test_root_and_dirty_untracked_are_refused_in_all_modes(self):
        self.refuse('--root', '.')
        self.refuse('--root', str(self.root / '..' / self.root.name))
        self.write('untracked.txt', 'not disposable')
        self.refuse('--dry-run')
        self.refuse('--apply')

    def test_committed_destination_file_and_ignored_directory_collision(self):
        self.write('backend/vendas-core/pom.xml', 'collision')
        self.git('add', '.')
        self.commit()
        self.refuse('--apply')

    def test_ignored_output_ancestor_wrong_type(self):
        self.write('backend/vendas-core', 'directory collision')
        self.git('add', '.')
        self.commit()
        self.refuse('--apply')

    def test_hardlinks_inputs_and_protected_outputs_are_refused(self):
        for relative in ['backend/pom.xml', 'AGENTS.md', 'scripts/demo-manifest.json', 'frontend/package-lock.json']:
            p = self.root / relative
            sentinel = OUTPUT / ('h-' + uuid.uuid4().hex)
            sentinel.write_bytes(p.read_bytes())
            p.unlink()
            os.link(sentinel, p)
            try:
                result = self.refuse('--apply')
                self.assertIn('Hardlinked', result.stderr)
            finally:
                data = sentinel.read_bytes()
                p.unlink()
                p.write_bytes(data)

    def test_reparse_root_and_ignored_destination_are_refused(self):
        target = OUTPUT / ('external-' + uuid.uuid4().hex)
        target.mkdir()
        self.write('.gitignore', '.env\nnode_modules/\ntarget/\nbackend/vendas-core\n')
        self.git('add', '.')
        self.commit()
        link = self.root / 'backend/vendas-core'
        if os.name == 'nt':
            subprocess.run(['powershell', '-NoProfile', '-Command',
                            'New-Item -ItemType Junction -Path $env:T028_LINK -Target $env:T028_TARGET | Out-Null'],
                           env=dict(os.environ, T028_LINK=str(link), T028_TARGET=str(target)), check=True, capture_output=True)
        else:
            link.symlink_to(target, target_is_directory=True)
        result = self.invoke('--apply')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Linked/reparse', result.stderr)
        self.assertEqual(list(target.iterdir()), [])

    def test_secret_is_excluded_before_read(self):
        spec = importlib.util.spec_from_file_location('initialize_fixture', self.root / 'scripts/sample.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        from unittest.mock import patch
        original = Path.read_bytes
        def guarded(p):
            if p.name == '.env':
                raise AssertionError('Secret was read')
            return original(p)
        with patch.object(Path, 'read_bytes', guarded):
            module.initialize_plan(self.root, 'vendas', 'com.acme', '')

    def test_powershell_default_and_custom_group_contract(self):
        for group in ['br.com.gems', 'com.acme']:
            if group == 'com.acme':
                self.setUp()
            before = hashes(self.root)
            argv = ['powershell', '-NoProfile', '-File', str(self.root / 'scripts/inicializar-projeto.ps1'), '-Nome', 'vendas', '-Grupo', group]
            dry = subprocess.run([*argv, '-DryRun'], cwd=self.root, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=45)
            self.assertEqual(dry.returncode, 0, dry.stderr)
            self.assertEqual(hashes(self.root), before)
            apply = subprocess.run(argv, cwd=self.root, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=45)
            self.assertEqual(apply.returncode, 0, apply.stderr)
            self.assertTrue((self.root / 'backend/vendas-deploy/pom.xml').exists())


class CleanDemoPayloadRegression(unittest.TestCase):
    """Cleaned test payloads must be runnable in a product copy (found by the first real full-build, T028C7)."""

    def payload(self, suffix):
        import re
        manifest = Path(os.environ.get('SAMPLE_TEST_MANIFEST', str(SOURCE / 'scripts/demo-manifest.json')))
        entries = [e for e in json.loads(manifest.read_text('utf-8'))['files'] if e['path'].endswith(suffix)]
        self.assertEqual(len(entries), 1)
        self.assertEqual(entries[0]['operation'], 'transform')
        self.assertEqual(hashlib.sha256(entries[0]['after'].encode('utf-8')).hexdigest(), entries[0]['after_sha256'])
        return re, entries[0]['after']

    def test_central_search_sends_a_deserializable_query(self):
        re, text = self.payload('app/FirebaseBackendIntegrationTest.java')
        calls = re.findall(r'post\( "/api/admin/central/tenants/search" \)(.*?)\.header\(', text, re.S)
        self.assertEqual(len(calls), 2)
        for call in calls:
            self.assertNotIn('content( "{}" )', call, 'SDK record Query rejects an empty object (HTTP 500)')
            self.assertIn('\\"limit\\":10', call)

    def test_tenant_reads_after_clicks_wait_for_the_async_context(self):
        re, text = self.payload('e2e/local-integration.spec.ts')
        racy = re.findall(r"\.click\(\);\n\s*expect\(\n\s*await page\.evaluate\(", text)
        self.assertEqual(racy, [], 'currentTenant() must be polled after a tenant click')
        self.assertGreaterEqual(len(re.findall(r'await expect\n\s+\.poll\(\(\) =>', text)), 5)


if __name__ == '__main__':
    unittest.main()
