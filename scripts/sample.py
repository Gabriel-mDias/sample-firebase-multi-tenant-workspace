"""Remove the optional sample domain from an explicit, clean checkout (stdlib only)."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import stat
import subprocess
import sys
import time
from datetime import datetime, timezone
import xml.etree.ElementTree as ET

MANIFEST_SHA256 = '1c2f95e0a20980ac81bde0e2833a6a7b142886c6b5c8736d6e2bd2398d97fed5'
MANIFEST = Path(__file__).with_name('demo-manifest.json')
PRUNED = {'.git', 'node_modules', 'target', 'dist', '.angular', '__pycache__', '.cache'}
EXECUTIONS = []
JAVA_KEYWORDS = set(('abstract assert boolean break byte case catch char class const continue '
                     'default do double else enum extends final finally float for goto if implements '
                     'import instanceof int interface long native new package private protected public '
                     'return short static strictfp super switch synchronized this throw throws transient '
                     'try void volatile while true false null var yield record sealed permits _').split())
APP_MODULES = {'app-security', 'app-model', 'app-events', 'app-core', 'app-deploy'}
WINDOWS_DEVICES = {'con', 'prn', 'aux', 'nul', *['com' + str(i) for i in range(1, 10)],
                   *['lpt' + str(i) for i in range(1, 10)]}
INITIALIZER_METADATA = {'backend/pom.xml', 'frontend/angular.json', 'frontend/package.json',
                        'frontend/package-lock.json', 'frontend/docker-entrypoint.sh',
                        'frontend/scripts/verify-authorization-actions.mjs',
                        'frontend/e2e/local-integration.spec.ts', 'frontend/playwright.config.ts',
                        'environments/local/docker-compose.yml', 'README.md', 'backend/README.md',
                        'backend/Dockerfile', 'frontend/Dockerfile', 'frontend/e2e/README.md'}


class Refusal(ValueError):
    pass


def digest(data):
    return hashlib.sha256(data).hexdigest()


def secret_path(relative):
    """Exclude credential surfaces before opening any contents (including tracked mistakes)."""
    path = PurePosixPath(relative)
    name = path.name.lower()
    return (any(part in PRUNED for part in path.parts)
            or name == '.env' or name.startswith('.env.') and name != '.env.example'
            or name in {'.npmrc', 'settings.xml', 'application_default_credentials.json'}
            or any(marker in name for marker in ['private-key', 'private_key', 'service-account',
                                                'service_account', 'credentials'])
            or path.suffix.lower() in {'.pem', '.p12', '.pfx', '.key'})


def managed_parts(text):
    begin, end = '<!-- GEMS-AI:BEGIN -->', '<!-- GEMS-AI:END -->'
    if text.count(begin) != text.count(end) or text.count(begin) > 1:
        raise Refusal('Malformed managed agent block')
    if begin not in text:
        return text, '', ''
    start, finish = text.index(begin), text.index(end) + len(end)
    if finish <= start:
        raise Refusal('Malformed managed agent block ordering')
    return text[:start], text[start:finish], text[finish:]


def rename_path(relative, name, group):
    parts = relative.split('/')
    if len(parts) > 1 and parts[0] == 'backend' and parts[1] in APP_MODULES:
        parts[1] = name + parts[1][3:]
    result = '/'.join(parts).replace('/java/br/com/gems/app/',
                                    '/java/' + group.replace('.', '/') + '/' + name + '/')
    if result.endswith('/AppApplication.java'):
        result = result[:-len('AppApplication.java')] + name.capitalize() + 'Application.java'
    return result


def sdk_coordinates(text):
    namespace = {'m': 'http://maven.apache.org/POM/4.0.0'}
    pom = ET.fromstring(text)
    return [ET.tostring(node, encoding='unicode') for node in pom.findall('.//m:dependency', namespace)
            if (node.findtext('m:artifactId', namespaces=namespace) or '').startswith('gems-')]


def rename_pom(text, relative, name, group, description):
    """Change application coordinates by artifact identity, never a whole group replacement."""
    namespace = {'m': 'http://maven.apache.org/POM/4.0.0'}
    original = ET.fromstring(text)
    is_parent = relative == 'backend/pom.xml'
    expected = 'app' if is_parent else relative.split('/')[1]
    if original.findtext('m:artifactId', namespaces=namespace) != expected:
        raise Refusal('Application artifact metadata conflict: ' + relative)
    if is_parent:
        if original.findtext('m:groupId', namespaces=namespace) != 'br.com.gems':
            raise Refusal('Application group metadata conflict')
        tag = 'parent' if original.find('m:parent', namespace) is not None else 'modelVersion'
        text, count = re.subn(r'(</' + tag + r'>\s*<groupId>)br\.com\.gems(</groupId>)',
                              lambda m: m[1] + group + m[2], text, count=1)
        if count != 1:
            raise Refusal('Nonunique application group anchor')
    else:
        parent = original.find('m:parent', namespace)
        if parent is None or parent.findtext('m:artifactId', namespaces=namespace) != 'app':
            raise Refusal('Application parent metadata conflict: ' + relative)
    def coordinate(match):
        block = match[0]
        artifact = re.search(r'<artifactId>([^<]+)</artifactId>', block)
        if artifact and artifact[1] in APP_MODULES | {'app'}:
            if len(re.findall(r'<groupId>br\.com\.gems</groupId>', block)) != 1:
                raise Refusal('Application coordinate group conflict: ' + relative)
            return block.replace('<groupId>br.com.gems</groupId>', '<groupId>' + group + '</groupId>')
        return block
    text = re.sub(r'<(?:parent|dependency)>.*?</(?:parent|dependency)>', coordinate, text, flags=re.S)
    for module in sorted(APP_MODULES):
        text = text.replace(module, name + module[3:])
    if is_parent:
        text, count = re.subn(r'<artifactId>app</artifactId>', '<artifactId>' + name + '</artifactId>', text)
        if count != 1:
            raise Refusal('Nonunique application artifact anchor')
        text = text.replace('<name>App</name>', '<name>' + name.capitalize() + '</name>')
        if description:
            from xml.sax.saxutils import escape
            text, count = re.subn(r'<description>[^<]*</description>',
                                  lambda _: '<description>' + escape(description) + '</description>', text, count=1)
            if count != 1:
                raise Refusal('Missing description anchor')
    else:
        text = text.replace('<artifactId>app</artifactId>', '<artifactId>' + name + '</artifactId>')
        text = text.replace('<name>App ::', '<name>' + name.capitalize() + ' ::')
    if sdk_coordinates(text) != sdk_coordinates(ET.tostring(original, encoding='unicode')):
        raise Refusal('Published SDK coordinates changed')
    return text


def rename_text(text, relative, name, group, description):
    if relative.endswith('/pom.xml'):
        return rename_pom(text, relative, name, group, description)
    if relative in {'frontend/package.json', 'frontend/package-lock.json'}:
        data = json.loads(text)
        if data.get('name') != 'app':
            raise Refusal('Frontend product metadata conflict')
        data['name'] = name
        if relative.endswith('package-lock.json'):
            if data['packages'][''].get('name') != 'app':
                raise Refusal('Frontend lock product metadata conflict')
            data['packages']['']['name'] = name
        return json.dumps(data, ensure_ascii=False, indent=2) + '\n'
    if relative == 'frontend/angular.json':
        data = json.loads(text)
        if set(data.get('projects', {})) != {'app'}:
            raise Refusal('Angular product metadata conflict')
        project = data['projects'].pop('app')
        def targets(value):
            if isinstance(value, dict):
                for key, item in value.items():
                    if key in {'buildTarget', 'browserTarget'} and isinstance(item, str) and item.startswith('app:'):
                        value[key] = name + item[3:]
                    else:
                        targets(item)
            elif isinstance(value, list):
                for item in value:
                    targets(item)
        targets(project)
        data['projects'][name] = project
        return json.dumps(data, ensure_ascii=False, indent=2) + '\n'
    prefix, managed, suffix = managed_parts(text)
    def transform(value):
        value = re.sub(r'br\.com\.gems\.app(?!\w)', group + '.' + name, value)
        value = value.replace('br/com/gems/app/', group.replace('.', '/') + '/' + name + '/')
        value = re.sub(r'\bAppApplication\b', name.capitalize() + 'Application', value)
        for module in sorted(APP_MODULES):
            value = value.replace(module, name + module[3:])
        value = value.replace('app.jobs.', name + '.jobs.')
        if relative == 'environments/local/docker-compose.yml':
            value = re.sub(r'\bapp(?=[_-](?:containers|postgres|auth-emulator|backend|frontend|network|localstack))', name, value)
            value = value.replace('gems-demo', name + '-demo')
        if relative.endswith('/application.yml'):
            value = value.replace('name: gems-firebase-multi-tenant', 'name: ' + name)
        if relative == 'frontend/Dockerfile':
            value = value.replace('COPY dist/app/browser/', 'COPY dist/' + name + '/browser/')
        return value
    result = transform(prefix) + managed + transform(suffix)
    if managed_parts(result)[1] != managed:
        raise Refusal('Managed block drift')
    return result


def initialize_plan(root, name, group, description, skip_format=False):
    if (not re.fullmatch(r'[a-z][a-z0-9]*', name) or name in JAVA_KEYWORDS | WINDOWS_DEVICES | {'app'}
            or not re.fullmatch(r'[a-z][a-z0-9]*(?:\.[a-z][a-z0-9]*)+', group)
            or any(part in JAVA_KEYWORDS | WINDOWS_DEVICES for part in group.split('.'))):
        raise Refusal('Invalid Java product/group identifier')
    if len(description) > 500 or any(ord(c) < 32 or ord(c) == 127 for c in description):
        raise Refusal('Description must be one printable line (maximum 500 characters)')
    checkout(root)
    tracked = command(['git', '--no-optional-locks', '-C', str(root), 'ls-files', '-z'], root).split('\x00')
    tracked = sorted(path for path in tracked if path)
    if len(tracked) != len({path.casefold() for path in tracked}):
        raise Refusal('Tracked path case collision')
    for relative in tracked:
        inside(root, relative)
    reactor = ET.fromstring(inside(root, 'backend/pom.xml').read_bytes())
    namespace = {'m': 'http://maven.apache.org/POM/4.0.0'}
    modules = [m.text for m in reactor.findall('m:modules/m:module', namespace)]
    if len(modules) != 5 or set(modules) != APP_MODULES:
        raise Refusal('Expected the five original application modules')
    selected = [relative for relative in tracked if not secret_path(relative)
                and (relative in INITIALIZER_METADATA or
                     len(relative.split('/')) > 2 and relative.split('/')[0] == 'backend'
                     and relative.split('/')[1] in APP_MODULES
                     and PurePosixPath(relative).name not in {'AGENTS.md', 'CLAUDE.md', 'CODEX.md', 'GEMINI.md'}
                     and PurePosixPath(relative).suffix in {'.java', '.xml', '.yml', '.yaml', '.json', '.md', '.properties'})]
    originals, staged, destinations = {}, {}, {}
    for relative in selected:
        original = inside(root, relative).read_bytes()
        destination = rename_path(relative, name, group)
        result = rename_text(original.decode('utf-8'), relative, name, group, description).encode('utf-8')
        if result != original or destination != relative:
            originals[relative] = original
            staged[destination] = result
            destinations[relative] = destination
    manifest_bytes = inside(root, 'scripts/demo-manifest.json').read_bytes()
    if digest(manifest_bytes) != MANIFEST_SHA256:
        raise Refusal('Closed manifest integrity conflict')
    manifest = json.loads(manifest_bytes)
    for entry in manifest['files'] + manifest['metadata_entries']:
        relative = entry['path']
        if secret_path(relative):
            raise Refusal('Secret path in manifest')
        data = inside(root, relative).read_bytes()
        if digest(data) != entry['sha256']:
            raise Refusal('Rename manifest content conflict: ' + relative)
        destination = rename_path(relative, name, group)
        entry['path'] = destination
        entry['sha256'] = digest(staged.get(destination, data))
        if entry.get('operation') == 'transform':
            if entry['before'].encode('utf-8') != data:
                raise Refusal('Rename manifest anchor conflict')
            for field in ['before', 'after']:
                entry[field] = rename_text(entry[field], relative, name, group, description)
            entry['after_sha256'] = digest(entry['after'].encode('utf-8'))
    manifest['metadata'] = [rename_path(relative, name, group) for relative in manifest['metadata']]
    manifest['initialized_product'] = {'name': name, 'group': group}
    manifest_new = (json.dumps(manifest, ensure_ascii=False, indent=2) + '\n').encode('utf-8')
    script = inside(root, 'scripts/sample.py').read_bytes()
    script_new, count = re.subn(rb"MANIFEST_SHA256 = '[a-f0-9]{64}'",
                               ("MANIFEST_SHA256 = '" + digest(manifest_new) + "'").encode(), script)
    if count != 1:
        raise Refusal('Nonunique manifest pin')
    for relative, before, after in [('scripts/demo-manifest.json', manifest_bytes, manifest_new),
                                    ('scripts/sample.py', script, script_new)]:
        originals[relative], staged[relative], destinations[relative] = before, after, relative
    if len(staged) != len({path.casefold() for path in staged}):
        raise Refusal('Rename destination collision')
    for source, destination in destinations.items():
        target = inside(root, destination, required=False)
        if destination != source and target.exists():
            raise Refusal('Rename destination already exists: ' + destination)
    for module in modules:
        target = root / 'backend' / (name + module[3:])
        unlinked(target)
        if target.exists():
            raise Refusal('Module destination already exists: ' + target.name)
    report = {'root': str(root), 'mode': 'dry-run', 'product': {'name': name, 'group': group},
              'changes': [{'source': source, 'destination': destination, 'before_sha256': digest(originals[source]),
                           'after_sha256': digest(staged[destination])} for source, destination in sorted(destinations.items())],
              'manifest_sha256': digest(manifest_new), 'clean_demo': ['python', '-B', 'scripts/sample.py',
                  'clean-demo', '--root', str(root), '--dry-run'],
              'services_started': [], 'managed_snapshot': 'byte-preserved',
              'formatting': {'enabled': not skip_format, 'cwd': '.',
                             'argv': ['gems-ai', *formatter_arguments(root)],
                             'manifest_java_payloads': True}, 'formatted': []}
    return report, originals, staged, destinations


def formatter_arguments(root):
    return ['format', 'java', '--project', str(root), '--all', '--apply']


def initializer_formatter(root, runner, resolve=True):
    """Check the installed GEMS AI CLI, its pinned version and Java before the first rename write."""
    executable = 'gems-ai'
    # Injected runners are offline unit doubles; production always resolves the installed CLI.
    if resolve:
        import shutil
        resolved = shutil.which(executable)
        if not resolved:
            raise Refusal('Backend formatting requires the GEMS AI CLI (gems-ai) on PATH')
        unlinked(Path(resolved).absolute())
        executable = resolved
    expected = json.loads(inside(root, 'gems-ai.json').read_bytes()).get('version')
    try:
        version = runner([executable, '--version'], root, timeout=30)
    except (OSError, Refusal, subprocess.TimeoutExpired) as error:
        raise Refusal('Backend formatting requires the GEMS AI CLI (gems-ai) on PATH') from error
    if not expected or version.strip() != expected:
        raise Refusal('GEMS AI CLI version differs from gems-ai.json')
    java_home = os.environ.get('JAVA_HOME')
    java = str(Path(java_home) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')) if java_home else 'java'
    try:
        runner([java, '-version'], root, timeout=30)
    except (OSError, Refusal, subprocess.TimeoutExpired) as error:
        raise Refusal('Backend formatting requires available Java (JAVA_HOME or PATH)') from error
    return [executable, *formatter_arguments(root)]


def format_initialized_backend(root, report, argv, java_paths, runner):
    """Use the project's formatter for both live Java and clean-demo replacement payloads."""
    before = {relative: inside(root, relative).read_bytes() for relative in java_paths}
    manifest_path = inside(root, 'scripts/demo-manifest.json')
    manifest = json.loads(manifest_path.read_bytes())
    try:
        runner(argv, root, timeout=600)
        formatted = {relative: inside(root, relative).read_bytes() for relative in java_paths}
        replacements = [entry for entry in manifest['files'] if entry.get('operation') == 'transform'
                        and entry['path'] in formatted]
        # A subsequent clean-demo must not reintroduce lines made too long by the rename.
        try:
            for entry in replacements:
                inside(root, entry['path']).write_bytes(entry['after'].encode('utf-8'))
            if replacements:
                runner(argv, root, timeout=600)
                for entry in replacements:
                    entry['after'] = inside(root, entry['path']).read_bytes().decode('utf-8')
                    entry['after_sha256'] = digest(entry['after'].encode('utf-8'))
        finally:
            for relative, data in formatted.items():
                inside(root, relative).write_bytes(data)
    except (OSError, Refusal, subprocess.TimeoutExpired) as error:
        raise Refusal('Backend GEMS AI formatting failed; initialization rolled back. '
                      'Check the GEMS AI CLI and Java 21+; --skip-format is an offline '
                      'fallback only, never for gates.') from error
    for entry in manifest['files'] + manifest['metadata_entries']:
        if entry['path'] in formatted:
            entry['sha256'] = digest(formatted[entry['path']])
            if entry.get('operation') == 'transform':
                entry['before'] = formatted[entry['path']].decode('utf-8')
    manifest_data = (json.dumps(manifest, ensure_ascii=False, indent=2) + '\n').encode('utf-8')
    script_path = inside(root, 'scripts/sample.py')
    before.update({'scripts/demo-manifest.json': manifest_path.read_bytes(),
                   'scripts/sample.py': script_path.read_bytes()})
    script_data, count = re.subn(rb"MANIFEST_SHA256 = '[a-f0-9]{64}'",
                                ("MANIFEST_SHA256 = '" + digest(manifest_data) + "'").encode(),
                                before['scripts/sample.py'])
    if count != 1:
        raise Refusal('Nonunique formatted manifest pin')
    manifest_path.write_bytes(manifest_data)
    script_path.write_bytes(script_data)
    formatted.update({'scripts/demo-manifest.json': manifest_data, 'scripts/sample.py': script_data})
    report['formatted'] = [{'path': relative, 'before_sha256': digest(before[relative]),
                            'after_sha256': digest(data)} for relative, data in sorted(formatted.items())
                           if data != before[relative]]
    report['formatted_manifest_sha256'] = digest(manifest_data)


