import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HistoryStore } from './history-store';

const RATE_FORMAT = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

@Component({
  imports: [RouterLink],
  selector: 'app-history',
  template: `
    <section>
      <h1>Histórico</h1>
      @if (history.entries().length === 0) {
        <p>Nenhum leilão acompanhado ainda.</p>
      } @else {
        <button type="button" (click)="history.clear()">Limpar histórico</button>
        <ul>
          @for (entry of history.entries(); track entry.proposalId) {
            <li>
              <a routerLink="/" [queryParams]="{ proposta: entry.proposalId }">{{ entry.proposalId }}</a>
              @if (entry.outcome; as outcome) {
                @if (outcome.winningFunderId) {
                  — vencedor {{ outcome.winningFunderId }} a {{ rate(outcome.winningRate) }}
                } @else {
                  — sem vencedor
                }
              }
            </li>
          }
        </ul>
      }
    </section>
  `,
  styles: `
    section {
      max-width: 640px;
      margin: 2rem auto;
    }
  `,
})
export class HistoryView {
  protected readonly history = inject(HistoryStore);

  protected rate(value: number | null): string {
    return `${RATE_FORMAT.format(value ?? 0)}%`;
  }
}
