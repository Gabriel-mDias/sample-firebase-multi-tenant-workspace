# Ambientes — local, CI e homologação isolada

A constituição 3.1.0 é autoridade. A spec aprovada é contrato de comportamento; código/testes
mostram o estado real. Regras GEMS vencem código consolidado sob constituição, segurança e API
publicada. Nenhum waiver/exceção ou baseline é criado pelo agente para passar gate. Referências
anteriores servem à forma de código. Adoção SDK exige publicação, alcance medido, garantia nomeada,
equivalência/mutação no consumidor e remoção da classe duplicada na mesma alteração.

Procurar primeiro enviroments; pasta real aqui environments. Usar arquivos de ambiente para
identificar container/porta/banco/URL antes de conectarMCP/PostgreSQL. Nunca copiar credenciais
para config.toml ou respostas. Segredos externos porADC/variável/mount; .envreal/keys foraGit/imagem/CI.

PostgreSQL 16 e Auth Emulator 9099/UI 4000 projeto demo-gems, Firebase CLI 15.32.1 fixada. Hostlocalhost;
container hostname de serviço. Emu só demo/offline com demo-id. Suíte/CI usam Firebase real de testes via ADC/FIREBASE_TEST_SERVICE_ACCOUNT_JSON, nunca prod; PR de fork pula E2E real com aviso;
LocalStack/S3 profile optativo útil com denyall preservado.
Nunca dockercomposedown-v; cleanupdemo não tocaDBvolumes. Backupalterado exige restoreexecutado.

Homologação Spark Web emprojetoisolado, Google/email/password/authorized localhost configurados manualmente,
ADC/Admin mínimo/banco separado, bootstrapoperatorprojectUIDverified e dois testersalpha-only/alpha+beta
comloginGoogle/password/reset nativo/revoke/disable/spoof. Nenhumsenha/token/key/action link é logado.
Hosting permanece decisão posterior; runbookherdado não autoriza publicação nesta fase.