def initialize_apply(root, report, originals, staged, destinations, tool_runner=None):
    checkout(root)
    for relative, original in originals.items():
        if inside(root, relative).read_bytes() != original:
            raise Refusal('Concurrent source conflict: ' + relative)
    for source, destination in destinations.items():
        if source != destination and inside(root, destination, required=False).exists():
            raise Refusal('Concurrent destination conflict: ' + destination)
    runner = tool_runner or command
    java_originals = {}
    if report['formatting']['enabled']:
        argv = initializer_formatter(root, runner, resolve=tool_runner is None)
        tracked = command(['git', '--no-optional-locks', '-C', str(root), 'ls-files', '-z'], root).split('\x00')
        java_originals = {relative: inside(root, relative).read_bytes() for relative in tracked
                          if relative.startswith('backend/') and relative.endswith('.java')
                          and not secret_path(relative)}
    print(json.dumps({'validated_plan': report}), file=sys.stderr, flush=True)
    created = []
    try:
        for destination, data in staged.items():
            target = inside(root, destination, required=False)
            target.parent.mkdir(parents=True, exist_ok=True)
            if destination not in originals:
                created.append(destination)
            target.write_bytes(data)
        for source, destination in destinations.items():
            if source != destination:
                inside(root, source).unlink()
        if report['formatting']['enabled']:
            format_initialized_backend(root, report, argv,
                                       [destinations.get(relative, relative) for relative in java_originals], runner)
    except (OSError, Refusal, subprocess.TimeoutExpired):
        for relative in created:
            path = inside(root, relative, required=False)
            if path.exists():
                path.unlink()
        for relative, original in originals.items():
            inside(root, relative, required=False).write_bytes(original)
        for relative, original in java_originals.items():
            inside(root, relative, required=False).write_bytes(original)
        raise
    report['mode'], report['result'] = 'apply', 'INITIALIZED'


