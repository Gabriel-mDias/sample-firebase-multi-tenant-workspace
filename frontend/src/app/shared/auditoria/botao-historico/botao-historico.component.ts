import { Component, inject, input, signal } from '@angular/core';
import { Acoes, globalAuthorization } from '../../../core/auth/acoes';
import { AuthService } from '../../../core/auth/auth.service';
import { EntidadeAuditada } from '../auditoria.model';
import { HistoricoRegistroComponent } from '../historico-registro/historico-registro.component';

/**
 * O botão que abre a trilha de um registro, com o modal junto.
 *
 * ## Por que botão e modal vêm no mesmo componente
 * Porque são consumidos pelas **seis** telas de visualização já entregues, e a alternativa era cada
 * uma declarar o sinal de permissão, o sinal de aberto, o modal e a ligação entre eles — quatro
 * coisas replicadas seis vezes, que divergiriam na primeira alteração. Aqui cada tela ganha uma
 * linha, e a regra de quem vê o histórico vive em um lugar só.
 *
 * ## Ele desaparece do DOM quando o perfil não tem a ação
 * Não fica desabilitado: some. Um botão desabilitado anuncia a existência de uma informação que o
 * perfil não pode ver, e a trilha é justamente informação de gestão — quem alterou o quê e quando.
 *
 * A UI espelha a autorização, mas não a implementa: o backend recusa a consulta com 403 de todo
 * jeito. Esconder aqui é sobre não oferecer o que não se pode entregar.
 */
@Component({
  selector: 'app-botao-historico',
  imports: [HistoricoRegistroComponent],
  templateUrl: './botao-historico.component.html',
  styleUrl: './botao-historico.component.css',
})
export class BotaoHistoricoComponent {
  private readonly auth = inject(AuthService);

  readonly entidade = input.required<EntidadeAuditada>();
  readonly registroId = input<string | null>(null);

  protected readonly podeVerAuditoria = signal(
    this.auth.isAuthorized(globalAuthorization(Acoes.CONSULTAR_AUDITORIA)),
  );
  protected readonly aberto = signal(false);

  protected abrir(): void {
    this.aberto.set(true);
  }

  protected fechar(): void {
    this.aberto.set(false);
  }
}
