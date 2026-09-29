import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AUTH, GATEWAY_BASE_URL } from './auth';
import { KnownTenants, slugify } from './known-tenants';

interface TenantRow {
  id: string;
  name: string;
}

// Painel do administrador da plataforma (Milestone 17, ADR-0029): lista e cria tenants.
// O gateway responde 403 a quem não tem o papel; aqui só se esconde o que não se pode usar.
@Component({
  selector: 'app-admin',
  imports: [FormsModule],
  template: `
    <section class="entry">
      <h1>Administração</h1>

      <form (ngSubmit)="create()">
        <label for="tenant-name">Novo tenant</label>
        <input id="tenant-name" type="text" name="tenantName" [(ngModel)]="tenantName" />
        <button class="primary" type="submit" [disabled]="submitting()">Criar tenant</button>
      </form>

      @if (error(); as message) {
        <p role="alert">{{ message }}</p>
      }

      <ul>
        @for (tenant of tenants(); track tenant.id) {
          <li class="tenant-strip">
            <span class="holder" aria-hidden="true"></span>
            <span class="name">{{ tenant.name }}</span>
            <span class="slug">{{ tenant.id }}</span>
          </li>
        }
      </ul>
    </section>
  `,
  styles: `
    .entry {
      display: grid;
      gap: var(--space-4);
      max-width: 40rem;
      margin: var(--space-6) auto;
    }

    form {
      display: grid;
      gap: var(--space-2);
      padding: var(--space-4);
      background: var(--rack-2);
      border: 1px solid var(--rack-line);
    }

    label {
      color: var(--ink-2);
      font-size: var(--text-sm);
      font-weight: 600;
    }

    button {
      justify-self: start;
    }

    p[role='alert'] {
      margin: 0;
      padding: var(--space-2) var(--space-3);
      border: 1px solid var(--accent-text);
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
      min-height: 3.5rem;
      padding: 0 var(--space-4) 0 0;
      background: var(--paper);
      color: var(--paper-ink);
      border: 1px solid var(--strip-edge);
    }

    .name {
      font-size: var(--text-lg);
      font-weight: 700;
    }

    .slug {
      color: var(--paper-ink-2);
      font-family: var(--font-data);
      font-size: var(--text-xs);
      overflow-wrap: anywhere;
    }
  `,
})
export class AdminView {
  private readonly auth = inject(AUTH);
  private readonly http = inject(HttpClient);
  private readonly known = inject(KnownTenants);

  protected tenantName = '';
  protected readonly tenants = signal<TenantRow[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  private readonly url = `${GATEWAY_BASE_URL}/admin/tenants`;

  constructor() {
    this.http.get<TenantRow[]>(this.url, { headers: this.headers() }).subscribe({
      next: (rows) => this.tenants.set(rows),
      error: () => this.error.set('Não foi possível listar os tenants.'),
    });
  }

  protected create(): void {
    const name = this.tenantName.trim();
    if (!name || !slugify(name)) {
      this.error.set('Informe o nome do tenant.');
      return;
    }
    this.error.set(null);
    this.submitting.set(true);
    this.http.post<TenantRow>(this.url, { name }, { headers: this.headers() }).subscribe({
      next: (created) => {
        this.submitting.set(false);
        this.tenants.update((rows) => [...rows, created]);
        this.known.add({ slug: slugify(created.name), name: created.name });
        this.tenantName = '';
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.error.set(err.error?.message ?? 'Não foi possível criar o tenant.');
      },
    });
  }

  private headers() {
    return { Authorization: `Bearer ${this.auth.idToken() ?? ''}` };
  }
}
