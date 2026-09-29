import { signal } from '@angular/core';
import { AuctionClosed, AuctionFeed, AuctionOpened, Bid, ConnectionState } from './auction-feed';

interface Step {
  at: number;
  run: (feed: DemoAuctionFeed) => void;
}

// Roteiro fixo, em unidades: conecta, quatro lances (o líder muda no terceiro) e fecha com vencedor.
const SCRIPT: Step[] = [
  { at: 0.3, run: (f) => f.connection.set('connected') },
  { at: 0.4, run: (f) => f.open() },
  { at: 1.2, run: (f) => f.emit('funder-1', 2.6, 24) },
  { at: 2.2, run: (f) => f.emit('funder-3', 2.15, 12) },
  { at: 3.4, run: (f) => f.emit('funder-2', 1.92, 18) },
  { at: 4.6, run: (f) => f.emit('funder-1', 2.05, 24) },
  {
    at: 6.5,
    run: (f) =>
      f.closed.set({ status: 'CLOSED_WITH_WINNER', winningFunderId: 'funder-2', winningRate: 1.92 }),
  },
];

// Feed de demonstração (?demo): uma história previsível para desenvolver o visual, tirar capturas e apresentar sem cluster.
export class DemoAuctionFeed implements AuctionFeed {
  readonly demo = true;
  readonly connection = signal<ConnectionState>('idle');
  readonly bids = signal<Bid[]>([]);
  readonly opened = signal<AuctionOpened | null>(null);
  readonly closed = signal<AuctionClosed | null>(null);
  readonly watching = signal<string | null>(null);

  private timers: ReturnType<typeof setTimeout>[] = [];

  // pace: milissegundos de uma unidade do roteiro (o teste usa um ritmo curto).
  constructor(private readonly pace = 1000) {}

  watch(proposalId: string): void {
    this.stop();
    this.bids.set([]);
    this.opened.set(null);
    this.closed.set(null);
    this.watching.set(proposalId);
    this.connection.set('connecting');
    this.timers = SCRIPT.map((step) => setTimeout(() => step.run(this), step.at * this.pace));
  }

  stop(): void {
    this.timers.forEach(clearTimeout);
    this.timers = [];
    this.watching.set(null);
    this.connection.set('idle');
  }

  // Abre com o prazo do último passo do roteiro (o fechamento), para a contagem chegar a zero junto dele.
  open(): void {
    this.opened.set({ expiresAt: new Date(Date.now() + 6.1 * this.pace).toISOString() });
  }

  emit(funderId: string, rate: number, termMonths: number): void {
    this.bids.update((current) => [
      ...current,
      { funderId, rate, termMonths, receivedAt: new Date().toISOString() },
    ]);
  }
}
