import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AUTH } from './auth';

interface DemoTenant {
  slug: string;
  name: string;
}

// Sem campo de senha: a senha demo é fixa e já documentada (ADR-0017); a tela só escolhe o tenant.
const DEMO_TENANTS: DemoTenant[] = [
  { slug: 'alfa', name: 'Banco Alfa' },
  { slug: 'beta', name: 'Banco Beta' },
  { slug: 'gama', name: 'Fintech Gama' },
];

@Component({
  selector: 'app-login',
  template: `
    <section class="entry">
      <h1>Entrar</h1>
      <p>Escolha um tenant de demonstração para acompanhar e criar propostas em nome dele.</p>

      @if (error(); as message) {
        <p role="alert">{{ message }}</p>
      }

      <ul class="rack">
        @for (tenant of tenants; track tenant.slug) {
          <li>
            <button type="button" [disabled]="loading()" (click)="login(tenant.slug)">
              {{ tenant.name }}
            </button>
          </li>
        }
      </ul>
    </section>
  `,
})
export class LoginView {
  private readonly auth = inject(AUTH);
  private readonly router = inject(Router);

  protected readonly tenants = DEMO_TENANTS;
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected async login(tenant: string): Promise<void> {
    this.error.set(null);
    this.loading.set(true);
    try {
      await this.auth.login(tenant);
      await this.router.navigateByUrl('/');
    } catch {
      this.error.set('Não foi possível entrar. Tente novamente.');
    } finally {
      this.loading.set(false);
    }
  }
}