def unlinked(path):
    """Inspect lexical ancestors before resolve(), including Windows reparse points."""
    for item in [*reversed(path.parents), path]:
        try:
            info = item.lstat()
        except FileNotFoundError:
            continue
        if stat.S_ISLNK(info.st_mode) or getattr(info, 'st_file_attributes', 0) & 0x400:
            raise Refusal('Linked/reparse path: ' + str(item))
        if item != path and not stat.S_ISDIR(info.st_mode):
            raise Refusal('Non-directory ancestor: ' + str(item))


def inside(root, relative, required=True):
    pure = PurePosixPath(relative)
    if (not relative or '\\' in relative or ':' in relative or pure.is_absolute()
            or any(part in {'', '.', '..'} for part in relative.split('/'))):
        raise Refusal('Invalid closed path: ' + relative)
    path = root.joinpath(*pure.parts)
    unlinked(path)
    if not path.resolve().is_relative_to(root):
        raise Refusal('Path escapes root: ' + relative)
    if required and not path.is_file():
        raise Refusal('Required file missing: ' + relative)
    if path.exists() and path.is_dir():
        raise Refusal('File destination is a directory: ' + relative)
    if path.exists():
        info = path.lstat()
        if not stat.S_ISREG(info.st_mode):
            raise Refusal('Non-regular file destination: ' + relative)
        if info.st_nlink > 1:
            raise Refusal('Hardlinked file destination: ' + relative)
    return path


