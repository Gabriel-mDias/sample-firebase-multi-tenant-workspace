"""Adversarial initializer checks in disposable, locally committed fixtures."""
import hashlib
import importlib.util
import json
import os
import re
from pathlib import Path
import subprocess
import sys
import unittest
import uuid
from urllib.parse import parse_qs, urlsplit

SOURCE = Path(__file__).resolve().parents[1]
OUTPUT = Path(os.environ.get('SAMPLE_INITIALIZER_OUTPUT', str(
    Path(os.environ.get('TEMP', str(SOURCE))) / 't028c9-initializer-fixtures')))
CLI = Path(os.environ.get('SAMPLE_INITIALIZER_CLI', str(SOURCE / 'scripts/sample.py')))
MODULES = ['security', 'model', 'events', 'core', 'deploy']
NS = 'http://maven.apache.org/POM/4.0.0'


def fake_formatter(argv, cwd, timeout=120):
    """External tool double: no Maven, Java, dependency resolution or network."""
    if argv[-1] == '-version':
        return 'java 21'
    if argv[1:] != ['-B', '-q', 'spotless:apply']:
        raise AssertionError('Unexpected formatter command: ' + repr(argv))
    for path in Path(cwd).rglob('*.java'):
        lines = []
        for line in path.read_text('utf-8').splitlines():
            line = line.rstrip()
            while len(line) > 120:
                split = line.rfind(', ', 0, 120)
                if split == -1:
                    break
                lines.append(line[:split + 1])
                line = '            ' + line[split + 2:]
            lines.append(line)
        path.write_text('\n'.join(lines) + '\n', encoding='utf-8', newline='\n')
    return ''


def fake_prettier(argv, cwd, timeout=120):
    """Offline project-Prettier double; explicit closed file list, idempotent output."""
    if argv[:3] != ['node', 'node_modules/prettier/bin/prettier.cjs', '--write']:
        raise AssertionError('Unexpected Prettier command: ' + repr(argv))
    for relative in argv[3:]:
        path = Path(cwd) / relative
        text = path.read_text('utf-8')
        path.write_text('\n'.join(line.rstrip() for line in text.splitlines()) + '\n',
                        encoding='utf-8', newline='\n')
    return ''


