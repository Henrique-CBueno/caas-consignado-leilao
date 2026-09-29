import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AUTH } from './auth';
import { KnownTenant, KnownTenants } from './known-tenants';

// Sem campo de senha: a senha demo é fixa e já documentada (ADR-0017); a tela só escolhe o tenant.
const DEMO_TENANTS: KnownTenant[] = [
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

      <ul>
        @for (tenant of tenants(); track tenant.slug) {
          <li>
            <button class="tenant-strip" type="button" [disabled]="loading()" (click)="login(tenant.slug)">
              <span class="holder" aria-hidden="true"></span>
              <span class="name">{{ tenant.name }}</span>
              <span class="slug">{{ tenant.slug }}@caas.local</span>
            </button>
          </li>
        }
      </ul>

      <button class="admin-entry" type="button" [disabled]="loading()" (click)="login('admin')">
        Entrar como administrador
      </button>
    </section>
  `,
  styles: `
    .entry {
      display: grid;
      gap: var(--space-4);
      max-width: 36rem;
      margin: var(--space-6) auto;
    }

    .entry p {
      margin: 0;
      color: var(--ink-2);
    }

    .entry p[role='alert'] {
      padding: var(--space-2) var(--space-3);
      border: 1px solid var(--accent-text);
      color: var(--ink);
    }

    ul {
      display: grid;
      gap: var(--space-2);
      margin: 0;
      padding: 0;
      list-style: none;
    }

    .tenant-strip {
      display: grid;
      grid-template-columns: 1.5rem minmax(0, 1fr) auto;
      align-items: center;
      column-gap: var(--space-3);
      width: 100%;
      min-height: 4rem;
      padding: 0 var(--space-4) 0 0;
      background: var(--paper);
      color: var(--paper-ink);
      border: 1px solid var(--strip-edge);
      text-align: left;
    }

    .tenant-strip:hover {
      background: var(--paper);
      filter: brightness(1.04);
    }

    .admin-entry {
      justify-self: start;
    }

    .name {
      font-size: var(--text-lg);
      font-weight: 700;
    }

    .slug {
      color: var(--paper-ink-2);
      font-family: var(--font-data);
      font-size: var(--text-xs);
    }
  `,
})
export class LoginView {
  private readonly auth = inject(AUTH);
  private readonly router = inject(Router);

  // Os três do seed mais os que o administrador criou neste navegador (não há listagem pública).
  private readonly known = inject(KnownTenants);
  protected readonly tenants = computed(() => [...DEMO_TENANTS, ...this.known.created()]);
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
