import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withHashLocation } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';
import { AUCTION_FEED, AuctionClosed, AuctionFeed, Bid, ConnectionState } from './auction-feed';
import { NAVIGATE } from './browser';
import { DemoAuctionFeed } from './demo-auction-feed';

class FakeFeed implements AuctionFeed {
  readonly connection = signal<ConnectionState>('idle');
  readonly bids = signal<Bid[]>([]);
  readonly closed = signal<AuctionClosed | null>(null);
  readonly watching = signal<string | null>(null);
  readonly watched: string[] = [];

  watch(proposalId: string): void {
    this.watched.push(proposalId);
    this.watching.set(proposalId);
  }

  stop(): void {}
}

function bid(funderId: string, rate: number, termMonths: number, receivedAt: string): Bid {
  return { funderId, rate, termMonths, receivedAt };
}

const A_PROPOSAL_ID = '2ac7ad63-7381-418c-942a-3aebf59290e3';

async function render(feed: AuctionFeed, navigate: (url: string) => void = () => {}) {
  TestBed.configureTestingModule({
    imports: [App],
    providers: [
      { provide: AUCTION_FEED, useValue: feed },
      { provide: NAVIGATE, useValue: navigate },
      provideRouter(routes, withHashLocation()),
    ],
  });
  const fixture = TestBed.createComponent(App);
  await TestBed.inject(Router).navigateByUrl('/');
  await fixture.whenStable();
  const page: HTMLElement = fixture.nativeElement;

  return {
    page,
    async settle() {
      await fixture.whenStable();
    },
    async clickButton(text: string) {
      const button = Array.from(page.querySelectorAll('button')).find((b) =>
        b.textContent?.includes(text),
      ) as HTMLButtonElement;
      button.click();
      await fixture.whenStable();
    },
    async clickLink(text: string) {
      const link = Array.from(page.querySelectorAll('a')).find((a) =>
        a.textContent?.includes(text),
      ) as HTMLAnchorElement;
      link.click();
      await fixture.whenStable();
    },
    async navigateTo(label: string) {
      const link = Array.from(page.querySelectorAll('a')).find((a) =>
        a.textContent?.includes(label),
      ) as HTMLAnchorElement;
      link.click();
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
  beforeEach(() => localStorage.clear());

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

  it('navega entre as vistas Ao vivo e Histórico', async () => {
    const screen = await render(new FakeFeed());

    await screen.navigateTo('Histórico');
    expect(screen.page.textContent).toContain('Nenhum leilão acompanhado ainda');

    await screen.navigateTo('Ao vivo');
    expect(screen.page.querySelector('input')).not.toBeNull();
  });

  describe('histórico local', () => {
    it('lista na vista Histórico o leilão que foi acompanhado', async () => {
      const screen = await render(new FakeFeed());

      await screen.typeProposalId(A_PROPOSAL_ID);
      await screen.clickWatch();
      await screen.navigateTo('Histórico');

      expect(screen.page.textContent).toContain(A_PROPOSAL_ID);
      expect(screen.page.textContent).not.toContain('Nenhum leilão acompanhado ainda');
    });

    it('guarda o desfecho quando o resultado é visto, mesmo com outra vista aberta', async () => {
      const feed = new FakeFeed();
      const screen = await render(feed);

      await screen.typeProposalId(A_PROPOSAL_ID);
      await screen.clickWatch();
      await screen.navigateTo('Histórico');
      feed.closed.set({ status: 'CLOSED_WITH_WINNER', winningFunderId: 'funder-2', winningRate: 1.92 });
      await screen.settle();

      expect(screen.page.textContent).toContain('funder-2');
      expect(screen.page.textContent).toContain('1,92%');
    });

    it('reabre o leilão ao clicar numa entrada do histórico', async () => {
      const feed = new FakeFeed();
      const screen = await render(feed);

      await screen.typeProposalId(A_PROPOSAL_ID);
      await screen.clickWatch();
      await screen.navigateTo('Histórico');
      await screen.clickLink(A_PROPOSAL_ID);

      expect(feed.watched).toEqual([A_PROPOSAL_ID, A_PROPOSAL_ID]);
      expect((screen.page.querySelector('input') as HTMLInputElement).value).toBe(A_PROPOSAL_ID);
    });

    it('limpa o histórico', async () => {
      const screen = await render(new FakeFeed());
      await screen.typeProposalId(A_PROPOSAL_ID);
      await screen.clickWatch();
      await screen.navigateTo('Histórico');

      await screen.clickButton('Limpar histórico');

      expect(screen.page.textContent).toContain('Nenhum leilão acompanhado ainda');
      expect(screen.page.textContent).not.toContain(A_PROPOSAL_ID);
    });

    it('continua funcionando quando o armazenamento do navegador está bloqueado', async () => {
      vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new Error('bloqueado');
      });
      vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
        throw new Error('bloqueado');
      });
      const feed = new FakeFeed();
      const screen = await render(feed);

      await screen.typeProposalId(A_PROPOSAL_ID);
      await screen.clickWatch();
      await screen.navigateTo('Histórico');

      expect(feed.watched).toEqual([A_PROPOSAL_ID]);
      expect(screen.page.textContent).toContain(A_PROPOSAL_ID);
      vi.restoreAllMocks();
    });
  });

  describe('tema', () => {
    const theme = () => document.documentElement.dataset['theme'];
    const systemPrefers = (scheme: 'dark' | 'light') =>
      vi.stubGlobal('matchMedia', (query: string) => ({
        matches: query.includes('dark') && scheme === 'dark',
        media: query,
        addEventListener: () => {},
        removeEventListener: () => {},
      }));

    beforeEach(() => document.documentElement.removeAttribute('data-theme'));
    afterEach(() => vi.unstubAllGlobals());

    it('segue a preferência do sistema quando não há escolha guardada', async () => {
      systemPrefers('dark');
      await render(new FakeFeed());

      expect(theme()).toBe('dark');
    });

    it('a escolha guardada vale mais que a preferência do sistema', async () => {
      systemPrefers('light');
      localStorage.setItem('caas.theme', 'dark');
      await render(new FakeFeed());

      expect(theme()).toBe('dark');
    });

    it('alterna o tema e lembra a escolha', async () => {
      systemPrefers('dark');
      const screen = await render(new FakeFeed());

      await screen.clickButton('Alternar tema');

      expect(theme()).toBe('light');
      expect(localStorage.getItem('caas.theme')).toBe('light');
    });
  });

  describe('modo demonstração', () => {
    it('avisa que é demonstração e conta a história completa do leilão', async () => {
      const screen = await render(new DemoAuctionFeed(5));
      expect(screen.page.textContent).toContain('Modo demonstração');

      await screen.typeProposalId(A_PROPOSAL_ID);
      await screen.clickWatch();
      await new Promise((resolve) => setTimeout(resolve, 200));
      await screen.settle();

      const rows = Array.from(screen.page.querySelectorAll('ol li'));
      expect(rows).toHaveLength(4);
      expect(rows[0].textContent).toContain('funder-2');
      expect(rows[0].textContent).toContain('Melhor lance');
      expect(screen.page.querySelector('[aria-label="Resultado do leilão"]')?.textContent).toContain(
        'funder-2',
      );
      expect(screen.page.querySelector('[role="status"]')?.textContent?.trim()).toBe('conectado');
    });
  });

  describe('atalhos e saídas do estado inicial', () => {
    it('mostra atalhos dos últimos leilões quando nada está sendo acompanhado', async () => {
      const feed = new FakeFeed();
      const screen = await render(feed);
      await screen.typeProposalId(A_PROPOSAL_ID);
      await screen.clickWatch();

      feed.watching.set(null);
      await screen.settle();

      const shortcuts = screen.page.querySelector('[aria-label="Últimos leilões"]');
      expect(shortcuts?.textContent).toContain(A_PROPOSAL_ID);

      await screen.clickLink(A_PROPOSAL_ID);
      expect(feed.watched).toEqual([A_PROPOSAL_ID, A_PROPOSAL_ID]);
    });

    it('oferece "Ver com dados simulados" e leva ao modo demonstração', async () => {
      const navigated: string[] = [];
      const screen = await render(new FakeFeed(), (url) => navigated.push(url));

      await screen.clickButton('Ver com dados simulados');

      expect(navigated).toHaveLength(1);
      expect(navigated[0]).toContain('?demo');
    });

    it('não oferece "Ver com dados simulados" quando já está no modo demonstração', async () => {
      const screen = await render(new DemoAuctionFeed(5));

      const labels = Array.from(screen.page.querySelectorAll('button')).map((b) => b.textContent);
      expect(labels.join(' ')).not.toContain('Ver com dados simulados');
    });
  });

  describe('atalhos de observabilidade', () => {
    it('leva ao Trace e às Métricas do mesmo host', async () => {
      const screen = await render(new FakeFeed());

      const hrefs = (text: string) =>
        Array.from(screen.page.querySelectorAll('a'))
          .filter((a) => a.textContent?.includes(text))
          .map((a) => a.getAttribute('href'));
      expect(hrefs('Trace')).toEqual([`http://${window.location.hostname}:30686`]);
      expect(hrefs('Métricas')).toEqual([`http://${window.location.hostname}:30300`]);
    });

    it('esconde os atalhos no modo demonstração, onde não há cluster', async () => {
      const screen = await render(new DemoAuctionFeed(5));

      const links = Array.from(screen.page.querySelectorAll('a')).map((a) => a.textContent);
      expect(links.join(' ')).not.toContain('Trace');
      expect(links.join(' ')).not.toContain('Métricas');
    });
  });

  describe('estado de cada tira', () => {
    const rowOf = (page: HTMLElement, funder: string) =>
      Array.from(page.querySelectorAll('ol li')).find((li) => li.textContent?.includes(funder));

    it('marca como ultrapassada a tira que já liderou e perdeu a liderança', async () => {
      const feed = new FakeFeed();
      const screen = await render(feed);

      feed.bids.set([
        bid('funder-a', 2.5, 24, '2026-09-28T12:49:22.000000000Z'),
        bid('funder-b', 1.9, 18, '2026-09-28T12:49:24.000000000Z'),
      ]);
      await screen.settle();

      expect(rowOf(screen.page, 'funder-a')?.textContent).toContain('Ultrapassada');
      expect(rowOf(screen.page, 'funder-b')?.textContent).not.toContain('Ultrapassada');
    });

    it('chama de "Em disputa" a tira que nunca liderou', async () => {
      const feed = new FakeFeed();
      const screen = await render(feed);

      feed.bids.set([
        bid('funder-b', 1.9, 18, '2026-09-28T12:49:22.000000000Z'),
        bid('funder-a', 2.5, 24, '2026-09-28T12:49:24.000000000Z'),
      ]);
      await screen.settle();

      expect(rowOf(screen.page, 'funder-a')?.textContent).toContain('Em disputa');
      expect(rowOf(screen.page, 'funder-a')?.textContent).not.toContain('Ultrapassada');
    });
  });

  it('marca no histórico o último leilão acompanhado', async () => {
    const screen = await render(new FakeFeed());
    await screen.typeProposalId(A_PROPOSAL_ID);
    await screen.clickWatch();
    await screen.navigateTo('Histórico');

    expect(screen.page.querySelector('ul li')?.textContent).toContain('Último acompanhado');
  });
});
