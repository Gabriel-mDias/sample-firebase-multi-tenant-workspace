import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PageResponse } from '../../models/page-response';
import { RegistroAuditoriaModel } from '../auditoria.model';
import { AuditoriaStore } from '../auditoria.store';
import { HistoricoRegistroComponent } from './historico-registro.component';

function pagina(registros: RegistroAuditoriaModel[]): PageResponse<RegistroAuditoriaModel> {
  return {
    content: registros,
    totalElements: registros.length,
    totalPages: 1,
    size: 20,
    number: 0,
  } as PageResponse<RegistroAuditoriaModel>;
}

const ALTERACAO_COM_SIGILOSO: RegistroAuditoriaModel = {
  id: 'r1',
  entidade: 'Solicitacao',
  registroAfetado: 'v1',
  operacao: 'ALTERACAO',
  operacaoDescricao: 'Alteração',
  dataOperacao: '2026-08-30T10:15:00',
  autorNome: 'maria.operadora',
  camposAlterados: [
    { campo: 'comissao', valorAnterior: null, valorNovo: null, sigiloso: true },
    {
      campo: 'perfilAtuacao',
      valorAnterior: 'RASCUNHO',
      valorNovo: 'OPERACIONAL',
      sigiloso: false,
    },
  ],
};

const INSERCAO: RegistroAuditoriaModel = {
  id: 'r2',
  entidade: 'Solicitacao',
  registroAfetado: 'v1',
  operacao: 'INSERCAO',
  operacaoDescricao: 'Cadastro',
  dataOperacao: '2026-08-29T09:00:00',
  autorNome: 'sistema',
  camposAlterados: [],
};

function montar(registros: RegistroAuditoriaModel[]) {
  const historico = vi.fn().mockReturnValue(of(pagina(registros)));

  TestBed.overrideProvider(AuditoriaStore, { useValue: { historico } });

  const fixture = TestBed.createComponent(HistoricoRegistroComponent);
  fixture.componentRef.setInput('entidade', 'Solicitacao');
  fixture.componentRef.setInput('registroId', 'v1');
  fixture.componentRef.setInput('open', true);
  fixture.detectChanges();

  return { fixture, historico };
}

describe('HistoricoRegistroComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HistoricoRegistroComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('deveRenderizarOperacoesComAutorEData_quandoHistoricoCarregar', () => {
    const { fixture } = montar([ALTERACAO_COM_SIGILOSO, INSERCAO]);
    const texto = fixture.nativeElement.textContent;

    expect(texto).toContain('Alteração');
    expect(texto).toContain('maria.operadora');
    expect(texto).toContain('Cadastro');
    // A data sai em pt-BR, nunca no ISO que o backend devolve.
    expect(texto).toContain('30/08/2026');
    expect(texto).not.toContain('2026-08-30T10:15:00');
  });

  /**
   * O ponto do sigilo. Se a tela deixasse em branco, o operador leria "o campo ficou vazio" — que é
   * uma informação diferente e falsa. O valor sigiloso **nunca foi gravado**.
   */
  it('deveExibirValorNaoRegistrado_quandoCampoForSigiloso', () => {
    const { fixture } = montar([ALTERACAO_COM_SIGILOSO]);
    const texto = fixture.nativeElement.textContent;

    expect(texto).toContain('comissao');
    expect(texto).toContain('valor não registrado');
  });

  it('deveExibirValoresAnteriorENovo_quandoCampoNaoForSigiloso', () => {
    const { fixture } = montar([ALTERACAO_COM_SIGILOSO]);
    const texto = fixture.nativeElement.textContent;

    expect(texto).toContain('RASCUNHO');
    expect(texto).toContain('OPERACIONAL');
  });

  /**
   * O defeito que a homologação encontrou: o telefone de um parceiro mora em `Contato`, e o
   * histórico do cadastro soma os agregados. Sem o rótulo de origem, `telefone` apareceria sem
   * dizer de onde — e dois agregados do mesmo cadastro podem ter campo de mesmo nome.
   */
  it('deveExibirOrigem_quandoRegistroVierDeAgregado', () => {
    const doContato: RegistroAuditoriaModel = {
      ...ALTERACAO_COM_SIGILOSO,
      id: 'r3',
      entidade: 'Contato',
      camposAlterados: [
        { campo: 'telefone', valorAnterior: '+55 27 9', valorNovo: '+55 27 8', sigiloso: false },
      ],
    };

    const { fixture } = montar([doContato]);

    expect(fixture.nativeElement.textContent).toContain('Contato');
  });

  it('naoDeveRepetirEntidadeComoOrigem_quandoRegistroForDaRaiz', () => {
    const daRaiz: RegistroAuditoriaModel = {
      ...ALTERACAO_COM_SIGILOSO,
      id: 'r4',
      entidade: 'Solicitacao',
    };

    const { fixture } = montar([daRaiz]);
    const origem = fixture.nativeElement.querySelector('.historico-origem');

    expect(origem).toBeNull();
  });

  it('deveExibirMensagemVazia_quandoRegistroNaoTiverAlteracoes', () => {
    const { fixture } = montar([]);

    expect(fixture.nativeElement.textContent).toContain('não tem alterações registradas');
  });

  /**
   * Sem isto, a abertura seguinte mostraria o histórico do registro anterior enquanto a requisição
   * não volta — e o operador leria o dado de outra pessoa como se fosse deste cadastro.
   */
  it('naoDeveBuscarHistorico_quandoModalEstiverFechado', () => {
    const historico = vi.fn().mockReturnValue(of(pagina([INSERCAO])));
    TestBed.overrideProvider(AuditoriaStore, { useValue: { historico } });

    const fixture = TestBed.createComponent(HistoricoRegistroComponent);
    fixture.componentRef.setInput('entidade', 'Solicitacao');
    fixture.componentRef.setInput('registroId', 'v1');
    fixture.componentRef.setInput('open', false);
    fixture.detectChanges();

    expect(historico).not.toHaveBeenCalled();
  });

  it('deveEmitirFechamento_quandoModalForFechado', () => {
    const { fixture } = montar([INSERCAO]);
    let fechou = false;
    fixture.componentInstance.fechar.subscribe(() => (fechou = true));

    (fixture.componentInstance as unknown as { cancelar: () => void }).cancelar();

    expect(fechou).toBe(true);
  });
});