def output_preflight(root, classes, main_class):
    """Validate every compiler/backup/argument destination before any mutation."""
    package = main_class.rsplit('.', 1)[0].replace('.', '/')
    package_directory = classes / package
    for directory in [classes.parent, classes, package_directory]:
        unlinked(directory)
        if directory.exists() and not directory.is_dir():
            raise Refusal('Directory destination is not a directory: ' + str(directory))
    classes_relative = classes.relative_to(root).as_posix()
    for name in ['AcaoSistema.class', 'GenerateActionCatalog.class']:
        inside(root, classes_relative + '/' + package + '/' + name, required=False)
    # javac can also write nested class outputs; inspect the complete existing output tree.
    if classes.exists():
        for directory, directories, files in os.walk(classes, followlinks=False):
            for name in directories:
                unlinked(Path(directory) / name)
            for name in files:
                inside(root, (Path(directory) / name).relative_to(root).as_posix(), required=False)
    target = classes.parent.relative_to(root).as_posix()
    for name in ['catalog-classpath.txt', 'clean-demo-backup.json',
                 *['clean-demo-java-' + str(index) + '.args' for index in range(3)]]:
        inside(root, target + '/' + name, required=False)


def command(argv, cwd, timeout=120):
    record = {'argv': argv.copy(), 'cwd': str(cwd), 'expected_exit': 0,
              'timeout_seconds': timeout, 'started_utc': datetime.now(timezone.utc).isoformat()}
    start = time.monotonic()
    result = subprocess.run(argv, cwd=cwd, shell=False, capture_output=True, timeout=timeout)
    record.update(exit=result.returncode, duration_seconds=time.monotonic() - start,
                  completed_utc=datetime.now(timezone.utc).isoformat())
    EXECUTIONS.append(record)
    if result.returncode:
        # Dependency tools can print credentials; never relay their output.
        raise Refusal('Command failed (exit %s): %s' % (result.returncode, Path(argv[0]).name))
    return result.stdout.decode('utf-8').strip()


