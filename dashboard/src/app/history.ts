import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ArchiveStrip } from './archive-strip';
import { HistoryStore } from './history-store';

@Component({
  imports: [RouterLink, ArchiveStrip],
  selector: 'app-history',
  template: `
    <section class="archive">
      <div class="head">
        <h1>Histórico</h1>
        @if (history.entries().length > 0) {
          <button type="button" (click)="history.clear()">Limpar histórico</button>
        }
      </div>
      @if (history.entries().length === 0) {
        <p class="empty">
          Nenhum leilão acompanhado ainda. Cole o ID de uma proposta na vista
          <a routerLink="/">Ao vivo</a> e ele aparece aqui.
        </p>
      } @else {
        <ul>
          @for (entry of history.entries(); track entry.proposalId; let isLatest = $first) {
            <li app-archive-strip [entry]="entry" [latest]="isLatest"></li>
          }
        </ul>
      }
    </section>
  `,
  styles: `
    .archive {
      display: grid;
      gap: var(--space-4);
      max-width: 52rem;
      margin: 0 auto;
    }

    .head {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: var(--space-4);
    }

    .empty {
      margin: 0;
      padding: var(--space-4);
      border: 1px dashed var(--rack-line);
      color: var(--ink-2);
    }

    ul {
      display: grid;
      gap: var(--space-2);
      margin: 0;
      padding: 0;
      list-style: none;
    }
  `,
})
export class HistoryView {
  protected readonly history = inject(HistoryStore);
}
