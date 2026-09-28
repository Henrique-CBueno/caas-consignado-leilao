import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { AUCTION_FEED, AuctionClosed, AuctionFeed, Bid, ConnectionState } from './auction-feed';

class FakeFeed implements AuctionFeed {
  readonly connection = signal<ConnectionState>('idle');
  readonly bids = signal<Bid[]>([]);
  readonly closed = signal<AuctionClosed | null>(null);
  readonly watched: string[] = [];

  watch(proposalId: string): void {
    this.watched.push(proposalId);
  }

  stop(): void {}
}

function bid(funderId: string, rate: number, termMonths: number, receivedAt: string): Bid {
  return { funderId, rate, termMonths, receivedAt };
}

const A_PROPOSAL_ID = '2ac7ad63-7381-418c-942a-3aebf59290e3';

async function render(feed: FakeFeed) {
  TestBed.configureTestingModule({
    imports: [App],
    providers: [{ provide: AUCTION_FEED, useValue: feed }],
  });
  const fixture = TestBed.createComponent(App);
  await fixture.whenStable();
  const page: HTMLElement = fixture.nativeElement;

  return {
    page,
    async settle() {
      await fixture.whenStable();
    },
    async typeProposalId(value: string) {
      const input = page.querySelector('input') as HTMLInputElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
      await fixture.whenStable();
    },
    async clickWatch() {
      const button = Array.from(page.querySelectorAll('button')).find((b) =>
        b.textContent?.includes('Acompanhar'),
      ) as HTMLButtonElement;
      button.click();
      await fixture.whenStable();
    },
  };
}