def java_arguments(arguments):
    """Java launcher/javac argument files avoid Windows' 32767-character argv limit."""
    if any(any(character in argument for character in '\r\n\x00') for argument in arguments):
        raise Refusal('Control character in Java arguments')
    return '\n'.join('"' + argument.replace('\\', '\\\\').replace('"', '\\"') + '"'
                     for argument in arguments) + '\n'


def checkout(root):
    unlinked(root)
    if not root.is_dir() or root.resolve() != root:
        raise Refusal('Root must be a canonical absolute directory')
    unlinked(root / '.git')
    if not (root / '.git').is_dir():
        raise Refusal('A standalone Git checkout is required')
    # Check every directory entry without reading environment files or following links.
    for directory, directories, files in os.walk(root, followlinks=False):
        for name in directories + files:
            unlinked(Path(directory) / name)
        directories[:] = [name for name in directories if name not in PRUNED]
    top = command(['git', '--no-optional-locks', '-C', str(root),
                   'rev-parse', '--show-toplevel'], root)
    if Path(top).resolve() != root:
        raise Refusal('Root is not the Git top level')
    status = command(['git', '--no-optional-locks', '-c', 'core.fsmonitor=false',
                      '-C', str(root), 'status', '--porcelain=v1', '--untracked-files=all'], root)
    if status:
        raise Refusal('Git tree must be clean, including untracked files')


