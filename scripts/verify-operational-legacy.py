"""Report legacy operational references without printing potentially sensitive lines.

Runtime is the closed T026 subset. The default scans the remaining initializer/CI/docs
as well; runtime success never implies the full migration gate succeeded.
"""
import argparse
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ['environments', 'backend/Dockerfile', 'backend/.dockerignore',
           'backend/app-deploy/src/main/resources/application.yml', 'frontend/Dockerfile',
           'frontend/.dockerignore', 'frontend/docker-entrypoint.sh', 'frontend/nginx.conf',
           'frontend/public/env.js', '.github/workflows/build-images.yml', '.github/workflows/deploy.yml',
           'README.md', 'backend/README.md']
LATER = ['scripts/inicializar-projeto.ps1', '.github/workflows/ci.yml', 'CODEX.md', 'CLAUDE.md', 'GEMINI.md',
         'backend/app-deploy/README.md', 'frontend/README.md', 'frontend/package.json', 'frontend/package-lock.json',
         'frontend/src', 'frontend/public', 'frontend/scripts', 'backend/pom.xml', 'backend/.mvn',
         'backend/app-core/pom.xml', 'backend/app-model/pom.xml', 'backend/app-security/pom.xml',
         'backend/app-events/pom.xml', 'backend/app-deploy/pom.xml',
         'backend/app-core/src/main', 'backend/app-model/src/main',
         'backend/app-security/src/main', 'backend/app-events/src/main', 'backend/app-deploy/src/main']
# Exact governance files are protected by the runtime contract. The full gate reports them.
GOVERNANCE = {'environments/AGENTS.md', 'environments/GEMINI.md'}
PATTERN = re.compile(r'keycloak|\boidc\b|app[-_]realm|\bvps\b|\bcaddy\b|silent-check-sso', re.I)

def files(entries, runtime=False):
    result=set()
    for entry in entries:
        target=ROOT/entry
        candidates=target.rglob('*') if target.is_dir() else [target]
        for path in candidates:
            relative=path.relative_to(ROOT).as_posix()
            # Never open actual environment/credential files, links, or generated dependencies.
            if (not path.is_file() or path.is_symlink() or path.name.endswith(('.env','.key','.pem','.p12'))
                    or path.name in {'.env','.npmrc','settings.xml','config.toml'}
                    or path.name.startswith('.env') and path.name!='.env.example'
                    or any(part in {'node_modules','target','dist','.angular','__pycache__'} for part in path.parts)
                    or runtime and relative in GOVERNANCE):
                continue
            result.add(path)
    return sorted(result)

def scan(entries, runtime=False):
    findings=[]
    for path in files(entries,runtime):
        relative=path.relative_to(ROOT).as_posix()
        if PATTERN.search(relative):
            findings.append({'path':relative,'line':0,'kind':'legacy filename'})
        try:text=path.read_text(encoding='utf-8-sig')
        except UnicodeError:continue
        for number,line in enumerate(text.splitlines(),1):
            if PATTERN.search(line):findings.append({'path':relative,'line':number,'kind':'legacy operational reference'})
    return findings

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--stage',choices=['runtime','all'],default='all')
    parser.add_argument('--strict',action='store_true')
    args=parser.parse_args()
    current=scan(RUNTIME,args.stage=='runtime')
    later=scan(LATER)+scan(sorted(GOVERNANCE))
    if args.stage=='all':current+=later
    report={'stage':args.stage,'status':'FAIL' if current else 'PASS','findings':current,
            'pending_paths':sorted({f['path'] for f in later}) if args.stage=='runtime' else [],
            'full_gate_status':'NOT_RUN' if args.stage=='runtime' else ('FAIL' if current else 'PASS')}
    print(json.dumps(report,indent=2))
    return 1 if args.strict and current else 0

if __name__=='__main__':sys.exit(main())
