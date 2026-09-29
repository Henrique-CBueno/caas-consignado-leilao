import { Component, ElementRef, afterRenderEffect, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ArchiveStrip } from './archive-strip';
import { AUCTION_FEED, Bid } from './auction-feed';
import { AUTH } from './auth';
import { CLOCK } from './clock';
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
  imports: [FormsModule, ArchiveStrip, RouterLink],
  selector: 'app-live',
  styleUrls: ['./live.css', './rack.css'],
  templateUrl: './live.html',
})
export class LiveView {
  protected readonly feed = inject(AUCTION_FEED);
  protected readonly history = inject(HistoryStore);
  private readonly auth = inject(AUTH);
  private readonly wallClock = inject(CLOCK);
  // O WebSocket exige o ID token (Milestone 18); só o modo demonstração, sem rede, segue público.
  protected readonly canWatch = computed(() => this.feed.demo === true || this.auth.tenant() !== null);
  private readonly navigate = inject(NAVIGATE);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private tops = new Map<string, number>();

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
  // A vencedora só existe depois do fechamento; vem do evento, não do ranking local.
  private readonly winner = computed(() => this.feed.closed());
  // Baias numeradas: o rack tem sempre seis posições; as vazias mostram o número que a próxima tira ocupará.
  protected readonly emptyBays = computed(() => {
    const taken = this.strips().length;
    return Array.from({ length: Math.max(0, 6 - taken) }, (_, index) => taken + index + 1);
  });
  protected readonly leaderAnnouncement = computed(() => {
    const leader = this.ranking()[0];
    return leader ? `Melhor lance: ${leader.funderId} a ${this.formatRate(leader.rate)}` : '';
  });
  protected readonly closedAuction = this.feed.closed;
  // Contagem regressiva (Milestone 20): só existe se a abertura foi recebida ao vivo; nunca estima um prazo.
  private readonly remainingMs = computed(() => {
    const opened = this.feed.opened();
    return opened && this.closedAuction() === null ? Date.parse(opened.expiresAt) - this.wallClock.now() : null;
  });
  protected readonly auctionState = computed(() => {
    const remaining = this.remainingMs();
    return remaining === null ? null : remaining > 0 ? ('open' as const) : ('waiting' as const);
  });
  protected readonly remaining = computed(() => {
    const seconds = Math.max(0, Math.ceil((this.remainingMs() ?? 0) / 1000));
    return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
  });
  protected readonly closesAt = computed(() => {
    const opened = this.feed.opened();
    return opened ? new Date(opened.expiresAt).toLocaleTimeString('pt-BR') : '';
  });

  constructor() {
    // A tira que muda de posição desliza até ela (FLIP); sem movimento quando o usuário pede menos.
    afterRenderEffect(() => {
      this.strips();
      this.slideMovedStrips();
    });

    // Link do histórico, atalho ou endereço compartilhado (#/?proposta=<id>): começa acompanhando.
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const shared = params.get('proposta');
      if (shared && this.canWatch()) {
        this.proposalId = shared;
        this.watch();
      }
    });
  }

  protected showDemo(): void {
    this.navigate(`${window.location.pathname}?demo#/`);
  }

  protected label(strip: { bid: Bid; state: 'leader' | 'passed' | 'contending' }): string {
    if (this.isWinner(strip.bid)) {
      return 'Vencedora';
    }
    if (strip.state === 'passed') {
      return 'Ultrapassada';
    }
    if (this.closedAuction() !== null && strip.state === 'contending') {
      return 'Não vencedora';
    }
    return strip.state === 'leader' ? 'Melhor lance' : 'Em disputa';
  }

  private slideMovedStrips(): void {
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
    const next = new Map<string, number>();
    this.host.nativeElement.querySelectorAll<HTMLElement>('li.strip[data-key]').forEach((strip) => {
      const key = strip.dataset['key'] as string;
      const top = strip.offsetTop;
      const before = this.tops.get(key);
      next.set(key, top);
      if (!reduceMotion && before !== undefined && Math.abs(before - top) > 1 && typeof strip.animate === 'function') {
        strip.animate(
          [{ transform: `translateY(${before - top}px)` }, { transform: 'none' }],
          { duration: 260, easing: 'cubic-bezier(0.16, 1, 0.3, 1)' },
        );
      }
    });
    this.tops = next;
  }

  protected isWinner(bid: Bid): boolean {
    const closed = this.winner();
    return closed?.winningFunderId === bid.funderId && closed.winningRate === bid.rate;
  }

  protected clock(iso: string): string {
    return iso.slice(11, 19);
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
    this.history.record(this.proposalId, this.feed.demo === true);
    this.feed.watch(this.proposalId);
  }
}