def metadata(root, manifest):
    """Validate Maven module and Java package facts, rather than shell substitutions."""
    for relative in manifest['metadata']:
        inside(root, relative)
    for entry in manifest['metadata_entries']:
        if digest(inside(root, entry['path']).read_bytes()) != entry['sha256']:
            raise Refusal('Protected metadata integrity conflict: ' + entry['path'])
    namespace = {'m': 'http://maven.apache.org/POM/4.0.0'}
    reactor = ET.fromstring(inside(root, 'backend/pom.xml').read_bytes())
    modules = [node.text for node in reactor.findall('m:modules/m:module', namespace)]
    if len(modules) != len(set(modules)) or any(
            not re.fullmatch(r'[a-z][a-z0-9-]*', module or '') for module in modules):
        raise Refusal('Invalid/colliding reactor modules')
    generator = [entry['path'] for entry in manifest['metadata_entries']
                 if entry['kind'] == 'generator']
    if len(generator) != 1:
        raise Refusal('Generator metadata must be unique')
    relative = generator[0]
    module = relative.split('/')[1]
    if module not in modules:
        raise Refusal('Generator module is absent from the reactor')
    pom = ET.fromstring(inside(root, 'backend/' + module + '/pom.xml').read_bytes())
    if pom.findtext('m:artifactId', namespaces=namespace) != module:
        raise Refusal('Module path/artifactId mismatch')
    source = inside(root, relative).read_text('utf-8')
    packages = re.findall(r'^package ([a-zA-Z_][\w]*(?:\.[a-zA-Z_][\w]*)*);', source, re.M)
    if len(packages) != 1 or source.count('public final class GenerateActionCatalog') != 1:
        raise Refusal('Generator package/class metadata is not unique')
    if ('import br.com.gems.security.authorization.FrontendActionCatalogGenerator;' not in source
            or 'FrontendActionCatalogGenerator.writeTo( Path.of( args[0] ), AcaoSistema.class );' not in source):
        raise Refusal('Official SDK generator delegation changed')
    return module, packages[0] + '.GenerateActionCatalog', relative


