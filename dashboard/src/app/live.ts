import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AUCTION_FEED, Bid } from './auction-feed';
import { HistoryStore } from './history-store';

const RATE_FORMAT = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

// Espelha o WinnerSelector do auction-service (ADR-0018): menor taxa, depois menor prazo, depois lance mais antigo.
// O backend corta zeros à direita da fração de segundos; padronizar em 9 dígitos torna a comparação de texto correta.
function instantKey(iso: string): string {
  const [seconds, fraction = ''] = iso.replace(/Z$/, '').split('.');
  return `${seconds}.${fraction.padEnd(9, '0')}`;
}

function byAuctionRank(a: Bid, b: Bid): number {
  return (
    a.rate - b.rate ||
    a.termMonths - b.termMonths ||
    instantKey(a.receivedAt).localeCompare(instantKey(b.receivedAt))
  );
}

const UUID_FORMAT = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  imports: [FormsModule],
  selector: 'app-live',
  styleUrl: './live.css',
  templateUrl: './live.html',
})
export class LiveView {
  private readonly feed = inject(AUCTION_FEED);
  private readonly history = inject(HistoryStore);

  protected proposalId = '';
  private readonly route = inject(ActivatedRoute);
  protected readonly proposalIdError = signal<string | null>(null);
  protected readonly ranking = computed(() => [...this.feed.bids()].sort(byAuctionRank));
  protected readonly leaderAnnouncement = computed(() => {
    const leader = this.ranking()[0];
    return leader ? `Melhor lance: ${leader.funderId} a ${this.formatRate(leader.rate)}` : '';
  });
  protected readonly closedAuction = this.feed.closed;

  constructor() {
    // Link do histórico (ou compartilhado): #/?proposta=<id> já começa acompanhando.
    const shared = this.route.snapshot.queryParamMap.get('proposta');
    if (shared) {
      this.proposalId = shared;
      this.watch();
    }
  }

  protected formatRate(rate: number): string {
    return `${RATE_FORMAT.format(rate)}%`;
  }

  protected watch(): void {
    if (!this.proposalId) {
      this.proposalIdError.set('Informe o ID da proposta para acompanhar o leilão.');
      return;
    }
    if (!UUID_FORMAT.test(this.proposalId)) {
      this.proposalIdError.set('Informe o ID da proposta no formato UUID.');
      return;
    }
    this.proposalIdError.set(null);
    this.history.record(this.proposalId);
    this.feed.watch(this.proposalId);
  }
}
