# Quickstart — validação da 005

Pré-requisitos: Docker; Node e `npm ci` no `frontend/`; Python 3; CLI `gems-ai` 3.1.0 no PATH para a A11
da 006; ADC do Firebase de testes só pelo caminho, para o backend real.

| # | Gate | Comando | Esperado |
| :-- | :--- | :--- | :--- |
| F1 | Runtime | `python -B -m unittest discover -s scripts -p "test_runtime_config.py"` | verde, com os casos novos de CSP e upstream |
| F2 | Config do nginx | `docker exec <container> nginx -t` no container já no ar, porque os includes só existem depois do entrypoint | `syntax is ok` |
| F3 | Subida sem backend | `docker run` sem host `backend` resolvível, depois `GET /healthz` | 200, container saudável |
| F4 | Headers | `curl -I` em `/`, num asset com hash, em `/env.js` e em `/robots.txt` | 7 headers de segurança nas 4 respostas |
| F5 | Cache | os mesmos `curl -I` | asset: `max-age=31536000, immutable`; `/`: `no-cache`; `/env.js`: `no-store` |
| F6 | robots | `GET /robots.txt` | `text/plain`, conteúdo do arquivo, não o `index.html` |
| F7 | Upstream | `API_UPSTREAM` apontando para um backend real; `GET /api/...` | chega ao backend; valor inválido recusa a subida sem ecoar o valor |
| F8 | Login em tela | Google (popup) e senha, com a CSP ativa | login concluído; 0 violações de CSP no console (Google: product owner) |
| F9 | Lighthouse `/login` | antes e depois | Acessibilidade 100; Melhores práticas > 77; SEO > 83; Performance registrada |
| F10 | Frontend | `npm test -- --watch=false`, `npm run build`, `npx prettier --check .` | verdes |
| F11 | Scripts | `python -B -m unittest discover -s scripts -p "test_*.py"` e `clean-demo --dry-run` | 60 verdes; dry-run aceito; nenhum `.pyc` no `git status` |
| F12 | Inicializador | A11 da 006 (`initialize` + `clean-demo` + `check-java` + `mvnw clean install` + frontend) | verde |
| F13 | Gate GEMS e CI | `scripts/gems-ai-gate.ps1`; PR com os 4 jobs | verdes |