def plan(root):
    unlinked(MANIFEST.absolute())
    data = MANIFEST.read_bytes()
    if digest(data) != MANIFEST_SHA256:
        raise Refusal('Closed manifest integrity conflict')
    manifest = json.loads(data)
    entries = manifest['files']
    paths = [entry['path'] for entry in entries]
    if len(paths) != len(set(path.casefold() for path in paths)):
        raise Refusal('Manifest path collision')
    checkout(root)
    module, main_class, generator = metadata(root, manifest)
    originals, staged = {}, {}
    report = {'root': str(root), 'mode': 'dry-run', 'delete': [], 'transform': [],
              'regenerate': [], 'hashes_before': {}, 'catalog': {}}
    for entry in entries:
        relative = entry['path']
        path = inside(root, relative)
        original = path.read_bytes()
        originals[relative] = original
        if digest(original) != entry['sha256']:
            raise Refusal('Content/anchor conflict: ' + relative)
        operation = entry['operation']
        if operation not in {'delete', 'transform', 'regenerate'}:
            raise Refusal('Unknown manifest operation')
        report[operation].append(relative)
        report['hashes_before'][relative] = digest(original)
        if operation == 'transform':
            before = entry['before'].encode('utf-8')
            after = entry['after'].encode('utf-8')
            if original.count(before) != 1 or original != before or before == after:
                raise Refusal('Missing/nonunique transformation anchor: ' + relative)
            if digest(after) != entry['after_sha256']:
                raise Refusal('Staged transform integrity conflict')
            staged[relative] = after
    security = 'backend/' + module
    catalog_cp = inside(root, security + '/target/catalog-classpath.txt', required=False)
    classes = root / security / 'target/classes'
    output_preflight(root, classes, main_class)
    classpath = str(classes) + os.pathsep + '<validated dependency classpath>'
    destinations = report['regenerate']
    if len(destinations) != 3:
        raise Refusal('Exactly three catalog destinations are required')
    report['catalog'] = {
        'prerequisite': ['mvnw.cmd', '-pl', module, 'dependency:build-classpath',
                         '-Dmdep.outputFile=target/catalog-classpath.txt'],
        'classpath_file': str(catalog_cp),
        'commands': [['javac', '-cp', '<validated dependency classpath>', '-d', str(classes),
                      str(root / generator), str(root / generator.replace('GenerateActionCatalog.java', 'AcaoSistema.java'))],
                     *[['java', '-cp', classpath, main_class, str(root / destination)]
                       for destination in destinations if destination.endswith('.json')],
                     ['node', 'scripts/action-catalog.mjs', '--write'],
                     ['node', 'scripts/verify-authorization-actions.mjs']],
    }
    report['formatted'] = sorted(relative for relative in report['transform'] + report['regenerate']
                                 if relative.startswith('frontend/')
                                 and PurePosixPath(relative).suffix in {'.ts', '.html', '.scss', '.json'})
    return report, originals, staged, catalog_cp, classes


def clean_demo_prettier(root):
    """Resolve only the installed project CLI; never download a global formatter."""
    try:
        inside(root, 'frontend/node_modules/prettier/package.json')
        cli = inside(root, 'frontend/node_modules/prettier/bin/prettier.cjs')
        import shutil
        node = shutil.which('node')
        if not node:
            raise Refusal('Node is unavailable')
        unlinked(Path(node).absolute())
    except (OSError, Refusal) as error:
        raise Refusal('Project Prettier is unavailable; run npm ci in the copy frontend '
                      'before clean-demo. No files were changed.') from error
    return [node, str(cli), '--write']


