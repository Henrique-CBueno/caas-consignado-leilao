import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AUCTION_FEED, Bid, ConnectionState } from './auction-feed';

const CONNECTION_LABELS: Record<ConnectionState, string> = {
  idle: 'desconectado',
  connecting: 'conectando',
  connected: 'conectado',
  reconnecting: 'reconectando',
};

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
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnDestroy {
  private readonly feed = inject(AUCTION_FEED);

  protected proposalId = '';
  protected readonly proposalIdError = signal<string | null>(null);
  protected readonly connection = this.feed.connection;
  protected readonly connectionLabel = computed(() => CONNECTION_LABELS[this.connection()]);
  protected readonly ranking = computed(() => [...this.feed.bids()].sort(byAuctionRank));
  protected readonly leaderAnnouncement = computed(() => {
    const leader = this.ranking()[0];
    return leader ? `Melhor lance: ${leader.funderId} a ${this.formatRate(leader.rate)}` : '';
  });
  protected readonly closedAuction = this.feed.closed;

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
    this.feed.watch(this.proposalId);
  }

  ngOnDestroy(): void {
    this.feed.stop();
  }
}
