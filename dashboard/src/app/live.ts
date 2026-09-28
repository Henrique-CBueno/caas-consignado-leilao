import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AUCTION_FEED, Bid } from './auction-feed';
import { NAVIGATE } from './browser';
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
  imports: [FormsModule, RouterLink],
  selector: 'app-live',
  styleUrl: './live.css',
  templateUrl: './live.html',
})
export class LiveView {
  protected readonly feed = inject(AUCTION_FEED);
  protected readonly history = inject(HistoryStore);
  private readonly navigate = inject(NAVIGATE);

  protected proposalId = '';
  private readonly route = inject(ActivatedRoute);
  protected readonly proposalIdError = signal<string | null>(null);
  protected readonly ranking = computed(() => [...this.feed.bids()].sort(byAuctionRank));
  // Estado de cada tira: a primeira é a líder; quem já liderou e foi superado é "ultrapassada".
  protected readonly strips = computed(() => {
    const chronological = [...this.feed.bids()].sort((a, b) =>
      instantKey(a.receivedAt).localeCompare(instantKey(b.receivedAt)),
    );
    const everLed = new Set<Bid>();
    let best: Bid | null = null;
    for (const bid of chronological) {
      if (best === null || byAuctionRank(bid, best) < 0) {
        best = bid;
        everLed.add(bid);
      }
    }
    return this.ranking().map((bid, index) => ({
      bid,
      state: index === 0 ? ('leader' as const) : everLed.has(bid) ? ('passed' as const) : ('contending' as const),
    }));
  });
  protected readonly leaderAnnouncement = computed(() => {
    const leader = this.ranking()[0];
    return leader ? `Melhor lance: ${leader.funderId} a ${this.formatRate(leader.rate)}` : '';
  });
  protected readonly closedAuction = this.feed.closed;

  constructor() {
    // Link do histórico, atalho ou endereço compartilhado (#/?proposta=<id>): começa acompanhando.
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const shared = params.get('proposta');
      if (shared) {
        this.proposalId = shared;
        this.watch();
      }
    });
  }

  protected showDemo(): void {
    this.navigate(`${window.location.pathname}?demo#/`);
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
