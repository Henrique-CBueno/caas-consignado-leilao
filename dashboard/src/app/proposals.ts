import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AUTH, GATEWAY_BASE_URL } from './auth';

interface ProposalItem {
  id: string;
  borrowerId: string;
  requestedAmount: number;
  termMonths: number;
  createdAt: string;
}

interface ProposalPage {
  items: ProposalItem[];
  hasNext: boolean;
}

const PAGE_SIZE = 20;
const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

// Propostas do tenant, direto do servidor (Milestone 19, ADR-0031). Sem status: ele não avança do valor
// inicial, então o desfecho fica no acompanhamento ao vivo, para onde cada tira leva.
@Component({
  selector: 'app-proposals',
  imports: [RouterLink, DatePipe],
  template: `
    <section class="entry">
      <h1>Propostas</h1>

      @if (!auth.tenant()) {
        <p>Entre com um tenant para ver as propostas dele. <a routerLink="/entrar">Entrar</a></p>
      } @else {
        @if (error(); as message) {
          <p role="alert">{{ message }}</p>
        }

        @if (loaded() && items().length === 0 && !error()) {
          <p>Nenhuma proposta ainda. <a routerLink="/nova-proposta">Nova proposta</a></p>
        }

        <ul>
          @for (item of items(); track item.id) {
            <li>
              <a class="proposal-strip" routerLink="/" [queryParams]="{ proposta: item.id }">
                <span class="holder" aria-hidden="true"></span>
                <span class="name">{{ item.borrowerId }}</span>
                <span class="meta">
                  {{ money(item.requestedAmount) }} · {{ item.termMonths }} meses ·
                  {{ item.createdAt | date: 'dd/MM/yyyy HH:mm' }}
                </span>
              </a>
            </li>
          }
        </ul>

        @if (hasNext()) {
          <button type="button" [disabled]="loading()" (click)="loadMore()">Carregar mais</button>
        }
      }
    </section>
  `,
  styles: `
    .entry {
      display: grid;
      gap: var(--space-4);
      max-width: 40rem;
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

    .proposal-strip {
      display: grid;
      grid-template-columns: 1.5rem minmax(0, 1fr) auto;
      align-items: center;
      column-gap: var(--space-3);
      min-height: 3.5rem;
      padding: 0 var(--space-4) 0 0;
      background: var(--paper);
      color: var(--paper-ink);
      border: 1px solid var(--strip-edge);
      text-decoration: none;
    }

    .proposal-strip:hover {
      filter: brightness(1.04);
    }

    .name {
      font-size: var(--text-lg);
      font-weight: 700;
      overflow-wrap: anywhere;
    }

    .meta {
      color: var(--paper-ink-2);
      font-family: var(--font-data);
      font-size: var(--text-xs);
      text-align: right;
    }

    button {
      justify-self: start;
    }
  `,
})
export class ProposalsView {
  protected readonly auth = inject(AUTH);
  private readonly http = inject(HttpClient);

  protected readonly items = signal<ProposalItem[]>([]);
  protected readonly hasNext = signal(false);
  protected readonly loaded = signal(false);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  private page = 0;

  constructor() {
    if (this.auth.tenant()) {
      this.load();
    }
  }

  protected money(value: number): string {
    return BRL.format(value);
  }

  protected loadMore(): void {
    this.page += 1;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http
      .get<ProposalPage>(`${GATEWAY_BASE_URL}/proposals?page=${this.page}&size=${PAGE_SIZE}`, {
        headers: { Authorization: `Bearer ${this.auth.idToken() ?? ''}` },
      })
      .subscribe({
        next: (result) => {
          this.items.update((current) => [...current, ...result.items]);
          this.hasNext.set(result.hasNext);
          this.loaded.set(true);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.error.set('Não foi possível carregar as propostas.');
        },
      });
  }
}
