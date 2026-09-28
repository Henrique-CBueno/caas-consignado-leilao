import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HistoryEntry } from './history-store';

const RATE_FORMAT = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

// Uma tira de arquivo: o leilão já acompanhado, com o desfecho quando foi visto.
@Component({
  selector: 'li[app-archive-strip]',
  imports: [RouterLink],
  host: { '[attr.data-outcome]': 'outcomeKind()', '[class.latest]': 'latest()' },
  template: `
    <span class="holder" aria-hidden="true"></span>
    <a class="id" routerLink="/" [queryParams]="{ proposta: entry().proposalId }">{{ entry().proposalId }}</a>
    <span class="outcome">
      @if (entry().outcome; as outcome) {
        @if (outcome.winningFunderId) {
          Vencedor {{ outcome.winningFunderId }} a {{ rate(outcome.winningRate) }}
        } @else {
          Sem vencedor
        }
      } @else {
        Desfecho não visto
      }
    </span>
    <span class="marks">
      @if (latest()) {
        <strong>Último acompanhado</strong>
      }
      @if (entry().simulated) {
        <strong>Simulado</strong>
      }
    </span>
  `,
  styles: `
    :host {
      position: relative;
      display: grid;
      grid-template-columns: 1.5rem minmax(0, 1fr) auto;
      grid-template-areas: 'holder id outcome' 'holder marks marks';
      align-items: center;
      column-gap: var(--space-3);
      min-height: 3.5rem;
      padding: 0 var(--space-6) 0 0;
      background: var(--paper);
      color: var(--paper-ink);
      border: 1px solid var(--strip-edge);
      border-radius: 2px;
    }

    .holder {
      grid-area: holder;
    }

    :host([data-outcome='winner']) .holder {
      background: var(--leader);
    }

    :host([data-outcome='none']) .holder {
      background: var(--passed);
    }

    .id {
      grid-area: id;
      padding-block: var(--space-1);
      color: var(--paper-ink);
      font-family: var(--font-data);
      font-size: var(--text-sm);
      overflow-wrap: anywhere;
    }

    .outcome {
      grid-area: outcome;
      font-weight: 700;
    }

    .marks {
      grid-area: marks;
      display: flex;
      gap: var(--space-3);
      padding-bottom: var(--space-1);
      color: var(--paper-ink-2);
      font-size: var(--text-xs);
      letter-spacing: 0.06em;
      text-transform: uppercase;
    }

    /* Aba dobrada: onde você parou (a última acompanhada). */
    :host(.latest)::after {
      content: '';
      position: absolute;
      top: 0;
      right: 0;
      width: 1.25rem;
      height: 1.25rem;
      background: linear-gradient(225deg, var(--rack) 50%, var(--pen) 50%);
    }

    @media (max-width: 52rem) {
      :host {
        grid-template-columns: 1.5rem minmax(0, 1fr);
        grid-template-areas: 'holder id' 'holder outcome' 'holder marks';
        padding-right: var(--space-4);
      }
    }
  `,
})
export class ArchiveStrip {
  readonly entry = input.required<HistoryEntry>();
  readonly latest = input(false);

  protected outcomeKind(): string {
    const outcome = this.entry().outcome;
    return outcome ? (outcome.winningFunderId ? 'winner' : 'none') : 'open';
  }

  protected rate(value: number | null): string {
    return `${RATE_FORMAT.format(value ?? 0)}%`;
  }
}