def apply(root, report, originals, staged, catalog_cp, classes, prettier_runner=None):
    # Preconditions for external generators are checked before the first source write.
    output_preflight(root, classes, report['catalog']['commands'][1][3])
    unlinked(catalog_cp)
    if not catalog_cp.is_file():
        raise Refusal('Build the catalog dependency classpath shown in the dry-run first')
    dependencies = catalog_cp.read_text('utf-8').strip()
    if not dependencies:
        raise Refusal('Empty dependency classpath')
    for element in dependencies.split(os.pathsep):
        jar = Path(element)
        if not jar.is_absolute() or not jar.is_file() or jar.suffix != '.jar':
            raise Refusal('Invalid dependency classpath entry')
        unlinked(jar)
    inside(root, 'frontend/scripts/action-catalog.mjs')
    inside(root, 'frontend/scripts/verify-authorization-actions.mjs')
    # Injectable only for offline unit tests; production always resolves the copy's CLI.
    prettier_argv = (['node', 'node_modules/prettier/bin/prettier.cjs', '--write']
                     if prettier_runner is not None else clean_demo_prettier(root))
    prettier_argv += [relative.removeprefix('frontend/') for relative in report['formatted']]
    for executable in ['java', 'javac', 'node']:
        import shutil
        resolved = shutil.which(executable)
        if not resolved:
            raise Refusal('Required catalog tool is unavailable: ' + executable)
        unlinked(Path(resolved).absolute())
    version = command(['javac', '-version'], root)
    if not version.startswith('javac 21.'):
        raise Refusal('Catalog compilation requires Java 21')
    package_directory = report['catalog']['commands'][1][3].rsplit('.', 1)[0].replace('.', '/')
    classes_relative = classes.relative_to(root).as_posix()
    for name in ['AcaoSistema.class', 'GenerateActionCatalog.class']:
        inside(root, classes_relative + '/' + package_directory + '/' + name, required=False)
    argument_paths = [inside(root, classes.parent.relative_to(root).as_posix()
                            + '/clean-demo-java-' + str(index) + '.args', required=False)
                      for index in range(3)]
    backup_path = inside(root, classes.parent.relative_to(root).as_posix()
                         + '/clean-demo-backup.json', required=False)
    for relative, original in originals.items():
        if inside(root, relative).read_bytes() != original:
            raise Refusal('Concurrent source conflict before mutation: ' + relative)
    argv = report['catalog']['commands']
    argv[0][2] = dependencies
    for instruction in argv[1:3]:
        instruction[2] = str(classes) + os.pathsep + dependencies
    encoded_arguments = [java_arguments(instruction[1:]) for instruction in argv[:3]]
    report['mode'] = 'apply'
    report['backup'] = str(backup_path)
    report['catalog']['execution_argv'] = [[instruction[0], '@' + str(argument_paths[index])]
                                           if index < 3 else instruction.copy()
                                           for index, instruction in enumerate(argv)]
    print(json.dumps({'validated_plan': report}), file=sys.stderr, flush=True)
    # The complete byte backup and staged replacements stay in memory until all generators pass.
    # No directory is deleted, including empty former domain directories.
    try:
        backup_path.write_text(json.dumps({relative: {'sha256': digest(data),
                                                      'base64': base64.b64encode(data).decode('ascii')}
                                          for relative, data in originals.items()}, indent=2), encoding='utf-8')
        for relative, data in staged.items():
            inside(root, relative).write_bytes(data)
        for relative in report['delete']:
            inside(root, relative).unlink()
        for index, instruction in enumerate(argv):
            cwd = root / ('frontend' if index >= 3 else 'backend')
            if index < 3:
                # These exact, prevalidated ignored target outputs contain data args only.
                argument_paths[index].write_text(encoded_arguments[index], encoding='utf-8', newline='\n')
                command([instruction[0], '@' + str(argument_paths[index])], cwd)
            else:
                command(instruction, cwd)
        if report['formatted']:
            try:
                (prettier_runner or command)(prettier_argv, root / 'frontend', timeout=120)
            except (OSError, Refusal, subprocess.TimeoutExpired) as error:
                raise Refusal('Project Prettier formatting failed; clean-demo rolled back.') from error
    except (OSError, Refusal, subprocess.TimeoutExpired):
        for relative, original in originals.items():
            inside(root, relative, required=False).write_bytes(original)
        raise
    report['hashes_after'] = {relative: digest(inside(root, relative).read_bytes())
                              for relative in report['transform'] + report['regenerate']}
    report['result'] = 'APPLIED'


def main(tool_runner=None, prettier_runner=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('operation', choices=['clean-demo', 'initialize'])
    parser.add_argument('--root', required=True)
    parser.add_argument('--name')
    parser.add_argument('--group', default='br.com.gems')
    parser.add_argument('--description', default='')
    parser.add_argument('--skip-format', action='store_true',
                        help='initialize only: offline fallback without the GEMS AI formatter; never use for gates')
    flags = parser.add_mutually_exclusive_group()
    flags.add_argument('--dry-run', action='store_true')
    flags.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    try:
        root = Path(args.root)
        if not root.is_absolute() or '..' in root.parts:
            raise Refusal('An explicit absolute root without parent traversal is required')
        if args.operation == 'initialize':
            if args.name is None:
                raise Refusal('Product name is required')
            report, originals, staged, destinations = initialize_plan(root, args.name, args.group, args.description,
                                                                     args.skip_format)
            if args.apply:
                initialize_apply(root, report, originals, staged, destinations, tool_runner=tool_runner)
        else:
            if args.name is not None or args.group != 'br.com.gems' or args.description or args.skip_format:
                raise Refusal('Initializer options are not valid for clean-demo')
            report, originals, staged, catalog_cp, classes = plan(root)
            if args.apply:
                apply(root, report, originals, staged, catalog_cp, classes,
                      prettier_runner=prettier_runner)
        report['commands_executed'] = EXECUTIONS
        print(json.dumps(report, indent=2))
        return 0
    except (Refusal, OSError, ET.ParseError, json.JSONDecodeError, subprocess.TimeoutExpired) as error:
        print('Refused: ' + str(error), file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