describe('Leilão ao vivo', () => {
  it('mostra erro inline e não conecta quando o ID não é um UUID', async () => {
    const feed = new FakeFeed();
    const screen = await render(feed);

    await screen.typeProposalId('isto-nao-e-um-uuid');
    await screen.clickWatch();

    expect(screen.page.querySelector('[role="alert"]')?.textContent).toContain('UUID');
    expect(feed.watched).toEqual([]);
  });

  it('pede o ID e não conecta quando o campo está vazio', async () => {
    const feed = new FakeFeed();
    const screen = await render(feed);

    await screen.clickWatch();

    expect(screen.page.querySelector('[role="alert"]')?.textContent).toContain('ID da proposta');
    expect(feed.watched).toEqual([]);
  });

  it('assina o leilão da proposta quando o ID é um UUID válido', async () => {
    const feed = new FakeFeed();
    const screen = await render(feed);

    await screen.typeProposalId(A_PROPOSAL_ID);
    await screen.clickWatch();

    expect(feed.watched).toEqual([A_PROPOSAL_ID]);
    expect(screen.page.querySelector('[role="alert"]')).toBeNull();
  });

  it.each([
    ['idle', 'desconectado'],
    ['connecting', 'conectando'],
    ['connected', 'conectado'],
    ['reconnecting', 'reconectando'],
  ] as const)('mostra o estado da conexão %s como "%s"', async (state, label) => {
    const feed = new FakeFeed();
    const screen = await render(feed);

    feed.connection.set(state);
    await screen.settle();

    expect(screen.page.querySelector('[role="status"]')?.textContent?.trim()).toBe(label);
  });

  it('mostra o lance recebido com taxa e prazo formatados em pt-BR', async () => {
    const feed = new FakeFeed();
    const screen = await render(feed);

    feed.bids.set([bid('funder-2', 1.92, 18, '2026-09-28T12:49:22.389749090Z')]);
    await screen.settle();

    const row = screen.page.querySelector('ol li')?.textContent;
    expect(row).toContain('funder-2');
    expect(row).toContain('1,92%');
    expect(row).toContain('18 meses');
  });

  it.each([
    [
      'menor taxa primeiro',
      [
        bid('funder-a', 2.5, 24, '2026-09-28T12:49:26.005211354Z'),
        bid('funder-b', 1.92, 18, '2026-09-28T12:49:22.389749090Z'),
        bid('funder-c', 2.15, 12, '2026-09-28T12:49:29.334937650Z'),
      ],
      ['funder-b', 'funder-c', 'funder-a'],
    ],
    [
      'empate de taxa: menor prazo primeiro',
      [
        bid('funder-a', 2, 24, '2026-09-28T12:49:22.000000000Z'),
        bid('funder-b', 2, 12, '2026-09-28T12:49:23.000000000Z'),
      ],
      ['funder-b', 'funder-a'],
    ],
    [
      'empate de taxa e prazo: lance mais antigo primeiro',
      [
        bid('funder-a', 2, 12, '2026-09-28T12:49:23.500000000Z'),
        bid('funder-b', 2, 12, '2026-09-28T12:49:23.250000000Z'),
      ],
      ['funder-b', 'funder-a'],
    ],
    [
      'horários com precisões diferentes (o backend corta zeros à direita)',
      [
        bid('funder-a', 2, 12, '2026-09-28T12:49:22.38975Z'),
        bid('funder-b', 2, 12, '2026-09-28T12:49:22.389749090Z'),
      ],
      ['funder-b', 'funder-a'],
    ],
  ] as const)('ordena os lances: %s', async (_caso, bids, expectedOrder) => {
    const feed = new FakeFeed();
    const screen = await render(feed);

    feed.bids.set([...bids]);
    await screen.settle();

    const shown = Array.from(screen.page.querySelectorAll('ol li')).map(
      (li) => expectedOrder.find((funder) => li.textContent?.includes(funder)),
    );
    expect(shown).toEqual([...expectedOrder]);
  });

  it('destaca só o melhor lance', async () => {
    const feed = new FakeFeed();
    const screen = await render(feed);

    feed.bids.set([
      bid('funder-a', 2.5, 24, '2026-09-28T12:49:26.005211354Z'),
      bid('funder-b', 1.92, 18, '2026-09-28T12:49:22.389749090Z'),
    ]);
    await screen.settle();

    const rows = Array.from(screen.page.querySelectorAll('ol li'));
    expect(rows[0].textContent).toContain('Melhor lance');
    expect(rows[1].textContent).not.toContain('Melhor lance');
  });

  it('anuncia em região viva quando o líder muda', async () => {
    const feed = new FakeFeed();
    const screen = await render(feed);
    const announcement = () => screen.page.querySelector('[aria-live]')?.textContent;

    feed.bids.set([bid('funder-a', 2.5, 24, '2026-09-28T12:49:26.005211354Z')]);
    await screen.settle();
    expect(announcement()).toContain('funder-a');

    feed.bids.set([
      bid('funder-a', 2.5, 24, '2026-09-28T12:49:26.005211354Z'),
      bid('funder-b', 1.92, 18, '2026-09-28T12:49:29.334937650Z'),
    ]);
    await screen.settle();
    expect(announcement()).toContain('funder-b');
    expect(announcement()).toContain('1,92%');
  });

  describe('resultado do leilão', () => {
    const result = (page: HTMLElement) => page.querySelector('[aria-label="Resultado do leilão"]');

    it('não mostra resultado enquanto o leilão está aberto', async () => {
      const screen = await render(new FakeFeed());

      expect(result(screen.page)).toBeNull();
    });

    it('mostra o vencedor e a taxa vencedora quando o leilão fecha', async () => {
      const feed = new FakeFeed();
      const screen = await render(feed);

      feed.closed.set({ status: 'CLOSED_WITH_WINNER', winningFunderId: 'funder-2', winningRate: 1.92 });
      await screen.settle();

      expect(result(screen.page)?.textContent).toContain('funder-2');
      expect(result(screen.page)?.textContent).toContain('1,92%');
      expect(result(screen.page)?.textContent).toContain('vencedor');
    });

    it('informa quando o leilão fecha sem vencedor', async () => {
      const feed = new FakeFeed();
      const screen = await render(feed);

      feed.closed.set({ status: 'CLOSED_NO_WINNER', winningFunderId: null, winningRate: null });
      await screen.settle();

      expect(result(screen.page)?.textContent).toContain('sem vencedor');
    });
  });
});
