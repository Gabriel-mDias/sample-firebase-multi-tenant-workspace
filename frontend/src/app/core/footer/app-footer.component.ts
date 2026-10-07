import { Component } from '@angular/core';

/**
 * Rodapé fixo global da casca autenticada. Puramente decorativo (crédito de
 * desenvolvimento) — não recebe inputs nem depende de estado da aplicação,
 * por isso não injeta nada.
 */
@Component({
  selector: 'app-footer',
  templateUrl: './app-footer.component.html',
  styleUrl: './app-footer.component.css',
})
export class AppFooterComponent {}