def fixture_module(root):
    spec = importlib.util.spec_from_file_location('initialize_fixture', root / 'scripts/sample.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


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
                   + '</modules><properties><gems.version>3.4.1</gems.version></properties>'
                   '<dependencyManagement><dependencies><dependency><groupId>br.com.gems</groupId>'
                   '<artifactId>gems-firebase-auth</artifactId><version>${gems.version}</version></dependency>'
                   '</dependencies></dependencyManagement></project>\n')
        self.write('backend/mvnw.cmd', '@exit /b 0\n')
        self.write('backend/mvnw', '#!/bin/sh\nexit 0\n')
        (self.root / 'backend/mvnw').chmod(0o755)
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
        launch = ('import sys; sys.path.insert(0, sys.argv.pop(1)); '
                  'from test_initializer import fake_formatter, fixture_module; '
                  'from pathlib import Path; module = fixture_module(Path(sys.argv.pop(1))); '
                  'sys.exit(module.main(tool_runner=fake_formatter))')
        return subprocess.run([sys.executable, '-B', '-c', launch, str(SOURCE / 'scripts'), str(self.root), 'initialize',
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
            raw = fixture_module(self.root).rename_text(
                'package br.com.gems.app.config;\nclass JobsConfiguration {\n'
                '    public BootstrapOperator bootstrapOperator( @Value("${app.jobs.operator-uid}") String uid, IdTokenVerifier verifier,\n'
                '            FirebaseAdminGateway admin, FirebaseAuthProperties properties ) {}\n}\n',
                'backend/app-deploy/src/main/java/br/com/gems/app/config/JobsConfiguration.java', 'vendas', group, '')
            self.assertIn('String uid, IdTokenVerifier verifier,', raw, 'Rename must not format individual files')
            self.assertNotEqual(jobs.read_text('utf-8'), raw)
            result = hashes(self.root)
            fake_formatter(['mvnw.cmd', '-B', '-q', 'spotless:apply'], self.root / 'backend')
            self.assertEqual(hashes(self.root), result, 'Result must equal another formatter apply')
            report = json.loads(applied.stdout)
            self.assertTrue(json.loads(dry.stdout)['formatting']['enabled'])
            self.assertIn(jobs.relative_to(self.root).as_posix(), [e['path'] for e in report['formatted']])
            self.assertEqual(report['formatted_manifest_sha256'],
                             hashlib.sha256((self.root / 'scripts/demo-manifest.json').read_bytes()).hexdigest())
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

    def test_wrapper_and_java_are_required_before_mutation(self):
        module = fixture_module(self.root)
        wrapper = self.root / 'backend' / ('mvnw.cmd' if os.name == 'nt' else 'mvnw')
        wrapper.unlink()
        self.git('add', '.')
        self.commit()
        self.assertIn('Maven wrapper', self.refuse('--apply').stderr)
        self.write(wrapper.relative_to(self.root).as_posix(), 'fixture wrapper\n')
        wrapper.chmod(0o755)
        self.git('add', '.')
        self.commit()
        init = module.initialize_plan(self.root, 'vendas', 'br.com.gems', '')
        before = hashes(self.root)
        def unavailable(*args, **kwargs):
            raise FileNotFoundError('unit Java unavailable')
        with self.assertRaisesRegex(module.Refusal, 'requires available Java'):
            module.initialize_apply(self.root, *init, tool_runner=unavailable)
        self.assertEqual(hashes(self.root), before)

    def test_formatter_failure_restores_all_sources_and_manifest(self):
        import contextlib
        import io
        module = fixture_module(self.root)
        init = module.initialize_plan(self.root, 'vendas', 'br.com.gems', '')
        before = hashes(self.root)
        def failing(argv, cwd, timeout):
            if argv[-1] == '-version':
                return ''
            fake_formatter(argv, cwd, timeout)
            raise module.Refusal('unit formatter failure')
        with contextlib.redirect_stderr(io.StringIO()), self.assertRaisesRegex(module.Refusal, 'rolled back'):
            module.initialize_apply(self.root, *init, tool_runner=failing)
        self.assertEqual(hashes(self.root), before)

    def test_offline_switch_is_explicit_and_dryrun_never_calls_formatter(self):
        module = fixture_module(self.root)
        before = hashes(self.root)
        self.assertEqual(self.invoke('--dry-run').returncode, 0)
        self.assertEqual(hashes(self.root), before)
        def unexpected(*args, **kwargs):
            self.fail('Dry-run or --skip-format called an external tool')
        import contextlib
        import io
        from unittest.mock import patch
        argv = ['sample.py', 'initialize', '--root', str(self.root), '--name', 'vendas', '--dry-run']
        with patch.object(sys, 'argv', argv), contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(module.main(tool_runner=unexpected), 0)
        with contextlib.redirect_stderr(io.StringIO()):
            init = module.initialize_plan(self.root, 'vendas', 'br.com.gems', '', skip_format=True)
            module.initialize_apply(self.root, *init, tool_runner=unexpected)
        self.assertFalse(init[0]['formatting']['enabled'])
        self.assertEqual(init[0]['formatted'], [])

    def test_formatted_manifest_payloads_and_independent_replay_are_byte_identical(self):
        relative = 'backend/app-core/src/test/java/br/com/gems/app/BoundaryTest.java'
        before = (self.root / relative).read_text('utf-8')
        after = before.replace('String job', 'String renamedJob') + ' ' * 4
        manifest = {'version': 1, 'metadata': ['backend/pom.xml'], 'metadata_entries': [],
                    'files': [{'path': relative, 'operation': 'transform', 'before': before, 'after': after,
                               'sha256': hashlib.sha256(before.encode()).hexdigest(),
                               'after_sha256': hashlib.sha256(after.encode()).hexdigest()}]}
        self.write('scripts/demo-manifest.json', json.dumps(manifest, indent=2) + '\n')
        source = (self.root / 'scripts/sample.py').read_text('utf-8')
        pin = hashlib.sha256((self.root / 'scripts/demo-manifest.json').read_bytes()).hexdigest()
        self.write('scripts/sample.py', re.sub(r"MANIFEST_SHA256 = '[a-f0-9]+'",
                                              "MANIFEST_SHA256 = '" + pin + "'", source))
        self.git('add', '.')
        self.commit()
        originals = {p.relative_to(self.root).as_posix(): p.read_bytes() for p in self.root.rglob('*')
                     if p.is_file() and '.git' not in p.parts}
        for iteration in range(2):
            if iteration:
                self.setUp()
                for path, data in originals.items():
                    (self.root / path).write_bytes(data)
                self.git('add', '.')
                self.commit()
            applied = self.invoke('--apply')
            self.assertEqual(applied.returncode, 0, applied.stderr)
            module = fixture_module(self.root)
            entry = json.loads((self.root / 'scripts/demo-manifest.json').read_bytes())['files'][0]
            java = self.root / entry['path']
            actual = java.read_bytes()
            self.assertEqual(entry['before'].encode(), actual)
            self.assertEqual(entry['sha256'], module.digest(actual))
            result = hashes(self.root)
            java.write_bytes(entry['after'].encode())
            fake_formatter(['mvnw.cmd', '-B', '-q', 'spotless:apply'], self.root / 'backend')
            self.assertEqual(java.read_bytes(), entry['after'].encode(), 'Stored payload must already be formatted')
            java.write_bytes(actual)
            if iteration:
                self.assertEqual(result, first, 'Independent copies must produce identical bytes')
            else:
                first = result

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
            # The real wrapper's offline switch is explicit; Maven is never run by unittests.
            argv = ['powershell', '-NoProfile', '-File', str(self.root / 'scripts/inicializar-projeto.ps1'), '-Nome', 'vendas', '-Grupo', group, '-SkipFormat']
            dry = subprocess.run([*argv, '-DryRun'], cwd=self.root, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=45)
            self.assertEqual(dry.returncode, 0, dry.stderr)
            self.assertEqual(hashes(self.root), before)
            apply = subprocess.run(argv, cwd=self.root, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=45)
            self.assertEqual(apply.returncode, 0, apply.stderr)
            self.assertTrue((self.root / 'backend/vendas-deploy/pom.xml').exists())


class CleanDemoPayloadRegression(unittest.TestCase):
    """Cleaned test payloads must be runnable in a product copy (found by the first real full-build, T028C7)."""

    def manifest(self):
        path = Path(os.environ.get('SAMPLE_TEST_MANIFEST', str(SOURCE / 'scripts/demo-manifest.json')))
        return json.loads(path.read_text('utf-8'))

    def expected_frontend_catalog(self, manifest):
        """Derive surviving actions independently of the mocked catalog generator."""
        current = (SOURCE / 'frontend/src/app/core/auth/authorization.actions.ts').read_text('utf-8')
        catalog = {scope: set(re.findall(r"'([A-Z][A-Z_]+)'", re.search(
            rf'\b{scope}: \[(.*?)\]', current, re.S).group(1))) for scope in ['global', 'tenant']}
        removed = {action for actions in catalog.values() for action in actions if action.endswith('_DEMO')}
        for entry in manifest['files']:
            if entry['path'].endswith('/AcaoSistema.java'):
                declared = lambda text: set(re.findall(r'\b([A-Z][A-Z_]+)\(\s*AuthorizationScope\.', text))
                if entry['operation'] == 'transform':
                    removed.update(declared(entry['before']) - declared(entry['after']))
                elif entry['operation'] == 'delete':
                    removed.update(declared((SOURCE / entry['path']).read_text('utf-8')))
        return {scope: actions - removed for scope, actions in catalog.items()}

    def assert_frontend_actions_exist(self, text, catalog, relative):
        expected = set().union(*catalog.values())
        # Check every property access, including invented names. String actions follow
        # the catalog's action verbs; HTTP methods and provider error codes are unrelated.
        verbs = sorted({action.split('_', 1)[0] for action in expected}
                       | {'EXCLUIR', 'VISUALIZAR'})
        references = set(re.findall(r'\bAcoes\.([A-Za-z_$][\w$]*)', text))
        references.update(re.findall(r"['\"`]((?:" + '|'.join(verbs) + r")_[A-Z_]+)['\"`]", text))
        self.assertEqual(sorted(references - expected), [], 'G16 unknown frontend action: ' + relative)

    def assert_role_mode_semantics_preserved(self, before, after, catalog):
        def cases(text):
            callback = text.split("'deveAplicarModoDeAcoes_quandoEscopo%sComAcoes%jEModo%s'", 1)[1]
            granted = {scope: set(re.findall(r'Acoes\.(\w+)', re.search(
                scope + r'Actions = \[(.*?)\]', callback).group(1))) for scope in ['global', 'tenant']}
            rows = re.findall(r"\['(tenant|global)', \[([^\]]+)\], (undefined|'any'|'all'), "
                              r"'(liberado|/acesso-negado)'\]", text)
            self.assertEqual(len(rows), 9, 'G16 role mode cases lost')
            result = []
            for scope, actions, mode, expected in rows:
                roles = re.findall(r'Acoes\.(\w+)', actions)
                held = [role in granted[scope] for role in roles]
                allowed = all(held) if mode == "'all'" else any(held)
                self.assertEqual('liberado' if allowed else '/acesso-negado', expected)
                result.append((scope, mode, expected, held))
            return result, granted

        original, _ = cases(before)
        transformed, granted = cases(after)
        self.assertEqual(transformed, original, 'G16 role mode semantics changed')
        self.assertEqual(granted['tenant'], {'CONSULTAR_MEMBERSHIP', 'CONCEDER_GRANT'})
        for scope in ['global', 'tenant']:
            self.assertLessEqual(granted[scope], catalog[scope])

    def test_g16_all_transformed_frontend_actions_exist_after_clean_demo(self):
        manifest = self.manifest()
        catalog = self.expected_frontend_catalog(manifest)
        self.assertNotIn('VISUALIZAR_VALOR_RESTRITO', set().union(*catalog.values()))
        for entry in manifest['files']:
            if entry['path'].startswith('frontend/') and entry['operation'] == 'transform':
                with self.subTest(path=entry['path']):
                    self.assert_frontend_actions_exist(entry['after'], catalog, entry['path'])

    def cleaned_frontend_contract(self, manifest, root=SOURCE):
        payloads = {e['path']: e['after'] for e in manifest['files'] if e['operation'] == 'transform'}
        deleted = {e['path'] for e in manifest['files'] if e['operation'] == 'delete'}

        def read(relative):
            self.assertNotIn(relative, deleted, 'G16 deleted lazy route: ' + relative)
            if root != SOURCE:
                return (root / relative).read_text('utf-8')
            return payloads.get(relative) or (root / relative).read_text('utf-8')

        def visit(relative, prefix=''):
            paths = set()
            for block in re.findall(r'\{([^{}]*)\}', read(relative), re.S):
                path = re.search(r"\bpath:\s*'([^']*)'", block)
                if not path or path[1] == '**':
                    continue  # A wildcard redirect does not prove that a route exists.
                full = '/'.join(part for part in [prefix, path[1]] if part)
                lazy = re.search(r"loadChildren:.*?import\('([^']+)'\)", block, re.S)
                if lazy:
                    child = (Path(relative).parent / (lazy[1] + '.ts')).as_posix()
                    paths.update(visit(child, full))
                else:
                    paths.add('/' + full)
            return paths

        policy = read('frontend/src/app/core/auth/safe-return-path.ts')
        accepted = set()
        for condition in re.findall(r'if\s*\((.*?)\)\s*return value;', policy, re.S):
            accepted.update(re.findall(r"value === '([^']+)'", condition))
        selection = read('frontend/src/app/features/tenants/tenants.component.ts')
        loops = set(re.findall(r"requested !== '([^']+)'", selection)) - {'/'}
        return visit('frontend/src/app/app.routes.ts'), accepted - loops

    def without_rejected_return_inputs(self, text, relative):
        if relative.endswith('/login.component.spec.ts'):
            text = re.sub(r"(queryParamMap:\s*\{\s*get:\s*\(\)\s*=>\s*)'//evil.test'",
                          r"\1REJECTED_RETURN", text)
        if relative.endswith('/local-integration.spec.ts'):
            negative = re.search(r"test\('external return is rejected after password login and context validation'"
                                 r".*?\n\}\);", text, re.S)
            if negative:
                body = negative[0].replace("passwordLogin(page, '//untrusted.example')",
                                           'passwordLogin(page, REJECTED_RETURN)')
                text = text[:negative.start()] + body + text[negative.end():]
        return text

    def frontend_route_literals(self, text, relative):
        text = self.without_rejected_return_inputs(text, relative)
        # HTTP fixture URLs are API paths, including the interceptor's boundary cases.
        text = re.sub(r"\bapiUrl\((['\"])(.*?)\1\)", 'apiUrl(API_PATH)', text)
        text = re.sub(r"environment\.apiBaseUrl\s*\+\s*(['\"])(.*?)\1", 'API_URL', text)
        if relative.endswith('/auth.service.spec.ts'):
            text = '\n'.join(re.findall(
                r"(?:\.(?:navigate|navigateByUrl|rememberReturnPath)\([^;]+|"
                r"\bconst (?:path|returnTo) = [^;]+)", text))
        # Security inputs deliberately test rejection, rather than navigating to a route.
        text = re.sub(r"it\.each\(\[.*?\]\)\('(?:G14 deveDescartarRetornoInseguro|"
                      r"deveDescartarRetornoNaIntegracao)[^']*'", 'REJECTED_INPUT_TABLE', text, flags=re.S)
        if relative.endswith('/safe-return-path.spec.ts'):
            text = re.sub(r'for \(const value of \[.*?\]\)', 'REJECTED_INPUTS', text, flags=re.S)
        if relative.endswith('/app.spec.ts'):
            # These two preserved mock Router.url cases prove exact login-boundary matching.
            table = re.search(r"it\.each\(\[([^\]]*)\]\)\(\s*"
                              r"'deveRedirecionar_quandoSessaoInvalidadaForaDoLoginEm %s'", text)
            if table:
                inputs = table[1].replace("'/loginOutra'", '').replace("'/login/outro'", '')
                text = text[:table.start(1)] + inputs + text[table.end(1):]
        return [value for _, value in re.findall(r"(['\"`])((?:\\.|(?!\1).)*?)\1", text, re.S)
                if value.startswith('/')]

    def assert_frontend_routes_exist(self, text, routes, relative):
        for literal in self.frontend_route_literals(text, relative):
            parsed = urlsplit(literal)
            self.assertFalse(parsed.netloc, 'G16 external route: ' + relative + ': ' + literal)
            self.assertIn(parsed.path, routes, 'G16 missing frontend route: ' + relative + ': ' + literal)
            for target in parse_qs(parsed.query).get('returnTo', []):
                if '${' not in target:  # passwordLogin's literal/default arguments are checked separately.
                    self.assertIn(target, routes, 'G16 missing return route: ' + target)

    def assert_frontend_returns_safe(self, text, accepted, relative):
        text = self.without_rejected_return_inputs(text, relative)
        # Storage accepts /tenants, while the selector navigates away from itself.
        # Exclude only the direct storage/reset proof, never a navigation target.
        text = re.sub(
            r"auth\.rememberReturnPath\('/tenants'\);\s*"
            r"expect\(auth\.consumeReturnPath\(\)\)\.toBe\('/tenants'\);\s*"
            r"expect\(auth\.consumeReturnPath\(\)\)\.toBe\('/'\);", '', text)
        if relative.endswith('/local-integration.spec.ts'):
            text = re.sub(
                r"auth\.rememberReturnPath\('/tenants'\);\s*"
                r"const first = auth\.consumeReturnPath\(\);\s*"
                r"const second = auth\.consumeReturnPath\(\);", '', text)
        # Resolve literal return targets as well as the variables used by remembered returns.
        returns = re.findall(r"rememberReturnPath\(\s*(['\"`])([^'\"`]+)\1\s*\)", text)
        returns += re.findall(r"passwordLogin\(page(?:\s*:\s*Page)?,\s*"
                              r"(?:returnTo\s*=\s*)?(['\"`])([^'\"`]+)\1", text)
        for match in re.finditer(r'rememberReturnPath\((\w+)\)', text):
            values = re.findall(r"\bconst " + re.escape(match[1]) + r" = (['\"`])([^'\"`]+)\1;",
                                text[:match.start()])
            self.assertTrue(values, 'G16 unresolved remembered return: ' + relative)
            returns.append(values[-1])
        returns += re.findall(r"consumeReturnPath:\s*\(\)\s*=>\s*(['\"`])([^'\"`]+)\1", text)
        if re.search(r'consumeReturnPath:\s*\(\)\s*=>\s*path', text):
            returns += re.findall(r"\bconst path = (['\"`])([^'\"`]+)\1;", text)
        # AuthGuard only propagates an unvalidated URL. Integration tests actually consume it.
        if not relative.endswith(('/auth.guard.spec.ts', '/role.guard.spec.ts',
                                  '/app.spec.ts', '/safe-return-path.spec.ts')):
            for literal in self.frontend_route_literals(text, relative):
                returns += [('', target) for target in parse_qs(urlsplit(literal).query).get('returnTo', [])
                            if '${' not in target]
        for _, target in returns:
            self.assertIn(target, accepted, 'G16 rejected expected return: ' + relative + ': ' + target)

    def test_g16_all_transformed_frontend_routes_and_returns_survive_clean_demo(self):
        manifest = self.manifest()
        routes, accepted = self.cleaned_frontend_contract(manifest)
        self.assertEqual(routes, {'/', '/login', '/tenants', '/acesso-negado'})
        self.assertEqual(accepted, {'/'})
        for entry in manifest['files']:
            if entry['path'].startswith('frontend/') and entry['operation'] == 'transform':
                self.assert_frontend_routes_exist(entry['after'], routes, entry['path'])
                self.assert_frontend_returns_safe(entry['after'], accepted, entry['path'])
                self.assert_distinct_return_proofs(entry['after'], entry['path'])

    def assert_distinct_return_proofs(self, text, relative):
        """A constant fallback must fail the cleaned return tests, independently of spies."""
        proof = (r"auth\.rememberReturnPath\('/tenants'\);\s*"
                 r"expect\(auth\.consumeReturnPath\(\)\)\.toBe\('/tenants'\);\s*"
                 r"expect\(auth\.consumeReturnPath\(\)\)\.toBe\('/'\);")

        def body(title):
            self.assertIn(title, text, 'G16 missing return test: ' + relative)
            end = '\n});' if relative.endswith('/local-integration.spec.ts') else '\n  });'
            return text.split(title, 1)[1].split(end, 1)[0]

        titles = []
        if relative.endswith('/tenants.component.spec.ts'):
            titles = ['deveNavegarParaRetornoEConsumirOCaminho_quandoContextoDestinoValidado',
                      'naoDeveNavegarNemConsumirRetorno_quandoSelecaoAntigaConcluiAposContextoNovoComMesmoAlias_%s']
        elif relative.endswith('/app.routes.spec.ts'):
            titles = ['deveEsperarMeEEncaminharAoSeletor_quandoSessaoRestauradaSemContexto',
                      'devePreservarTenantERetornoAutorizado_quandoContextoJaValidado',
                      'deveAbrirHome_quandoTenantValidadoSemAcoesDeNegocio',
                      'deveEsperarContexto_quandoLoginAbertoDuranteSelecaoPendente']
        for title in titles:
            self.assertIn('auth = TestBed.inject(AuthService);', text)
            self.assertRegex(body(title), proof, 'G16 missing distinct storage/reset: ' + relative + ': ' + title)

        if relative.endswith('/safe-return-path.spec.ts'):
            self.assertIn("expect(safeReturnPath('/tenants')).toBe('/tenants');", text)
            self.assertIn("expect(safeReturnPath(null)).toBe('/');", text)
        elif relative.endswith('/auth.guard.spec.ts'):
            tested = body('deveEncaminharAoLoginComRetornoInterno_quandoRotaProtegida')
            self.assertIn("const view = '/tenants';", tested)
            self.assertIn("expect(serialize(run(view))).toBe('/login?returnTo=' + encodeURIComponent(view));", tested)
        elif relative.endswith('/role.guard.spec.ts'):
            tested = body('deveEncaminharAoLoginComRetornoInterno_quandoUsuarioAnonimo')
            self.assertIn("authorizationScope: 'tenant' }, '/tenants')", tested)
            self.assertIn(".toBe('/login?returnTo=%2Ftenants');", tested)
            self.assertIn('{ url } as RouterStateSnapshot', text)
        elif relative.endswith('/app.spec.ts'):
            self.assertRegex(text, r"(?s)it\.each\(\[.*?'/login\?returnTo=%2Ftenants'.*?\]\)\(\s*"
                             r"'devePreservarRetorno_quandoLoginEmAndamentoEm %s'")
        elif relative.endswith('/local-integration.spec.ts'):
            tested = body('password login loads real context and resumes the protected home return')
            self.assertIn(".ng.getComponent(document.querySelector('app-root')!).auth;", tested)
            self.assertRegex(tested, r"auth\.rememberReturnPath\('/tenants'\);\s*"
                             r"const first = auth\.consumeReturnPath\(\);\s*"
                             r"const second = auth\.consumeReturnPath\(\);\s*"
                             r"auth\.rememberReturnPath\('/'\);\s*return \[first, second\];")
            self.assertIn("expect(consumed).toEqual(['/tenants', '/']);", tested)

    def test_g16_rejects_fallback_only_proofs_in_each_transformed_return_payload(self):
        checked = 0
        for entry in self.manifest()['files']:
            if entry['operation'] != 'transform' or not entry['path'].startswith('frontend/'):
                continue
            text = entry['after']
            self.assert_distinct_return_proofs(text, entry['path'])
            mutant = text.replace('/tenants', '/').replace('%2Ftenants', '%2F')
            if mutant == text or not entry['path'].endswith((
                    '/tenants.component.spec.ts', '/app.routes.spec.ts', '/safe-return-path.spec.ts',
                    '/auth.guard.spec.ts', '/role.guard.spec.ts', '/app.spec.ts', '/local-integration.spec.ts')):
                continue
            checked += 1
            with self.subTest(path=entry['path']), self.assertRaises(AssertionError):
                self.assert_distinct_return_proofs(mutant, entry['path'])
            # Losing just one consumption/reset assertion must also fail each real-service proof.
            for match in re.finditer(r"expect\(auth\.consumeReturnPath\(\)\)\.toBe\('/(?:tenants)?'\);", text):
                if not re.search(r"auth\.rememberReturnPath\('/tenants'\);\s*$", text[:match.start()]) and not re.search(
                        r"expect\(auth\.consumeReturnPath\(\)\)\.toBe\('/tenants'\);\s*$", text[:match.start()]):
                    continue
                with self.subTest(path=entry['path'], assertion=match.start()), self.assertRaises(AssertionError):
                    self.assert_distinct_return_proofs(text[:match.start()] + text[match.end():], entry['path'])
        self.assertEqual(checked, 7)

    def test_g16_rejects_missing_routes_and_unsafe_expected_returns(self):
        manifest = self.manifest()
        routes, accepted = self.cleaned_frontend_contract(manifest)
        relative = 'frontend/src/app/features/tenants/tenants.component.spec.ts'
        text = next(e['after'] for e in manifest['files'] if e['path'] == relative)
        for route in ['/removed-example/list', '/tenant-route-fixtures/list', '/unknown',
                      '/login?returnTo=%2Funknown']:
            with self.subTest(route=route), self.assertRaises(AssertionError):
                self.assert_frontend_routes_exist(text + "\nnavigation.navigate(['" + route + "']);", routes, relative)
        for target in ['/login', '/tenants', '/acesso-negado', '//evil.test', 'https://evil.test',
                       '/?returnTo=//evil.test', '/#form', '/%2f%2fevil.test']:
            mutant = text.replace("const path = '/';", "const path = '" + target + "';")
            with self.subTest(target=target), self.assertRaises(AssertionError):
                self.assert_frontend_returns_safe(mutant, accepted, relative)

    def test_g16_valid_home_return_proves_consumption_after_context_validation(self):
        _, text = self.payload('/tenants.component.spec.ts')
        test = text.split("it('deveNavegarParaRetornoEConsumirOCaminho_quandoContextoDestinoValidado'", 1)[1].split('\n  });', 1)[0]
        for anchor in ["const path = '/';", "vi.spyOn(auth, 'consumeReturnPath')",
                       'expect(consume).not.toHaveBeenCalled();', 'expect(consume).toHaveBeenCalledTimes(1);',
                       'expect(consume.mock.results[0].value).toBe(path);',
                       'expect(navigation.navigate).toHaveBeenCalledWith([path]);',
                       "expect(auth.consumeReturnPath()).toBe('/');"]:
            self.assertIn(anchor, test)
        self.assertLess(test.index('expect(consume).not.toHaveBeenCalled();'), test.index(".flush(context('beta'))"))
        self.assertLess(test.index(".flush(context('beta'))"), test.index('expect(consume).toHaveBeenCalledTimes(1);'))
        # Home equals the default; a spy also keeps stale callbacks from silently consuming it.
        self.assertIn("expect(consume).not.toHaveBeenCalled();\n      expect(auth.consumeReturnPath()).toBe(path);", text)

    def test_g16_rejects_unknown_or_removed_frontend_action_properties_and_strings(self):
        manifest = self.manifest()
        catalog = self.expected_frontend_catalog(manifest)
        for entry in manifest['files']:
            if entry['path'].startswith('frontend/') and entry['operation'] == 'transform':
                for action in ['ALTERAR_MEMBERSHIP', 'CONSULTAR_DEMO', 'VISUALIZAR_VALOR_RESTRITO']:
                    for reference in ['Acoes.' + action, "'" + action + "'", '"' + action + '"']:
                        with self.subTest(path=entry['path'], reference=reference), self.assertRaises(AssertionError):
                            self.assert_frontend_actions_exist(entry['after'] + '\n' + reference,
                                                               catalog, entry['path'])

    def test_g16_role_mode_cases_preserve_granted_actions_and_outcomes(self):
        manifest = self.manifest()
        entry = next(e for e in manifest['files'] if e['path'].endswith('/role.guard.spec.ts'))
        catalog = self.expected_frontend_catalog(manifest)
        self.assert_role_mode_semantics_preserved(entry['before'], entry['after'], catalog)
        mutant = entry['after'].replace('Acoes.ENCERRAR_MEMBERSHIP', 'Acoes.CONCEDER_GRANT')
        with self.assertRaises(AssertionError):
            self.assert_role_mode_semantics_preserved(entry['before'], mutant, catalog)

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

    def assert_e2e_helpers_defined(self, original, transformed):
        """Every retained call to an original top-level helper needs its declaration."""
        helpers = set(re.findall(r'^async function (\w+)\(', original, re.M))
        defined = set(re.findall(r'^async function (\w+)\(', transformed, re.M))
        for name in helpers:
            calls = re.findall(r'(?<![.\w])' + re.escape(name) + r'\(', transformed)
            self.assertTrue(not calls or name in defined, 'G16 undefined E2E helper: ' + name)

    def assert_revocation_keeps_other_tenant_action(self, transformed):
        name = 'deveRecusarContext403_quandoApenasConsultarProprioContextoTenantEhRevogada'
        body = re.search(r'void ' + name + r'\(.*?\n    }', transformed, re.S).group()
        self.assertIn('revokeMemberAction( alphaId, aliceMember, "CONSULTAR_PROPRIO_CONTEXTO_TENANT" )', body)
        requests = re.findall(r'mvc.perform\( (.*?)\.andExpect\( status\(\)\.(\w+)\(\) \)', body, re.S)
        context = [(call, status) for call, status in requests if '"/api/auth/context"' in call]
        self.assertEqual(len(context), 1, 'G16 revoked context used again as a successful tenant probe')
        self.assertEqual(context[0][1], 'isForbidden')
        permitted = [call for call, status in requests if status == 'isOk'
                     and '"/api/admin/tenants/" + alphaId + "/memberships/search"' in call]
        self.assertEqual(len(permitted), 1, 'G16 remaining tenant permission must be exercised')
        for anchor in ['post(', '"Bearer " + aliceUid', '"X-Tenant-Alias", alpha',
                       '.contentType( "application/json" )', '\\"limit\\":10']:
            self.assertIn(anchor, permitted[0])
        self.assertIn('identityAndMembershipRemainValid( aliceId, alphaId ) ).isTrue()', body)

    def assert_migration_boundaries_dynamic(self, transformed):
        self.assertIsNone(re.search(r'\b(?:migrate|migrationDatabase)\([^;]*,\s*[1-9]\d*\s*\);',
                                    transformed), 'G16 fixed changeset prefix changes meaning after clean-demo')
        compact = re.sub(r'\s+', ' ', transformed)
        for call in ['migrate( url, "public", PLATFORM, changeSetsBeforeRename( PLATFORM, PLATFORM_RENAME ) )',
                     'migrate( url, SCHEMA, TENANT, changeSetsBeforeRename( TENANT, TENANT_RENAME ) )',
                     'migrationDatabase( schema, changelog, changeSetsBeforeRename( changelog, rename ) )',
                     'migrationDatabase( schema, changelog, 0 )']:
            self.assertIn(call, compact)
        for anchor in ['parser.parse( changelog, new ChangeLogParameters(), resources ).getChangeSets()',
                       'change.getId()', 'change.getAuthor()', 'change.getFilePath()',
                       'if ( rename.equals( key ) )', 'assertThat( count ).isPositive()', 'return count;',
                       'count++;', 'throw new IllegalStateException( "Rename changeset not found: " + rename )']:
            self.assertIn(anchor, transformed)

    def test_g16_rejects_undefined_retained_e2e_helper(self):
        _, text = self.payload('e2e/local-integration.spec.ts')
        self.assert_e2e_helpers_defined(text, text)
        helpers = re.findall(r'^async function (\w+)\(', text, re.M)
        for name in helpers:
            mutant = re.sub(r'async function ' + name + r'\(.*?\n}\n\n', '', text, count=1, flags=re.S)
            if re.search(r'(?<![.\w])' + name + r'\(', mutant):
                with self.subTest(helper=name), self.assertRaises(AssertionError):
                    self.assert_e2e_helpers_defined(text, mutant)

    def test_g16_rejects_success_probe_using_the_revoked_context(self):
        _, text = self.payload('app/FirebaseBackendIntegrationTest.java')
        self.assert_revocation_keeps_other_tenant_action(text)
        mutant = text.replace('post( "/api/admin/tenants/" + alphaId + "/memberships/search" )',
                              'get( "/api/auth/context" )')
        with self.assertRaises(AssertionError):
            self.assert_revocation_keeps_other_tenant_action(mutant)

    def test_g16_rejects_fixed_migration_prefix_at_each_call_site(self):
        _, text = self.payload('app/MigrationContractIntegrationTest.java')
        self.assert_migration_boundaries_dynamic(text)
        for arguments in ['PLATFORM, PLATFORM_RENAME', 'TENANT, TENANT_RENAME', 'changelog, rename']:
            mutant = text.replace('changeSetsBeforeRename( ' + arguments + ' )', '3', 1)
            with self.subTest(arguments=arguments), self.assertRaises(AssertionError):
                self.assert_migration_boundaries_dynamic(mutant)

    def test_g16_clean_demo_removes_all_current_references_and_preserves_foundations(self):
        """Use real initialize/plan/apply; only external catalog tools are substituted.

        The full-build harness separately proves the official catalog regeneration.
        This unit proof scans the resulting runtime tree independently of the manifest.
        """
        import contextlib
        import io
        import re
        from unittest.mock import patch
        manifest_path = Path(os.environ.get('SAMPLE_TEST_MANIFEST', str(SOURCE / 'scripts/demo-manifest.json')))
        manifest_bytes = manifest_path.read_bytes()
        manifest = json.loads(manifest_bytes)
        specification = importlib.util.spec_from_file_location('g16_initializer', CLI)
        module = importlib.util.module_from_spec(specification)
        specification.loader.exec_module(module)
        tracked = subprocess.check_output(['git', '-C', str(SOURCE), 'ls-files', '-z']).decode('utf-8').split('\0')
        runtime = [relative for relative in tracked if relative
                   and relative.startswith(('backend/', 'frontend/'))
                   and not module.secret_path(relative)
                   and Path(relative).suffix in {'.java', '.xml', '.ts', '.html', '.css', '.json',
                                                 '.mjs', '.yml', '.yaml', '.properties'}]
        selected = sorted(set(runtime + ['backend/mvnw', 'backend/mvnw.cmd'] + [entry['path'] for entry in manifest['files']
                                         + manifest['metadata_entries']] + manifest['metadata']))
        demo = re.compile(r'demo[-_ ]?registro|(?:CONSULTAR|INSERIR|ALTERAR|EXCLUIR)_DEMO'
                          r'|VISUALIZAR_VALOR_RESTRITO|/api/demo/', re.I)
        foundation = re.compile(r'/auth/|/tenant/|/tenants/|/identity/|/administracao/|/security/'
                                r'|/login/|/guards/|/menu/|/home/|/acesso-negado/'
                                r'|Firebase(?:Wiring|Real|Backend)|MigrationContract|EndToEndSupport'
                                r'|app\.routes|db/changelog/changelog-firebase')
        # These tests belong exclusively to the removed domain. All other current
        # Java contract methods and TS tests in the foundation surfaces must survive.
        removed_java = {
            'deveOcultarCampoNaLeituraBuscaEGravacao_quandoAcaoRestritaFalta',
            'deveValidarESoftDeleteAuditarPorTenant_quandoExecutaCRUD',
            'devePreservarDecimalExatoNoCRUD_quandoValorUsaContratoDeDezesseteInteirosEDoisDecimais',
            'deveRecusarDecimalForaDoLimiteSemAlterarSQL_quandoValorChegaComoString',
            'devePermitirLeituraEGravacaoDoProprioTenant_quandoAlphaEBetaCompartilhamPK',
            'deveRecusar403SemMutacaoNemAuditoria_quandoMembroValidoNaoTemAAcaoDemo',
        }
        initial_pin = module.MANIFEST_SHA256
        for group in ['br.com.gems', 'com.acme']:
            with self.subTest(group=group):
                root = OUTPUT / ('g16-' + uuid.uuid4().hex[:12])
                root.mkdir(parents=True)
                for relative in selected:
                    source = module.inside(SOURCE, relative)
                    target = root / relative
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_bytes(source.read_bytes())
                    if relative == 'backend/mvnw':
                        target.chmod(0o755)
                (root / 'scripts').mkdir(exist_ok=True)
                (root / 'scripts/demo-manifest.json').write_bytes(manifest_bytes)
                (root / 'scripts/sample.py').write_bytes(CLI.read_bytes())
                module.MANIFEST_SHA256 = initial_pin
                with patch.object(module, 'checkout'), patch.object(module, 'command',
                        return_value='\x00'.join(selected + ['scripts/sample.py', 'scripts/demo-manifest.json'])):
                    init = module.initialize_plan(root, 'vendas', group, '')
                    with contextlib.redirect_stderr(io.StringIO()):
                        # This regression concerns payload removals; external formatting is injected.
                        module.initialize_apply(root, *init, tool_runner=lambda *args, **kwargs: '')
                module.MANIFEST = root / 'scripts/demo-manifest.json'
                module.MANIFEST_SHA256 = module.digest(module.MANIFEST.read_bytes())
                initialized = json.loads(module.MANIFEST.read_text('utf-8'))
                operations = {entry['path']: entry['operation'] for entry in initialized['files']}
                sources = {module.rename_path(relative, 'vendas', group): (root /
                           module.rename_path(relative, 'vendas', group)).read_bytes() for relative in runtime}
                security = root / 'backend/vendas-security/target'
                security.mkdir(parents=True)
                jar = security / 'fixture.jar'
                jar.write_bytes(b'unit catalog prerequisite')
                (security / 'catalog-classpath.txt').write_text(str(jar), encoding='utf-8', newline='\n')
                prettier = root / 'frontend/node_modules/prettier/package.json'
                prettier.parent.mkdir(parents=True)
                prettier.write_text('{}', encoding='utf-8', newline='\n')
                tool = root / 'catalog-tool.exe'
                tool.write_bytes(b'unit tool prerequisite')

                def catalog(argv, cwd):
                    if argv == ['javac', '-version']:
                        return 'javac 21.0.0'
                    if argv[0] == 'java' and argv[1].startswith('@'):
                        for relative, original in sources.items():
                            if operations.get(relative) == 'regenerate' and relative.endswith('.json'):
                                value = json.loads(original)
                                for scope in ['global', 'tenant']:
                                    value[scope] = [action for action in value[scope] if not demo.search(action)]
                                (root / relative).write_text(json.dumps(value, indent=2) + '\n',
                                                            encoding='utf-8', newline='\n')
                    if argv[:3] == ['node', 'scripts/action-catalog.mjs', '--write']:
                        for relative, original in sources.items():
                            if operations.get(relative) == 'regenerate' and relative.endswith('.ts'):
                                text = ''.join(line for line in original.decode('utf-8').splitlines(True)
                                               if not demo.search(line))
                                (root / relative).write_text(text, encoding='utf-8', newline='\n')
                    return ''

                with patch.object(module, 'checkout'), patch.object(module, 'command', side_effect=catalog), \
                        patch('shutil.which', return_value=str(tool)), contextlib.redirect_stderr(io.StringIO()):
                    clean = module.plan(root)
                    module.apply(root, *clean, prettier_runner=fake_prettier)
                self.assertEqual(clean[0]['formatted'], sorted(relative for relative, operation in operations.items()
                    if operation in {'transform', 'regenerate'} and relative.startswith('frontend/')
                    and Path(relative).suffix in {'.ts', '.html', '.scss', '.json'}))
                frontend_after = {relative: (root / relative).read_bytes() for relative in clean[0]['formatted']}
                fake_prettier(['node', 'node_modules/prettier/bin/prettier.cjs', '--write']
                              + [relative.removeprefix('frontend/') for relative in clean[0]['formatted']],
                              root / 'frontend')
                self.assertEqual(frontend_after, {relative: (root / relative).read_bytes()
                                                 for relative in clean[0]['formatted']})
                for relative, original in sources.items():
                    path = root / relative
                    if demo.search(relative):
                        self.assertFalse(path.exists(), 'G16 demo file survived: ' + relative)
                    elif foundation.search(relative):
                        self.assertTrue(path.is_file(), 'G16 foundation removed: ' + relative)
                        after = path.read_bytes()
                        if relative not in operations:
                            self.assertEqual(after, original, 'G16 foundation changed: ' + relative)
                        elif relative.endswith('.java'):
                            methods = lambda text: set(re.findall(r'\bvoid (deve\w+|naoDeve\w+)\(', text))
                            required = methods(original.decode('utf-8')) - removed_java
                            self.assertLessEqual(required, methods(after.decode('utf-8')), relative)
                            bodies = lambda text: dict(re.findall(
                                r'\bvoid (deve\w+|naoDeve\w+)\([^\n]*(.*?\n    })', text, re.S))
                            preserved = bodies(after.decode('utf-8'))
                            for name, body in bodies(original.decode('utf-8')).items():
                                if name not in removed_java and not demo.search(body):
                                    if relative.endswith('MigrationContractIntegrationTest.java'):
                                        # Preserve every assertion; only the approved prefix computation changes.
                                        body = body.replace('// Três changesets originais de cada master, sem reconstruir DDL à mão.',
                                                            '// Histórico anterior ao rename, independente de changesets removíveis.')
                                        for master in ['PLATFORM', 'TENANT']:
                                            body = body.replace(master + ', 3 );', master + ', changeSetsBeforeRename( '
                                                                + master + ', ' + master + '_RENAME ) );')
                                        body = body.replace('migrationDatabase( schema, changelog, 3 );',
                                                            'migrationDatabase( schema, changelog, changeSetsBeforeRename( changelog, rename ) );')
                                    self.assertEqual(preserved.get(name), body,
                                                     'G16 foundation test weakened: ' + name)
                        elif relative.endswith('.spec.ts'):
                            tests = lambda text: set(re.findall(r"['\"`]((?:G\d+ )?(?:deve|naoDeve)[^'\"`]*?)['\"`]", text))
                            required = tests(original.decode('utf-8'))
                            required = {name for name in required if not demo.search(name)
                                        and name != 'deveNegarRetornoInterno_quandoTenantValidadoNaoTemAcaoDaRota'
                                        and not name.startswith('deveRespeitarAcaoCanonica_quandoRota_')}
                            if relative.endswith('/auth.guard.spec.ts'):
                                required.discard('deveEncaminharAoLoginComRetornoInterno_quandoRotaTemUUID')
                                required.add('deveEncaminharAoLoginComRetornoInterno_quandoRotaProtegida')
                            self.assertLessEqual(required, tests(after.decode('utf-8')), relative)
                for relative in runtime:
                    path = root / module.rename_path(relative, 'vendas', group)
                    if path.exists():
                        self.assertIsNone(demo.search(path.read_text('utf-8')), 'G16 reference survived: ' + str(path))
                anchors = {
                    'security/config/SecurityConfig.java': ['@EnableMethodSecurity', 'SessionCreationPolicy.STATELESS',
                        'auth.anyRequest().authenticated()', 'addFilterBefore( firebase,'],
                    'db/changelog/changelog-firebase-tenant.xml': ['changelog-firebase-business-audit.xml',
                        'firebase-tenant-revision-1', 'changelog-firebase-tenant-constraint-names.xml'],
                    'frontend/src/app/app.routes.ts': ["path: 'login'", "path: 'tenants'", 'canActivate: [authGuard]'],
                }
                for suffix, required in anchors.items():
                    paths = [relative for relative in sources if relative.endswith(suffix)]
                    self.assertEqual(len(paths), 1, suffix)
                    text = (root / paths[0]).read_text('utf-8')
                    for anchor in required:
                        self.assertIn(anchor, text, 'G16 foundation anchor removed: ' + suffix)
                catalog_path = root / 'backend/vendas-deploy/src/main/resources/authorization.actions.json'
                before = json.loads(sources[catalog_path.relative_to(root).as_posix()])
                after = json.loads(catalog_path.read_text('utf-8'))
                for scope in ['global', 'tenant']:
                    self.assertEqual(after[scope], [action for action in before[scope] if not demo.search(action)])
                expected_frontend = self.expected_frontend_catalog(manifest)
                frontend_routes, safe_returns = self.cleaned_frontend_contract(initialized, root)
                for scope in ['global', 'tenant']:
                    self.assertEqual(set(after[scope]), expected_frontend[scope])
                for entry in initialized['files']:
                    if entry['path'].startswith('frontend/') and entry['operation'] == 'transform':
                        frontend_text = (root / entry['path']).read_text('utf-8')
                        self.assert_frontend_actions_exist(frontend_text, expected_frontend, entry['path'])
                        self.assert_frontend_routes_exist(frontend_text, frontend_routes, entry['path'])
                        self.assert_frontend_returns_safe(frontend_text, safe_returns, entry['path'])
                        self.assert_distinct_return_proofs(frontend_text, entry['path'])
                        if entry['path'].endswith('/role.guard.spec.ts'):
                            self.assert_role_mode_semantics_preserved(entry['before'],
                                (root / entry['path']).read_text('utf-8'), expected_frontend)
                # Evaluate the generated files, after both rename and actual clean-demo application.
                e2e = 'frontend/e2e/local-integration.spec.ts'
                self.assert_e2e_helpers_defined(sources[e2e].decode('utf-8'), (root / e2e).read_text('utf-8'))
                deploy = 'backend/vendas-deploy/src/test/java/' + group.replace('.', '/') + '/vendas/'
                self.assert_revocation_keeps_other_tenant_action((root / deploy / 'FirebaseBackendIntegrationTest.java').read_text('utf-8'))
                self.assert_migration_boundaries_dynamic((root / deploy / 'MigrationContractIntegrationTest.java').read_text('utf-8'))
                controller = root / ('backend/vendas-core/src/main/java/' + group.replace('.', '/')
                                     + '/vendas/administracao/controller/TenantAdministrationController.java')
                controller_text = controller.read_text('utf-8')
                self.assertIn('@TenantEndpoint', controller_text)
                self.assertRegex(controller_text, r'@PostMapping\("/memberships/search"\)\s*'
                                 r'@PreAuthorize\("hasRole\(\x27CONSULTAR_MEMBERSHIP\x27\)"\)')
                self.assertIn('CONSULTAR_MEMBERSHIP', after['tenant'])
                # Independently expand tenant XML in include order with and without DemoRegistro.
                import xml.etree.ElementTree as ET
                resources = 'backend/vendas-deploy/src/main/resources/'
                master = resources + 'db/changelog/changelog-firebase-tenant.xml'
                def changesets(relative, read):
                    result = []
                    for child in ET.fromstring(read(relative)):
                        tag = child.tag.rsplit('}', 1)[-1]
                        if tag == 'include':
                            included = (str(Path(relative).parent / child.attrib['file']).replace('\\', '/')
                                        if child.attrib.get('relativeToChangelogFile') == 'true'
                                        else resources + child.attrib['file'])
                            result.extend(changesets(included, read))
                        elif tag == 'changeSet':
                            result.append(child.attrib['id'] + '|' + child.attrib['author'] + '|'
                                          + relative.removeprefix(resources))
                    return result
                old = changesets(master, lambda relative: sources[relative])
                new = changesets(master, lambda relative: (root / relative).read_bytes())
                rename = 'firebase-name-tenant-constraints|app|db/changelog/changelog-firebase-tenant-constraint-names.xml'
                self.assertEqual((old.index(rename), new.index(rename)), (3, 2))
                self.assertEqual(new[:new.index(rename)], [change for change in old[:old.index(rename)]
                                                          if 'changelog-demo-registro.xml' not in change])


if __name__ == '__main__':
    unittest.main()
