import { GemsWizardStep } from '@gabriel-mdias/angular-gems-sdk';

/**
 * Passos do wizard de parceiro. A ordem é contratual: os índices usados por
 * `ParceiroFormStateService.currentStep` (1-based, como o `gems-wizard` espera) referenciam
 * posições desta lista.
 */
export const PARCEIRO_FORM_STEPS: GemsWizardStep[] = [
  { label: 'Dados', icon: 'fa-solid fa-id-card' },
  { label: 'Contato', icon: 'fa-solid fa-address-book' },
  { label: 'Endereço', icon: 'fa-solid fa-location-dot' },
  { label: 'Resumo', icon: 'fa-solid fa-clipboard-check' },
];

export const STEP_DADOS = 1;
export const STEP_CONTATO = 2;
export const STEP_ENDERECO = 3;
export const STEP_RESUMO = 4;
