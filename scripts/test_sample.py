"""Adversarial clean-demo checks; fixtures stay under the task evidence directory."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import unittest
import uuid
from unittest.mock import patch
import contextlib
import io

SOURCE = Path(__file__).resolve().parents[1]
OUTPUT = Path(os.environ.get('SAMPLE_TEST_OUTPUT', str(
    Path(os.environ.get('TEMP', str(SOURCE))) / 't028c9-clean-demo-fixtures')))
CLI = Path(os.environ.get('SAMPLE_TEST_CLI', str(SOURCE / 'scripts/sample.py')))


def hashes(root):
    return {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in root.rglob('*') if p.is_file() and '.git' not in p.parts}


class CleanDemoSafety(unittest.TestCase):
    def test_manifest_operation_counts_match_files(self):
        manifest = json.loads((SOURCE / 'scripts/demo-manifest.json').read_text('utf-8'))
        actual = {operation: sum(entry['operation'] == operation for entry in manifest['files'])
                  for operation in manifest['counts']}
        self.assertEqual(actual['delete'], 27)
        self.assertEqual(manifest['counts'], actual)

    def setUp(self):
        self.root = OUTPUT / ('fixture-' + uuid.uuid4().hex)
        self.root.mkdir(parents=True)
        manifest = json.loads((SOURCE / 'scripts/demo-manifest.json').read_text('utf-8'))
        for entry in manifest['files']:
            path = entry['path']
            destination = self.root / path
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(SOURCE / path, destination)
        for path in manifest['metadata']:
            destination = self.root / path
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(SOURCE / path, destination)
        for name in ['.gitattributes', '.gitignore']:
            shutil.copyfile(SOURCE / name, self.root / name)
        (self.root / 'protected.txt').write_bytes(b'foundation bytes')
        self.git('init', '-q')
        self.git('add', '.')
        self.commit()
        self.sentinel = OUTPUT / ('sentinel-' + uuid.uuid4().hex)
        self.sentinel.write_bytes(b'EXTERNAL SENTINEL\x00\xff')

    def git(self, *args):
        return subprocess.run(['git', '-C', str(self.root), *args], check=True,
                              capture_output=True, timeout=30)

    def commit(self):
        self.git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.test',
                 'commit', '-qm', 'Disposable current content')

    def invoke(self, *args):
        return subprocess.run([sys.executable, '-B', str(CLI), 'clean-demo', *args],
                              cwd=OUTPUT, capture_output=True, text=True, timeout=60)

    def refused(self, *args):
        before = hashes(self.root)
        result = self.invoke(*args)
        self.assertNotEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(hashes(self.root), before)
        self.assertEqual(self.sentinel.read_bytes(), b'EXTERNAL SENTINEL\x00\xff')

    def test_default_and_explicit_dryrun_are_byte_preserving(self):
        before = hashes(self.root)
        for flags in ([], ['--dry-run']):
            result = self.invoke('--root', str(self.root), *flags)
            self.assertEqual(result.returncode, 0, result.stderr)
            plan = json.loads(result.stdout)
            self.assertEqual(plan['mode'], 'dry-run')
            self.assertEqual(len(plan['delete']), 27)
            self.assertEqual(len(plan['regenerate']), 3)
            self.assertEqual(hashes(self.root), before)

    def test_explicit_absolute_root_is_required(self):
        self.refused()
        self.refused('--root', '.')

    def test_flags_are_mutually_exclusive(self):
        self.refused('--root', str(self.root), '--apply', '--dry-run')

    def test_modified_tracked_file_is_refused(self):
        (self.root / 'protected.txt').write_bytes(b'changed foundation')
        self.refused('--root', str(self.root), '--apply')
        self.refused('--root', str(self.root), '--dry-run')

    def test_untracked_file_is_refused(self):
        (self.root / 'untracked.txt').write_bytes(b'untracked')
        self.refused('--root', str(self.root), '--apply')
        self.refused('--root', str(self.root), '--dry-run')

    def test_committed_missing_anchor_refuses_before_first_delete(self):
        path = self.root / 'frontend/src/app/app.routes.ts'
        path.write_text(path.read_text('utf-8').replace('demo-registro', 'unexpected'), encoding='utf-8')
        self.git('add', '.')
        self.commit()
        self.refused('--root', str(self.root), '--apply')

    def test_committed_missing_item_refuses_whole_plan(self):
        path = self.root / 'backend/app-model/src/main/java/br/com/gems/app/model/dto/DemoRegistroDTO.java'
        path.unlink()
        self.git('add', '.')
        self.commit()
        self.refused('--root', str(self.root), '--apply')

    def test_junction_ancestor_is_refused(self):
        link = OUTPUT / ('junction-' + uuid.uuid4().hex)
        if os.name == 'nt':
            environment = dict(os.environ, SAMPLE_TEST_LINK=str(link), SAMPLE_TEST_TARGET=str(self.root))
            subprocess.run(['powershell', '-NoProfile', '-Command',
                            'New-Item -ItemType Junction -Path $env:SAMPLE_TEST_LINK -Target $env:SAMPLE_TEST_TARGET | Out-Null'],
                           env=environment, check=True, capture_output=True, timeout=30)
        else:
            link.symlink_to(self.root, target_is_directory=True)
        self.refused('--root', str(link), '--apply')

    def test_junction_in_source_tree_is_refused(self):
        source = self.root / 'frontend/src/app/core/auth'
        destination = OUTPUT / ('external-auth-' + uuid.uuid4().hex)
        self.assertTrue(source.resolve().is_relative_to(self.root.resolve()))
        self.assertTrue(destination.absolute().is_relative_to(OUTPUT.absolute()))
        source.rename(destination)
        if os.name == 'nt':
            environment = dict(os.environ, SAMPLE_TEST_LINK=str(source), SAMPLE_TEST_TARGET=str(destination))
            subprocess.run(['powershell', '-NoProfile', '-Command',
                            'New-Item -ItemType Junction -Path $env:SAMPLE_TEST_LINK -Target $env:SAMPLE_TEST_TARGET | Out-Null'],
                           env=environment, check=True, capture_output=True, timeout=30)
        else:
            source.symlink_to(destination, target_is_directory=True)
        self.refused('--root', str(self.root), '--apply')

    def test_closed_paths_refuse_escape_and_absolute_destination(self):
        specification = importlib.util.spec_from_file_location('sample_cli', CLI)
        module = importlib.util.module_from_spec(specification)
        specification.loader.exec_module(module)
        for relative in ['../sentinel', str(self.sentinel), '/outside', 'frontend/../outside',
                         'frontend\\outside', 'frontend//outside', 'frontend/./outside', 'C:outside']:
            with self.subTest(path=relative), self.assertRaises(module.Refusal):
                module.inside(self.root, relative, required=False)
        self.assertEqual(self.sentinel.read_bytes(), b'EXTERNAL SENTINEL\x00\xff')

    def test_missing_catalog_prerequisite_refuses_without_mutation(self):
        self.refused('--root', str(self.root), '--apply')

    def test_ignored_catalog_destination_junction_is_refused(self):
        source = self.root / 'backend/app-security/target/classes'
        destination = OUTPUT / ('external-classes-' + uuid.uuid4().hex)
        self.assertTrue(source.absolute().is_relative_to(self.root.absolute()))
        destination.mkdir()
        (destination / 'sentinel').write_bytes(b'classes sentinel')
        source.parent.mkdir()
        if os.name == 'nt':
            environment = dict(os.environ, SAMPLE_TEST_LINK=str(source), SAMPLE_TEST_TARGET=str(destination))
            subprocess.run(['powershell', '-NoProfile', '-Command',
                            'New-Item -ItemType Junction -Path $env:SAMPLE_TEST_LINK -Target $env:SAMPLE_TEST_TARGET | Out-Null'],
                           env=environment, check=True, capture_output=True, timeout=30)
        else:
            source.symlink_to(destination, target_is_directory=True)
        self.refused('--root', str(self.root), '--dry-run')
        self.assertEqual((destination / 'sentinel').read_bytes(), b'classes sentinel')

    def test_git_worktree_metadata_link_is_refused(self):
        git = self.root / '.git'
        destination = OUTPUT / ('git-metadata-' + uuid.uuid4().hex)
        # Only this verified fixture directory is moved; original checkout is untouched.
        git.rename(destination)
        git.write_text('gitdir: ' + str(destination), encoding='utf-8')
        self.refused('--root', str(self.root), '--apply')

    def module(self):
        specification = importlib.util.spec_from_file_location('sample_regression', CLI)
        module = importlib.util.module_from_spec(specification)
        specification.loader.exec_module(module)
        return module

    def prepared_apply(self):
        module = self.module()
        clean = module.plan(self.root)
        security = clean[3].parent
        security.mkdir(parents=True, exist_ok=True)
        jar = security / 'fixture.jar'
        jar.write_bytes(b'offline jar')
        clean[3].write_text(str(jar), encoding='utf-8')
        tool = security / 'fixture-tool.exe'
        tool.write_bytes(b'offline tool')
        return module, clean, tool

    def test_prettier_formats_closed_frontend_list_and_replay_matches(self):
        module, clean, tool = self.prepared_apply()
        dry_plan = json.loads(json.dumps(clean[0]))
        expected = sorted(relative for relative in clean[0]['transform'] + clean[0]['regenerate']
                          if relative.startswith('frontend/')
                          and Path(relative).suffix in {'.ts', '.html', '.scss', '.json'})
        self.assertEqual(clean[0]['formatted'], expected)
        calls = []

        def catalog(argv, cwd):
            if argv == ['javac', '-version']:
                return 'javac 21.0.0'
            if argv[:3] == ['node', 'scripts/action-catalog.mjs', '--write']:
                for relative in clean[0]['regenerate']:
                    (self.root / relative).write_bytes(b'{}  \n')
            return ''

        def prettier(argv, cwd, timeout=120):
            self.assertEqual(argv[:3], ['node', 'node_modules/prettier/bin/prettier.cjs', '--write'])
            self.assertEqual(cwd, self.root / 'frontend')
            self.assertEqual(argv[3:], [relative.removeprefix('frontend/') for relative in expected])
            calls.append(argv.copy())
            for relative in argv[3:]:
                path = cwd / relative
                path.write_text('\n'.join(line.rstrip() for line in path.read_text('utf-8').splitlines())
                                + '\n', encoding='utf-8', newline='\n')

        def run(plan):
            with patch.object(module, 'command', side_effect=catalog), \
                    patch('shutil.which', return_value=str(tool)), contextlib.redirect_stderr(io.StringIO()):
                module.apply(self.root, *plan, prettier_runner=prettier)

        run(clean)
        first = {relative: (self.root / relative).read_bytes()
                 for relative in clean[0]['transform'] + clean[0]['regenerate']}
        self.assertEqual(clean[0]['formatted'], expected, 'Dry-run/apply formatting plans must match')
        self.assertTrue(all(clean[0]['hashes_after'][relative] == module.digest(data)
                            for relative, data in first.items()))
        prettier(calls[0], self.root / 'frontend')
        self.assertEqual(first, {relative: (self.root / relative).read_bytes() for relative in first})
        # Independent replay starts from the exact original source bytes, with no network.
        for relative, data in clean[1].items():
            (self.root / relative).write_bytes(data)
        replay = module.plan(self.root)
        self.assertEqual(replay[0], dry_plan, 'Independent dry-run plans must be identical')
        self.assertEqual(replay[0]['formatted'], expected)
        run(replay)
        self.assertEqual(first, {relative: (self.root / relative).read_bytes() for relative in first})

    def test_prettier_failure_rolls_back_all_manifest_sources(self):
        module, clean, tool = self.prepared_apply()

        def failed(argv, cwd, timeout=120):
            (cwd / argv[3]).write_bytes(b'partial formatter output')
            raise module.Refusal('offline formatter failure')

        def catalog(argv, cwd):
            return 'javac 21.0.0' if argv == ['javac', '-version'] else ''

        with patch.object(module, 'command', side_effect=catalog), \
                patch('shutil.which', return_value=str(tool)), contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaisesRegex(module.Refusal, 'Prettier formatting failed.*rolled back'):
                module.apply(self.root, *clean, prettier_runner=failed)
        for relative, original in clean[1].items():
            self.assertEqual((self.root / relative).read_bytes(), original, relative)

    def test_missing_project_prettier_refuses_before_mutation(self):
        module, clean, tool = self.prepared_apply()
        before = hashes(self.root)
        with self.assertRaisesRegex(module.Refusal, 'Project Prettier is unavailable.*npm ci'):
            module.apply(self.root, *clean)
        self.assertEqual(hashes(self.root), before)
        self.assertFalse((clean[3].parent / 'clean-demo-backup.json').exists())

    def test_prettier_resolves_only_the_copy_project_cli(self):
        module = self.module()
        cli = self.root / 'frontend/node_modules/prettier/bin/prettier.cjs'
        cli.parent.mkdir(parents=True)
        cli.write_bytes(b'offline fixture CLI')
        (cli.parent.parent / 'package.json').write_bytes(b'{}')
        tool = cli.parent / 'node.exe'
        tool.write_bytes(b'offline node')
        with patch('shutil.which', return_value=str(tool)):
            self.assertEqual(module.clean_demo_prettier(self.root), [str(tool), str(cli), '--write'])

    def test_app_spec_payload_and_manifest_pin_are_updated(self):
        module = self.module()
        data = module.MANIFEST.read_bytes()
        self.assertEqual(module.digest(data), module.MANIFEST_SHA256)
        entry = next(entry for entry in json.loads(data)['files']
                     if entry['path'] == 'frontend/src/app/app.spec.ts')
        self.assertIn("])('devePreservarRetorno_quandoLoginEmAndamentoEm %s', (url) => {", entry['after'])
        self.assertEqual(module.digest(entry['after'].encode('utf-8')), entry['after_sha256'])

    def test_regular_external_hardlinks_refuse_before_any_write(self):
        module = self.module()
        manifest = json.loads((SOURCE / 'scripts/demo-manifest.json').read_text('utf-8'))
        surfaces = [entry['path'] for entry in manifest['files']
                    if entry['operation'] in {'transform', 'regenerate'}]
        surfaces += ['backend/app-security/target/clean-demo-backup.json',
                     'backend/app-security/target/clean-demo-java-0.args',
                     'backend/app-security/target/classes/br/com/gems/app/security/AcaoSistema.class']
        for relative in surfaces:
            with self.subTest(path=relative):
                target = self.root / relative
                original = target.read_bytes() if target.exists() else None
                target.parent.mkdir(parents=True, exist_ok=True)
                if target.exists():
                    target.unlink()
                self.sentinel.write_bytes(original if original is not None else b'external output')
                os.link(self.sentinel, target)
                before = hashes(self.root)
                external = self.sentinel.read_bytes()
                self.assertGreater(target.stat().st_nlink, 1)
                # No apply/rollback runs: total preflight must refuse the real inode.
                try:
                    with self.assertRaisesRegex(module.Refusal, 'Hardlinked'):
                        module.plan(self.root)
                    self.assertEqual(hashes(self.root), before)
                    self.assertEqual(self.sentinel.read_bytes(), external)
                    self.assertFalse((self.root / 'backend/app-security/target/clean-demo-java-2.args').exists())
                finally:
                    target.unlink()
                    if original is not None:
                        target.write_bytes(original)

    def test_output_type_collisions_refuse_total_preflight(self):
        module = self.module()
        directories = ['target', 'target/classes', 'target/classes/br',
                       'target/classes/br/com/gems/app/security']
        files = ['target/clean-demo-backup.json', 'target/clean-demo-java-0.args',
                 'target/clean-demo-java-1.args', 'target/clean-demo-java-2.args',
                 'target/catalog-classpath.txt',
                 'target/classes/br/com/gems/app/security/AcaoSistema.class',
                 'target/classes/br/com/gems/app/security/GenerateActionCatalog.class']
        for relative in directories + files:
            with self.subTest(path=relative):
                path = self.root / 'backend/app-security' / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                if relative in directories:
                    path.write_bytes(b'directory collision')
                else:
                    path.mkdir()
                if relative == 'target':
                    # Git ignores target directories, not a file named target.
                    self.git('add', '.')
                    self.commit()
                before = hashes(self.root)
                try:
                    with self.assertRaisesRegex(module.Refusal, 'directory|Directory|ancestor'):
                        module.plan(self.root)
                    self.assertEqual(hashes(self.root), before)
                finally:
                    # Remove this one collision only; never recursively delete fixture trees.
                    if path.is_file():
                        path.unlink()
                    else:
                        path.rmdir()
                    if relative == 'target':
                        self.git('add', '.')
                        self.commit()

    def test_native_switch_payload_uses_service_and_preserves_disabled_ui(self):
        manifest = json.loads((CLI.with_name('demo-manifest.json')).read_text('utf-8'))
        payload = next(entry['after'] for entry in manifest['files']
                       if entry['path'] == 'frontend/e2e/local-integration.spec.ts')
        start = payload.index("for (const invalidation of ['switch', 'logout'])")
        switch = payload[start:payload.index("test('failed Beta", start)]
        self.assertIn("toBeDisabled()", switch)
        self.assertIn("await root.auth.selectTenant('beta')", switch)
        self.assertNotIn("name: 'Trocar empresa' }).click()", switch)
        self.assertIn('releaseNativeToken(page)', switch)
        self.assertIn('finally', switch)

    def test_apply_hardlink_refuses_before_backup_or_generator(self):
        target = self.root / 'frontend/src/app/app.routes.ts'
        self.sentinel.write_bytes(target.read_bytes())
        external = self.sentinel.read_bytes()
        target.unlink()
        os.link(self.sentinel, target)
        before = hashes(self.root)
        result = self.invoke('--root', str(self.root), '--apply')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Hardlinked', result.stderr)
        self.assertNotIn('validated_plan', result.stderr)
        self.assertEqual(hashes(self.root), before)
        self.assertEqual(self.sentinel.read_bytes(), external)
        self.assertFalse((self.root / 'backend/app-security/target/clean-demo-backup.json').exists())

    def test_apply_classes_file_refuses_before_backup_or_generator(self):
        classes = self.root / 'backend/app-security/target/classes'
        classes.parent.mkdir(parents=True)
        classes.write_bytes(b'classes file sentinel')
        before = hashes(self.root)
        result = self.invoke('--root', str(self.root), '--apply')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Directory destination', result.stderr)
        self.assertNotIn('validated_plan', result.stderr)
        self.assertEqual(hashes(self.root), before)
        self.assertFalse((classes.parent / 'clean-demo-backup.json').exists())


if __name__ == '__main__':
    unittest.main()
