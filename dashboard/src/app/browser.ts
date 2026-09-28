import { InjectionToken } from '@angular/core';

// Navegação de página inteira (ex.: entrar no modo demonstração); troca-se nos testes.
export const NAVIGATE = new InjectionToken<(url: string) => void>('Navigate', {
  providedIn: 'root',
  factory: () => (url: string) => window.location.assign(url),
});
