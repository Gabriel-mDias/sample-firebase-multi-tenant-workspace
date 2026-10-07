import { Component, inject } from '@angular/core';
import { GemsButtonComponent, GemsNavigationService } from '@gabriel-mdias/angular-gems-sdk';

@Component({
  selector: 'app-acesso-negado',
  imports: [GemsButtonComponent],
  templateUrl: './acesso-negado.component.html',
  styleUrl: './acesso-negado.component.css',
})
export class AcessoNegadoComponent {
  private readonly navigationService = inject(GemsNavigationService);

  protected voltarInicio(): void {
    this.navigationService.navigate(['/']);
  }
}
